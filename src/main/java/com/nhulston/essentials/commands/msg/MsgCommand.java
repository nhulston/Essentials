package com.nhulston.essentials.commands.msg;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.commands.socialspy.SocialSpyCommand;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Command to send a private message to another player.
 * Usage: /msg <player> <message>
 * Aliases: /m, /message, /whisper, /pm
 */
public class MsgCommand extends AbstractPlayerCommand {
    // Track last conversation partner for /reply
    private static final Map<UUID, UUID> lastMessagePartner = new ConcurrentHashMap<>();
    private final MessageManager messages;

    public MsgCommand() {
        // Don't register any args - we'll parse everything from raw input
        // This allows multi-word messages without quotes
        super("msg", "Send a private message to a player");
        this.messages = Essentials.getInstance().getMessageManager();
        
        // Allow extra arguments since we parse them manually
        setAllowsExtraArguments(true);
        
        addAliases("m", "message", "whisper", "pm");
        requirePermission("essentials.msg");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // Parse from raw input: "/msg <player> <message...>"
        String rawInput = context.getInputString();
        String[] parts = rawInput.split("\\s+", 3); // Split into [command, player, message]
        
        if (parts.length < 3) {
            Msg.send(context, messages.get("commands.msg.usage"));
            return;
        }
        
        String targetName = parts[1];
        String message = parts[2];

        // Find target player by name
        PlayerRef target = findPlayer(targetName);
        
        if (target == null) {
            Msg.send(context, messages.get("commands.msg.player-not-found", Map.of("player", targetName)));
            return;
        }

        if (target.getUuid().equals(playerRef.getUuid())) {
            Msg.send(context, messages.get("commands.msg.cannot-self"));
            return;
        }

        sendMessage(playerRef, target, message, context, messages);
    }

    /**
     * Send a private message and track for /reply.
     */
    public static void sendMessage(@Nonnull PlayerRef sender, @Nonnull PlayerRef target, 
                                   @Nonnull String message, @Nullable CommandContext context,
                                   @Nonnull MessageManager messages) {
        // Send to target: [From PlayerName] message
        Msg.send(target, messages.get("commands.msg.format-from", Map.of("player", sender.getUsername(), "message", message)));
        
        // Confirm to sender: [To PlayerName] message
        String toMessage = messages.get("commands.msg.format-to", Map.of("player", target.getUsername(), "message", message));
        if (context != null) {
            Msg.send(context, toMessage);
        } else {
            Msg.send(sender, toMessage);
        }

        // Track last message partner for both players (for /reply)
        lastMessagePartner.put(sender.getUuid(), target.getUuid());
        lastMessagePartner.put(target.getUuid(), sender.getUuid());

        // Notify socialspy users (excluding sender and target)
        Set<UUID> spyPlayers = SocialSpyCommand.getSocialSpyPlayers();
        if (!spyPlayers.isEmpty()) {
            String spyMessage = messages.get("commands.socialspy.format", Map.of(
                "sender", sender.getUsername(),
                "target", target.getUsername(),
                "message", message
            ));

            for (UUID spyUuid : spyPlayers) {
                // Skip sender and target - they already see the message
                if (spyUuid.equals(sender.getUuid()) || spyUuid.equals(target.getUuid())) {
                    continue;
                }

                PlayerRef spy = Universe.get().getPlayer(spyUuid);
                if (spy != null) {
                    Msg.send(spy, spyMessage);
                }
            }
        }
    }

    /**
     * Get the last message partner for a player.
     */
    @Nullable
    public static UUID getLastMessagePartner(@Nonnull UUID playerUuid) {
        return lastMessagePartner.get(playerUuid);
    }

    /**
     * Find a player by name (case-insensitive).
     */
    @Nullable
    public static PlayerRef findPlayer(String name) {
        List<PlayerRef> players = Universe.get().getPlayers();
        for (PlayerRef player : players) {
            if (player.getUsername().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Remove player from tracking when they disconnect.
     */
    public static void onPlayerQuit(UUID uuid) {
        lastMessagePartner.remove(uuid);
    }
}
