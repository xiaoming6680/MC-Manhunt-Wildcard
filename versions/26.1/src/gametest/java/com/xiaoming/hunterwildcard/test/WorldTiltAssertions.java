package com.xiaoming.hunterwildcard.test;

import com.xiaoming.hunterwildcard.client.WorldTiltClient;
import com.xiaoming.hunterwildcard.wildcard.rules.WorldTiltPhysics;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

final class WorldTiltAssertions {
    private static void updateCamera(Camera camera, net.minecraft.client.Minecraft client, boolean third, boolean inverse) {
        var previous = client.options.getCameraType();
        client.options.setCameraType(!third ? net.minecraft.client.CameraType.FIRST_PERSON : inverse ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT : net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        camera.setLevel(client.level);
        camera.setEntity(client.player);
        camera.update(net.minecraft.client.DeltaTracker.ONE);
        client.options.setCameraType(previous);
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static void run(ClientGameTestContext c, TestSingleplayerContext world) {
        for (int[] direction : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            c.runOnClient(client -> {
                WorldTiltClient.reset();
                client.player.setYRot(37); client.player.setXRot(21);
                WorldTiltClient.set(true, 1, direction[0], direction[1]);
            });
            c.waitTicks(3);
            c.runOnClient(client -> {
                WorldTiltClient.handleMouse(client.player, 23, -17);
                var aim=client.player.getViewVector(1);
                Camera camera=new Camera();
                for (boolean third : new boolean[]{false,true}) for (boolean inverse : new boolean[]{false,true}) {
                    updateCamera(camera, client, third, inverse);
                    Vector3f expected=new Vector3f((float)aim.x,(float)aim.y,(float)aim.z);
                    if(third&&inverse)expected.negate();
                    check(camera.forwardVector().distance(expected)<.001,"Camera forward agrees with aiming in every perspective");
                }
                WorldTiltClient.set(false,1,direction[0],direction[1]);
            });
            c.waitTicks(3);
            c.runOnClient(client -> {
                check(!WorldTiltClient.isFrameActive(),"Camera returns to upright frame");
                check(Math.abs(client.player.getXRot()-WorldTiltClient.localPitch())<.001,"Exit preserves final local pitch without snapping: actual="+client.player.getXRot()+", local="+WorldTiltClient.localPitch());
                WorldTiltClient.reset();
            });
        }
        world.getServer().runOnServer(server -> {
            var player=server.getPlayerList().getPlayers().getFirst();
            var dimension=player.level();
            player.setPos(20,220.5,20.5);
            WorldTiltPhysics.set(player,true,1,1,0);
            for(int x=17;x<=21;x++)for(int y=219;y<=222;y++)for(int z=18;z<=26;z++)
                dimension.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            for(int x=18;x<=20;x++)for(int z=18;z<=25;z++) {
                dimension.setBlockAndUpdate(new BlockPos(x,219,z),Blocks.STONE.defaultBlockState());
                dimension.setBlockAndUpdate(new BlockPos(x,221,z),Blocks.STONE.defaultBlockState());
            }
            for(int z=18;z<=25;z++)dimension.setBlockAndUpdate(new BlockPos(20,220,z),Blocks.STONE.defaultBlockState());
            player.setPos(20,220.5,20.5);
            var box=player.getBoundingBox();
            check(Math.abs(box.getXsize()-player.getBbHeight())<.001&&Math.abs(box.getYsize()-player.getBbWidth())<.001,"Collision body rotates its long axis with gravity");
            check(dimension.noCollision(player,box.deflate(.001)),"Rotated body fits one-block-high world-space passage");
            player.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(.08,0,1));
            check(player.onGround()&&Math.abs(player.getX()-20)<.001&&Math.abs(player.getZ()-21.5)<.001,"Walk along wall through narrow passage without drifting feet");
            check(player.getBoundingBox().contains(player.getEyePosition()),"Eye position lies inside rotated body");
            com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.look(player,0,0);
            dimension.setBlockAndUpdate(new BlockPos(18,220,24),Blocks.STONE.defaultBlockState());
            var hit=player.pick(6,1,false);
            check(hit instanceof net.minecraft.world.phys.BlockHitResult block&&block.getBlockPos().equals(new BlockPos(18,220,24)),"Block aiming starts at rotated eyes and hits correct passage wall: hit="+hit.getLocation()+", type="+hit.getType()+", eye="+player.getEyePosition()+", camera="+player.getEyePosition(1)+", rotation="+player.getViewVector(1));
            player.jumpFromGround();
            check(player.getDeltaMovement().x<-.3,"Jump pushes away from gravity-facing wall");
            player.setPose(net.minecraft.world.entity.Pose.CROUCHING);player.refreshDimensions();
            check(player.getBoundingBox().getXsize()<1.6,"Crouching shrinks along rotated up axis");
            player.setPose(net.minecraft.world.entity.Pose.STANDING);player.refreshDimensions();
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            WorldTiltPhysics.set(player,false,1,0,0);
            check(!com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.active(player)&&dimension.noCollision(player,player.getBoundingBox().deflate(.001)),"Returning to normal gravity inside low passage uses a fitting pose without clipping");
            WorldTiltPhysics.clear();
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        });
        world.getServer().runOnServer(server -> {
            var player=server.getPlayerList().getPlayers().getFirst();
            player.setPos(0,200,0);player.setHealth(20);player.fallDistance=0;
            WorldTiltPhysics.set(player,true,1,1,0);
            for(int y=199;y<=203;y++)for(int z=-1;z<=1;z++)
                player.level().setBlockAndUpdate(new BlockPos(10,y,z),Blocks.STONE.defaultBlockState());
        });
        c.waitTicks(3);
        world.getServer().runOnServer(server -> {
            var player=server.getPlayerList().getPlayers().getFirst();
            player.setPos(0,200,0);player.setHealth(20);player.invulnerableTime=0;player.fallDistance=0;
            player.doCheckFallDamage(6,0,0,false);
            check(player.fallDistance>=5.99,"Sideways fall distance accumulates on the server");
            player.setPos(10,200,0);
            player.doCheckFallDamage(2,0,0,false);
            check(player.getHealth()<20,"Wall landing invokes vanilla fall damage without client ground flag");
            check(player.fallDistance==0,"Landing clears accumulated distance");
            float landed=player.getHealth();player.doCheckFallDamage(0,0,0,true);
            check(player.getHealth()==landed,"Standing on wall does not repeatedly damage");
            player.setHealth(20);player.invulnerableTime=0;player.fallDistance=0;
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,100));
            player.doCheckFallDamage(10,0,0,true);
            check(player.getHealth()==20&&player.fallDistance==0,"Slow falling retains native protection");
            player.removeEffect(MobEffects.SLOW_FALLING);
            for(int y=199;y<=203;y++)for(int z=-1;z<=1;z++)
                player.level().setBlockAndUpdate(new BlockPos(10,y,z),Blocks.HAY_BLOCK.defaultBlockState());
            player.setPos(0,200,0);player.doCheckFallDamage(6,0,0,false);
            player.setPos(10,200,0);player.doCheckFallDamage(2,0,0,false);
            check(player.getHealth()>landed && player.getHealth()<20,"Hay block applies native fall damage reduction");
            WorldTiltPhysics.clear();
            player.setHealth(20);player.invulnerableTime=0;player.fallDistance=0;
            player.setPos(0,200,0);player.doCheckFallDamage(0,-6,0,false);
            check(player.fallDistance>=5.99,"Normal vertical fall tracking remains intact after wildcard");
            player.fallDistance=0;player.randomTeleport(0,100,0,false);player.setNoGravity(false);
        });
        world.getServer().runOnServer(server -> {
            var player=server.getPlayerList().getPlayers().getFirst();
            for(int y=209;y<=218;y++)for(int z=60;z<=72;z++)
                player.level().setBlockAndUpdate(new BlockPos(60,y,z),Blocks.STONE_BRICKS.defaultBlockState());
            player.setPos(58,213,64);
            WorldTiltPhysics.set(player,true,1,1,0);
            com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.look(player,0,15);
            player.connection.teleport(60,213,64,player.getYRot(),player.getXRot());
            com.xiaoming.hunterwildcard.network.HunterWildcardPackets.sendWorldTilt(player,true,1,1,0);
        });
        c.waitTicks(20);
        c.runOnClient(client -> {
            check(com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.active(client.player),"Authoritative gravity metadata reaches client");
            check(Math.abs(client.player.getBoundingBox().getXsize()-client.player.getBbHeight())<.001,"Client and server collision dimensions agree");
            check(client.player.onGround(),"Real client remains grounded on wall after twenty movement ticks: pos="+client.player.position()+", velocity="+client.player.getDeltaMovement()+", pose="+client.player.getPose()+", support="+client.level.getBlockState(com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.support(client.player))+", flying="+client.player.getAbilities().flying);
            Camera camera=new Camera();updateCamera(camera, client, false, false);
            check(camera.position().distanceTo(client.player.getEyePosition())<.001,"Rendered camera origin matches interaction ray origin");
            try {
                var overlay=net.minecraft.client.renderer.ScreenEffectRenderer.class.getDeclaredMethod("getViewBlockingState",net.minecraft.world.entity.player.Player.class);
                overlay.setAccessible(true);
                check(overlay.invoke(null,client.player)==null,"Standing on a wall does not render a false in-block overlay");
            } catch(ReflectiveOperationException e){throw new AssertionError(e);}
            client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        });
        c.takeScreenshot("world-tilt-collision-first-person");
        c.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
        c.waitTicks(3);c.takeScreenshot("world-tilt-collision-third-person");
        c.getInput().holdKey(options->options.keyUp);c.waitTicks(10);
        c.getInput().releaseKey(options->options.keyUp);
        c.runOnClient(client->check(client.player.getZ()>64.8 && Math.abs(client.player.getX()-60)<.01,"Forward input walks along the wall while feet stay grounded"));
        c.getInput().holdKey(options->options.keyJump);c.waitTicks(3);
        c.getInput().releaseKey(options->options.keyJump);
        c.runOnClient(client->check(client.player.getX()<59.8,"Real client jump moves away from gravity floor: pos="+client.player.position()+", velocity="+client.player.getDeltaMovement()+", ground="+client.player.onGround()));
        c.waitTicks(20);
        c.runOnClient(client->check(client.player.onGround()&&Math.abs(client.player.getX()-60)<.01,"Real client lands on wall after jumping"));
        world.getServer().runOnServer(server->{
            var player=server.getPlayerList().getPlayers().getFirst();
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);player.setHealth(20);player.invulnerableTime=0;player.fallDistance=0;
            player.connection.teleport(48,213,66,player.getYRot(),player.getXRot());
        });
        c.waitTicks(8);
        world.getServer().runOnServer(server->{
            var player=server.getPlayerList().getPlayers().getFirst();
            check(player.getX()>48&&player.getX()<59,"Sideways descent is synchronized before landing");
            try {
                var floating=net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("clientIsFloating");
                floating.setAccessible(true);
                check(!floating.getBoolean(player.connection),"Sideways descent does not trigger native flight detection");
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        });
        c.waitTicks(25);
        world.getServer().runOnServer(server->{
            var player=server.getPlayerList().getPlayers().getFirst();
            check(player.onGround()&&player.getHealth()<20,"Actual synchronized sideways fall lands and applies native damage: health="+player.getHealth()+", pos="+player.position());
        });
        world.getServer().runOnServer(server -> WorldTiltPhysics.stop(1));c.waitTicks(8);
        c.runOnClient(client -> {
            check(!WorldTiltClient.isFrameActive(),"Ending wildcard restores upright client collision frame");
            client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        });
        world.getServer().runOnServer(server -> {
            var player=server.getPlayerList().getPlayers().getFirst();
            int y=player.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,0,0);
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);player.fallDistance=0;player.setHealth(20);
            player.connection.teleport(.5,y,.5,0,0);
        });
    }
}
