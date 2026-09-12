package com.xiaoming.hunterwildcard.client.ui;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.ConfigSnapshot;
import com.xiaoming.hunterwildcard.client.screen.widget.DropdownWidget;
import com.xiaoming.hunterwildcard.game.*;
import com.xiaoming.hunterwildcard.respawn.*;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardIds;
import java.util.*;
/** Typed field metadata, formatting and rule mutations shared by editor pages. */
public final class RuleFields {
    private RuleFields() {}
    private static String key(String path){return HunterWildcardText.key(path);}
    public static int getNumber(ConfigSnapshot config, NumberField field) {
        return switch (field) {
            case PREPARING_SECONDS -> config.preparingSeconds();
            case HUNTER_RESPAWN_SECONDS -> config.hunterRespawnSeconds();
            case WILDCARD_INTERVAL_SECONDS -> config.wildcardIntervalSeconds();
            case WILDCARD_DURATION_SECONDS -> config.wildcardDurationSeconds();
            case WILDCARD_INTERVAL_MIN_SECONDS -> config.wildcardIntervalMinSeconds();
            case WILDCARD_INTERVAL_MAX_SECONDS -> config.wildcardIntervalMaxSeconds();
            case WILDCARD_DURATION_MIN_SECONDS -> config.wildcardDurationMinSeconds();
            case WILDCARD_DURATION_MAX_SECONDS -> config.wildcardDurationMaxSeconds();
            case HUNTER_RADAR_WARNING_DISTANCE -> config.hunterRadarWarningDistance();
            case SPACE_SHIFT_INTERVAL_SECONDS -> config.spaceShiftIntervalSeconds();
            case SUPPLY_DROP_INTERVAL_SECONDS -> config.supplyDropIntervalSeconds();
            case BLOCK_DECAY_SECONDS -> config.blockDecaySeconds();
            case PEARL_FRENZY_MAX_PEARLS -> config.pearlFrenzyMaxPearls();
            case PEARL_FRENZY_INTERVAL_SECONDS -> config.pearlFrenzyIntervalSeconds();
            case WIND_CHARGE_BRAWL_INTERVAL_SECONDS -> config.windChargeBrawlIntervalSeconds();
            case WIND_CHARGE_EXPLOSION_MULTIPLIER_PERCENT -> config.windChargeExplosionMultiplierPercent();
            case BACKROOMS_DURATION_SECONDS -> config.backroomsDurationSeconds();
            case HUNTER_PREPARE_BOUNDARY_RADIUS -> config.hunterPrepareBoundaryRadius();
            case PIGLIN_PEARL_CHANCE_PERCENT -> config.piglinPearlChancePercent();
            case BLAZE_ROD_CHANCE_PERCENT -> config.blazeRodChancePercent();
            case HUNTER_DAMAGE_MULTIPLIER_PERCENT -> config.hunterDamageMultiplierPercent();
            case HUNTER_SPEED_PERCENT -> config.hunterSpeedPercent();
            case RUNNER_SPEED_PERCENT -> config.runnerSpeedPercent();
            case HUNTER_HIT_CREDIT_SECONDS -> config.hunterHitCreditSeconds();
            case ENVIRONMENT_DEATHS_PER_KILL -> config.environmentDeathsPerKill();
            case RUNNER_RESPAWN_DISTANCE -> config.runnerRespawnDistance();
            case HUNTER_RESPAWN_DISTANCE -> config.hunterRespawnDistance();
            case HUNTER_RESPAWN_RUNNER_CLEARANCE -> config.hunterRespawnRunnerClearance();
            case HUNTER_RESPAWN_PENALTY_SECONDS -> config.hunterRespawnPenaltySeconds();
            case SURVIVE_TIME_SECONDS -> config.surviveTimeSeconds();
            case SURVIVE_BORDER_RADIUS -> config.surviveBorderRadius();
            case TARGET_X -> config.targetX();
            case TARGET_Y -> config.targetY();
            case TARGET_Z -> config.targetZ();
            case TARGET_RADIUS -> config.targetRadius();
            case TARGET_ITEM_COUNT -> config.targetItemCount();
            case HUNTER_LIVES -> config.hunterLives();
            case RUNNER_LIVES -> config.runnerLives();
            case RUNNER_RESPAWN_SECONDS -> config.runnerRespawnSeconds();
            case HUNTER_RUNNER_KILL_TARGET -> config.hunterRunnerKillTarget();
        };
    }

    public static ConfigSnapshot setNumber(ConfigSnapshot config, NumberField field, int value) {
        return setNumbers(config,java.util.Map.of(field,value));
    }
    public static ConfigSnapshot setNumbers(ConfigSnapshot config, java.util.Map<NumberField,Integer> values) {
        ModConfig copy = config.toConfig();
        values.forEach((field,value) -> assignNumber(copy,field,value));
        copy.validate();
        return ConfigSnapshot.from(copy);
    }
    private static void assignNumber(ModConfig copy, NumberField field, int value) {
        switch (field) {
            case PREPARING_SECONDS -> copy.preparingSeconds = value;
            case HUNTER_RESPAWN_SECONDS -> copy.hunterRespawnSeconds = value;
            case WILDCARD_INTERVAL_SECONDS -> copy.wildcardIntervalSeconds = value;
            case WILDCARD_DURATION_SECONDS -> copy.wildcardDurationSeconds = value;
            case WILDCARD_INTERVAL_MIN_SECONDS -> copy.wildcardIntervalMinSeconds = value;
            case WILDCARD_INTERVAL_MAX_SECONDS -> copy.wildcardIntervalMaxSeconds = value;
            case WILDCARD_DURATION_MIN_SECONDS -> copy.wildcardDurationMinSeconds = value;
            case WILDCARD_DURATION_MAX_SECONDS -> copy.wildcardDurationMaxSeconds = value;
            case HUNTER_RADAR_WARNING_DISTANCE -> copy.hunterRadarWarningDistance = value;
            case SPACE_SHIFT_INTERVAL_SECONDS -> copy.spaceShiftIntervalSeconds = value;
            case SUPPLY_DROP_INTERVAL_SECONDS -> copy.supplyDropIntervalSeconds = value;
            case BLOCK_DECAY_SECONDS -> copy.blockDecaySeconds = value;
            case PEARL_FRENZY_MAX_PEARLS -> copy.pearlFrenzyMaxPearls = value;
            case PEARL_FRENZY_INTERVAL_SECONDS -> copy.pearlFrenzyIntervalSeconds = value;
            case WIND_CHARGE_BRAWL_INTERVAL_SECONDS -> copy.windChargeBrawlIntervalSeconds = value;
            case WIND_CHARGE_EXPLOSION_MULTIPLIER_PERCENT -> copy.windChargeExplosionMultiplierPercent = value;
            case BACKROOMS_DURATION_SECONDS -> copy.backroomsDurationSeconds = value;
            case HUNTER_PREPARE_BOUNDARY_RADIUS -> copy.hunterPrepareBoundaryRadius = value;
            case PIGLIN_PEARL_CHANCE_PERCENT -> copy.piglinPearlChancePercent = value;
            case BLAZE_ROD_CHANCE_PERCENT -> copy.blazeRodChancePercent = value;
            case HUNTER_DAMAGE_MULTIPLIER_PERCENT -> copy.hunterDamageMultiplierPercent = value;
            case HUNTER_SPEED_PERCENT -> copy.hunterSpeedPercent = value;
            case RUNNER_SPEED_PERCENT -> copy.runnerSpeedPercent = value;
            case HUNTER_HIT_CREDIT_SECONDS -> copy.hunterHitCreditSeconds = value;
            case ENVIRONMENT_DEATHS_PER_KILL -> copy.environmentDeathsPerKill = value;
            case RUNNER_RESPAWN_DISTANCE -> copy.runnerRespawnDistance = value;
            case HUNTER_RESPAWN_DISTANCE -> copy.hunterRespawnDistance = value;
            case HUNTER_RESPAWN_RUNNER_CLEARANCE -> copy.hunterRespawnRunnerClearance = value;
            case HUNTER_RESPAWN_PENALTY_SECONDS -> copy.hunterRespawnPenaltySeconds = value;
            case SURVIVE_TIME_SECONDS -> copy.surviveTimeSeconds = value;
            case SURVIVE_BORDER_RADIUS -> copy.surviveBorderRadius = value;
            case TARGET_X -> copy.targetX = value;
            case TARGET_Y -> copy.targetY = value;
            case TARGET_Z -> copy.targetZ = value;
            case TARGET_RADIUS -> copy.targetRadius = value;
            case TARGET_ITEM_COUNT -> copy.targetItemCount = value;
            case HUNTER_LIVES -> copy.hunterLives = value;
            case RUNNER_LIVES -> copy.runnerLives = value;
            case RUNNER_RESPAWN_SECONDS -> copy.runnerRespawnSeconds = value;
            case HUNTER_RUNNER_KILL_TARGET -> copy.hunterRunnerKillTarget = value;
        }
    }

    public static String getString(ConfigSnapshot config, StringField field) {
        return switch (field) {
            case TARGET_ITEM_ID -> config.targetItemId();
        };
    }

    public static ConfigSnapshot setString(ConfigSnapshot config, StringField field, String value) {
        ModConfig copy = config.toConfig();
        switch (field) {
            case TARGET_ITEM_ID -> copy.targetItemId = value;
        }
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    public static String getDropdownValue(ConfigSnapshot config, DropdownField field) {
        return switch (field) {
            case RUNNER_VICTORY_TYPE -> config.runnerVictoryType();
            case HUNTER_VICTORY_TYPE -> config.hunterVictoryType();
            case HUNTER_RESPAWN_MODE -> config.hunterRespawnMode();
            case RUNNER_RESPAWN_MODE -> config.runnerRespawnMode();
            case RUNNER_TEAM_LOSS_MODE -> config.runnerTeamLossMode();
            case TARGET_DIMENSION -> config.targetDimension();
            case WILDCARD_INTERVAL_MODE -> config.wildcardIntervalMode();
            case WILDCARD_DURATION_MODE -> config.wildcardDurationMode();
        };
    }

    public static ConfigSnapshot setDropdownValue(ConfigSnapshot config, DropdownField field, String value) {
        ModConfig copy = config.toConfig();
        switch (field) {
            case RUNNER_VICTORY_TYPE -> copy.runnerVictoryType = value;
            case HUNTER_VICTORY_TYPE -> copy.hunterVictoryType = value;
            case HUNTER_RESPAWN_MODE -> copy.hunterRespawnMode = value;
            case RUNNER_RESPAWN_MODE -> copy.runnerRespawnMode = value;
            case RUNNER_TEAM_LOSS_MODE -> copy.runnerTeamLossMode = value;
            case TARGET_DIMENSION -> copy.targetDimension = value;
            case WILDCARD_INTERVAL_MODE -> copy.wildcardIntervalMode = value;
            case WILDCARD_DURATION_MODE -> copy.wildcardDurationMode = value;
        }
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    public static boolean getToggle(ConfigSnapshot config, ToggleField field) {
        return config.enabledWildcards().getOrDefault(field.id, Boolean.TRUE);
    }

    public static ConfigSnapshot setToggle(ConfigSnapshot config, ToggleField field, boolean value) {
        ModConfig copy = config.toConfig();
        copy.enabledWildcards.put(field.id, value);
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    public static boolean getBoolean(ConfigSnapshot config, BooleanField field) {
        return switch (field) {
            case HUNTER_PREPARE_BOUNDARY_ENABLED -> config.hunterPrepareBoundaryEnabled();
            case ENVIRONMENT_KILLS_ENABLED -> config.environmentKillsEnabled();
            case RUNNER_DEATH_NO_DROPS -> config.runnerDeathNoDrops();
            case HUNTER_DEATH_NO_DROPS -> config.hunterDeathNoDrops();
            case PIGLIN_PEARL_BOOST_ENABLED -> config.piglinPearlBoostEnabled();
            case BLAZE_ROD_CHANCE_ENABLED -> config.blazeRodChanceEnabled();
            case RANDOM_RESPAWN_ENABLED -> config.randomRespawnEnabled();
            case LOCATOR_BAR_TEAM_ONLY -> config.locatorBarTeamOnly();
            case SURVIVE_BORDER_ENABLED -> config.surviveBorderEnabled();
        };
    }

    public static ConfigSnapshot setBoolean(ConfigSnapshot config, BooleanField field, boolean value) {
        ModConfig copy = config.toConfig();
        switch (field) {
            case HUNTER_PREPARE_BOUNDARY_ENABLED -> copy.hunterPrepareBoundaryEnabled = value;
            case ENVIRONMENT_KILLS_ENABLED -> copy.environmentKillsEnabled = value;
            case RUNNER_DEATH_NO_DROPS -> copy.runnerDeathNoDrops = value;
            case HUNTER_DEATH_NO_DROPS -> copy.hunterDeathNoDrops = value;
            case PIGLIN_PEARL_BOOST_ENABLED -> copy.piglinPearlBoostEnabled = value;
            case BLAZE_ROD_CHANCE_ENABLED -> copy.blazeRodChanceEnabled = value;
            case RANDOM_RESPAWN_ENABLED -> copy.randomRespawnEnabled = value;
            case LOCATOR_BAR_TEAM_ONLY -> copy.locatorBarTeamOnly = value;
            case SURVIVE_BORDER_ENABLED -> copy.surviveBorderEnabled = value;
        }
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    public static boolean multiplier(NumberField field) {
        return field.name().contains("MULTIPLIER") || field == NumberField.HUNTER_SPEED_PERCENT || field == NumberField.RUNNER_SPEED_PERCENT;
    }
    public static String numberUnit(NumberField field) {
        return multiplier(field) ? "×" : field.name().endsWith("SECONDS") ? "m:ss" : field.unit;
    }
    public static String formatNumber(NumberField field, int n) {
        if (multiplier(field)) return java.math.BigDecimal.valueOf(n, 2).stripTrailingZeros().toPlainString();
        if (field.name().endsWith("SECONDS")) return n / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", n % 60);
        return Integer.toString(n);
    }
    public static int parseNumber(NumberField field, String raw) {
        raw = java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFKC).trim();
        if (field.name().endsWith("SECONDS") && raw.endsWith("秒")) raw = raw.substring(0, raw.length() - 1).trim();
        try {
            if (multiplier(field)) return new java.math.BigDecimal(raw.replace("×", "")).multiply(java.math.BigDecimal.valueOf(100)).intValueExact();
            if (field.name().endsWith("SECONDS") && raw.contains(":")) {
                String[] parts = raw.split(":", -1);
                if (parts.length != 2) throw new NumberFormatException();
                int m = Integer.parseInt(parts[0]), sec = Integer.parseInt(parts[1]);
                if (m < 0 || sec < 0 || sec > 59) throw new NumberFormatException();
                return Math.addExact(Math.multiplyExact(m, 60), sec);
            }
            return Integer.parseInt(raw);
        } catch (ArithmeticException ex) { throw new NumberFormatException(); }
    }
    public static int maxNumber(NumberField f) {
        return switch(f) {
            case PIGLIN_PEARL_CHANCE_PERCENT, BLAZE_ROD_CHANCE_PERCENT -> 100;
            case HUNTER_DAMAGE_MULTIPLIER_PERCENT -> 1000;
            case HUNTER_SPEED_PERCENT, RUNNER_SPEED_PERCENT -> 500;
            case HUNTER_HIT_CREDIT_SECONDS -> 120;
            case HUNTER_RESPAWN_PENALTY_SECONDS -> 60;
            case RUNNER_RESPAWN_DISTANCE, HUNTER_RESPAWN_DISTANCE -> 2000;
            case HUNTER_RESPAWN_RUNNER_CLEARANCE -> 5000;
            case SURVIVE_BORDER_RADIUS -> 30000;
            default -> f.name().endsWith("SECONDS") ? Integer.MAX_VALUE / 20 : Integer.MAX_VALUE;
        };
    }
    public static boolean validNumber(NumberField f, String raw) {
        try { int n = parseNumber(f, raw.trim()); return n >= f.minValue && n <= maxNumber(f); }
        catch (NumberFormatException ex) { return false; }
    }
    public static boolean validItem(String raw) {
        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(raw.trim());
        return id != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id) && !id.toString().equals("minecraft:air");
    }
    public enum NumberField {
        PREPARING_SECONDS(key("config.number.preparing_seconds"), key("unit.seconds"), 1, false),
        HUNTER_RESPAWN_SECONDS(key("config.number.hunter_respawn_seconds"), key("unit.seconds"), 1, true),
        WILDCARD_INTERVAL_SECONDS(key("config.number.wildcard_interval_seconds"), key("unit.seconds"), 1, true),
        WILDCARD_DURATION_SECONDS(key("config.number.wildcard_duration_seconds"), key("unit.seconds"), 1, true),
        WILDCARD_INTERVAL_MIN_SECONDS(key("config.number.wildcard_interval_min_seconds"), key("unit.seconds"), 1, true),
        WILDCARD_INTERVAL_MAX_SECONDS(key("config.number.wildcard_interval_max_seconds"), key("unit.seconds"), 1, true),
        WILDCARD_DURATION_MIN_SECONDS(key("config.number.wildcard_duration_min_seconds"), key("unit.seconds"), 1, true),
        WILDCARD_DURATION_MAX_SECONDS(key("config.number.wildcard_duration_max_seconds"), key("unit.seconds"), 1, true),
        HUNTER_RADAR_WARNING_DISTANCE(key("config.number.hunter_radar_warning_distance"), key("unit.blocks"), 1, true),
        SPACE_SHIFT_INTERVAL_SECONDS(key("config.number.space_shift_interval_seconds"), key("unit.seconds"), 1, true),
        SUPPLY_DROP_INTERVAL_SECONDS(key("config.number.supply_drop_interval_seconds"), key("unit.seconds"), 1, true),
        BLOCK_DECAY_SECONDS(key("config.number.block_decay_seconds"), key("unit.seconds"), 1, true),
        PEARL_FRENZY_MAX_PEARLS(key("config.number.pearl_frenzy_max_pearls"), key("unit.items"), 1, true),
        PEARL_FRENZY_INTERVAL_SECONDS(key("config.number.pearl_frenzy_interval_seconds"), key("unit.seconds"), 1, true),
        WIND_CHARGE_BRAWL_INTERVAL_SECONDS(key("config.number.wind_charge_brawl_interval_seconds"), key("unit.seconds"), 1, true),
        WIND_CHARGE_EXPLOSION_MULTIPLIER_PERCENT(key("config.number.wind_charge_explosion_multiplier_percent"), key("unit.percent"), 1, true),
        BACKROOMS_DURATION_SECONDS(key("config.number.backrooms_duration_seconds"), key("unit.seconds"), 1, true),
        HUNTER_PREPARE_BOUNDARY_RADIUS(key("config.number.hunter_prepare_boundary_radius"), key("unit.blocks"), 1, false),
        PIGLIN_PEARL_CHANCE_PERCENT(key("config.number.piglin_pearl_chance_percent"), key("unit.percent"), 0, true),
        BLAZE_ROD_CHANCE_PERCENT(key("config.number.blaze_rod_chance_percent"), key("unit.percent"), 0, true),
        HUNTER_DAMAGE_MULTIPLIER_PERCENT(key("config.number.hunter_damage_multiplier_percent"), key("unit.percent"), 1, true),
        HUNTER_SPEED_PERCENT(key("config.number.hunter_speed_percent"), key("unit.percent"), 10, true),
        RUNNER_SPEED_PERCENT(key("config.number.runner_speed_percent"), key("unit.percent"), 10, true),
        HUNTER_HIT_CREDIT_SECONDS(key("config.number.hunter_hit_credit_seconds"), key("unit.seconds"), 0, true),
        ENVIRONMENT_DEATHS_PER_KILL(key("config.number.environment_deaths_per_kill"), key("unit.times"), 1, true),
        RUNNER_RESPAWN_DISTANCE(key("config.number.runner_respawn_distance"), key("unit.blocks"), 16, true),
        HUNTER_RESPAWN_DISTANCE(key("config.number.hunter_respawn_distance"), key("unit.blocks"), 16, true),
        HUNTER_RESPAWN_RUNNER_CLEARANCE(key("config.number.hunter_respawn_runner_clearance"), key("unit.blocks"), 0, true),
        HUNTER_RESPAWN_PENALTY_SECONDS(key("config.number.hunter_respawn_penalty_seconds"), key("unit.seconds"), 0, true),
        SURVIVE_TIME_SECONDS(key("config.number.survive_time_seconds"), key("unit.seconds"), 1, true),
        SURVIVE_BORDER_RADIUS(key("config.number.survive_border_radius"), key("unit.blocks"), 32, false),
        TARGET_X(key("config.number.target_x"), "", Integer.MIN_VALUE, true),
        TARGET_Y(key("config.number.target_y"), "", Integer.MIN_VALUE, true),
        TARGET_Z(key("config.number.target_z"), "", Integer.MIN_VALUE, true),
        TARGET_RADIUS(key("config.number.target_radius"), key("unit.blocks"), 1, true),
        TARGET_ITEM_COUNT(key("config.number.target_item_count"), key("unit.items"), 1, true),
        HUNTER_LIVES(key("config.number.hunter_lives"), key("unit.lives"), 0, false),
        RUNNER_LIVES(key("config.number.runner_lives"), key("unit.lives"), 1, false),
        RUNNER_RESPAWN_SECONDS(key("config.number.runner_respawn_seconds"), key("unit.seconds"), 1, true),
        HUNTER_RUNNER_KILL_TARGET(key("config.number.hunter_runner_kill_target"), key("unit.times"), 1, true);

        public final String label;
        public final String unit;
        public final int minValue;
        public final boolean live;

        NumberField(String label, String unit, int minValue, boolean live) {
            this.label = label;
            this.unit = unit;
            this.minValue = minValue;
            this.live = live;
        }

        public boolean allowsNegative() {
            return minValue < 0;
        }
    }

    public enum StringField {
        TARGET_ITEM_ID(key("config.string.target_item_id"), 128, true);

        public final String label;
        public final int maxLength;
        public final boolean live;

        StringField(String label, int maxLength, boolean live) {
            this.label = label;
            this.maxLength = maxLength;
            this.live = live;
        }
    }

    public enum DropdownField {
        WILDCARD_INTERVAL_MODE(key("config.dropdown.wildcard_interval_mode"), true, List.of(
                option("FIXED", key("config.timing_mode.fixed")),
                option("RANDOM", key("config.timing_mode.random"))
        )),
        WILDCARD_DURATION_MODE(key("config.dropdown.wildcard_duration_mode"), true, List.of(
                option("FIXED", key("config.timing_mode.fixed")),
                option("RANDOM", key("config.timing_mode.random"))
        )),
        RUNNER_VICTORY_TYPE(key("config.dropdown.runner_victory_type"), false, List.of(
                option(RunnerVictoryType.DRAGON.name(), RunnerVictoryType.DRAGON.getDisplayName()),
                option(RunnerVictoryType.SURVIVE_TIME.name(), RunnerVictoryType.SURVIVE_TIME.getDisplayName()),
                option(RunnerVictoryType.REACH_LOCATION.name(), RunnerVictoryType.REACH_LOCATION.getDisplayName()),
                option(RunnerVictoryType.COLLECT_ITEM.name(), RunnerVictoryType.COLLECT_ITEM.getDisplayName())
        )),
        HUNTER_VICTORY_TYPE(key("config.dropdown.hunter_victory_type"), false, List.of(
                option(HunterVictoryType.RUNNERS_OUT.name(), HunterVictoryType.RUNNERS_OUT.getDisplayName()),
                option(HunterVictoryType.RUNNER_KILL_COUNT.name(), HunterVictoryType.RUNNER_KILL_COUNT.getDisplayName())
        )),
        HUNTER_RESPAWN_MODE(key("config.dropdown.hunter_respawn_mode"), false, List.of(
                option(RespawnMode.INFINITE.name(), key("config.respawn_mode.infinite")),
                option(RespawnMode.LIMITED_LIVES.name(), key("config.respawn_mode.limited_lives")),
                option(RespawnMode.NO_RESPAWN.name(), key("config.respawn_mode.no_respawn"))
        )),
        RUNNER_RESPAWN_MODE(key("config.dropdown.runner_respawn_mode"), false, List.of(
                option(RespawnMode.INFINITE.name(), key("config.respawn_mode.infinite")),
                option(RespawnMode.LIMITED_LIVES.name(), key("config.respawn_mode.limited_lives")),
                option(RespawnMode.NO_RESPAWN.name(), key("config.respawn_mode.no_respawn"))
        )),
        RUNNER_TEAM_LOSS_MODE(key("config.dropdown.runner_team_loss_mode"), true, List.of(
                option(RunnerTeamLossMode.ANY_RUNNER_OUT.name(), key("config.runner_team_loss.any_runner_out")),
                option(RunnerTeamLossMode.ALL_RUNNERS_OUT.name(), key("config.runner_team_loss.all_runners_out"))
        )),
        TARGET_DIMENSION(key("config.dropdown.target_dimension"), true, List.of(
                option("minecraft:overworld", key("config.dimension.overworld")),
                option("minecraft:the_nether", key("config.dimension.the_nether")),
                option("minecraft:the_end", key("config.dimension.the_end"))
        ));

        public final String label;
        public final boolean live;
        public final List<DropdownWidget.Option> options;

        DropdownField(String label, boolean live, List<DropdownWidget.Option> options) {
            this.label = label;
            this.live = live;
            this.options = options;
        }
    }

    public static DropdownWidget.Option option(String value, String displayName) {
        return new DropdownWidget.Option(value, displayName);
    }

    public enum BooleanField {
        HUNTER_PREPARE_BOUNDARY_ENABLED(key("config.boolean.hunter_prepare_boundary_enabled"), false),
        RUNNER_DEATH_NO_DROPS(key("config.boolean.runner_death_no_drops"), true),
        HUNTER_DEATH_NO_DROPS(key("config.boolean.hunter_death_no_drops"), true),
        PIGLIN_PEARL_BOOST_ENABLED(key("config.boolean.piglin_pearl_boost_enabled"), true),
        BLAZE_ROD_CHANCE_ENABLED(key("config.boolean.blaze_rod_chance_enabled"), true),
        RANDOM_RESPAWN_ENABLED(key("config.boolean.random_respawn_enabled"), true),
        LOCATOR_BAR_TEAM_ONLY(key("config.boolean.locator_bar_team_only"), true),
        SURVIVE_BORDER_ENABLED(key("config.boolean.survive_border_enabled"), false),
        ENVIRONMENT_KILLS_ENABLED(key("config.boolean.environment_kills_enabled"), true);

        public final String label;
        public final boolean live;

        BooleanField(String label, boolean live) {
            this.label = label;
            this.live = live;
        }
    }

    public enum WildcardCategory {
        COMBAT(key("screen.wildcard_category.combat")),
        MOBILITY(key("screen.wildcard_category.mobility")),
        VISION(key("screen.wildcard_category.vision")),
        WORLD(key("screen.wildcard_category.world"));

        public final String label;

        WildcardCategory(String label) {
            this.label = label;
        }
    }

    /** Same order as {@link WildcardIds#ALL}, grouped by category for the toggle page. */
    public enum ToggleField {
        BACKSTAB(WildcardIds.BACKSTAB, WildcardCategory.COMBAT),
        VAMPIRE(WildcardIds.VAMPIRE, WildcardCategory.COMBAT),
        BLOOD_RAGE(WildcardIds.BLOOD_RAGE, WildcardCategory.COMBAT),
        WEAPON_OVERHEAT(WildcardIds.WEAPON_OVERHEAT, WildcardCategory.COMBAT),
        STAY_AWAY(WildcardIds.STAY_AWAY, WildcardCategory.COMBAT),
        FRAGILE(WildcardIds.FRAGILE, WildcardCategory.COMBAT),
        EXPLOSIVE_DEATH(WildcardIds.EXPLOSIVE_DEATH, WildcardCategory.COMBAT),
        KEY_SCRAMBLE(WildcardIds.KEY_SCRAMBLE, WildcardCategory.COMBAT),
        FLASH(WildcardIds.FLASH, WildcardCategory.MOBILITY),
        SHADOW_STEP(WildcardIds.SHADOW_STEP, WildcardCategory.MOBILITY),
        HURT_TELEPORT(WildcardIds.HURT_TELEPORT, WildcardCategory.MOBILITY),
        SPACE_SHIFT(WildcardIds.SPACE_SHIFT, WildcardCategory.MOBILITY),
        PORTAL(WildcardIds.PORTAL, WildcardCategory.MOBILITY),
        PEARL_FRENZY(WildcardIds.PEARL_FRENZY, WildcardCategory.MOBILITY),
        WIND_CHARGE_BRAWL(WildcardIds.WIND_CHARGE_BRAWL, WildcardCategory.MOBILITY),
        LIGHT_LOAD(WildcardIds.LIGHT_LOAD, WildcardCategory.MOBILITY),
        HUNGER_CHASE(WildcardIds.HUNGER_CHASE, WildcardCategory.MOBILITY),
        STILL_GLOW(WildcardIds.STILL_GLOW, WildcardCategory.VISION),
        SNEAK_FREEZE(WildcardIds.SNEAK_FREEZE, WildcardCategory.VISION),
        HUNTER_RADAR(WildcardIds.HUNTER_RADAR, WildcardCategory.VISION),
        WHO_ARE_YOU(WildcardIds.WHO_ARE_YOU, WildcardCategory.VISION),
        TINY_PLAYERS(WildcardIds.TINY_PLAYERS, WildcardCategory.VISION),
        WORLD_TILT(WildcardIds.WORLD_TILT, WildcardCategory.VISION),
        SUPPLY_DROP(WildcardIds.SUPPLY_DROP, WildcardCategory.WORLD),
        DROP_BOMB(WildcardIds.DROP_BOMB, WildcardCategory.WORLD),
        CHAIN_MINING(WildcardIds.CHAIN_MINING, WildcardCategory.WORLD),
        BLOCK_DECAY(WildcardIds.BLOCK_DECAY, WildcardCategory.WORLD),
        BACKROOMS(WildcardIds.BACKROOMS, WildcardCategory.WORLD);

        public final String id;
        public final String label;
        public final String description;
        public final WildcardCategory category;

        ToggleField(String id, WildcardCategory category) {
            this.id = id;
            this.label = HunterWildcardText.wildcardNameKey(id);
            this.description = HunterWildcardText.wildcardDescriptionKey(id);
            this.category = category;
        }

        public static ToggleField[] inCategory(WildcardCategory category) {
            List<ToggleField> fields = new ArrayList<>();
            for (ToggleField field : values()) {
                if (field.category == category) {
                    fields.add(field);
                }
            }
            return fields.toArray(new ToggleField[0]);
        }
    }

}
