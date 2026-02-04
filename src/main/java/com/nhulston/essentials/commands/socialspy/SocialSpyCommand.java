package com.nhulston.essentials.commands.socialspy;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Command to toggle social spy mode, allowing admins to see all private messages.
 * Usage: /socialspy
 */
public class SocialSpyCommand extends AbstractPlayerCommand {
    // Thread-safe set to track players with socialspy enabled
    private static final Set<UUID> socialSpyPlayers = ConcurrentHashMap.newKeySet();
    private final MessageManager messages;

    public SocialSpyCommand() {
        super("socialspy", "Toggle viewing all private messages");
        this.messages = Essentials.getInstance().getMessageManager();
        requirePermission("essentials.socialspy");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        UUID uuid = playerRef.getUuid();
        boolean enabling = !socialSpyPlayers.contains(uuid);

        if (enabling) {
            socialSpyPlayers.add(uuid);
            Msg.send(context, messages.get("commands.socialspy.enabled"));
        } else {
            socialSpyPlayers.remove(uuid);
            Msg.send(context, messages.get("commands.socialspy.disabled"));
        }
    }

    /**
     * Get the set of players with socialspy enabled.
     * Returns an unmodifiable view for safe iteration.
     */
    @Nonnull
    public static Set<UUID> getSocialSpyPlayers() {
        return Collections.unmodifiableSet(socialSpyPlayers);
    }

    /**
     * Remove player from tracking when they disconnect.
     */
    public static void onPlayerQuit(UUID uuid) {
        socialSpyPlayers.remove(uuid);
    }
}
