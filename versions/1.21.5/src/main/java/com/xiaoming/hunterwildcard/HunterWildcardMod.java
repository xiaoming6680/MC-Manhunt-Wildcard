package com.xiaoming.hunterwildcard;

import com.xiaoming.hunterwildcard.backrooms.BackroomsBlocks;
import com.xiaoming.hunterwildcard.backrooms.BackroomsDimension;
import com.xiaoming.hunterwildcard.backrooms.BackroomsSession;
import com.xiaoming.hunterwildcard.command.HunterWildcardCommand;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.sound.HunterWildcardSounds;
import com.xiaoming.hunterwildcard.wildcard.rules.SupplyDropRule;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HunterWildcardMod implements ModInitializer {
    public static final String MOD_ID = "hunterwildcard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        HunterWildcardSounds.register();
        BackroomsBlocks.register();
        BackroomsDimension.register();
        BackroomsSession.initialize();
        HunterWildcardPackets.registerPayloadTypes();
        HunterWildcardPackets.registerServerReceivers();
        com.xiaoming.hunterwildcard.wildcard.rules.PearlFrenzyRule.registerEvents();
        ServerTickEvents.END_SERVER_TICK.register(SupplyDropRule::tickTrackedDrops);
        ServerTickEvents.END_SERVER_TICK.register(com.xiaoming.hunterwildcard.wildcard.rules.WorldTiltPhysics::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> com.xiaoming.hunterwildcard.wildcard.rules.WorldTiltPhysics.clear());
        GameManager.getInstance().registerEvents();
        HunterWildcardCommand.register();
        LOGGER.info("Manhunt Wildcard loaded. Server commands registered.");
    }
}
