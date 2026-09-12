package com.xiaoming.hunterwildcard.test;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.team.TeamManager;
import com.xiaoming.hunterwildcard.wildcard.rules.PortalRule;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class PortalSafetyAssertions {
    static void run(ServerPlayer player) {
        var server = player.level().getServer();
        var nether = server.getLevel(Level.NETHER);
        BlockPos feet = new BlockPos(160, 80, 160);
        nether.getChunk(feet);
        for (int y = 0; y < 128; y++) nether.setBlockAndUpdate(new BlockPos(160, y, 160), Blocks.NETHERRACK.defaultBlockState());
        nether.setBlockAndUpdate(new BlockPos(160, 127, 160), Blocks.BEDROCK.defaultBlockState());
        check(find(nether, feet) == null, "No cave space must not fall back to the Nether roof heightmap");
        nether.setBlockAndUpdate(feet, Blocks.AIR.defaultBlockState());
        nether.setBlockAndUpdate(feet.above(), Blocks.AIR.defaultBlockState());
        check(feet.equals(find(nether, feet)), "Nearby safe cave floor is accepted");
        check(find(nether, new BlockPos(160, 128, 160)) == null, "Roof origin cannot create a roof portal");
        nether.setBlockAndUpdate(feet, Blocks.LAVA.defaultBlockState());
        check(find(nether, feet) == null, "Lava is never a safe exit");
        nether.setBlockAndUpdate(feet, Blocks.AIR.defaultBlockState());
        nether.setBlockAndUpdate(feet.below(), Blocks.MAGMA_BLOCK.defaultBlockState());
        check(find(nether, feet) == null, "Magma floor is rejected");
        nether.setBlockAndUpdate(feet.below(), Blocks.NETHERRACK.defaultBlockState());
        nether.setBlockAndUpdate(feet, Blocks.FIRE.defaultBlockState());
        check(find(nether, feet) == null, "Fire is rejected despite its empty collision shape");

        var fromWorld = player.level();
        Vec3 from = player.position();
        GameContext context = new GameContext(server, new ModConfig(), new TeamManager(), new Random(0), List.of(player));
        travel(context, player, nether, feet);
        check(player.level() == fromWorld && player.position().equals(from), "Exit changed to fire: traversal leaves player in place");
        nether.setBlockAndUpdate(feet, Blocks.AIR.defaultBlockState());
        travel(context, player, nether, feet);
        check(player.level() == nether && player.getY() == 80, "Valid traversal arrives in the cave below the roof");
        check(player.getDeltaMovement().equals(Vec3.ZERO) && player.fallDistance == 0, "Traversal clears falling momentum");
    }

    private static BlockPos find(ServerLevel world, BlockPos pos) {
        try {
            var method = PortalRule.class.getDeclaredMethod("findStanding", ServerLevel.class, BlockPos.class);
            method.setAccessible(true);
            return (BlockPos) method.invoke(null, world, pos);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void travel(GameContext context, ServerPlayer player, ServerLevel targetWorld, BlockPos pos) {
        try {
            Class<?> portal = Class.forName(PortalRule.class.getName() + "$Portal");
            Class<?> group = Class.forName(PortalRule.class.getName() + "$PortalGroup");
            var portalConstructor = portal.getDeclaredConstructor(ResourceKey.class, Vec3.class);
            portalConstructor.setAccessible(true);
            Object entry = portalConstructor.newInstance(player.level().dimension(), player.position());
            Object exit = portalConstructor.newInstance(targetWorld.dimension(), Vec3.atBottomCenterOf(pos));
            var groupConstructor = group.getDeclaredConstructor(List.class);
            groupConstructor.setAccessible(true);
            var method = PortalRule.class.getDeclaredMethod("travel", GameContext.class, ServerPlayer.class, group, portal);
            method.setAccessible(true);
            method.invoke(new PortalRule(), context, player, groupConstructor.newInstance(List.of(entry, exit)), entry);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void check(boolean passed, String message) { if (!passed) throw new AssertionError(message); }
}
