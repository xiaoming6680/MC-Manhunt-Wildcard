package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Every N seconds (configurable) all participants in the same dimension are dealt new positions at random
 * (possibly their own). The action bar counts down the whole interval; the last three seconds also beep.
 */
public class SpaceShiftRule implements WildcardRule {
    private static final int COUNTDOWN_SECONDS = 3;

    private int ticks;

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        int interval = Math.max(COUNTDOWN_SECONDS * 20 + 20, context.getConfig().getSpaceShiftIntervalTicks());
        int untilShift = interval - (ticks % interval);
        if (untilShift % 20 == 0 && untilShift / 20 >= 1) {
            countdown(context, untilShift / 20);
        }
        if (ticks % interval == 0) {
            shift(context);
        }
    }

    private void countdown(GameContext context, int seconds) {
        for (ServerPlayer player : context.getParticipants()) {
            player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.space_shift.countdown", seconds).withStyle(seconds <= COUNTDOWN_SECONDS ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE), true);
            if (seconds <= COUNTDOWN_SECONDS) {
                player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, seconds == 1 ? 1.6F : 1.0F);
            }
        }
    }

    private void shift(GameContext context) {
        Map<ServerLevel, List<ServerPlayer>> byWorld = new LinkedHashMap<>();
        for (ServerPlayer player : context.getParticipants()) {
            if (player.isAlive() && player.level() instanceof ServerLevel world) {
                byWorld.computeIfAbsent(world, ignored -> new ArrayList<>()).add(player);
            }
        }

        boolean shifted = false;
        for (Map.Entry<ServerLevel, List<ServerPlayer>> entry : byWorld.entrySet()) {
            List<ServerPlayer> players = entry.getValue();
            if (players.size() < 2) {
                continue;
            }
            shifted = true;
            ServerLevel world = entry.getKey();
            List<Vec3> positions = new ArrayList<>(players.size());
            for (ServerPlayer player : players) {
                positions.add(player.position());
            }
            // A plain shuffle: you may well draw your own spot and stay put, which is part of the gamble.
            Collections.shuffle(positions, context.getRandom());
            for (int i = 0; i < players.size(); i++) {
                ServerPlayer player = players.get(i);
                Vec3 destination = positions.get(i);
                Vec3 origin = positions.get(i);
                player.teleportTo(world, destination.x, destination.y, destination.z, Set.of(), player.getYRot(), player.getXRot(), false);
                player.fallDistance = 0.0F;
                world.playSound(null, origin.x, origin.y, origin.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.7F);
                world.sendParticles(ParticleTypes.PORTAL, origin.x, origin.y + 1.0, origin.z, 50, 0.4, 0.8, 0.4, 0.6);
            }
        }

        String key = shifted ? "msg.wildcard.space_shift.shifted" : "msg.wildcard.space_shift.nobody";
        for (ServerPlayer player : context.getParticipants()) {
            player.sendSystemMessage(HunterWildcardText.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }
}
