package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Every time a participant takes damage, their movement keys get shuffled on the client.
 * The client owns the actual key bindings; the server only tells it when to shuffle and when to restore.
 */
public class KeyScrambleRule implements WildcardRule {
    private static final int SHUFFLE_COOLDOWN_TICKS = 15;
    private static final int RESYNC_INTERVAL_TICKS = 100;

    private final Map<UUID, Integer> lastShuffleTick = new HashMap<>();
    private int ticks;

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        lastShuffleTick.clear();
        for (ServerPlayer player : context.getParticipants()) {
            HunterWildcardPackets.sendKeyScramble(player, HunterWildcardPackets.KeyScrambleAction.ENABLE);
        }
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        if (ticks % RESYNC_INTERVAL_TICKS != 0) {
            return;
        }

        // Keeps the HUD alive for players who respawned or reconnected mid-wildcard. ENABLE never shuffles.
        for (ServerPlayer player : context.getParticipants()) {
            HunterWildcardPackets.sendKeyScramble(player, HunterWildcardPackets.KeyScrambleAction.ENABLE);
        }
    }

    @Override
    public void onPlayerDamaged(GameContext context, ServerPlayer player, DamageSource source, float damageTaken) {
        UUID playerId = player.getUUID();
        Integer last = lastShuffleTick.get(playerId);
        if (last != null && ticks - last < SHUFFLE_COOLDOWN_TICKS) {
            return;
        }

        lastShuffleTick.put(playerId, ticks);
        HunterWildcardPackets.sendKeyScramble(player, HunterWildcardPackets.KeyScrambleAction.SHUFFLE);
        player.sendSystemMessage(HunterWildcardText.translatable("msg.wildcard.key_scramble.shuffled"), true);
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayer player : context.getParticipants()) {
            HunterWildcardPackets.sendKeyScramble(player, HunterWildcardPackets.KeyScrambleAction.RESTORE);
        }
        lastShuffleTick.clear();
    }
}
