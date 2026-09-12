package com.xiaoming.hunterwildcard.test;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.team.TeamManager;
import com.xiaoming.hunterwildcard.wildcard.rules.PortalRule;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;
import java.util.Set;

final class PortalSafetyAssertions {
    static void run(ServerPlayerEntity player) {
        var server = player.getEntityWorld().getServer();
        var nether = server.getWorld(World.NETHER);
        BlockPos feet = new BlockPos(160, 80, 160);
        nether.getChunk(feet);
        for (int y = 0; y < 128; y++) nether.setBlockState(new BlockPos(160, y, 160), Blocks.NETHERRACK.getDefaultState());
        nether.setBlockState(new BlockPos(160, 127, 160), Blocks.BEDROCK.getDefaultState());
        check(find(nether, feet) == null, "No cave space must not fall back to the Nether roof heightmap");
        nether.setBlockState(feet, Blocks.AIR.getDefaultState());
        nether.setBlockState(feet.up(), Blocks.AIR.getDefaultState());
        check(feet.equals(find(nether, feet)), "Nearby safe cave floor is accepted");
        check(find(nether, new BlockPos(160, 128, 160)) == null, "Roof origin cannot create a roof portal");
        nether.setBlockState(feet, Blocks.LAVA.getDefaultState());
        check(find(nether, feet) == null, "Lava is never a safe exit");
        nether.setBlockState(feet, Blocks.AIR.getDefaultState());
        nether.setBlockState(feet.down(), Blocks.MAGMA_BLOCK.getDefaultState());
        check(find(nether, feet) == null, "Magma floor is rejected");
        nether.setBlockState(feet.down(), Blocks.NETHERRACK.getDefaultState());
        nether.setBlockState(feet, Blocks.FIRE.getDefaultState());
        check(find(nether, feet) == null, "Fire is rejected despite its empty collision shape");

        var fromWorld = player.getEntityWorld();
        Vec3d from = player.getEntityPos();
        GameContext context = new GameContext(server, new ModConfig(), new TeamManager(), new Random(0), List.of(player));
        travel(context, player, nether, feet);
        check(player.getEntityWorld() == fromWorld && player.getEntityPos().equals(from), "Exit changed to fire: traversal leaves player in place");
        nether.setBlockState(feet, Blocks.AIR.getDefaultState());
        travel(context, player, nether, feet);
        check(player.getEntityWorld() == nether && player.getY() == 80, "Valid traversal arrives in the cave below the roof");
        check(player.getVelocity().equals(Vec3d.ZERO) && player.fallDistance == 0, "Traversal clears falling momentum");
    }

    private static BlockPos find(ServerWorld world, BlockPos pos) {
        try {
            var method = PortalRule.class.getDeclaredMethod("findStanding", ServerWorld.class, BlockPos.class);
            method.setAccessible(true);
            return (BlockPos) method.invoke(null, world, pos);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void travel(GameContext context, ServerPlayerEntity player, ServerWorld targetWorld, BlockPos pos) {
        try {
            Class<?> portal = Class.forName(PortalRule.class.getName() + "$Portal");
            Class<?> group = Class.forName(PortalRule.class.getName() + "$PortalGroup");
            var portalConstructor = portal.getDeclaredConstructor(RegistryKey.class, Vec3d.class);
            portalConstructor.setAccessible(true);
            Object entry = portalConstructor.newInstance(player.getEntityWorld().getRegistryKey(), player.getEntityPos());
            Object exit = portalConstructor.newInstance(targetWorld.getRegistryKey(), Vec3d.ofBottomCenter(pos));
            var groupConstructor = group.getDeclaredConstructor(List.class);
            groupConstructor.setAccessible(true);
            var method = PortalRule.class.getDeclaredMethod("travel", GameContext.class, ServerPlayerEntity.class, group, portal);
            method.setAccessible(true);
            method.invoke(new PortalRule(), context, player, groupConstructor.newInstance(List.of(entry, exit)), entry);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void check(boolean passed, String message) { if (!passed) throw new AssertionError(message); }
}
