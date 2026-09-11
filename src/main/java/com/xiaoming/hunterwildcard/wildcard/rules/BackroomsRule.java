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
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "Backrooms!": everyone drops into the yellow maze. Hunters glow red for a second every twenty
 * seconds so runners know where the danger is; hunters keep their compass. Finding a hole in the
 * floor sends you home; once a whole side is out, the wildcard ends and everyone sinks back.
 */
public class BackroomsRule implements WildcardRule {
    private static final String HUNTER_TEAM = "hw_backrooms_hunters";
    private static final int GLOW_INTERVAL_TICKS = 20 * 20;
    private static final int GLOW_TICKS = 20;
    private static final int PLACED_BLOCK_TTL_TICKS = 20 * 10;
    private static final int SIDE_CHECK_INTERVAL_TICKS = 20;

    private final List<PlacedBlock> placedBlocks = new ArrayList<>();
    private int ticks;

    @Override
    public int getDurationTicks(ModConfig config) {
        return config.getBackroomsDurationTicks();
    }

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        placedBlocks.clear();
        Scoreboard scoreboard = context.getServer().getScoreboard();
        Team team = scoreboard.getTeam(HUNTER_TEAM);
        if (team == null) {
            team = scoreboard.addTeam(HUNTER_TEAM);
        }
        team.setColor(Formatting.RED);
        for (ServerPlayerEntity hunter : context.getHunters()) {
            scoreboard.addScoreHolderToTeam(hunter.getNameForScoreboard(), team);
        }
        BackroomsSession.begin(context);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        if (ticks % GLOW_INTERVAL_TICKS == 0) {
            for (ServerPlayerEntity hunter : context.getHunters()) {
                if (BackroomsDimension.isInBackrooms(hunter)) {
                    hunter.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, GLOW_TICKS, 0, false, false, false));
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
        Scoreboard scoreboard = context.getServer().getScoreboard();
        Team team = scoreboard.getTeam(HUNTER_TEAM);
        if (team != null) {
            scoreboard.removeTeam(team);
        }
        BackroomsSession.end(context);
    }

    private record PlacedBlock(RegistryKey<World> worldKey, BlockPos pos, BlockState state, int expireTick) {
    }
}
