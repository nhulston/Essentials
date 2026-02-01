package com.nhulston.essentials.commands.rules;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.util.ColorUtil;
import com.nhulston.essentials.util.ConfigManager;

import javax.annotation.Nonnull;

/**
 * Command to display server rules to players.
 * Usage: /rules
 */
public class RulesCommand extends AbstractPlayerCommand {
    private final ConfigManager configManager;

    public RulesCommand(@Nonnull ConfigManager configManager) {
        super("rules", "Display server rules");
        this.configManager = configManager;
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String rulesMessage = configManager.getRulesMessage();
        
        if (rulesMessage.trim().isEmpty()) {
            playerRef.sendMessage(ColorUtil.colorize("&cNo rules configured."));
            return;
        }
        
        // Replace placeholder
        rulesMessage = rulesMessage.replace("%player%", playerRef.getUsername());
        
        // Normalize line endings
        rulesMessage = rulesMessage.replace("\r", "");
        
        // Split by newlines and send each line
        String[] lines = rulesMessage.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                playerRef.sendMessage(ColorUtil.colorize(line));
            }
        }
    }
}
