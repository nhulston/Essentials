package com.nhulston.essentials.commands.freecam;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.camera.SetFlyCameraMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Command to toggle freecam mode (fly around freely, return to original position when disabled).
 * Usage: /freecam
 */
public class FreecamCommand extends AbstractPlayerCommand {
    // Track players with freecam enabled
    private static final Set<UUID> freecamPlayers = new HashSet<>();
    private final MessageManager messages;

    public FreecamCommand() {
        super("freecam", "Toggle freecam mode");
        this.messages = Essentials.getInstance().getMessageManager();
        requirePermission("essentials.freecam");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        UUID uuid = playerRef.getUuid();
        boolean enabling = !freecamPlayers.contains(uuid);

        // Send the fly camera mode packet
        SetFlyCameraMode packet = new SetFlyCameraMode(enabling);
        playerRef.getPacketHandler().write(packet);

        if (enabling) {
            freecamPlayers.add(uuid);
            Msg.send(context, messages.get("commands.freecam.enabled"));
        } else {
            freecamPlayers.remove(uuid);
            Msg.send(context, messages.get("commands.freecam.disabled"));
        }
    }

    /**
     * Remove player from tracking when they disconnect.
     */
    public static void onPlayerQuit(UUID uuid) {
        freecamPlayers.remove(uuid);
    }
}
