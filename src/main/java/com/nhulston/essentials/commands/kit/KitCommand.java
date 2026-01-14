package com.nhulston.essentials.commands.kit;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.gui.KitPage;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;

/**
 * Command to open the kit selection GUI.
 * Usage: /kit
 */
public class KitCommand extends AbstractPlayerCommand {

    public KitCommand() {
        super("kit", "Open the kit selection menu");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // Get the Player component to access PageManager
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            Msg.fail(context, "Could not get player component.");
            return;
        }

        // Create and open the kit selection page
        KitPage kitPage = new KitPage(playerRef);
        player.getPageManager().openCustomPage(ref, store, kitPage);
    }
}
