package com.xiaoming.hunterwildcard.client.hud;

import com.xiaoming.hunterwildcard.wildcard.WildcardIds;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class WildcardIcons {
    private WildcardIcons() {
    }

    public static ItemStack iconFor(String wildcardId) {
        if (wildcardId == null) {
            return new ItemStack(Items.NETHER_STAR);
        }

        return switch (wildcardId) {
            case WildcardIds.BACKSTAB -> new ItemStack(Items.NETHERITE_SWORD);
            case WildcardIds.VAMPIRE -> new ItemStack(Items.REDSTONE);
            case WildcardIds.BLOOD_RAGE -> new ItemStack(Items.FERMENTED_SPIDER_EYE);
            case WildcardIds.WEAPON_OVERHEAT -> new ItemStack(Items.IRON_SWORD);
            case WildcardIds.STAY_AWAY -> new ItemStack(Items.GOLDEN_SWORD);
            case WildcardIds.FRAGILE -> new ItemStack(Items.GLASS);
            case WildcardIds.EXPLOSIVE_DEATH -> new ItemStack(Items.TNT);
            case WildcardIds.KEY_SCRAMBLE -> new ItemStack(Items.TRIPWIRE_HOOK);
            case WildcardIds.FLASH -> new ItemStack(Items.SUGAR);
            case WildcardIds.SHADOW_STEP -> new ItemStack(Items.ENDER_EYE);
            case WildcardIds.HURT_TELEPORT -> new ItemStack(Items.CHORUS_FRUIT);
            case WildcardIds.SPACE_SHIFT -> new ItemStack(Items.RECOVERY_COMPASS);
            case WildcardIds.PORTAL -> new ItemStack(Items.END_PORTAL_FRAME);
            case WildcardIds.PEARL_FRENZY -> new ItemStack(Items.ENDER_PEARL);
            case WildcardIds.WIND_CHARGE_BRAWL -> new ItemStack(Items.WIND_CHARGE);
            case WildcardIds.LIGHT_LOAD -> new ItemStack(Items.LEATHER_BOOTS);
            case WildcardIds.HUNGER_CHASE -> new ItemStack(Items.COOKED_BEEF);
            case WildcardIds.STILL_GLOW -> new ItemStack(Items.GLOWSTONE_DUST);
            case WildcardIds.SNEAK_FREEZE -> new ItemStack(Items.PACKED_ICE);
            case WildcardIds.HUNTER_RADAR -> new ItemStack(Items.SPYGLASS);
            case WildcardIds.WHO_ARE_YOU -> new ItemStack(Items.PLAYER_HEAD);
            case WildcardIds.TINY_PLAYERS -> new ItemStack(Items.RABBIT_FOOT);
            case WildcardIds.WORLD_TILT -> new ItemStack(Items.COMPASS);
            case WildcardIds.SUPPLY_DROP -> new ItemStack(Items.CHEST);
            case WildcardIds.DROP_BOMB -> new ItemStack(Items.FIRE_CHARGE);
            case WildcardIds.CHAIN_MINING -> new ItemStack(Items.DIAMOND_PICKAXE);
            case WildcardIds.BLOCK_DECAY -> new ItemStack(Items.CRACKED_STONE_BRICKS);
            case WildcardIds.BACKROOMS -> new ItemStack(Items.YELLOW_WOOL);
            default -> new ItemStack(Items.NETHER_STAR);
        };
    }
}
