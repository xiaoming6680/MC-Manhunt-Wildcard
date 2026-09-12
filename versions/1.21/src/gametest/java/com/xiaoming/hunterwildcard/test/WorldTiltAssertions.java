package com.xiaoming.hunterwildcard.test;

import com.xiaoming.hunterwildcard.client.WorldTiltClient;
import com.xiaoming.hunterwildcard.wildcard.rules.WorldTiltPhysics;
import com.xiaoming.hunterwildcard.test.harness.*;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

final class WorldTiltAssertions {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static void run(ClientGameTestContext c, TestSingleplayerContext world) {
        for (int[] direction : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            c.runOnClient(client -> {
                WorldTiltClient.reset();
                client.player.setYaw(37); client.player.setPitch(21);
                WorldTiltClient.set(true, 1, direction[0], direction[1]);
            });
            c.waitTicks(3);
            c.runOnClient(client -> {
                WorldTiltClient.handleMouse(client.player, 23, -17);
                var aim=client.player.getRotationVec(1);
                Camera camera=new Camera();
                for (boolean third : new boolean[]{false,true}) for (boolean inverse : new boolean[]{false,true}) {
                    camera.update(client.world,client.player,third,inverse,1);
                    Vector3f expected=new Vector3f((float)aim.x,(float)aim.y,(float)aim.z);
                    if(third&&inverse)expected.negate();
                    check(camera.getHorizontalPlane().distance(expected)<.001,"Camera forward agrees with aiming in every perspective");
                }
                WorldTiltClient.set(false,1,direction[0],direction[1]);
            });
            c.waitTicks(3);
            c.runOnClient(client -> {
                check(!WorldTiltClient.isFrameActive(),"Camera returns to upright frame");
                check(Math.abs(client.player.getPitch()-WorldTiltClient.localPitch())<.001,"Exit preserves final local pitch without snapping: actual="+client.player.getPitch()+", local="+WorldTiltClient.localPitch());
                WorldTiltClient.reset();
            });
        }
        world.getServer().runOnServer(server -> {
            var player=server.getPlayerManager().getPlayerList().getFirst();
            var dimension=player.getServerWorld();
            player.setPosition(20,220.5,20.5);
            WorldTiltPhysics.set(player,true,1,1,0);
            for(int x=17;x<=21;x++)for(int y=219;y<=222;y++)for(int z=18;z<=26;z++)
                dimension.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
            for(int x=18;x<=20;x++)for(int z=18;z<=25;z++) {
                dimension.setBlockState(new BlockPos(x,219,z),Blocks.STONE.getDefaultState());
                dimension.setBlockState(new BlockPos(x,221,z),Blocks.STONE.getDefaultState());
            }
            for(int z=18;z<=25;z++)dimension.setBlockState(new BlockPos(20,220,z),Blocks.STONE.getDefaultState());
            player.setPosition(20,220.5,20.5);
            var box=player.getBoundingBox();
            check(Math.abs(box.getLengthX()-player.getHeight())<.001&&Math.abs(box.getLengthY()-player.getWidth())<.001,"Collision body rotates its long axis with gravity");
            check(dimension.isSpaceEmpty(player,box.contract(.001)),"Rotated body fits one-block-high world-space passage");
            player.move(net.minecraft.entity.MovementType.SELF,new net.minecraft.util.math.Vec3d(.08,0,1));
            check(player.isOnGround()&&Math.abs(player.getX()-20)<.001&&Math.abs(player.getZ()-21.5)<.001,"Walk along wall through narrow passage without drifting feet");
            check(player.getBoundingBox().contains(player.getEyePos()),"Eye position lies inside rotated body");
            com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.look(player,0,0);
            dimension.setBlockState(new BlockPos(18,220,24),Blocks.STONE.getDefaultState());
            var hit=player.raycast(6,1,false);
            check(hit instanceof net.minecraft.util.hit.BlockHitResult block&&block.getBlockPos().equals(new BlockPos(18,220,24)),"Block aiming starts at rotated eyes and hits correct passage wall: hit="+hit.getPos()+", type="+hit.getType()+", eye="+player.getEyePos()+", camera="+player.getCameraPosVec(1)+", rotation="+player.getRotationVec(1));
            player.jump();
            check(player.getVelocity().x<-.3,"Jump pushes away from gravity-facing wall");
            player.setPose(net.minecraft.entity.EntityPose.CROUCHING);player.calculateDimensions();
            check(player.getBoundingBox().getLengthX()<1.6,"Crouching shrinks along rotated up axis");
            player.setPose(net.minecraft.entity.EntityPose.STANDING);player.calculateDimensions();
            player.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
            WorldTiltPhysics.set(player,false,1,0,0);
            check(!com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.active(player)&&dimension.isSpaceEmpty(player,player.getBoundingBox().contract(.001)),"Returning to normal gravity inside low passage uses a fitting pose without clipping");
            WorldTiltPhysics.clear();
            player.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
        });
        world.getServer().runOnServer(server -> {
            var player=server.getPlayerManager().getPlayerList().getFirst();
            player.setPosition(0,200,0);player.setHealth(20);player.fallDistance=0;
            WorldTiltPhysics.set(player,true,1,1,0);
            for(int y=199;y<=203;y++)for(int z=-1;z<=1;z++)
                player.getServerWorld().setBlockState(new BlockPos(10,y,z),Blocks.STONE.getDefaultState());
        });
        c.waitTicks(3);
        world.getServer().runOnServer(server -> {
            var player=server.getPlayerManager().getPlayerList().getFirst();
            player.setPosition(0,200,0);player.setHealth(20);player.timeUntilRegen=0;player.fallDistance=0;
            player.handleFall(6,0,0,false);
            check(player.fallDistance>=5.99,"Sideways fall distance accumulates on the server");
            player.setPosition(10,200,0);
            player.handleFall(2,0,0,false);
            check(player.getHealth()<20,"Wall landing invokes vanilla fall damage without client ground flag");
            check(player.fallDistance==0,"Landing clears accumulated distance");
            float landed=player.getHealth();player.handleFall(0,0,0,true);
            check(player.getHealth()==landed,"Standing on wall does not repeatedly damage");
            player.setHealth(20);player.timeUntilRegen=0;player.fallDistance=0;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING,100));
            player.handleFall(10,0,0,true);
            check(player.getHealth()==20&&player.fallDistance==0,"Slow falling retains native protection");
            player.removeStatusEffect(StatusEffects.SLOW_FALLING);
            for(int y=199;y<=203;y++)for(int z=-1;z<=1;z++)
                player.getServerWorld().setBlockState(new BlockPos(10,y,z),Blocks.HAY_BLOCK.getDefaultState());
            player.setPosition(0,200,0);player.handleFall(6,0,0,false);
            player.setPosition(10,200,0);player.handleFall(2,0,0,false);
            check(player.getHealth()>landed && player.getHealth()<20,"Hay block applies native fall damage reduction");
            WorldTiltPhysics.clear();
            player.setHealth(20);player.timeUntilRegen=0;player.fallDistance=0;
            player.setPosition(0,200,0);player.handleFall(0,-6,0,false);
            check(player.fallDistance>=5.99,"Normal vertical fall tracking remains intact after wildcard");
            player.fallDistance=0;player.teleport(0,100,0,false);player.setNoGravity(false);
        });
        world.getServer().runOnServer(server -> {
            var player=server.getPlayerManager().getPlayerList().getFirst();
            for(int y=209;y<=218;y++)for(int z=60;z<=72;z++)
                player.getServerWorld().setBlockState(new BlockPos(60,y,z),Blocks.STONE_BRICKS.getDefaultState());
            player.setPosition(58,213,64);
            WorldTiltPhysics.set(player,true,1,1,0);
            com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.look(player,0,15);
            player.networkHandler.requestTeleport(60,213,64,player.getYaw(),player.getPitch());
            com.xiaoming.hunterwildcard.network.HunterWildcardPackets.sendWorldTilt(player,true,1,1,0);
        });
        c.waitTicks(20);
        c.runOnClient(client -> {
            check(com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.active(client.player),"Authoritative gravity metadata reaches client");
            check(Math.abs(client.player.getBoundingBox().getLengthX()-client.player.getHeight())<.001,"Client and server collision dimensions agree");
            check(client.player.isOnGround(),"Real client remains grounded on wall after twenty movement ticks: pos="+client.player.getPos()+", velocity="+client.player.getVelocity()+", pose="+client.player.getPose()+", support="+client.world.getBlockState(com.xiaoming.hunterwildcard.wildcard.rules.TiltFrame.support(client.player))+", flying="+client.player.getAbilities().flying);
            Camera camera=new Camera();camera.update(client.world,client.player,false,false,1);
            check(camera.getPos().distanceTo(client.player.getEyePos())<.001,"Rendered camera origin matches interaction ray origin");
            try {
                var overlay=net.minecraft.client.gui.hud.InGameOverlayRenderer.class.getDeclaredMethod("getInWallBlockState",net.minecraft.entity.player.PlayerEntity.class);
                overlay.setAccessible(true);
                check(overlay.invoke(null,client.player)==null,"Standing on a wall does not render a false in-block overlay");
            } catch(ReflectiveOperationException e){throw new AssertionError(e);}
            client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);
        });
        c.takeScreenshot("world-tilt-collision-first-person");
        c.runOnClient(client -> client.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK));
        c.waitTicks(3);c.takeScreenshot("world-tilt-collision-third-person");
        c.getInput().holdKey(options->options.forwardKey);c.waitTicks(10);
        c.getInput().releaseKey(options->options.forwardKey);
        c.runOnClient(client->check(client.player.getZ()>64.8 && Math.abs(client.player.getX()-60)<.01,"Forward input walks along the wall while feet stay grounded"));
        c.getInput().holdKey(options->options.jumpKey);c.waitTicks(3);
        c.getInput().releaseKey(options->options.jumpKey);
        c.runOnClient(client->check(client.player.getX()<59.8,"Real client jump moves away from gravity floor: pos="+client.player.getPos()+", velocity="+client.player.getVelocity()+", ground="+client.player.isOnGround()));
        c.waitTicks(20);
        c.runOnClient(client->check(client.player.isOnGround()&&Math.abs(client.player.getX()-60)<.01,"Real client lands on wall after jumping"));
        world.getServer().runOnServer(server->{
            var player=server.getPlayerManager().getPlayerList().getFirst();
            player.setVelocity(net.minecraft.util.math.Vec3d.ZERO);player.setHealth(20);player.timeUntilRegen=0;player.fallDistance=0;
            player.networkHandler.requestTeleport(48,213,66,player.getYaw(),player.getPitch());
        });
        c.waitTicks(8);
        world.getServer().runOnServer(server->{
            var player=server.getPlayerManager().getPlayerList().getFirst();
            check(player.getX()>48&&player.getX()<59,"Sideways descent is synchronized before landing");
            try {
                var floating=net.minecraft.server.network.ServerPlayNetworkHandler.class.getDeclaredField("floating");
                floating.setAccessible(true);
                check(!floating.getBoolean(player.networkHandler),"Sideways descent does not trigger native flight detection");
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        });
        c.waitTicks(25);
        world.getServer().runOnServer(server->{
            var player=server.getPlayerManager().getPlayerList().getFirst();
            check(player.isOnGround()&&player.getHealth()<20,"Actual synchronized sideways fall lands and applies native damage: health="+player.getHealth()+", pos="+player.getPos());
        });
        world.getServer().runOnServer(server -> WorldTiltPhysics.stop(1));c.waitTicks(8);
        c.runOnClient(client -> {
            check(!WorldTiltClient.isFrameActive(),"Ending wildcard restores upright client collision frame");
            client.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);
        });
        world.getServer().runOnServer(server -> {
            var player=server.getPlayerManager().getPlayerList().getFirst();
            int y=player.getServerWorld().getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,0,0);
            player.setVelocity(net.minecraft.util.math.Vec3d.ZERO);player.fallDistance=0;player.setHealth(20);
            player.networkHandler.requestTeleport(.5,y,.5,0,0);
        });
    }
}
