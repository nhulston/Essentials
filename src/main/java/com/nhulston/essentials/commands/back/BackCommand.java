package com.nhulston.essentials.commands.back;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.TeleportUtil;

import javax.annotation.Nonnull;

/**
 * Command to teleport back to the player's last location (death or pre-teleport).
 * Usage: /back
 */
public class BackCommand extends AbstractPlayerCommand {
    private final BackManager backManager;
    private final TeleportManager teleportManager;
    private final MessageManager messages;

    public BackCommand(@Nonnull BackManager backManager, @Nonnull TeleportManager teleportManager) {
        super("back", "Teleport to your previous location");
        this.backManager = backManager;
        this.teleportManager = teleportManager;
        this.messages = Essentials.getInstance().getMessageManager();

        requirePermission("essentials.back");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        java.util.UUID playerUuid = playerRef.getUuid();
        BackManager.BackLocation backLocation = backManager.getBackLocation(playerUuid);

        if (backLocation == null) {
            Msg.send(context, messages.get("commands.back.no-location"));
            return;
        }

        backManager.setBackLocation(store, ref, playerRef, world);
        Vector3d startPosition = TeleportUtil.getStartPosition(store, ref);
        if (startPosition == null) {
            Msg.send(context, messages.get("errors.generic"));
            return;
        }

        teleportManager.queueTeleport(
            playerRef, ref, store, startPosition,
            backLocation.getWorldName(),
            backLocation.getX(),
            backLocation.getY(),
            backLocation.getZ(),
            backLocation.getYaw(),
            backLocation.getPitch(),
            messages.get("commands.back.teleported"),
            () -> backManager.clearBackLocation(playerUuid)
        );
    }
}
