package com.xiaoming.hunterwildcard.client;

import com.xiaoming.hunterwildcard.wildcard.rules.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import org.joml.Quaternionf;

/** Camera and mouse use the same exact frame as server collision and aiming. */
public final class WorldTiltClient {
    private WorldTiltClient() {}
    public static void register() { ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->reset()); }
    public static void set(boolean active,int transitionTicks,double x,double z) {
        var player=MinecraftClient.getInstance().player;
        if(player==null)return;
        // Position and rotation are authoritative teleport data; metadata also updates remote players.
        ((TiltTrackedGravity)player).hunterwildcard$gravity(active?Direction.getFacing(x,0,z):Direction.DOWN);
        player.setBoundingBox(TiltFrame.box(player,player.getDimensions(player.getPose()),player.getPos()));
    }
    public static void reset() {
        var player=MinecraftClient.getInstance().player;
        if(player!=null) {
            ((TiltTrackedGravity)player).hunterwildcard$gravity(Direction.DOWN);
            player.setBoundingBox(TiltFrame.box(player,player.getDimensions(player.getPose()),player.getPos()));
        }
    }
    public static boolean isLocalPlayer(Entity entity) { return entity!=null&&entity==MinecraftClient.getInstance().player; }
    public static boolean isFrameActive() { var player=MinecraftClient.getInstance().player;return player!=null&&TiltFrame.active(player); }
    public static float currentBlend() { return isFrameActive()?1:0; }
    public static Vec3d currentGravity() { var player=MinecraftClient.getInstance().player;return player==null?new Vec3d(0,-1,0):TiltFrame.down(player); }
    public static Quaternionf frame() { var player=MinecraftClient.getInstance().player;return player==null?new Quaternionf():TiltFrame.rotation(player); }
    public static float localYaw() { return TiltFrame.localAngles(MinecraftClient.getInstance().player)[0]; }
    public static float localPitch() { return TiltFrame.localAngles(MinecraftClient.getInstance().player)[1]; }
    public static void handleMouse(Entity player,double dx,double dy) {
        float[] angles=TiltFrame.localAngles(player);
        TiltFrame.look(player,angles[0]+(float)dx*.15F,MathHelper.clamp(angles[1]+(float)dy*.15F,-90,90));
    }
    public static Quaternionf cameraRotation(boolean inverse) {
        float yaw=localYaw()+(inverse?180:0),pitch=localPitch()*(inverse?-1:1);
        return frame().rotateYXZ((float)Math.PI-yaw*((float)Math.PI/180),-pitch*((float)Math.PI/180),0);
    }
    public static Vec3d toWorld(Vec3d local) { return TiltFrame.world(TiltFrame.direction(MinecraftClient.getInstance().player),local); }
}
