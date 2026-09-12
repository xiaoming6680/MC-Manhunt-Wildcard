package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/**
 * Thirty seconds after the draw gravity turns sideways: vertical gravity is switched off for every participant
 * and a horizontal pull in a random compass direction takes its place, on the server and on each client
 * (the client also rolls its camera so the new "down" points down on screen). The action bar counts down
 * so everyone can get somewhere safe first.
 */
public class WorldTiltRule implements WildcardRule {
    public static final double GRAVITY_PER_TICK = 0.08;
    public static final double TERMINAL_SPEED = 3.0;
    private static final int TILT_DELAY_TICKS = 30 * 20;
    private static final int TRANSITION_TICKS = 60;
    private static final int RESYNC_INTERVAL_TICKS = 100;

    private int ticks;
    private boolean tilted;
    private double dirX;
    private double dirZ;

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        tilted = false;
        double angle = context.getRandom().nextInt(4) * Math.PI / 2.0;
        dirX = Math.round(Math.cos(angle));
        dirZ = Math.round(Math.sin(angle));
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        if (tilted) {
            for (ServerPlayerEntity player : context.getParticipants()) {
                applyGravity(player, dirX, dirZ);
                if (ticks % RESYNC_INTERVAL_TICKS == 0) {
                    HunterWildcardPackets.sendWorldTilt(player, true, 1, dirX, dirZ);
                }
            }
            return;
        }

        int remaining = TILT_DELAY_TICKS - ticks;
        if (remaining > 0) {
            if (remaining % 20 == 0) {
                int seconds = remaining / 20;
                for (ServerPlayerEntity player : context.getParticipants()) {
                    player.sendMessage(HunterWildcardText.translatable("msg.wildcard.world_tilt.countdown", seconds)
                            .formatted(seconds <= 5 ? Formatting.RED : Formatting.YELLOW), true);
                    if (seconds <= 5) {
                        player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.6F);
                    }
                }
            }
            return;
        }

        tilted = true;
        for (ServerPlayerEntity player : context.getParticipants()) {
            HunterWildcardPackets.sendWorldTilt(player, true, TRANSITION_TICKS, dirX, dirZ);
            player.sendMessage(HunterWildcardText.translatable("msg.wildcard.world_tilt.tilted").formatted(Formatting.RED), true);
            player.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 0.6F);
            player.playSound(SoundEvents.BLOCK_ANVIL_LAND, 0.8F, 0.5F);
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            player.setNoGravity(false);
            player.fallDistance = 0.0F;
            HunterWildcardPackets.sendWorldTilt(player, false, TRANSITION_TICKS, dirX, dirZ);
        }
        tilted = false;
    }

    /** Shared by server and client: no vertical gravity, a constant sideways pull, capped at terminal speed. */
    public static void applyGravity(net.minecraft.entity.Entity entity, double dirX, double dirZ) {
        if (!entity.hasNoGravity()) {
            entity.setNoGravity(true);
        }
        entity.fallDistance = 0.0F;
        Vec3d velocity = entity.getVelocity();
        double along = velocity.x * dirX + velocity.z * dirZ;
        if (along < TERMINAL_SPEED) {
            entity.setVelocity(velocity.add(dirX * GRAVITY_PER_TICK, 0.0, dirZ * GRAVITY_PER_TICK));
        }
    }
}
