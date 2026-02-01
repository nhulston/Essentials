package com.nhulston.essentials.commands.trash;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * Opens a trash bin GUI with 27 slots (full chest size).
 * Items are instantly destroyed when placed in the trash.
 * Based on the Hytale trash-plugin pattern using ContainerWindow.
 */
public class TrashCommand extends AbstractPlayerCommand {

    public TrashCommand() {
        super("trash", "Opens a trash bin to destroy unwanted items");
        requirePermission("essentials.trash");
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        MessageManager messages = Essentials.getInstance().getMessageManager();

        // Create a 27-slot container (full chest size)
        SimpleItemContainer trashContainer = getSimpleItemContainer();

        // Open trash GUI using bench page with container window
        player.getPageManager().setPageWithWindows(
            ref,
            store,
            Page.Bench,
            true,
            new ContainerWindow(trashContainer)
        );

        Msg.send(playerRef, messages.get("trash.opened"));
    }

    private static @NotNull SimpleItemContainer getSimpleItemContainer() {
        SimpleItemContainer trashContainer = new SimpleItemContainer((short) 9);

        // Register change event to instantly delete items when placed
        trashContainer.registerChangeEvent(_ -> {
            // Delete all items in the container immediately
            for (short slot = 0; slot < trashContainer.getCapacity(); slot++) {
                ItemStack item = trashContainer.getItemStack(slot);
                if (!ItemStack.isEmpty(item)) {
                    trashContainer.removeItemStackFromSlot(slot);
                }
            }
        });
        return trashContainer;
    }
}
