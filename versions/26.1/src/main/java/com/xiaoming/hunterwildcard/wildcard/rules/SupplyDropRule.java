package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SupplyDropRule implements WildcardRule {
    private static final int SCATTER_RADIUS = 6;
    private static final int MAX_CHESTS = 4;
    private static final int ANNOUNCE_LEAD_TICKS = 20 * 20;
    private static final int DROP_HEIGHT = 44;
    private static final int MAX_FALL_TICKS = 220;
    private static final int LANDING_BEAM_TICKS = 20 * 20;
    private static final int BEAM_HEIGHT = 42;
    private static final int MIN_USEFUL_STACKS = 4;
    private static final int EXTRA_USEFUL_STACKS = 3;
    private static final int MIN_JUNK_STACKS = 1;
    private static final int EXTRA_JUNK_STACKS = 3;
    private static final List<ActiveDrop> ACTIVE_DROPS = new ArrayList<>();
    private static final Random LOOT_RANDOM = new Random();
    private static final List<LootEntry> RARE_GEAR_POOL = List.of(
            new LootEntry(Items.DIAMOND_SWORD, 1, 1),
            new LootEntry(Items.DIAMOND_AXE, 1, 1),
            new LootEntry(Items.DIAMOND_PICKAXE, 1, 1),
            new LootEntry(Items.DIAMOND_SPEAR, 1, 1),
            new LootEntry(Items.MACE, 1, 1),
            new LootEntry(Items.DIAMOND_HELMET, 1, 1),
            new LootEntry(Items.DIAMOND_CHESTPLATE, 1, 1),
            new LootEntry(Items.DIAMOND_LEGGINGS, 1, 1),
            new LootEntry(Items.DIAMOND_BOOTS, 1, 1)
    );
    private static final List<LootEntry> USEFUL_LOOT_POOL = List.of(
            new LootEntry(Items.COOKED_BEEF, 5, 10),
            new LootEntry(Items.COOKED_PORKCHOP, 5, 10),
            new LootEntry(Items.COOKED_CHICKEN, 5, 10),
            new LootEntry(Items.GOLDEN_CARROT, 3, 6),
            new LootEntry(Items.GOLDEN_APPLE, 1, 2),
            new LootEntry(Items.ENCHANTED_GOLDEN_APPLE, 1, 1),
            new LootEntry(Items.ARROW, 16, 40),
            new LootEntry(Items.SPECTRAL_ARROW, 6, 14),
            new LootEntry(Items.IRON_INGOT, 4, 10),
            new LootEntry(Items.DIAMOND, 1, 2),
            new LootEntry(Items.IRON_SWORD, 1, 1),
            new LootEntry(Items.IRON_AXE, 1, 1),
            new LootEntry(Items.IRON_CHESTPLATE, 1, 1),
            new LootEntry(Items.SHIELD, 1, 1),
            new LootEntry(Items.BOW, 1, 1),
            new LootEntry(Items.CROSSBOW, 1, 1),
            new LootEntry(Items.WATER_BUCKET, 1, 1),
            new LootEntry(Items.LAVA_BUCKET, 1, 1),
            new LootEntry(Items.MILK_BUCKET, 1, 1),
            new LootEntry(Items.HONEY_BOTTLE, 2, 5),
            new LootEntry(Items.TORCH, 12, 32),
            new LootEntry(Items.OAK_PLANKS, 12, 32),
            new LootEntry(Items.COBBLESTONE, 12, 32),
            new LootEntry(Items.WIND_CHARGE, 2, 5),
            new LootEntry(Items.ENDER_PEARL, 1, 3),
            new LootEntry(Items.EXPERIENCE_BOTTLE, 4, 10),
            new LootEntry(Items.FIREWORK_ROCKET, 4, 10)
    );
    private static final List<LootEntry> JUNK_LOOT_POOL = List.of(
            new LootEntry(Items.ROTTEN_FLESH, 3, 8),
            new LootEntry(Items.POISONOUS_POTATO, 2, 6),
            new LootEntry(Items.SPIDER_EYE, 2, 5),
            new LootEntry(Items.STICK, 8, 20),
            new LootEntry(Items.BOWL, 2, 6),
            new LootEntry(Items.GRAVEL, 12, 32),
            new LootEntry(Items.DIRT, 12, 32),
            new LootEntry(Items.STRING, 4, 10),
            new LootEntry(Items.BONE, 4, 10),
            new LootEntry(Items.COBWEB, 2, 5)
    );

    private final List<ScheduledDrop> scheduled = new ArrayList<>();
    private int ticks;

    public static void tickTrackedDrops(MinecraftServer server) {
        if (ACTIVE_DROPS.isEmpty()) {
            return;
        }

        Iterator<ActiveDrop> iterator = ACTIVE_DROPS.iterator();
        while (iterator.hasNext()) {
            ActiveDrop drop = iterator.next();
            drop.age++;
            ServerLevel world = server.getLevel(drop.worldKey);
            if (world == null) {
                iterator.remove();
                continue;
            }

            if (drop.chestPos != null) {
                if (tickLandedDrop(world, drop)) {
                    iterator.remove();
                }
                continue;
            }

            Entity entity = world.getEntityInAnyDimension(drop.fallingEntityUuid);
            if (entity != null && !entity.isRemoved() && drop.age <= MAX_FALL_TICKS) {
                spawnFallingTrail(world, entity, drop);
                continue;
            }

            BlockPos chestPos = findLandedChest(world, drop.landingPos);
            if (chestPos == null && drop.age > MAX_FALL_TICKS) {
                if (entity != null && !entity.isRemoved()) {
                    entity.discard();
                }
                chestPos = forcePlaceChest(world, drop.landingPos);
            }
            if (chestPos != null && world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                fillChest(chest, LOOT_RANDOM);
                drop.chestPos = chestPos;
                drop.beamActive = true;
                drop.beamExpireAge = drop.age + LANDING_BEAM_TICKS;
                playLandingEffects(world, chestPos);
            }
        }
    }

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        scheduled.clear();
        announceSupplyDrop(context);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        if (remainingTicks > 0 && remainingTicks % context.getConfig().getSupplyDropIntervalTicks() == 0) {
            announceSupplyDrop(context);
        }

        if (scheduled.isEmpty()) {
            return;
        }
        Iterator<ScheduledDrop> iterator = scheduled.iterator();
        while (iterator.hasNext()) {
            ScheduledDrop drop = iterator.next();
            ServerLevel world = context.getServer().getLevel(drop.worldKey());
            if (world == null) {
                iterator.remove();
                continue;
            }
            if (ticks >= drop.spawnTick()) {
                iterator.remove();
                spawnScheduledDrop(context, world, drop);
                continue;
            }
            if (ticks % 10 == 0) {
                for (DropPoint point : drop.points()) {
                    spawnGroundMarker(world, point.landingPos());
                }
            }
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (ScheduledDrop drop : scheduled) {
            ServerLevel world = context.getServer().getLevel(drop.worldKey());
            if (world != null) {
                for (BeaconMarker marker : drop.markers()) {
                    restoreBeaconMarker(world, marker);
                }
            }
        }
        scheduled.clear();
    }

    /**
     * Picks the dimension holding the most participants, aims at the midpoint between the runners' and the
     * hunters' centres there, and tells everyone where the chests will land 20 seconds ahead of time.
     */
    private void announceSupplyDrop(GameContext context) {
        ServerLevel world = pickWorld(context);
        if (world == null) {
            return;
        }
        Vec3 center = midpoint(context, world);
        if (center == null) {
            return;
        }

        int chestCount = Math.max(1, Math.min(MAX_CHESTS, context.getRunners().size()));
        List<DropPoint> points = findDropPositions(world, BlockPos.containing(center), chestCount, context.getRandom());
        if (points.isEmpty()) {
            return;
        }
        List<BeaconMarker> markers = new ArrayList<>();
        for (DropPoint point : points) {
            markers.add(createBeaconMarker(world, point.landingPos()));
        }
        scheduled.add(new ScheduledDrop(world.dimension(), points, markers, ticks + ANNOUNCE_LEAD_TICKS));

        BlockPos first = points.get(0).landingPos();
        Component message = HunterWildcardText.prefixed(HunterWildcardText.translatable(
                "msg.wildcard.supply_drop.incoming",
                ANNOUNCE_LEAD_TICKS / 20,
                first.getX(), first.getY(), first.getZ(),
                points.size()
        ).withStyle(ChatFormatting.GOLD));
        for (ServerPlayer player : context.getParticipants()) {
            player.sendSystemMessage(message, false);
            player.playSound(SoundEvents.BELL_BLOCK, 1.0F, 1.2F);
        }
        for (DropPoint point : points) {
            playDropStartEffects(world, point.landingPos());
        }
    }

    private void spawnScheduledDrop(GameContext context, ServerLevel world, ScheduledDrop drop) {
        int spawned = 0;
        BlockPos first = null;
        for (int i = 0; i < drop.points().size(); i++) {
            DropPoint point = drop.points().get(i);
            BeaconMarker marker = drop.markers().get(i);
            if (!world.getBlockState(point.spawnPos()).isAir()) {
                restoreBeaconMarker(world, marker);
                continue;
            }
            BlockState chestState = Blocks.CHEST.defaultBlockState();
            if (!world.setBlock(point.spawnPos(), chestState, Block.UPDATE_ALL)) {
                restoreBeaconMarker(world, marker);
                continue;
            }
            FallingBlockEntity fallingChest = FallingBlockEntity.fall(world, point.spawnPos(), chestState);
            fallingChest.dropItem = false;
            fallingChest.setGlowingTag(true);
            fallingChest.setDeltaMovement(0.0, -0.18, 0.0);
            ACTIVE_DROPS.add(new ActiveDrop(world.dimension(), point.landingPos(), point.spawnPos(), fallingChest.getUUID(), marker));
            playDropStartEffects(world, point.landingPos());
            spawned++;
            if (first == null) {
                first = point.landingPos();
            }
        }
        if (spawned == 0) {
            return;
        }

        Component message = HunterWildcardText.prefixed(HunterWildcardText.translatable(
                "msg.wildcard.supply_drop.dropping", first.getX(), first.getY(), first.getZ(), spawned
        ).withStyle(ChatFormatting.GOLD));
        for (ServerPlayer player : context.getParticipants()) {
            player.sendSystemMessage(message, false);
        }
    }

    private static ServerLevel pickWorld(GameContext context) {
        Map<ServerLevel, Integer> counts = new LinkedHashMap<>();
        for (ServerPlayer player : context.getParticipants()) {
            if (player.isAlive() && player.level() instanceof ServerLevel world) {
                counts.merge(world, 1, Integer::sum);
            }
        }
        ServerLevel best = null;
        int bestCount = 0;
        for (Map.Entry<ServerLevel, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }

    private static Vec3 midpoint(GameContext context, ServerLevel world) {
        Vec3 runners = centroid(context.getRunners(), world);
        Vec3 hunters = centroid(context.getHunters(), world);
        if (runners != null && hunters != null) {
            return runners.add(hunters).scale(0.5);
        }
        if (runners != null) {
            return runners;
        }
        if (hunters != null) {
            return hunters;
        }
        return centroid(context.getParticipants(), world);
    }

    private static Vec3 centroid(List<ServerPlayer> players, ServerLevel world) {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        int count = 0;
        for (ServerPlayer player : players) {
            if (player.isAlive() && player.level() == world) {
                x += player.getX();
                y += player.getY();
                z += player.getZ();
                count++;
            }
        }
        return count == 0 ? null : new Vec3(x / count, y / count, z / count);
    }

    private static List<DropPoint> findDropPositions(ServerLevel world, BlockPos center, int count, Random random) {
        List<DropPoint> points = new ArrayList<>();
        int maxY = world.dimensionType().minY() + world.dimensionType().height() - 2;
        for (int attempt = 0; attempt < 40 && points.size() < count; attempt++) {
            int dx = attempt == 0 ? 0 : random.nextInt(SCATTER_RADIUS * 2 + 1) - SCATTER_RADIUS;
            int dz = attempt == 0 ? 0 : random.nextInt(SCATTER_RADIUS * 2 + 1) - SCATTER_RADIUS;
            BlockPos searchPos = center.offset(dx, 0, dz);
            BlockPos landingPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, searchPos);
            int spawnY = Math.min(landingPos.getY() + DROP_HEIGHT, maxY);
            if (spawnY <= landingPos.getY() + 8) {
                continue;
            }
            BlockPos spawnPos = new BlockPos(landingPos.getX(), spawnY, landingPos.getZ());
            if (!world.getBlockState(landingPos).isAir() || !world.getBlockState(landingPos.above()).isAir() || !world.getBlockState(spawnPos).isAir()) {
                continue;
            }
            boolean tooClose = false;
            for (DropPoint existing : points) {
                if (existing.landingPos().distManhattan(landingPos) < 2) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                points.add(new DropPoint(landingPos.immutable(), spawnPos.immutable()));
            }
        }
        return points;
    }

    private static boolean tickLandedDrop(ServerLevel world, ActiveDrop drop) {
        if (!(world.getBlockEntity(drop.chestPos) instanceof ChestBlockEntity chest)) {
            restoreBeaconMarker(world, drop);
            return true;
        }

        boolean openedNow = !chest.getEntitiesWithContainerOpen().isEmpty();
        if (openedNow && !drop.opened) {
            drop.opened = true;
            drop.beamActive = false;
            playBeamStopEffects(world, drop.chestPos, true);
            restoreBeaconMarker(world, drop);
        }
        if (!drop.opened && drop.age >= drop.beamExpireAge) {
            removeUnopenedChest(world, drop);
            return true;
        }

        if (drop.beamActive) {
            spawnBeaconBeam(world, drop.chestPos);
        }

        if (!chest.isEmpty()) {
            if (drop.age % 10 == 0) {
                spawnGroundMarker(world, drop.chestPos);
            }
            return false;
        }

        world.sendParticles(ParticleTypes.CLOUD, drop.chestPos.getX() + 0.5, drop.chestPos.getY() + 0.8, drop.chestPos.getZ() + 0.5, 16, 0.25, 0.2, 0.25, 0.02);
        world.sendParticles(ParticleTypes.END_ROD, drop.chestPos.getX() + 0.5, drop.chestPos.getY() + 0.9, drop.chestPos.getZ() + 0.5, 8, 0.2, 0.25, 0.2, 0.01);
        world.playSound(null, drop.chestPos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.8F, 1.3F);
        world.removeBlock(drop.chestPos, false);
        restoreBeaconMarker(world, drop);
        return true;
    }

    private static BlockPos findLandedChest(ServerLevel world, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -2, -1), center.offset(1, 3, 1))) {
            if (world.getBlockEntity(pos) instanceof ChestBlockEntity) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static BlockPos forcePlaceChest(ServerLevel world, BlockPos landingPos) {
        BlockPos placePos = landingPos;
        if (!world.getBlockState(placePos).isAir()) {
            placePos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, landingPos);
        }
        if (!world.getBlockState(placePos).isAir()) {
            return null;
        }
        world.setBlock(placePos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        return world.getBlockEntity(placePos) instanceof ChestBlockEntity ? placePos.immutable() : null;
    }

    private static void fillChest(ChestBlockEntity chest, Random random) {
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, ItemStack.EMPTY);
        }

        List<LootEntry> gear = new ArrayList<>(RARE_GEAR_POOL);
        Collections.shuffle(gear, random);
        int gearCount = Math.min(gear.size(), randomRareGearCount(random));
        for (int i = 0; i < gearCount; i++) {
            if (!placeRandomStack(chest, gear.get(i), random)) {
                return;
            }
        }

        List<LootEntry> loot = new ArrayList<>(USEFUL_LOOT_POOL);
        Collections.shuffle(loot, random);
        int stackCount = Math.min(loot.size(), MIN_USEFUL_STACKS + random.nextInt(EXTRA_USEFUL_STACKS + 1));
        for (int i = 0; i < stackCount; i++) {
            if (!placeRandomStack(chest, loot.get(i), random)) {
                return;
            }
        }

        List<LootEntry> junk = new ArrayList<>(JUNK_LOOT_POOL);
        Collections.shuffle(junk, random);
        int junkCount = Math.min(junk.size(), MIN_JUNK_STACKS + random.nextInt(EXTRA_JUNK_STACKS + 1));
        for (int i = 0; i < junkCount; i++) {
            if (!placeRandomStack(chest, junk.get(i), random)) {
                return;
            }
        }
        chest.setChanged();
    }

    private static int randomRareGearCount(Random random) {
        int roll = random.nextInt(100);
        if (roll < 45) {
            return 0;
        }
        if (roll < 85) {
            return 1;
        }
        return 2;
    }

    private static boolean placeRandomStack(ChestBlockEntity chest, LootEntry entry, Random random) {
        int slot = randomEmptySlot(chest, random);
        if (slot < 0) {
            return false;
        }
        chest.setItem(slot, entry.createStack(random));
        return true;
    }

    private static int randomEmptySlot(ChestBlockEntity chest, Random random) {
        int start = random.nextInt(chest.getContainerSize());
        for (int offset = 0; offset < chest.getContainerSize(); offset++) {
            int slot = (start + offset) % chest.getContainerSize();
            if (chest.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static void playDropStartEffects(ServerLevel world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.7F, 0.7F);
        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2F, 1.45F);
        world.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5, 48, 0.75, 1.0, 0.75, 0.08);
        world.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 30, 0.65, 0.7, 0.65, 0.05);
        spawnGroundMarker(world, pos);
    }

    private static void playLandingEffects(ServerLevel world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.75F, 1.8F);
        world.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.9F, 1.2F);
        world.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.7F);
        world.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 35, 0.45, 0.15, 0.45, 0.04);
        world.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 38, 0.45, 0.65, 0.45, 0.025);
        spawnBeaconBeam(world, pos);
    }

    private static void spawnFallingTrail(ServerLevel world, Entity entity, ActiveDrop drop) {
        world.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.35, entity.getZ(), 8, 0.22, 0.16, 0.22, 0.02);
        world.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 0.35, entity.getZ(), 5, 0.18, 0.14, 0.18, 0.015);
        world.sendParticles(ParticleTypes.GLOW, entity.getX(), entity.getY() + 0.35, entity.getZ(), 6, 0.22, 0.18, 0.22, 0.02);
        if (drop.age % 4 == 0) {
            world.sendParticles(ParticleTypes.FIREWORK, entity.getX(), entity.getY() + 0.4, entity.getZ(), 3, 0.16, 0.16, 0.16, 0.04);
            spawnDescentColumn(world, drop.landingPos, entity.getY());
        }
        if (drop.age % 6 == 0) {
            spawnGroundMarker(world, drop.landingPos);
        }
    }

    private static void spawnGroundMarker(ServerLevel world, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        world.sendParticles(ParticleTypes.END_ROD, x, pos.getY() + 0.15, z, 14, 0.65, 0.05, 0.65, 0.0);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 1.0, z, 16, 0.75, 0.65, 0.75, 0.04);
        world.sendParticles(ParticleTypes.GLOW, x, pos.getY() + 1.0, z, 10, 0.8, 0.45, 0.8, 0.02);
    }

    private static void spawnDescentColumn(ServerLevel world, BlockPos pos, double topY) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        int bottomY = pos.getY() + 2;
        int maxY = Math.min((int) Math.ceil(topY), bottomY + BEAM_HEIGHT);
        for (int y = bottomY; y <= maxY; y += 5) {
            world.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.04, 0.04, 0.04, 0.0);
            world.sendParticles(ParticleTypes.GLOW, x, y + 0.5, z, 1, 0.06, 0.06, 0.06, 0.0);
        }
    }

    private static void spawnBeaconBeam(ServerLevel world, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        for (int offset = 1; offset <= BEAM_HEIGHT; offset += 3) {
            double y = pos.getY() + offset;
            world.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.035, 0.08, 0.035, 0.0);
            if (offset % 6 == 1) {
                world.sendParticles(ParticleTypes.GLOW, x, y + 0.5, z, 2, 0.08, 0.12, 0.08, 0.0);
            }
        }
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 1.0, z, 6, 0.28, 0.35, 0.28, 0.025);
    }

    private static void playBeamStopEffects(ServerLevel world, BlockPos pos, boolean opened) {
        world.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, opened ? 10 : 16, 0.25, 0.25, 0.25, 0.02);
        world.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.55F, opened ? 1.65F : 1.25F);
    }

    private static void removeUnopenedChest(ServerLevel world, ActiveDrop drop) {
        world.sendParticles(ParticleTypes.CLOUD, drop.chestPos.getX() + 0.5, drop.chestPos.getY() + 0.9, drop.chestPos.getZ() + 0.5, 28, 0.35, 0.3, 0.35, 0.03);
        world.sendParticles(ParticleTypes.END_ROD, drop.chestPos.getX() + 0.5, drop.chestPos.getY() + 1.1, drop.chestPos.getZ() + 0.5, 16, 0.25, 0.4, 0.25, 0.02);
        world.playSound(null, drop.chestPos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.8F, 0.9F);
        if (world.getBlockEntity(drop.chestPos) instanceof ChestBlockEntity chest) {
            clearChestContents(chest);
        }
        world.removeBlock(drop.chestPos, false);
        restoreBeaconMarker(world, drop);
    }

    private static void clearChestContents(ChestBlockEntity chest) {
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, ItemStack.EMPTY);
        }
        chest.setChanged();
    }

    private static BeaconMarker createBeaconMarker(ServerLevel world, BlockPos chestPos) {
        BlockPos beaconPos = chestPos.below();
        BlockPos baseCenter = chestPos.below(2);
        Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
        List<BlockPos> positions = new ArrayList<>();
        positions.add(beaconPos);
        for (BlockPos pos : BlockPos.betweenClosed(baseCenter.offset(-1, 0, -1), baseCenter.offset(1, 0, 1))) {
            positions.add(pos.immutable());
        }

        for (BlockPos pos : positions) {
            if (world.getBlockEntity(pos) != null) {
                return null;
            }
            originals.put(pos, world.getBlockState(pos));
        }

        world.setBlock(beaconPos, Blocks.BEACON.defaultBlockState(), Block.UPDATE_ALL);
        for (BlockPos pos : BlockPos.betweenClosed(baseCenter.offset(-1, 0, -1), baseCenter.offset(1, 0, 1))) {
            world.setBlock(pos, Blocks.IRON_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }
        return new BeaconMarker(originals);
    }

    private static void restoreBeaconMarker(ServerLevel world, ActiveDrop drop) {
        restoreBeaconMarker(world, drop.beaconMarker);
        drop.beaconMarker = null;
    }

    private static void restoreBeaconMarker(ServerLevel world, BeaconMarker marker) {
        if (marker == null) {
            return;
        }
        for (Map.Entry<BlockPos, BlockState> entry : marker.originalStates().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState current = world.getBlockState(pos);
            if (current.is(Blocks.BEACON) || current.is(Blocks.IRON_BLOCK)) {
                world.setBlock(pos, entry.getValue(), Block.UPDATE_ALL);
            }
        }
    }

    private record DropPoint(BlockPos landingPos, BlockPos spawnPos) {
    }

    private record ScheduledDrop(ResourceKey<Level> worldKey, List<DropPoint> points, List<BeaconMarker> markers, int spawnTick) {
    }

    private record LootEntry(net.minecraft.world.item.Item item, int minCount, int maxCount) {
        private ItemStack createStack(Random random) {
            int count = minCount == maxCount ? minCount : minCount + random.nextInt(maxCount - minCount + 1);
            return new ItemStack(item, count);
        }
    }

    private record BeaconMarker(Map<BlockPos, BlockState> originalStates) {
    }

    private static class ActiveDrop {
        private final ResourceKey<Level> worldKey;
        private final BlockPos landingPos;
        private final BlockPos spawnPos;
        private final UUID fallingEntityUuid;
        private BeaconMarker beaconMarker;
        private BlockPos chestPos;
        private int age;
        private boolean beamActive;
        private int beamExpireAge;
        private boolean opened;

        private ActiveDrop(ResourceKey<Level> worldKey, BlockPos landingPos, BlockPos spawnPos, UUID fallingEntityUuid, BeaconMarker beaconMarker) {
            this.worldKey = worldKey;
            this.landingPos = landingPos;
            this.spawnPos = spawnPos;
            this.fallingEntityUuid = fallingEntityUuid;
            this.beaconMarker = beaconMarker;
        }
    }
}
