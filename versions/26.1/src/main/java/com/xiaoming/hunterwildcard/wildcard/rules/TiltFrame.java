package com.xiaoming.hunterwildcard.wildcard.rules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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
    public static Vec3 down(Entity entity) { return vector(direction(entity)); }
    public static Vec3 vector(Direction d) { return new Vec3(d.getStepX(),d.getStepY(),d.getStepZ()); }
    public static Vec3 world(Direction d, Vec3 v) {
        return switch(d) {
            case DOWN -> v;
            case UP -> new Vec3(v.x,-v.y,-v.z);
            case EAST -> new Vec3(-v.y,v.x,v.z);
            case WEST -> new Vec3(v.y,-v.x,v.z);
            case SOUTH -> new Vec3(v.x,v.z,-v.y);
            case NORTH -> new Vec3(v.x,-v.z,v.y);
        };
    }
    public static Vec3 local(Direction d, Vec3 v) {
        return world(switch(d) {case EAST -> Direction.WEST; case WEST -> Direction.EAST;
            case NORTH -> Direction.SOUTH; case SOUTH -> Direction.NORTH; default -> d;},v);
    }
    public static float[] localAngles(Entity entity) {
        if(entity==null)return new float[]{0,0};
        if(!active(entity))return new float[]{entity.getYRot(),entity.getXRot()};
        double yaw=Math.toRadians(entity.getYRot()),pitch=Math.toRadians(entity.getXRot());
        Vec3 look=local(direction(entity),new Vec3(-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch)));
        return new float[]{(float)Math.toDegrees(Math.atan2(-look.x,look.z)),(float)-Math.toDegrees(Math.asin(Math.clamp(look.y,-1,1)))};
    }
    public static void look(Entity entity,float yaw,float pitch) {
        double yr=Math.toRadians(yaw),pr=Math.toRadians(pitch);
        Vec3 look=world(direction(entity),new Vec3(-Math.sin(yr)*Math.cos(pr),-Math.sin(pr),Math.cos(yr)*Math.cos(pr)));
        entity.setYRot(entity.getYRot()+Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(-look.x,look.z))-entity.getYRot()));
        entity.setXRot((float)-Math.toDegrees(Math.asin(Math.clamp(look.y,-1,1))));
        entity.yRotO=entity.getYRot();entity.xRotO=entity.getXRot();
        if(entity instanceof net.minecraft.world.entity.LivingEntity living) {living.setYHeadRot(entity.getYRot());living.yHeadRotO=entity.getYRot();}
    }
    public static Quaternionf rotation(Entity entity) {
        Vec3 d=down(entity);
        return new Quaternionf().rotationTo(new Vector3f(0,-1,0),new Vector3f((float)d.x,(float)d.y,(float)d.z));
    }
    public static AABB rotateBox(Direction d, AABB box, boolean inverse) {
        Vec3 a=new Vec3(box.minX,box.minY,box.minZ), b=new Vec3(box.maxX,box.maxY,box.maxZ);
        return new AABB(inverse?local(d,a):world(d,a), inverse?local(d,b):world(d,b));
    }
    public static AABB box(Entity entity, EntityDimensions size, Vec3 feet) {
        double half=size.width()/2.0;
        return rotateBox(direction(entity),new AABB(-half,0,-half,half,size.height(),half),false).move(feet);
    }
    public static Vec3 eye(Entity entity, Vec3 feet) {
        return feet.add(world(direction(entity),new Vec3(0,entity.getEyeHeight(),0)));
    }
    public static BlockPos support(Entity entity) {
        return supportingBlock(entity).orElseGet(()->BlockPos.containing(entity.position().add(down(entity).scale(.05))));
    }
    public static java.util.Optional<BlockPos> supportingBlock(Entity entity) {
        double half=entity.getBbWidth()/2.0;
        AABB feet=rotateBox(direction(entity),new AABB(-half,-1.0E-6,-half,half,0,half),false).move(entity.position());
        return entity.level().findSupportingBlock(entity,feet);
    }
    public static boolean grounded(Entity entity) {
        Vec3 probe=down(entity).scale(.05);
        return Entity.collideBoundingBox(entity,probe,entity.getBoundingBox(),entity.level(),List.of())
                .dot(down(entity)) < .049;
    }
    public static void change(Entity entity, Direction direction) {
        if (!(entity instanceof TiltTrackedGravity tracked) || direction(entity)==direction) return;
        AABB oldBox=entity.getBoundingBox();
        Vec3 center=oldBox.getCenter();
        float[] angles=localAngles(entity);
        Vec3 feet=center.add(vector(direction).scale(entity.getBbHeight()/2.0));
        Direction old=direction(entity);
        tracked.hunterwildcard$gravity(direction);
        AABB candidate=box(entity,entity.getDimensions(entity.getPose()),feet);
        // Keep the body center; resolve small rotation overlaps without moving through a wall.
        if (!entity.level().noCollision(entity,candidate)) {
            boolean found=false;
            for(double distance=.1;distance<=2.01&&!found;distance+=.1) {
                for(Direction escape:Direction.values()) {
                    Vec3 offset=vector(escape).scale(distance);
                    if (entity.level().noCollision(entity,candidate.move(offset))
                            && entity.level().noCollision(entity,oldBox.expandTowards(offset).deflate(.001))) {
                        feet=feet.add(offset); found=true; break;
                    }
                }
            }
            if(!found && direction==Direction.DOWN) {
                var small=entity.getDimensions(Pose.SWIMMING);
                Vec3 smallFeet=center.add(vector(direction).scale(small.height()/2));
                if(entity.level().noCollision(entity,box(entity,small,smallFeet))) {
                    entity.setPose(Pose.SWIMMING);entity.refreshDimensions();feet=smallFeet;found=true;
                }
            }
            if(!found) { tracked.hunterwildcard$gravity(old); return; }
        }
        entity.setPos(feet);
        look(entity,angles[0],angles[1]);
        entity.setOnGround(false);
        if(entity instanceof net.minecraft.server.level.ServerPlayer player)
            player.connection.teleport(feet.x,feet.y,feet.z,player.getYRot(),player.getXRot());
    }
    public static Vec3 collide(Entity entity, Vec3 movement) {
        Direction d=direction(entity);
        Vec3 origin=entity.position();
        double step=entity.maxUpStep();
        AABB box=entity.getBoundingBox();
        AABB search=box.expandTowards(movement).inflate(step+.001);
        List<VoxelShape> shapes=new ArrayList<>();
        List<VoxelShape> worldShapes=new ArrayList<>();
        entity.level().getCollisions(entity,search).forEach(worldShapes::add);
        if(entity.level().getWorldBorder().isInsideCloseToBorder(entity,search))worldShapes.add(entity.level().getWorldBorder().getCollisionShape());
        for(VoxelShape shape:worldShapes)
            for(AABB part:shape.toAabbs()) shapes.add(Shapes.create(rotateBox(d,part.move(origin.reverse()),true)));
        AABB localBox=rotateBox(d,box.move(origin.reverse()),true);
        Vec3 requested=local(d,movement), result=clip(localBox,requested,shapes);
        if(step>0 && (entity.onGround() || requested.y<0&&result.y>requested.y)
                && (result.x!=requested.x || result.z!=requested.z)) {
            Vec3 rise=clip(localBox,new Vec3(0,step,0),shapes);
            Vec3 across=clip(localBox.move(rise),new Vec3(requested.x,0,requested.z),shapes);
            Vec3 settle=clip(localBox.move(rise).move(across),new Vec3(0,requested.y-rise.y,0),shapes);
            if(across.horizontalDistanceSqr()>result.horizontalDistanceSqr()) result=rise.add(across).add(settle);
        }
        return world(d,result);
    }
    private static Vec3 clip(AABB box, Vec3 v, List<VoxelShape> shapes) {
        double y=Shapes.collide(Direction.Axis.Y,box,shapes,v.y);box=box.move(0,y,0);
        boolean zFirst=Math.abs(v.x)<Math.abs(v.z);
        double x=v.x,z=v.z;
        if(zFirst) {z=Shapes.collide(Direction.Axis.Z,box,shapes,z);box=box.move(0,0,z);}
        x=Shapes.collide(Direction.Axis.X,box,shapes,x);box=box.move(x,0,0);
        if(!zFirst) z=Shapes.collide(Direction.Axis.Z,box,shapes,z);
        return new Vec3(x,y,z);
    }
}
