package com.xiaoming.hunterwildcard.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public class HunterWildcardCommand {
    private static final Permission OP_LEVEL_TWO = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    private HunterWildcardCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hw")
                .then(Commands.literal("start")
                        .requires(HunterWildcardCommand::canManageGame)
                        .executes(context -> {
                            GameManager.getInstance().start(context.getSource());
                            HunterWildcardPackets.syncAll(context.getSource().getServer());
                            return 1;
                        }))
                .then(Commands.literal("stop")
                        .requires(HunterWildcardCommand::canManageGame)
                        .executes(context -> {
                            GameManager.getInstance().stop(context.getSource());
                            HunterWildcardPackets.syncAll(context.getSource().getServer());
                            return 1;
                        }))
                .then(Commands.literal("config")
                        .then(Commands.literal("reload")
                                .requires(HunterWildcardCommand::canManageGame)
                                .executes(context -> {
                                    GameManager.getInstance().reloadConfig(context.getSource());
                                    HunterWildcardPackets.syncAll(context.getSource().getServer());
                                    return 1;
                                }))
                        .then(Commands.literal("save")
                                .requires(HunterWildcardCommand::canManageGame)
                                .executes(context -> {
                                    GameManager.getInstance().saveConfig(context.getSource());
                                    HunterWildcardPackets.syncAll(context.getSource().getServer());
                                    return 1;
                                })))
                .then(Commands.literal("wildcard")
                        .then(Commands.literal("roll")
                                .requires(HunterWildcardCommand::canManageGame)
                                .executes(context -> {
                                    GameManager.getInstance().rollWildcard(context.getSource());
                                    HunterWildcardPackets.syncAll(context.getSource().getServer());
                                    return 1;
                                }))
                        .then(Commands.literal("stop")
                                .requires(HunterWildcardCommand::canManageGame)
                                .executes(context -> {
                                    GameManager.getInstance().stopWildcard(context.getSource());
                                    HunterWildcardPackets.syncAll(context.getSource().getServer());
                                    return 1;
                                }))
                        .then(Commands.literal("list")
                                .executes(context -> listWildcards(context.getSource()))))
                .then(Commands.literal("debug")
                        .requires(HunterWildcardCommand::canManageGame)
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    GameManager.getInstance().setDebugMenuEnabled(player, enabled);
                                    HunterWildcardPackets.sendSync(player);
                                    context.getSource().sendSuccess(() -> HunterWildcardText.translatable(enabled
                                            ? "command.debug_menu.enabled"
                                            : "command.debug_menu.disabled"), false);
                                    return 1;
                                })))
                .then(Commands.literal("join")
                        .then(Commands.literal("hunter")
                                .executes(context -> join(context.getSource(), PlayerRole.HUNTER)))
                        .then(Commands.literal("runner")
                                .executes(context -> join(context.getSource(), PlayerRole.RUNNER))))
                .then(Commands.literal("leave")
                        .executes(context -> {
                            GameManager.getInstance().leave(context.getSource().getPlayerOrException());
                            HunterWildcardPackets.syncAll(context.getSource().getServer());
                            return 1;
                        }))
                .then(Commands.literal("status")
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> GameManager.getInstance().getStatusText(), false);
                            return 1;
                        })));
    }

    private static int join(CommandSourceStack source, PlayerRole role) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        GameManager.getInstance().join(player, role);
        HunterWildcardPackets.syncAll(source.getServer());
        return 1;
    }

    private static int listWildcards(CommandSourceStack source) {
        GameManager manager = GameManager.getInstance();
        source.sendSuccess(() -> HunterWildcardText.translatable("command.wildcard.list.header"), false);
        for (WildcardManager.WildcardStatus status : manager.getWildcardManager().getRuleStatuses(manager.getConfig())) {
            Component state = HunterWildcardText.translatable(status.enabled()
                    ? "command.wildcard.list.enabled"
                    : "command.wildcard.list.disabled");
            source.sendSuccess(() -> HunterWildcardText.translatable("command.wildcard.list.entry", HunterWildcardText.wildcardName(status.name()), state), false);
        }
        return 1;
    }

    public static boolean canManageGame(CommandSourceStack source) {
        return source.permissions().hasPermission(OP_LEVEL_TWO);
    }
}
