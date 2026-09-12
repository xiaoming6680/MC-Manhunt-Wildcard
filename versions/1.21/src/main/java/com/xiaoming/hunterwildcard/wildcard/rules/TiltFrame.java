package com.xiaoming.hunterwildcard.wildcard.rules;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/** Exact axis permutations: block collision shapes remain exact, even for stairs and narrow gaps. */
public final class TiltFrame {
    private TiltFrame() {}
    public static Direction direction(Entity entity) {
        return entity instanceof TiltTrackedGravity tracked ? tracked.hunterwildcard$gravity() : Direction.DOWN;
    }
    public static boolean active(Entity entity) { return direction(entity) != Direction.DOWN; }
    public static Vec3d down(Entity entity) { return vector(direction(entity)); }
    public static Vec3d vector(Direction d) { return new Vec3d(d.getOffsetX(),d.getOffsetY(),d.getOffsetZ()); }
    public static Vec3d world(Direction d, Vec3d v) {
        return switch(d) {
            case DOWN -> v;
            case UP -> new Vec3d(v.x,-v.y,-v.z);
            case EAST -> new Vec3d(-v.y,v.x,v.z);
            case WEST -> new Vec3d(v.y,-v.x,v.z);
            case SOUTH -> new Vec3d(v.x,v.z,-v.y);
            case NORTH -> new Vec3d(v.x,-v.z,v.y);
        };
    }
    public static Vec3d local(Direction d, Vec3d v) {
        return world(switch(d) {case EAST -> Direction.WEST; case WEST -> Direction.EAST;
            case NORTH -> Direction.SOUTH; case SOUTH -> Direction.NORTH; default -> d;},v);
    }
    public static float[] localAngles(Entity entity) {
        if(entity==null)return new float[]{0,0};
        if(!active(entity))return new float[]{entity.getYaw(),entity.getPitch()};
        double yaw=Math.toRadians(entity.getYaw()),pitch=Math.toRadians(entity.getPitch());
        Vec3d look=local(direction(entity),new Vec3d(-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch)));
        return new float[]{(float)Math.toDegrees(Math.atan2(-look.x,look.z)),(float)-Math.toDegrees(Math.asin(Math.clamp(look.y,-1,1)))};
    }
    public static void look(Entity entity,float yaw,float pitch) {
        double yr=Math.toRadians(yaw),pr=Math.toRadians(pitch);
        Vec3d look=world(direction(entity),new Vec3d(-Math.sin(yr)*Math.cos(pr),-Math.sin(pr),Math.cos(yr)*Math.cos(pr)));
        entity.setYaw(entity.getYaw()+MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(-look.x,look.z))-entity.getYaw()));
        entity.setPitch((float)-Math.toDegrees(Math.asin(Math.clamp(look.y,-1,1))));
        entity.prevYaw=entity.getYaw();entity.prevPitch=entity.getPitch();
        if(entity instanceof net.minecraft.entity.LivingEntity living) {living.setHeadYaw(entity.getYaw());living.prevHeadYaw=entity.getYaw();}
    }
    public static Quaternionf rotation(Entity entity) {
        Vec3d d=down(entity);
        return new Quaternionf().rotationTo(new Vector3f(0,-1,0),new Vector3f((float)d.x,(float)d.y,(float)d.z));
    }
    public static Box rotateBox(Direction d, Box box, boolean inverse) {
        Vec3d a=new Vec3d(box.minX,box.minY,box.minZ), b=new Vec3d(box.maxX,box.maxY,box.maxZ);
        return new Box(inverse?local(d,a):world(d,a), inverse?local(d,b):world(d,b));
    }
    public static Box box(Entity entity, EntityDimensions size, Vec3d feet) {
        double half=size.width()/2.0;
        return rotateBox(direction(entity),new Box(-half,0,-half,half,size.height(),half),false).offset(feet);
    }
    public static Vec3d eye(Entity entity, Vec3d feet) {
        return feet.add(world(direction(entity),new Vec3d(0,entity.getStandingEyeHeight(),0)));
    }
    public static BlockPos support(Entity entity) {
        return supportingBlock(entity).orElseGet(()->BlockPos.ofFloored(entity.getPos().add(down(entity).multiply(.05))));
    }
    public static java.util.Optional<BlockPos> supportingBlock(Entity entity) {
        double half=entity.getWidth()/2.0;
        Box feet=rotateBox(direction(entity),new Box(-half,-1.0E-6,-half,half,0,half),false).offset(entity.getPos());
        return entity.getEntityWorld().findSupportingBlockPos(entity,feet);
    }
    public static boolean grounded(Entity entity) {
        Vec3d probe=down(entity).multiply(.05);
        return Entity.adjustMovementForCollisions(entity,probe,entity.getBoundingBox(),entity.getEntityWorld(),List.of())
                .dotProduct(down(entity)) < .049;
    }
    public static void change(Entity entity, Direction direction) {
        if (!(entity instanceof TiltTrackedGravity tracked) || direction(entity)==direction) return;
        Box oldBox=entity.getBoundingBox();
        Vec3d center=oldBox.getCenter();
        float[] angles=localAngles(entity);
        Vec3d feet=center.add(vector(direction).multiply(entity.getHeight()/2.0));
        Direction old=direction(entity);
        tracked.hunterwildcard$gravity(direction);
        Box candidate=box(entity,entity.getDimensions(entity.getPose()),feet);
        // Keep the body center; resolve small rotation overlaps without moving through a wall.
        if (!entity.getEntityWorld().isSpaceEmpty(entity,candidate)) {
            boolean found=false;
            for(double distance=.1;distance<=2.01&&!found;distance+=.1) {
                for(Direction escape:Direction.values()) {
                    Vec3d offset=vector(escape).multiply(distance);
                    if (entity.getEntityWorld().isSpaceEmpty(entity,candidate.offset(offset))
                            && entity.getEntityWorld().isSpaceEmpty(entity,oldBox.stretch(offset).contract(.001))) {
                        feet=feet.add(offset); found=true; break;
                    }
                }
            }
            if(!found && direction==Direction.DOWN) {
                var small=entity.getDimensions(EntityPose.SWIMMING);
                Vec3d smallFeet=center.add(vector(direction).multiply(small.height()/2));
                if(entity.getEntityWorld().isSpaceEmpty(entity,box(entity,small,smallFeet))) {
                    entity.setPose(EntityPose.SWIMMING);entity.calculateDimensions();feet=smallFeet;found=true;
                }
            }
            if(!found) { tracked.hunterwildcard$gravity(old); return; }
        }
        entity.setPosition(feet);
        look(entity,angles[0],angles[1]);
        entity.setOnGround(false);
        if(entity instanceof net.minecraft.server.network.ServerPlayerEntity player)
            player.networkHandler.requestTeleport(feet.x,feet.y,feet.z,player.getYaw(),player.getPitch());
    }
    public static Vec3d collide(Entity entity, Vec3d movement) {
        Direction d=direction(entity);
        Vec3d origin=entity.getPos();
        double step=entity.getStepHeight();
        Box box=entity.getBoundingBox();
        Box search=box.stretch(movement).expand(step+.001);
        List<VoxelShape> shapes=new ArrayList<>();
        List<VoxelShape> worldShapes=new ArrayList<>();
        entity.getEntityWorld().getCollisions(entity,search).forEach(worldShapes::add);
        if(entity.getEntityWorld().getWorldBorder().canCollide(entity,search))worldShapes.add(entity.getEntityWorld().getWorldBorder().asVoxelShape());
        for(VoxelShape shape:worldShapes)
            for(Box part:shape.getBoundingBoxes()) shapes.add(VoxelShapes.cuboid(rotateBox(d,part.offset(origin.negate()),true)));
        Box localBox=rotateBox(d,box.offset(origin.negate()),true);
        Vec3d requested=local(d,movement), result=clip(localBox,requested,shapes);
        if(step>0 && (entity.isOnGround() || requested.y<0&&result.y>requested.y)
                && (result.x!=requested.x || result.z!=requested.z)) {
            Vec3d rise=clip(localBox,new Vec3d(0,step,0),shapes);
            Vec3d across=clip(localBox.offset(rise),new Vec3d(requested.x,0,requested.z),shapes);
            Vec3d settle=clip(localBox.offset(rise).offset(across),new Vec3d(0,requested.y-rise.y,0),shapes);
            if(across.horizontalLengthSquared()>result.horizontalLengthSquared()) result=rise.add(across).add(settle);
        }
        return world(d,result);
    }
    private static Vec3d clip(Box box, Vec3d v, List<VoxelShape> shapes) {
        double y=VoxelShapes.calculateMaxOffset(Direction.Axis.Y,box,shapes,v.y);box=box.offset(0,y,0);
        boolean zFirst=Math.abs(v.x)<Math.abs(v.z);
        double x=v.x,z=v.z;
        if(zFirst) {z=VoxelShapes.calculateMaxOffset(Direction.Axis.Z,box,shapes,z);box=box.offset(0,0,z);}
        x=VoxelShapes.calculateMaxOffset(Direction.Axis.X,box,shapes,x);box=box.offset(x,0,0);
        if(!zFirst) z=VoxelShapes.calculateMaxOffset(Direction.Axis.Z,box,shapes,z);
        return new Vec3d(x,y,z);
    }
}
