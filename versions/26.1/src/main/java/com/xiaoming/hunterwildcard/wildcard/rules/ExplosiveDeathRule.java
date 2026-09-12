package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class ExplosiveDeathRule implements WildcardRule {
    private static final float EXPLOSION_POWER = 2.5F;

    @Override
    public void onPlayerDeath(GameContext context, ServerPlayer player) {
        explodeAt(player);
    }

    @Override
    public void onEntityKilled(GameContext context, ServerPlayer killer, LivingEntity killed) {
        explodeAt(killed);
    }

    private void explodeAt(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel world) {
            world.explode(null, entity.getX(), entity.getY(), entity.getZ(), EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
        }
    }
}
