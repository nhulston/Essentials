package com.nhulston.essentials.commands.shout;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.Universe;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.util.ColorUtil;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Command to broadcast a message to all players on the server.
 * Usage: /shout <message>
 * Aliases: /broadcast
 * Can be executed by console or players.
 */
public class ShoutCommand extends AbstractCommand {
    private final ConfigManager configManager;
    private final MessageManager messages;

    public ShoutCommand(@Nonnull ConfigManager configManager) {
        super("shout", "Broadcast a message to all players");
        this.configManager = configManager;
        this.messages = Essentials.getInstance().getMessageManager();

        addAliases("broadcast");
        requirePermission("essentials.shout");
        
        // Allow extra arguments since we parse them manually for multi-word messages
        setAllowsExtraArguments(true);
    }

    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        // Parse from raw input: "/shout <message...>"
        String rawInput = context.getInputString();
        String[] parts = rawInput.split("\\s+", 2); // Split into [command, message]
        
        if (parts.length < 2) {
            Msg.send(context, messages.get("commands.shout.usage"));
            return CompletableFuture.completedFuture(null);
        }
        
        String message = configManager.getShoutPrefix() + parts[1];
        Universe.get().sendMessage(ColorUtil.colorize(message));
        return CompletableFuture.completedFuture(null);
    }
}
