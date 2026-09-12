package com.xiaoming.hunterwildcard.wildcard.rules;

import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Tracks ownership only. Physics and collision run in the same replicated cardinal frame. */
public final class WorldTiltPhysics {
    private static final Map<ServerPlayer,Direction> FRAMES=new IdentityHashMap<>();
    private static final Set<ServerPlayer> RETURNING=Collections.newSetFromMap(new IdentityHashMap<>());
    private WorldTiltPhysics() {}
    public static void set(ServerPlayer player,boolean active,int ticks,double x,double z) {
        if(active)RETURNING.remove(player);else RETURNING.add(player);
        Direction target=active?Direction.getApproximateNearest(x,0,z):Direction.DOWN;
        TiltFrame.change(player,target);
        if(TiltFrame.direction(player)==target) {
            if(active)FRAMES.put(player,target);else {FRAMES.remove(player);RETURNING.remove(player);}
        }
    }
    public static Vec3 gravity(ServerPlayer player) { return TiltFrame.active(player)?TiltFrame.down(player):null; }
    public static void tick(MinecraftServer server) {
        for(var player:List.copyOf(RETURNING)) {
            if(player.isRemoved()||!player.isAlive()){RETURNING.remove(player);continue;}
            set(player,false,1,0,0);
            if(!TiltFrame.active(player))com.xiaoming.hunterwildcard.network.HunterWildcardPackets.sendWorldTilt(player,false,1,0,0);
        }
        for(var player:List.copyOf(FRAMES.keySet()))if(player.isRemoved()||!player.isAlive()) {
            ((TiltTrackedGravity)player).hunterwildcard$gravity(Direction.DOWN);FRAMES.remove(player);
        }
    }
    public static void stop(int ticks) {
        for(var player:List.copyOf(FRAMES.keySet())) {
            set(player,false,ticks,0,0);
            if(!TiltFrame.active(player))com.xiaoming.hunterwildcard.network.HunterWildcardPackets.sendWorldTilt(player,false,ticks,0,0);
        }
    }
    public static void clear() {
        for(var player:List.copyOf(FRAMES.keySet())) {
            ((TiltTrackedGravity)player).hunterwildcard$gravity(Direction.DOWN);
            player.setBoundingBox(TiltFrame.box(player,player.getDimensions(player.getPose()),player.position()));
        }
        FRAMES.clear();RETURNING.clear();
    }
}
