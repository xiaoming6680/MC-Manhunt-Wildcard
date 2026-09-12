package com.xiaoming.hunterwildcard.wildcard;

import com.xiaoming.hunterwildcard.wildcard.rules.BackroomsRule;
import com.xiaoming.hunterwildcard.wildcard.rules.BackstabRule;
import com.xiaoming.hunterwildcard.wildcard.rules.BlockDecayRule;
import com.xiaoming.hunterwildcard.wildcard.rules.BloodRageRule;
import com.xiaoming.hunterwildcard.wildcard.rules.ChainMiningRule;
import com.xiaoming.hunterwildcard.wildcard.rules.DropBombRule;
import com.xiaoming.hunterwildcard.wildcard.rules.ExplosiveDeathRule;
import com.xiaoming.hunterwildcard.wildcard.rules.FlashRule;
import com.xiaoming.hunterwildcard.wildcard.rules.FragileRule;
import com.xiaoming.hunterwildcard.wildcard.rules.HungerChaseRule;
import com.xiaoming.hunterwildcard.wildcard.rules.HunterRadarRule;
import com.xiaoming.hunterwildcard.wildcard.rules.HurtTeleportRule;
import com.xiaoming.hunterwildcard.wildcard.rules.KeyScrambleRule;
import com.xiaoming.hunterwildcard.wildcard.rules.LightLoadRule;
import com.xiaoming.hunterwildcard.wildcard.rules.PearlFrenzyRule;
import com.xiaoming.hunterwildcard.wildcard.rules.PortalRule;
import com.xiaoming.hunterwildcard.wildcard.rules.ShadowStepRule;
import com.xiaoming.hunterwildcard.wildcard.rules.SneakFreezeRule;
import com.xiaoming.hunterwildcard.wildcard.rules.SpaceShiftRule;
import com.xiaoming.hunterwildcard.wildcard.rules.StayAwayRule;
import com.xiaoming.hunterwildcard.wildcard.rules.StillGlowRule;
import com.xiaoming.hunterwildcard.wildcard.rules.SupplyDropRule;
import com.xiaoming.hunterwildcard.wildcard.rules.TinyPlayersRule;
import com.xiaoming.hunterwildcard.wildcard.rules.VampireRule;
import com.xiaoming.hunterwildcard.wildcard.rules.WeaponOverheatRule;
import com.xiaoming.hunterwildcard.wildcard.rules.WhoAreYouRule;
import com.xiaoming.hunterwildcard.wildcard.rules.WindChargeBrawlRule;
import com.xiaoming.hunterwildcard.wildcard.rules.WorldTiltRule;

import java.util.ArrayList;
import java.util.List;

final class WildcardRuleRegistry {
    private WildcardRuleRegistry() {
    }

    /** Same order as {@link WildcardIds#ALL}; a mismatch fails fast at startup. */
    static List<WildcardRule> createRegisteredRules() {
        List<WildcardRule> rules = List.of(
                new BackstabRule(),
                new VampireRule(),
                new BloodRageRule(),
                new WeaponOverheatRule(),
                new StayAwayRule(),
                new FragileRule(),
                new ExplosiveDeathRule(),
                new KeyScrambleRule(),
                new FlashRule(),
                new ShadowStepRule(),
                new HurtTeleportRule(),
                new SpaceShiftRule(),
                new PortalRule(),
                new PearlFrenzyRule(),
                new WindChargeBrawlRule(),
                new LightLoadRule(),
                new HungerChaseRule(),
                new StillGlowRule(),
                new SneakFreezeRule(),
                new HunterRadarRule(),
                new WhoAreYouRule(),
                new TinyPlayersRule(),
                new WorldTiltRule(),
                new SupplyDropRule(),
                new DropBombRule(),
                new ChainMiningRule(),
                new BlockDecayRule(),
                new BackroomsRule()
        );
        List<String> ids = new ArrayList<>();
        for (WildcardRule rule : rules) {
            ids.add(rule.getName());
        }
        if (!ids.equals(WildcardIds.ALL)) {
            throw new IllegalStateException("Wildcard registry out of sync with WildcardIds: " + ids + " vs " + WildcardIds.ALL);
        }
        return rules;
    }
}
