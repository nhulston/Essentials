package com.nhulston.essentials.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public final class Msg {
    private static final String SUCCESS_COLOR = "#55FF55";
    private static final String FAIL_COLOR = "#FF5555";
    private static final String INFO_COLOR = "#FFFF55";
    private static final String WHITE = "#FFFFFF";

    private Msg() {}

    public static void success(@Nonnull CommandContext context, @Nonnull String message) {
        context.sendMessage(Message.raw(message).color(SUCCESS_COLOR));
    }

    public static void fail(@Nonnull CommandContext context, @Nonnull String message) {
        context.sendMessage(Message.raw(message).color(FAIL_COLOR));
    }

    public static void prefix(@Nonnull CommandContext context, @Nonnull String prefix, @Nonnull String text) {
        context.sendMessage(Message.join(
                Message.raw(prefix).color(SUCCESS_COLOR),
                Message.raw(": ").color(WHITE),
                Message.raw(text).color(WHITE)
        ));
    }

    public static void success(@Nonnull PlayerRef player, @Nonnull String message) {
        player.sendMessage(Message.raw(message).color(SUCCESS_COLOR));
    }

    public static void info(@Nonnull PlayerRef player, @Nonnull String message) {
        player.sendMessage(Message.raw(message).color(INFO_COLOR));
    }

    public static void fail(@Nonnull PlayerRef player, @Nonnull String message) {
        player.sendMessage(Message.raw(message).color(FAIL_COLOR));
    }
}
