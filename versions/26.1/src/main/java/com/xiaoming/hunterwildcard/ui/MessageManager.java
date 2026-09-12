package com.xiaoming.hunterwildcard.ui;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class MessageManager {
    public void broadcast(MinecraftServer server, Component message) {
        server.getPlayerList().broadcastSystemMessage(HunterWildcardText.prefixed(message).withStyle(ChatFormatting.GOLD), false);
    }

    public void broadcastSpec(MinecraftServer server, String messageSpec) {
        broadcast(server, HunterWildcardText.fromSpec(messageSpec));
    }

    public void toParticipants(GameContext context, Component message) {
        Component text = HunterWildcardText.prefixed(message).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer player : context.getParticipants()) {
            player.sendSystemMessage(text);
        }
    }

    public void toParticipantsSpec(GameContext context, String messageSpec) {
        toParticipants(context, HunterWildcardText.fromSpec(messageSpec));
    }

    public void actionBar(ServerPlayer player, Component message) {
        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.YELLOW), true);
    }

    public void actionBar(GameContext context, Component message) {
        for (ServerPlayer player : context.getParticipants()) {
            actionBar(player, message);
        }
    }

    public void direct(ServerPlayer player, Component message) {
        player.sendSystemMessage(HunterWildcardText.prefixed(message).withStyle(ChatFormatting.GOLD));
    }

    public void directSpec(ServerPlayer player, String messageSpec) {
        direct(player, HunterWildcardText.fromSpec(messageSpec));
    }
}
