package com.xiaoming.hunterwildcard.client;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.backrooms.BackroomsDimension;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.BackroomsPhase;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Client half of the Backrooms: the blackout cover, the camera sinking through the floor, the quiet
 * ambience loop and the 6-chunk view distance lock. The server only tells us when to cover.
 */
public final class BackroomsClient {
    public static final SoundEvent AMBIENCE = Registry.register(Registries.SOUND_EVENT,
            Identifier.of(HunterWildcardMod.MOD_ID, "backrooms_ambience"),
            SoundEvent.of(Identifier.of(HunterWildcardMod.MOD_ID, "backrooms_ambience")));

    private static final double SINK_BLOCKS = 3.0D;
    private static final int SINK_TICKS = 10;
    private static final double ARRIVAL_CEILING_BLOCKS = 0.75D;
    private static final int ARRIVAL_HOLD_TICKS = 3;
    private static final int ARRIVAL_EASE_TICKS = 8;
    private static final float AMBIENCE_VOLUME = 0.08F;
    private static final int AMBIENCE_FADE_TICKS = 40;
    private static final int LOCKED_VIEW_DISTANCE = 6;

    private static int holdTicks;
    private static int sinkAge = -1;
    private static int arrivalAge = -1;
    private static boolean wasInBackrooms;
    private static Integer viewDistanceBefore;
    private static AmbienceLoop ambience;

    private BackroomsClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(BackroomsClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset(client));
        HudElementRegistry.addLast(Identifier.of(HunterWildcardMod.MOD_ID, "backrooms_cover"), (context, tickCounter) -> {
            if (holdTicks > 0) {
                context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0xFF000000);
            }
        });
    }

    public static void onPhase(BackroomsPhase phase, int ticks) {
        switch (phase) {
            case FALL -> {
                sinkAge = 0;
                arrivalAge = -1;
            }
            case ENTER, EXIT -> holdTicks = Math.max(holdTicks, ticks);
            case CLEAR -> holdTicks = 0;
        }
    }

    public static boolean isCovered() {
        return holdTicks > 0;
    }

    /** Vertical camera displacement this frame; moves the picture only, never the player. */
    public static double cameraOffset(float tickProgress) {
        if (sinkAge >= 0) {
            float age = Math.min(SINK_TICKS, sinkAge + tickProgress);
            float progress = age / SINK_TICKS;
            return -SINK_BLOCKS * progress * progress;
        }
        if (arrivalAge < 0) {
            return 0.0D;
        }
        float age = arrivalAge + tickProgress;
        if (age <= ARRIVAL_HOLD_TICKS) {
            return ARRIVAL_CEILING_BLOCKS;
        }
        float eased = MathHelper.clamp((age - ARRIVAL_HOLD_TICKS) / ARRIVAL_EASE_TICKS, 0.0F, 1.0F);
        float remaining = 1.0F - eased;
        return ARRIVAL_CEILING_BLOCKS * remaining * remaining * remaining;
    }

    private static void tick(MinecraftClient client) {
        // The cover's clock stops while a loading screen is up, so it measures black the player sees.
        boolean loading = client.currentScreen instanceof LevelLoadingScreen;
        if (!loading && holdTicks > 0) {
            holdTicks--;
        }
        if (sinkAge >= 0 && ++sinkAge > SINK_TICKS + 40) {
            sinkAge = -1;
        }
        if (arrivalAge >= 0 && ++arrivalAge >= ARRIVAL_HOLD_TICKS + ARRIVAL_EASE_TICKS) {
            arrivalAge = -1;
        }

        if (client.world == null || client.player == null) {
            reset(client);
            return;
        }

        boolean inBackrooms = BackroomsDimension.isBackrooms(client.world);
        if (inBackrooms && !wasInBackrooms) {
            sinkAge = -1;
            arrivalAge = 0;
            lockViewDistance(client);
            startAmbience(client);
        } else if (!inBackrooms && wasInBackrooms) {
            restoreViewDistance(client);
            stopAmbience();
        }
        wasInBackrooms = inBackrooms;
        if (inBackrooms) {
            lockViewDistance(client);
        }
    }

    private static void reset(MinecraftClient client) {
        holdTicks = 0;
        sinkAge = -1;
        arrivalAge = -1;
        wasInBackrooms = false;
        restoreViewDistance(client);
        stopAmbience();
    }

    private static void lockViewDistance(MinecraftClient client) {
        Integer current = client.options.getViewDistance().getValue();
        if (current != null && current != LOCKED_VIEW_DISTANCE) {
            if (viewDistanceBefore == null) {
                viewDistanceBefore = current;
            }
            client.options.getViewDistance().setValue(LOCKED_VIEW_DISTANCE);
        }
    }

    private static void restoreViewDistance(MinecraftClient client) {
        if (viewDistanceBefore == null) {
            return;
        }
        int restored = viewDistanceBefore;
        viewDistanceBefore = null;
        client.options.getViewDistance().setValue(restored);
    }

    private static void startAmbience(MinecraftClient client) {
        if (ambience != null && !ambience.isDone()) {
            ambience.cancelFade();
            return;
        }
        ambience = new AmbienceLoop();
        client.getSoundManager().play(ambience);
    }

    private static void stopAmbience() {
        if (ambience != null) {
            ambience.fadeOut();
        }
    }

    /** Looping, non-positional, quiet. All level adjustment lives in AMBIENCE_VOLUME. */
    private static final class AmbienceLoop extends MovingSoundInstance {
        private int age;
        private int fadeOutAge = -1;

        private AmbienceLoop() {
            super(AMBIENCE, SoundCategory.AMBIENT, Random.create());
            this.repeat = true;
            this.repeatDelay = 0;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
            this.volume = 0.0F;
        }

        @Override
        public boolean canPlay() {
            return true;
        }

        private void fadeOut() {
            if (fadeOutAge < 0) {
                fadeOutAge = 0;
            }
        }

        private void cancelFade() {
            fadeOutAge = -1;
        }

        @Override
        public void tick() {
            age++;
            if (fadeOutAge >= 0) {
                fadeOutAge++;
                float remaining = 1.0F - fadeOutAge / (float) AMBIENCE_FADE_TICKS;
                if (remaining <= 0.0F) {
                    volume = 0.0F;
                    setDone();
                    return;
                }
                volume = AMBIENCE_VOLUME * remaining;
                return;
            }
            volume = AMBIENCE_VOLUME * MathHelper.clamp(age / (float) AMBIENCE_FADE_TICKS, 0.0F, 1.0F);
        }
    }
}
