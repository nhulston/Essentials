package com.nhulston.essentials.util;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Simple message sending utility.
 * All formatting/colors should come from messages.toml.
 */
public final class Msg {
    private Msg() {}

    public static void send(@Nonnull CommandContext context, @Nonnull String message) {
        if (!message.isEmpty()) {
            context.sendMessage(ColorUtil.colorize(message));
        }
    }

    public static void send(@Nonnull PlayerRef player, @Nonnull String message) {
        if (!message.isEmpty()) {
            player.sendMessage(ColorUtil.colorize(message));
        }
    }
}
