package com.nhulston.essentials.managers;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.nhulston.essentials.integration.PAPIIntegration;
import com.nhulston.essentials.util.ColorUtil;
import com.nhulston.essentials.util.ConfigManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class ChatManager {
    private static final String COLOR_PERMISSION = "essentials.chat.color";
    // Matches color codes (&0-&f, &#RRGGBB) and formatting codes (&l, &r)
    private static final Pattern FORMAT_CODE_PATTERN = Pattern.compile("&[0-9a-fA-FlLrR]|&#[0-9a-fA-F]{6}");

    private final ConfigManager configManager;

    public ChatManager(@Nonnull ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Creates a custom formatter for the PlayerChatEvent based on player's groups.
     */
    @Nonnull
    public PlayerChatEvent.Formatter createFormatter() {
        return this::formatMessage;
    }

    /**
     * Formats a chat message for a player based on their permission groups.
     */
    @Nonnull
    public Message formatMessage(@Nonnull PlayerRef sender, @Nonnull String content) {
        String format = getFormatForPlayer(sender.getUuid());

        // Strip color codes from message unless player has permission
        String sanitizedContent = content;
        if (!PermissionsModule.get().hasPermission(sender.getUuid(), COLOR_PERMISSION)) {
            sanitizedContent = stripColorCodes(content);
        }

        String formatted = format
                .replace("%player%", sender.getUsername());

        if (PAPIIntegration.available()) {
            formatted = PAPIIntegration.setPlaceholders(sender, formatted);
        }

        formatted = formatted.replace("%message%", sanitizedContent);

        return ColorUtil.colorize(formatted);
    }

    /**
     * Strips color codes (&0-&f, &#RRGGBB) and formatting codes (&l, &r) from a string.
     */
    @Nonnull
    private String stripColorCodes(@Nonnull String text) {
        return FORMAT_CODE_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Gets the appropriate chat format for a player based on their permission groups.
     * Returns the first matching group format, or the fallback if no groups match.
     */
    @Nonnull
    private String getFormatForPlayer(@Nonnull UUID playerUuid) {
        List<ConfigManager.ChatFormat> formats = configManager.getChatFormats();

        if (formats.isEmpty()) {
            return configManager.getChatFallbackFormat();
        }

        Set<String> playerGroups = PermissionsModule.get().getGroupsForUser(playerUuid);

        // Check each configured format in order (List preserves insertion order)
        for (ConfigManager.ChatFormat chatFormat : formats) {
            // Check if player is in this group (case-insensitive)
            for (String playerGroup : playerGroups) {
                if (playerGroup.equalsIgnoreCase(chatFormat.group())) {
                    return chatFormat.format();
                }
            }
        }

        return configManager.getChatFallbackFormat();
    }

    /**
     * Checks if chat formatting is enabled.
     */
    public boolean isEnabled() {
        return configManager.isChatEnabled();
    }
}
