package com.xiaoming.hunterwildcard.wildcard;

import java.util.List;

/**
 * Snake-case ids of every wildcard, in display order. Config, network sync and the client screen key
 * off this list, so adding a wildcard means: a rule class, an entry here, an icon and lang keys.
 */
public final class WildcardIds {
    // Combat
    public static final String BACKSTAB = "backstab";
    public static final String VAMPIRE = "vampire";
    public static final String BLOOD_RAGE = "blood_rage";
    public static final String WEAPON_OVERHEAT = "weapon_overheat";
    public static final String STAY_AWAY = "stay_away";
    public static final String FRAGILE = "fragile";
    public static final String EXPLOSIVE_DEATH = "explosive_death";
    public static final String KEY_SCRAMBLE = "key_scramble";
    // Mobility
    public static final String FLASH = "flash";
    public static final String SHADOW_STEP = "shadow_step";
    public static final String HURT_TELEPORT = "hurt_teleport";
    public static final String SPACE_SHIFT = "space_shift";
    public static final String PORTAL = "portal";
    public static final String PEARL_FRENZY = "pearl_frenzy";
    public static final String WIND_CHARGE_BRAWL = "wind_charge_brawl";
    public static final String LIGHT_LOAD = "light_load";
    public static final String HUNGER_CHASE = "hunger_chase";
    // Vision
    public static final String STILL_GLOW = "still_glow";
    public static final String SNEAK_FREEZE = "sneak_freeze";
    public static final String HUNTER_RADAR = "hunter_radar";
    public static final String WHO_ARE_YOU = "who_are_you";
    public static final String TINY_PLAYERS = "tiny_players";
    public static final String WORLD_TILT = "world_tilt";
    // World
    public static final String SUPPLY_DROP = "supply_drop";
    public static final String DROP_BOMB = "drop_bomb";
    public static final String CHAIN_MINING = "chain_mining";
    public static final String BLOCK_DECAY = "block_decay";
    public static final String BACKROOMS = "backrooms";

    public static final List<String> ALL = List.of(
            BACKSTAB, VAMPIRE, BLOOD_RAGE, WEAPON_OVERHEAT, STAY_AWAY, FRAGILE, EXPLOSIVE_DEATH, KEY_SCRAMBLE,
            FLASH, SHADOW_STEP, HURT_TELEPORT, SPACE_SHIFT, PORTAL, PEARL_FRENZY, WIND_CHARGE_BRAWL, LIGHT_LOAD, HUNGER_CHASE,
            STILL_GLOW, SNEAK_FREEZE, HUNTER_RADAR, WHO_ARE_YOU, TINY_PLAYERS, WORLD_TILT,
            SUPPLY_DROP, DROP_BOMB, CHAIN_MINING, BLOCK_DECAY, BACKROOMS
    );

    private WildcardIds() {
    }
}
