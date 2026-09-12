package com.xiaoming.hunterwildcard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.game.HunterVictoryType;
import com.xiaoming.hunterwildcard.game.RunnerVictoryType;
import com.xiaoming.hunterwildcard.respawn.RespawnMode;
import com.xiaoming.hunterwildcard.respawn.RunnerTeamLossMode;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardIds;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hunterwildcard.json";

    public int preparingSeconds = 60;
    public int endingSeconds = 10;
    public int compassUpdateSeconds = 3;
    public int hunterRespawnSeconds = 10;
    /** Downtime after an event ends, not the time between event starts. */
    public int wildcardIntervalSeconds = 180;
    public int wildcardDurationSeconds = 120;
    /** FIXED uses the single value above; RANDOM rolls between min and max every time. */
    public String wildcardIntervalMode = "FIXED";
    public int wildcardIntervalMinSeconds = 120;
    public int wildcardIntervalMaxSeconds = 240;
    public String wildcardDurationMode = "FIXED";
    public int wildcardDurationMinSeconds = 90;
    public int wildcardDurationMaxSeconds = 150;
    public int actionBarIntervalSeconds = 1;
    public int hunterRadarWarningDistance = 40;
    public int supplyDropIntervalSeconds = 60;
    public int spaceShiftIntervalSeconds = 60;
    public int blockDecaySeconds = 10;
    public int pearlFrenzyMaxPearls = 4;
    public int pearlFrenzyIntervalSeconds = 45;
    public int windChargeBrawlIntervalSeconds = 5;
    public int windChargeExplosionMultiplierPercent = 150;
    public int backroomsDurationSeconds = 180;
    public boolean hunterPrepareBoundaryEnabled = true;
    public int hunterPrepareBoundaryRadius = 20;
    public int hunterPrepareBoundaryWarnDistance = 3;
    public boolean runnerDeathNoDrops = false;
    public boolean hunterDeathNoDrops = false;
    public boolean piglinPearlBoostEnabled = true;
    public int piglinPearlChancePercent = 20;
    public boolean blazeRodChanceEnabled = false;
    public int blazeRodChancePercent = 50;
    public int hunterDamageMultiplierPercent = 100;
    public int hunterSpeedPercent = 100;
    public int runnerSpeedPercent = 100;
    /** Seconds after a hunter last hit a runner during which any death of that runner still credits the hunter. */
    public int hunterHitCreditSeconds = 15;
    /** Pure environment deaths (no hunter involved) needed to count as one hunter kill in kill-count mode. */
    public int environmentDeathsPerKill = 1;
    /** Off: falls, mobs and other pure environment deaths never count towards the hunters' kill target. */
    public boolean environmentKillsEnabled = true;
    /** Respawn at a random surface spot away from the death point (and, for runners, away from hunters). */
    public boolean randomRespawnEnabled = true;
    /** Minimum distance from the death point; the maximum is twice this. */
    public int runnerRespawnDistance = 150;
    public int hunterRespawnDistance = 64;
    /** A respawning hunter is placed at least this far from every runner (best effort). */
    public int hunterRespawnRunnerClearance = 96;
    /** Extra respawn seconds per previous hunter death this round (capped at 60 extra). */
    public int hunterRespawnPenaltySeconds = 3;
    /** Vanilla locator bar stays on during a round but only shows players of your own side. */
    public boolean locatorBarTeamOnly = true;
    /** Survive-time rounds put a square world border of this radius (blocks from spawn) around the arena. */
    public boolean surviveBorderEnabled = true;
    public int surviveBorderRadius = 500;

    public String runnerVictoryType = "DRAGON";
    public String runnerWinMode = "ANY_ENABLED";
    public boolean enableDragonWin = true;
    public boolean enableSurviveTimeWin = false;
    public int surviveTimeSeconds = 900;
    public boolean enableReachLocationWin = false;
    public String targetDimension = "minecraft:overworld";
    public int targetX = 0;
    public int targetY = 80;
    public int targetZ = 0;
    public int targetRadius = 10;
    public boolean enableCollectItemWin = false;
    public String targetItemId = "minecraft:diamond";
    public int targetItemCount = 16;

    public String hunterRespawnMode = "INFINITE";
    public int hunterLives = 0;
    public String runnerRespawnMode = "LIMITED_LIVES";
    /** Three total lives leave room for wildcard accidents while deaths still cost inventory. */
    public int runnerLives = 3;
    public int runnerRespawnSeconds = 10;
    public String runnerTeamLossMode = "ALL_RUNNERS_OUT";
    public String hunterVictoryType = "RUNNERS_OUT";
    public boolean hunterWinByRunnerKillsEnabled = false;
    public int hunterRunnerKillTarget = 10;

    /** Wildcard id (snake_case) -> enabled. Missing ids count as enabled. */
    public Map<String, Boolean> enabledWildcards = new LinkedHashMap<>();

    public static ModConfig load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            ModConfig config = new ModConfig();
            config.save();
            return config;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            ModConfig config = json == null ? new ModConfig() : GSON.fromJson(json, ModConfig.class);
            if (config == null) {
                config = new ModConfig();
            }
            if (json == null || !json.has("runnerVictoryType")) {
                config.runnerVictoryType = inferRunnerVictoryType(config).name();
            }
            if (json == null || !json.has("hunterVictoryType")) {
                config.hunterVictoryType = inferHunterVictoryType(config).name();
            }
            ModConfig defaults = new ModConfig();
            if (json == null || !json.has("blockDecaySeconds")) {
                config.blockDecaySeconds = defaults.blockDecaySeconds;
            }
            if (json == null || !json.has("pearlFrenzyMaxPearls")) {
                config.pearlFrenzyMaxPearls = defaults.pearlFrenzyMaxPearls;
            }
            if (json == null || !json.has("pearlFrenzyIntervalSeconds")) {
                config.pearlFrenzyIntervalSeconds = defaults.pearlFrenzyIntervalSeconds;
            }
            if (json == null || !json.has("windChargeBrawlIntervalSeconds")) {
                config.windChargeBrawlIntervalSeconds = defaults.windChargeBrawlIntervalSeconds;
            }
            if (json == null || !json.has("windChargeExplosionMultiplierPercent")) {
                config.windChargeExplosionMultiplierPercent = defaults.windChargeExplosionMultiplierPercent;
            }
            if (json != null) {
                migrateLegacyWildcardToggles(json, config);
            }
            config.validate();
            config.save();
            return config;
        } catch (IOException | RuntimeException exception) {
            HunterWildcardMod.LOGGER.warn("Failed to load hunterwildcard config, using defaults.", exception);
            ModConfig config = new ModConfig();
            config.save();
            return config;
        }
    }

    /** Older files stored one {@code enableXxx} boolean per wildcard; carry those over for the wildcards that still exist. */
    private static void migrateLegacyWildcardToggles(JsonObject json, ModConfig config) {
        if (config.enabledWildcards == null) {
            config.enabledWildcards = new LinkedHashMap<>();
        }
        for (Map.Entry<String, String> legacy : LEGACY_TOGGLE_FIELDS.entrySet()) {
            if (json.has(legacy.getKey()) && !config.enabledWildcards.containsKey(legacy.getValue())) {
                try {
                    config.enabledWildcards.put(legacy.getValue(), json.get(legacy.getKey()).getAsBoolean());
                } catch (RuntimeException ignored) {
                    // Malformed legacy value: keep the default (enabled).
                }
            }
        }
    }

    private static final Map<String, String> LEGACY_TOGGLE_FIELDS = Map.ofEntries(
            Map.entry("enableExplosiveDeath", "explosive_death"),
            Map.entry("enableSupplyDrop", "supply_drop"),
            Map.entry("enableHunterRadar", "hunter_radar"),
            Map.entry("enableHungerChase", "hunger_chase"),
            Map.entry("enableWeaponOverheat", "weapon_overheat"),
            Map.entry("enableLightLoad", "light_load"),
            Map.entry("enableBlockDecay", "block_decay"),
            Map.entry("enablePearlFrenzy", "pearl_frenzy"),
            Map.entry("enableWindChargeBrawl", "wind_charge_brawl"),
            Map.entry("enableBloodRage", "blood_rage"),
            Map.entry("enableKeyScramble", "key_scramble"),
            Map.entry("enableTinyPlayers", "tiny_players"),
            Map.entry("enableFragile", "fragile"),
            Map.entry("enableWhoAreYou", "who_are_you"),
            Map.entry("enableStayAway", "stay_away"),
            Map.entry("enableBackrooms", "backrooms")
    );

    public boolean save() {
        validate();
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
            return true;
        } catch (IOException exception) {
            HunterWildcardMod.LOGGER.warn("Failed to save hunterwildcard config.", exception);
            return false;
        }
    }

    public void validate() {
        preparingSeconds = clampSeconds(preparingSeconds);
        endingSeconds = clampSeconds(endingSeconds);
        compassUpdateSeconds = clampSeconds(compassUpdateSeconds);
        hunterRespawnSeconds = clampSeconds(hunterRespawnSeconds);
        wildcardIntervalSeconds = clampSeconds(wildcardIntervalSeconds);
        wildcardDurationSeconds = clampSeconds(wildcardDurationSeconds);
        wildcardIntervalMode = sanitizeTimingMode(wildcardIntervalMode);
        wildcardDurationMode = sanitizeTimingMode(wildcardDurationMode);
        wildcardIntervalMinSeconds = clampSeconds(wildcardIntervalMinSeconds);
        wildcardIntervalMaxSeconds = Math.max(wildcardIntervalMinSeconds, clampSeconds(wildcardIntervalMaxSeconds));
        wildcardDurationMinSeconds = clampSeconds(wildcardDurationMinSeconds);
        wildcardDurationMaxSeconds = Math.max(wildcardDurationMinSeconds, clampSeconds(wildcardDurationMaxSeconds));
        actionBarIntervalSeconds = clampSeconds(actionBarIntervalSeconds);
        hunterRadarWarningDistance = clampPositive(hunterRadarWarningDistance);
        supplyDropIntervalSeconds = clampSeconds(supplyDropIntervalSeconds);
        spaceShiftIntervalSeconds = clampSeconds(spaceShiftIntervalSeconds);
        normalizeWildcardToggles();
        blockDecaySeconds = clampSeconds(blockDecaySeconds);
        pearlFrenzyMaxPearls = clampPositive(pearlFrenzyMaxPearls);
        pearlFrenzyIntervalSeconds = clampSeconds(pearlFrenzyIntervalSeconds);
        windChargeBrawlIntervalSeconds = clampSeconds(windChargeBrawlIntervalSeconds);
        windChargeExplosionMultiplierPercent = clampPositive(windChargeExplosionMultiplierPercent);
        backroomsDurationSeconds = clampSeconds(backroomsDurationSeconds);
        hunterPrepareBoundaryRadius = clampPositive(hunterPrepareBoundaryRadius);
        hunterPrepareBoundaryWarnDistance = Math.max(0, hunterPrepareBoundaryWarnDistance);
        piglinPearlChancePercent = Math.max(0, Math.min(100, piglinPearlChancePercent));
        blazeRodChancePercent = Math.max(0, Math.min(100, blazeRodChancePercent));
        hunterDamageMultiplierPercent = Math.max(1, Math.min(1000, hunterDamageMultiplierPercent));
        hunterSpeedPercent = Math.max(10, Math.min(500, hunterSpeedPercent));
        runnerSpeedPercent = Math.max(10, Math.min(500, runnerSpeedPercent));
        hunterHitCreditSeconds = Math.max(0, Math.min(120, hunterHitCreditSeconds));
        environmentDeathsPerKill = clampPositive(environmentDeathsPerKill);
        runnerRespawnDistance = Math.max(16, Math.min(2000, runnerRespawnDistance));
        hunterRespawnDistance = Math.max(16, Math.min(2000, hunterRespawnDistance));
        hunterRespawnRunnerClearance = Math.max(0, Math.min(5000, hunterRespawnRunnerClearance));
        hunterRespawnPenaltySeconds = Math.max(0, Math.min(60, hunterRespawnPenaltySeconds));
        surviveBorderRadius = Math.max(16, Math.min(30_000, surviveBorderRadius));
        runnerVictoryType = getRunnerVictoryType().name();
        syncLegacyRunnerWinFields();
        runnerWinMode = sanitizeRunnerWinMode(runnerWinMode);
        surviveTimeSeconds = clampSeconds(surviveTimeSeconds);
        targetDimension = sanitizeIdentifier(targetDimension, "minecraft:overworld");
        targetRadius = clampPositive(targetRadius);
        targetItemId = sanitizeIdentifier(targetItemId, "minecraft:diamond");
        targetItemCount = clampPositive(targetItemCount);
        hunterRespawnMode = getHunterRespawnMode().name();
        hunterLives = Math.max(0, hunterLives);
        runnerRespawnMode = getRunnerRespawnMode().name();
        runnerLives = Math.max(0, runnerLives);
        runnerRespawnSeconds = clampSeconds(runnerRespawnSeconds);
        runnerTeamLossMode = getRunnerTeamLossMode().name();
        hunterVictoryType = getHunterVictoryType().name();
        syncLegacyHunterWinFields();
        if (getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT) {
            runnerRespawnMode = RespawnMode.INFINITE.name();
        }
        hunterRunnerKillTarget = clampPositive(hunterRunnerKillTarget);
    }

    public void copyFrom(ModConfig other) {
        preparingSeconds = other.preparingSeconds;
        endingSeconds = other.endingSeconds;
        compassUpdateSeconds = other.compassUpdateSeconds;
        hunterRespawnSeconds = other.hunterRespawnSeconds;
        wildcardIntervalSeconds = other.wildcardIntervalSeconds;
        wildcardDurationSeconds = other.wildcardDurationSeconds;
        wildcardIntervalMode = other.wildcardIntervalMode;
        wildcardIntervalMinSeconds = other.wildcardIntervalMinSeconds;
        wildcardIntervalMaxSeconds = other.wildcardIntervalMaxSeconds;
        wildcardDurationMode = other.wildcardDurationMode;
        wildcardDurationMinSeconds = other.wildcardDurationMinSeconds;
        wildcardDurationMaxSeconds = other.wildcardDurationMaxSeconds;
        actionBarIntervalSeconds = other.actionBarIntervalSeconds;
        hunterRadarWarningDistance = other.hunterRadarWarningDistance;
        supplyDropIntervalSeconds = other.supplyDropIntervalSeconds;
        spaceShiftIntervalSeconds = other.spaceShiftIntervalSeconds;
        blockDecaySeconds = other.blockDecaySeconds;
        pearlFrenzyMaxPearls = other.pearlFrenzyMaxPearls;
        pearlFrenzyIntervalSeconds = other.pearlFrenzyIntervalSeconds;
        windChargeBrawlIntervalSeconds = other.windChargeBrawlIntervalSeconds;
        windChargeExplosionMultiplierPercent = other.windChargeExplosionMultiplierPercent;
        backroomsDurationSeconds = other.backroomsDurationSeconds;
        hunterPrepareBoundaryEnabled = other.hunterPrepareBoundaryEnabled;
        hunterPrepareBoundaryRadius = other.hunterPrepareBoundaryRadius;
        hunterPrepareBoundaryWarnDistance = other.hunterPrepareBoundaryWarnDistance;
        runnerDeathNoDrops = other.runnerDeathNoDrops;
        hunterDeathNoDrops = other.hunterDeathNoDrops;
        piglinPearlBoostEnabled = other.piglinPearlBoostEnabled;
        piglinPearlChancePercent = other.piglinPearlChancePercent;
        blazeRodChanceEnabled = other.blazeRodChanceEnabled;
        blazeRodChancePercent = other.blazeRodChancePercent;
        hunterDamageMultiplierPercent = other.hunterDamageMultiplierPercent;
        hunterSpeedPercent = other.hunterSpeedPercent;
        runnerSpeedPercent = other.runnerSpeedPercent;
        hunterHitCreditSeconds = other.hunterHitCreditSeconds;
        environmentDeathsPerKill = other.environmentDeathsPerKill;
        environmentKillsEnabled = other.environmentKillsEnabled;
        randomRespawnEnabled = other.randomRespawnEnabled;
        runnerRespawnDistance = other.runnerRespawnDistance;
        hunterRespawnDistance = other.hunterRespawnDistance;
        hunterRespawnRunnerClearance = other.hunterRespawnRunnerClearance;
        hunterRespawnPenaltySeconds = other.hunterRespawnPenaltySeconds;
        locatorBarTeamOnly = other.locatorBarTeamOnly;
        surviveBorderEnabled = other.surviveBorderEnabled;
        surviveBorderRadius = other.surviveBorderRadius;
        runnerVictoryType = other.runnerVictoryType;
        runnerWinMode = other.runnerWinMode;
        enableDragonWin = other.enableDragonWin;
        enableSurviveTimeWin = other.enableSurviveTimeWin;
        surviveTimeSeconds = other.surviveTimeSeconds;
        enableReachLocationWin = other.enableReachLocationWin;
        targetDimension = other.targetDimension;
        targetX = other.targetX;
        targetY = other.targetY;
        targetZ = other.targetZ;
        targetRadius = other.targetRadius;
        enableCollectItemWin = other.enableCollectItemWin;
        targetItemId = other.targetItemId;
        targetItemCount = other.targetItemCount;
        hunterRespawnMode = other.hunterRespawnMode;
        hunterLives = other.hunterLives;
        runnerRespawnMode = other.runnerRespawnMode;
        runnerLives = other.runnerLives;
        runnerRespawnSeconds = other.runnerRespawnSeconds;
        runnerTeamLossMode = other.runnerTeamLossMode;
        hunterVictoryType = other.hunterVictoryType;
        hunterWinByRunnerKillsEnabled = other.hunterWinByRunnerKillsEnabled;
        hunterRunnerKillTarget = other.hunterRunnerKillTarget;
        enabledWildcards = new LinkedHashMap<>(other.enabledWildcards);
        validate();
    }

    /** Copies only the settings that are safe to change while a round is running. */
    public void copyLiveFrom(ModConfig other) {
        hunterRespawnSeconds = other.hunterRespawnSeconds;
        runnerRespawnSeconds = other.runnerRespawnSeconds;
        wildcardIntervalSeconds = other.wildcardIntervalSeconds;
        wildcardDurationSeconds = other.wildcardDurationSeconds;
        wildcardIntervalMode = other.wildcardIntervalMode;
        wildcardIntervalMinSeconds = other.wildcardIntervalMinSeconds;
        wildcardIntervalMaxSeconds = other.wildcardIntervalMaxSeconds;
        wildcardDurationMode = other.wildcardDurationMode;
        wildcardDurationMinSeconds = other.wildcardDurationMinSeconds;
        wildcardDurationMaxSeconds = other.wildcardDurationMaxSeconds;
        hunterRadarWarningDistance = other.hunterRadarWarningDistance;
        supplyDropIntervalSeconds = other.supplyDropIntervalSeconds;
        spaceShiftIntervalSeconds = other.spaceShiftIntervalSeconds;
        blockDecaySeconds = other.blockDecaySeconds;
        pearlFrenzyMaxPearls = other.pearlFrenzyMaxPearls;
        pearlFrenzyIntervalSeconds = other.pearlFrenzyIntervalSeconds;
        windChargeBrawlIntervalSeconds = other.windChargeBrawlIntervalSeconds;
        windChargeExplosionMultiplierPercent = other.windChargeExplosionMultiplierPercent;
        backroomsDurationSeconds = other.backroomsDurationSeconds;
        runnerDeathNoDrops = other.runnerDeathNoDrops;
        hunterDeathNoDrops = other.hunterDeathNoDrops;
        piglinPearlBoostEnabled = other.piglinPearlBoostEnabled;
        piglinPearlChancePercent = other.piglinPearlChancePercent;
        blazeRodChanceEnabled = other.blazeRodChanceEnabled;
        blazeRodChancePercent = other.blazeRodChancePercent;
        hunterDamageMultiplierPercent = other.hunterDamageMultiplierPercent;
        hunterSpeedPercent = other.hunterSpeedPercent;
        runnerSpeedPercent = other.runnerSpeedPercent;
        hunterHitCreditSeconds = other.hunterHitCreditSeconds;
        environmentDeathsPerKill = other.environmentDeathsPerKill;
        environmentKillsEnabled = other.environmentKillsEnabled;
        randomRespawnEnabled = other.randomRespawnEnabled;
        runnerRespawnDistance = other.runnerRespawnDistance;
        hunterRespawnDistance = other.hunterRespawnDistance;
        hunterRespawnRunnerClearance = other.hunterRespawnRunnerClearance;
        hunterRespawnPenaltySeconds = other.hunterRespawnPenaltySeconds;
        locatorBarTeamOnly = other.locatorBarTeamOnly;
        surviveTimeSeconds = other.surviveTimeSeconds;
        targetDimension = other.targetDimension;
        targetX = other.targetX;
        targetY = other.targetY;
        targetZ = other.targetZ;
        targetRadius = other.targetRadius;
        targetItemId = other.targetItemId;
        targetItemCount = other.targetItemCount;
        runnerTeamLossMode = other.runnerTeamLossMode;
        hunterRunnerKillTarget = other.hunterRunnerKillTarget;
        enabledWildcards = new LinkedHashMap<>(other.enabledWildcards);
        validate();
    }

    public int getPreparingTicks() {
        return secondsToTicks(preparingSeconds);
    }

    public int getEndingTicks() {
        return secondsToTicks(endingSeconds);
    }

    public int getCompassUpdateTicks() {
        return secondsToTicks(compassUpdateSeconds);
    }

    public int getHunterRespawnTicks() {
        return secondsToTicks(hunterRespawnSeconds);
    }

    public int getWildcardIntervalTicks() {
        return secondsToTicks(wildcardIntervalSeconds);
    }

    public int getWildcardDurationTicks() {
        return secondsToTicks(wildcardDurationSeconds);
    }

    public boolean isWildcardIntervalRandom() {
        return "RANDOM".equals(wildcardIntervalMode);
    }

    public boolean isWildcardDurationRandom() {
        return "RANDOM".equals(wildcardDurationMode);
    }

    /** Next gap before a wildcard is drawn: the fixed value, or a fresh roll inside [min, max]. */
    public int rollWildcardIntervalTicks(java.util.Random random) {
        if (!isWildcardIntervalRandom()) {
            return getWildcardIntervalTicks();
        }
        return secondsToTicks(rollBetween(random, wildcardIntervalMinSeconds, wildcardIntervalMaxSeconds));
    }

    /** How long the next wildcard lasts: the fixed value, or a fresh roll inside [min, max]. */
    public int rollWildcardDurationTicks(java.util.Random random) {
        if (!isWildcardDurationRandom()) {
            return getWildcardDurationTicks();
        }
        return secondsToTicks(rollBetween(random, wildcardDurationMinSeconds, wildcardDurationMaxSeconds));
    }

    /** Upper bound of the interval, used to cap a pending countdown when the config shrinks mid-round. */
    public int getMaxWildcardIntervalTicks() {
        return secondsToTicks(isWildcardIntervalRandom() ? wildcardIntervalMaxSeconds : wildcardIntervalSeconds);
    }

    private static int rollBetween(java.util.Random random, int min, int max) {
        int low = Math.max(1, Math.min(min, max));
        int high = Math.max(low, Math.max(min, max));
        return low + random.nextInt(high - low + 1);
    }

    private static String sanitizeTimingMode(String value) {
        return value != null && "RANDOM".equalsIgnoreCase(value.trim()) ? "RANDOM" : "FIXED";
    }

    public int getActionBarIntervalTicks() {
        return secondsToTicks(actionBarIntervalSeconds);
    }

    public int getSpaceShiftIntervalTicks() {
        return secondsToTicks(spaceShiftIntervalSeconds);
    }

    public int getSupplyDropIntervalTicks() {
        return secondsToTicks(supplyDropIntervalSeconds);
    }

    public int getBlockDecayTicks() {
        return secondsToTicks(blockDecaySeconds);
    }

    public int getPearlFrenzyIntervalTicks() {
        return secondsToTicks(pearlFrenzyIntervalSeconds);
    }

    public int getWindChargeBrawlIntervalTicks() {
        return secondsToTicks(windChargeBrawlIntervalSeconds);
    }

    public int getHunterHitCreditTicks() {
        return Math.max(0, hunterHitCreditSeconds) * 20;
    }

    public float getHunterDamageMultiplier() {
        return Math.max(1, hunterDamageMultiplierPercent) / 100.0F;
    }

    public float getPiglinPearlChance() {
        return piglinPearlBoostEnabled ? Math.max(0, Math.min(100, piglinPearlChancePercent)) / 100.0F : -1.0F;
    }

    public int getBackroomsDurationTicks() {
        return secondsToTicks(backroomsDurationSeconds);
    }

    public float getWindChargeExplosionMultiplier() {
        return Math.max(1, windChargeExplosionMultiplierPercent) / 100.0F;
    }

    public int getSurviveTimeTicks() {
        return secondsToTicks(surviveTimeSeconds);
    }

    public int getRunnerRespawnTicks() {
        return secondsToTicks(runnerRespawnSeconds);
    }

    public RunnerVictoryType getRunnerVictoryType() {
        return RunnerVictoryType.fromConfig(runnerVictoryType, RunnerVictoryType.DRAGON);
    }

    public RespawnMode getHunterRespawnMode() {
        return RespawnMode.fromConfig(hunterRespawnMode, RespawnMode.INFINITE);
    }

    public RespawnMode getRunnerRespawnMode() {
        return RespawnMode.fromConfig(runnerRespawnMode, RespawnMode.LIMITED_LIVES);
    }

    public RunnerTeamLossMode getRunnerTeamLossMode() {
        return RunnerTeamLossMode.fromConfig(runnerTeamLossMode, RunnerTeamLossMode.ALL_RUNNERS_OUT);
    }

    public HunterVictoryType getHunterVictoryType() {
        return HunterVictoryType.fromConfig(hunterVictoryType, HunterVictoryType.RUNNERS_OUT);
    }

    public boolean isWildcardEnabled(String ruleName) {
        String id = normalizeWildcardId(ruleName);
        if (id == null || !WildcardIds.ALL.contains(id)) {
            return false;
        }
        return enabledWildcards.getOrDefault(id, Boolean.TRUE);
    }

    public void setWildcardEnabled(String ruleName, boolean enabled) {
        String id = normalizeWildcardId(ruleName);
        if (id != null) {
            enabledWildcards.put(id, enabled);
        }
    }

    /** Every registered wildcard gets an explicit entry so the JSON file lists them all; unknown ids are dropped. */
    private void normalizeWildcardToggles() {
        Map<String, Boolean> normalized = new LinkedHashMap<>();
        Map<String, Boolean> current = enabledWildcards == null ? Map.of() : enabledWildcards;
        for (String id : WildcardIds.ALL) {
            Boolean value = current.get(id);
            normalized.put(id, value == null || value);
        }
        enabledWildcards = normalized;
    }

    private static String normalizeWildcardId(String ruleName) {
        if (ruleName == null || ruleName.isBlank()) {
            return null;
        }
        boolean hasUpper = false;
        for (int i = 0; i < ruleName.length(); i++) {
            if (Character.isUpperCase(ruleName.charAt(i))) {
                hasUpper = true;
                break;
            }
        }
        return hasUpper ? HunterWildcardText.wildcardId(ruleName) : ruleName;
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static int clampSeconds(int value) {
        return Math.max(1, value);
    }

    private static int clampPositive(int value) {
        return Math.max(1, value);
    }

    private static String sanitizeRunnerWinMode(String value) {
        if (value == null || value.isBlank()) {
            return "ANY_ENABLED";
        }

        return "ANY_ENABLED".equalsIgnoreCase(value.trim()) ? "ANY_ENABLED" : "ANY_ENABLED";
    }

    private static RunnerVictoryType inferRunnerVictoryType(ModConfig config) {
        if (config.enableDragonWin) {
            return RunnerVictoryType.DRAGON;
        }
        if (config.enableSurviveTimeWin) {
            return RunnerVictoryType.SURVIVE_TIME;
        }
        if (config.enableReachLocationWin) {
            return RunnerVictoryType.REACH_LOCATION;
        }
        if (config.enableCollectItemWin) {
            return RunnerVictoryType.COLLECT_ITEM;
        }
        return RunnerVictoryType.DRAGON;
    }

    private static HunterVictoryType inferHunterVictoryType(ModConfig config) {
        return config.hunterWinByRunnerKillsEnabled ? HunterVictoryType.RUNNER_KILL_COUNT : HunterVictoryType.RUNNERS_OUT;
    }

    private void syncLegacyRunnerWinFields() {
        RunnerVictoryType victoryType = getRunnerVictoryType();
        enableDragonWin = victoryType == RunnerVictoryType.DRAGON;
        enableSurviveTimeWin = victoryType == RunnerVictoryType.SURVIVE_TIME;
        enableReachLocationWin = victoryType == RunnerVictoryType.REACH_LOCATION;
        enableCollectItemWin = victoryType == RunnerVictoryType.COLLECT_ITEM;
    }

    private void syncLegacyHunterWinFields() {
        hunterWinByRunnerKillsEnabled = getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT;
    }

    private static String sanitizeIdentifier(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String trimmed = value.trim();
        return Identifier.tryParse(trimmed) == null ? fallback : trimmed;
    }

    private static int secondsToTicks(int seconds) {
        long ticks = Math.max(1L, seconds) * 20L;
        return ticks > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ticks;
    }
}
