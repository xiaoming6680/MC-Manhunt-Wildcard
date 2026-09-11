package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.backrooms.BackroomsDimension;
import com.xiaoming.hunterwildcard.backrooms.BackroomsSession;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * "Backrooms!": everyone drops into the yellow maze. Hunters glow red for a second every twenty
 * seconds so runners know where the danger is; hunters keep their compass. Finding a hole in the
 * floor sends you home; once a whole side is out, the wildcard ends and everyone sinks back.
 */
public class BackroomsRule implements WildcardRule {
    private static final int GLOW_INTERVAL_TICKS = 20 * 20;
    private static final int GLOW_TICKS = 100;
    private static final int PLACED_BLOCK_TTL_TICKS = 20 * 10;
    private static final int SIDE_CHECK_INTERVAL_TICKS = 20;

    private final List<PlacedBlock> placedBlocks = new ArrayList<>();
    private int ticks;

    @Override
    public int getDurationTicks(ModConfig config, Random random) {
        return config.getBackroomsDurationTicks();
    }

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        placedBlocks.clear();
        // Glow colour comes from the round-wide scoreboard teams: hunters red, runners blue.
        BackroomsSession.begin(context);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        if (ticks % GLOW_INTERVAL_TICKS == 0) {
            for (ServerPlayerEntity participant : context.getParticipants()) {
                if (BackroomsDimension.isInBackrooms(participant)) {
                    participant.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, GLOW_TICKS, 0, false, false, false));
                    participant.sendMessage(HunterWildcardText.translatable("msg.wildcard.backrooms.glow", GLOW_TICKS / 20).formatted(Formatting.YELLOW), true);
                    participant.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 0.8F, 1.5F);
                }
            }
        }

        if (!placedBlocks.isEmpty()) {
            Iterator<PlacedBlock> iterator = placedBlocks.iterator();
            while (iterator.hasNext()) {
                PlacedBlock placed = iterator.next();
                if (ticks < placed.expireTick) {
                    continue;
                }
                ServerWorld world = context.getServer().getWorld(placed.worldKey);
                if (world != null && world.getBlockState(placed.pos).equals(placed.state)) {
                    world.removeBlock(placed.pos, false);
                }
                iterator.remove();
            }
        }

        if (ticks % SIDE_CHECK_INTERVAL_TICKS == 0 && BackroomsSession.isActive() && ticks > BackroomsSession.ENTRY_TELEPORT_AT + 20) {
            if (BackroomsSession.sideFullyOut(context, PlayerRole.HUNTER) || BackroomsSession.sideFullyOut(context, PlayerRole.RUNNER)) {
                context.getServer().getPlayerManager().broadcast(
                        HunterWildcardText.prefixed(HunterWildcardText.translatable("msg.wildcard.backrooms.side_out")), false);
                GameManager.getInstance().requestWildcardStop();
                ticks = 0; // stop is applied after this tick; don't spam the message meanwhile
            }
        }
    }

    @Override
    public void onBlockPlaced(GameContext context, ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
        if (BackroomsDimension.isBackrooms(world)) {
            placedBlocks.add(new PlacedBlock(world.getRegistryKey(), pos.toImmutable(), state, ticks + PLACED_BLOCK_TTL_TICKS));
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (PlacedBlock placed : placedBlocks) {
            ServerWorld world = context.getServer().getWorld(placed.worldKey);
            if (world != null && world.getBlockState(placed.pos).equals(placed.state)) {
                world.removeBlock(placed.pos, false);
            }
        }
        placedBlocks.clear();
        BackroomsSession.end(context);
    }

    private record PlacedBlock(RegistryKey<World> worldKey, BlockPos pos, BlockState state, int expireTick) {
    }
}
