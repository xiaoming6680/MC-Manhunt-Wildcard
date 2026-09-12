package com.xiaoming.hunterwildcard.wildcard;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.ui.BossBarManager;
import com.xiaoming.hunterwildcard.ui.MessageManager;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class WildcardManager {
    private static final int WILDCARD_DRAW_DELAY_TICKS = 100;

    private final List<WildcardRule> registeredRules = WildcardRuleRegistry.createRegisteredRules();
    private final BossBarManager bossBarManager;
    private final MessageManager messageManager;

    private WildcardRule activeRule;
    private WildcardRule pendingRule;
    private Class<?> lastDrawnRuleClass;
    private final Map<Class<?>, Integer> drawCounts = new HashMap<>();
    private int activeRuleRemainingTicks;
    private int activeRuleDurationTicks;
    private int pendingRuleDrawTicks;
    private int ticksUntilNextWildcard = -1;
    private boolean stopRequested;

    public WildcardManager(BossBarManager bossBarManager, MessageManager messageManager) {
        this.bossBarManager = bossBarManager;
        this.messageManager = messageManager;
    }

    public void reset() {
        drawCounts.clear();
        lastDrawnRuleClass = null;
        activeRule = null;
        pendingRule = null;
        activeRuleRemainingTicks = 0;
        pendingRuleDrawTicks = 0;
        ticksUntilNextWildcard = -1;
        stopRequested = false;
    }

    /**
     * Lets a rule end itself from inside its own onTick. Stopping synchronously there would null out
     * activeRule while tick() still uses it, which crashed the server; the stop is applied after onTick.
     */
    public void requestStop() {
        stopRequested = true;
    }

    public void tick(GameContext context) {
        if (pendingRule != null) {
            pendingRuleDrawTicks--;
            if (pendingRuleDrawTicks <= 0) {
                activatePendingRule(context);
            }
            return;
        }

        if (activeRule != null) {
            activeRuleRemainingTicks--;
            activeRule.onTick(context, activeRuleRemainingTicks);
            if (activeRule == null) {
                // A rule stopped itself synchronously anyway; nothing left to tick.
                stopRequested = false;
                return;
            }
            bossBarManager.updateWildcardBar(context, activeRule.getName(), activeRuleRemainingTicks, Math.max(1, activeRuleDurationTicks));

            if (activeRuleRemainingTicks <= 0 || stopRequested) {
                stopRequested = false;
                stopActiveRuleInternal(context, true);
            }
            return;
        }
        stopRequested = false;

        if (ticksUntilNextWildcard < 0) {
            ticksUntilNextWildcard = rollInterval(context);
        }

        ticksUntilNextWildcard--;
        if (ticksUntilNextWildcard <= 0) {
            startRandomRule(context);
        }
    }

    public void onPlayerDeath(GameContext context, ServerPlayer player) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onPlayerDeath(context, player);
        }
    }

    public void onEntityKilled(GameContext context, ServerPlayer killer, LivingEntity killed) {
        if (shouldForwardRuleEvent(context, killer)) {
            activeRule.onEntityKilled(context, killer, killed);
        }
    }

    public void onPlayerAttack(GameContext context, ServerPlayer player, Entity target) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onPlayerAttack(context, player, target);
        }
    }

    public void onPlayerDamaged(GameContext context, ServerPlayer player, DamageSource source, float damageTaken) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onPlayerDamaged(context, player, source, damageTaken);
        }
    }

    public void onPlayerAteFood(GameContext context, ServerPlayer player, ItemStack eatenStack) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onPlayerAteFood(context, player, eatenStack);
        }
    }

    public void onItemUse(GameContext context, ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onItemUse(context, player, hand, stack);
        }
    }

    public void onBlockPlaced(GameContext context, ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onBlockPlaced(context, player, world, pos, state);
        }
    }

    public void onBlockBroken(GameContext context, ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onBlockBroken(context, player, world, pos, state);
        }
    }

    public void onDamageDealt(GameContext context, ServerPlayer attacker, LivingEntity victim, float damageDealt) {
        if (shouldForwardRuleEvent(context, attacker)) {
            activeRule.onDamageDealt(context, attacker, victim, damageDealt);
        }
    }

    public void onItemDropped(GameContext context, ServerPlayer player, ItemEntity item) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onItemDropped(context, player, item);
        }
    }

    /** Applies the active rule's damage rescaling when the victim or the attacker takes part in the round. */
    public float modifyDamage(GameContext context, LivingEntity victim, DamageSource source, float amount) {
        if (activeRule == null) {
            return amount;
        }
        boolean involved = victim instanceof ServerPlayer victimPlayer && isParticipant(context, victimPlayer);
        if (!involved && source.getEntity() instanceof ServerPlayer attacker && isParticipant(context, attacker)) {
            involved = true;
        }
        return involved ? activeRule.modifyDamage(context, victim, source, amount) : amount;
    }

    public void clear(GameContext context) {
        if (activeRule != null) {
            activeRule.onStop(context);
        }

        activeRule = null;
        pendingRule = null;
        activeRuleRemainingTicks = 0;
        pendingRuleDrawTicks = 0;
        ticksUntilNextWildcard = -1;
        bossBarManager.clearWildcardBar();
        HunterWildcardPackets.clearWildcardIntro(context);
    }

    public void onConfigChanged(ModConfig config) {
        if (ticksUntilNextWildcard > config.getMaxWildcardIntervalTicks()) {
            ticksUntilNextWildcard = config.getMaxWildcardIntervalTicks();
        }
    }

    public boolean rollNow(GameContext context) {
        if (activeRule != null) {
            stopActiveRule(context);
        }
        if (pendingRule != null) {
            cancelPendingRule(context, false);
        }

        return startRandomRule(context);
    }

    public boolean startRuleByName(GameContext context, String ruleName) {
        if (activeRule != null) {
            stopActiveRule(context);
        }
        if (pendingRule != null) {
            cancelPendingRule(context, false);
        }

        for (WildcardRule rule : registeredRules) {
            if (rule.getName().equals(ruleName) && context.getConfig().isWildcardEnabled(rule.getName())) {
                return startRule(context, rule);
            }
        }

        messageManager.toParticipants(context, HunterWildcardText.translatable("msg.wildcard.unavailable_or_disabled", ruleName));
        return false;
    }

    public boolean stopActiveRule(GameContext context) {
        if (pendingRule != null) {
            cancelPendingRule(context, true);
            return true;
        }

        if (activeRule == null) {
            return false;
        }

        stopActiveRuleInternal(context, true);
        return true;
    }

    public WildcardRule getActiveRule() {
        return activeRule;
    }

    public boolean hasRuleInProgress() {
        return activeRule != null || pendingRule != null;
    }

    public String getActiveRuleName() {
        if (activeRule != null) {
            return activeRule.getName();
        }
        return pendingRule == null ? null : pendingRule.getName();
    }

    public int getActiveRemainingTicks() {
        return activeRule == null ? -1 : Math.max(0, activeRuleRemainingTicks);
    }

    public int getTicksUntilNextWildcard() {
        return activeRule == null && pendingRule == null ? ticksUntilNextWildcard : -1;
    }

    public List<WildcardStatus> getRuleStatuses(ModConfig config) {
        List<WildcardStatus> statuses = new ArrayList<>();
        for (WildcardRule rule : registeredRules) {
            statuses.add(new WildcardStatus(rule.getName(), config.isWildcardEnabled(rule.getName())));
        }
        return statuses;
    }

    private boolean startRandomRule(GameContext context) {
        List<WildcardRule> candidates = getEnabledRules(context.getConfig());
        if (lastDrawnRuleClass != null && candidates.size() > 1) {
            candidates.removeIf(rule -> rule.getClass() == lastDrawnRuleClass);
        }

        if (candidates.isEmpty()) {
            ticksUntilNextWildcard = rollInterval(context);
            messageManager.toParticipants(context, HunterWildcardText.translatable("msg.wildcard.none_available"));
            return false;
        }

        return startRule(context, pickWeighted(candidates, context.getRandom()));
    }

    /** Rules drawn earlier this round weigh less (1 / (1 + times drawn)), so fresh ones surface first. */
    private WildcardRule pickWeighted(List<WildcardRule> candidates, Random random) {
        double total = 0.0;
        double[] weights = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            weights[i] = 1.0 / (1 + drawCounts.getOrDefault(candidates.get(i).getClass(), 0));
            total += weights[i];
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights[i];
            if (roll <= 0.0) {
                return candidates.get(i);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private static int rollInterval(GameContext context) {
        return context.getConfig().rollWildcardIntervalTicks(context.getRandom());
    }

    private boolean startRule(GameContext context, WildcardRule rule) {
        pendingRule = rule;
        lastDrawnRuleClass = pendingRule.getClass();
        drawCounts.merge(lastDrawnRuleClass, 1, Integer::sum);
        pendingRuleDrawTicks = WILDCARD_DRAW_DELAY_TICKS;
        ticksUntilNextWildcard = -1;

        HunterWildcardPackets.sendWildcardDraw(context, pendingRule.getName());
        return true;
    }

    private void activatePendingRule(GameContext context) {
        if (pendingRule == null) {
            return;
        }

        activeRule = pendingRule;
        pendingRule = null;
        pendingRuleDrawTicks = 0;
        activeRuleDurationTicks = Math.max(20, activeRule.getDurationTicks(context.getConfig(), context.getRandom()));
        activeRuleRemainingTicks = activeRuleDurationTicks;

        activeRule.onStart(context);
        bossBarManager.updateWildcardBar(context, activeRule.getName(), activeRuleRemainingTicks, activeRuleDurationTicks);
        HunterWildcardPackets.sendWildcardIntro(context, activeRule.getName(), activeRule.getDescriptionKey());
        messageManager.toParticipants(context, HunterWildcardText.translatable("msg.wildcard.triggered", activeRule.getDisplayName()));
        HunterWildcardPackets.syncAll(context.getServer());
    }

    private void cancelPendingRule(GameContext context, boolean resetInterval) {
        if (pendingRule != null) {
            messageManager.toParticipants(context, HunterWildcardText.translatable("msg.wildcard.draw_cancelled", pendingRule.getDisplayName()));
        }

        pendingRule = null;
        pendingRuleDrawTicks = 0;
        ticksUntilNextWildcard = resetInterval ? rollInterval(context) : -1;
        bossBarManager.clearWildcardBar();
    }

    private void stopActiveRuleInternal(GameContext context, boolean resetInterval) {
        if (activeRule != null) {
            messageManager.toParticipants(context, HunterWildcardText.translatable("msg.wildcard.ended", activeRule.getDisplayName()));
            activeRule.onStop(context);
        }

        activeRule = null;
        activeRuleRemainingTicks = 0;
        ticksUntilNextWildcard = resetInterval ? rollInterval(context) : -1;
        bossBarManager.clearWildcardBar();
        HunterWildcardPackets.clearWildcardIntro(context);
        HunterWildcardPackets.syncAll(context.getServer());
    }

    private List<WildcardRule> getEnabledRules(ModConfig config) {
        List<WildcardRule> enabledRules = new ArrayList<>();
        for (WildcardRule rule : registeredRules) {
            if (config.isWildcardEnabled(rule.getName())) {
                enabledRules.add(rule);
            }
        }
        return enabledRules;
    }

    private boolean shouldForwardRuleEvent(GameContext context, ServerPlayer player) {
        return activeRule != null && isParticipant(context, player);
    }

    private boolean isParticipant(GameContext context, ServerPlayer player) {
        UUID playerId = player.getUUID();
        for (ServerPlayer participant : context.getParticipants()) {
            if (participant.getUUID().equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    public record WildcardStatus(String name, boolean enabled) {
    }
}
