package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

/**
 * After a thirty-second warning, participants switch to one exact cardinal gravity frame.
 * Camera, collision, movement and landing share that frame on the client and server.
 */
public class WorldTiltRule implements WildcardRule {
    private static final int TILT_DELAY_TICKS = 30 * 20;
    private static final int TRANSITION_TICKS = 1;
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
                if (WorldTiltPhysics.gravity(player) == null) WorldTiltPhysics.set(player, true, 1, dirX, dirZ);
                if (WorldTiltPhysics.gravity(player) != null && ticks % RESYNC_INTERVAL_TICKS == 0) {
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
            WorldTiltPhysics.set(player, true, TRANSITION_TICKS, dirX, dirZ);
            if(WorldTiltPhysics.gravity(player) != null)HunterWildcardPackets.sendWorldTilt(player, true, TRANSITION_TICKS, dirX, dirZ);
            player.sendMessage(HunterWildcardText.translatable("msg.wildcard.world_tilt.tilted").formatted(Formatting.RED), true);
            player.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 0.6F);
            player.playSound(SoundEvents.BLOCK_ANVIL_LAND, 0.8F, 0.5F);
        }
    }

    @Override
    public void onStop(GameContext context) {
        WorldTiltPhysics.stop(TRANSITION_TICKS);
        tilted = false;
    }

}
