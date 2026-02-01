package com.nhulston.essentials.commands.god;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;

/**
 * Command to toggle god mode (invincibility).
 * Usage: /god
 */
public class GodCommand extends AbstractPlayerCommand {
    private final MessageManager messages;

    public GodCommand() {
        super("god", "Toggle god mode (invincibility)");
        this.messages = Essentials.getInstance().getMessageManager();
        requirePermission("essentials.god");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // Check if player already has Invulnerable component
        Invulnerable current = store.getComponent(ref, Invulnerable.getComponentType());
        
        if (current != null) {
            // Disable god mode - remove component
            store.removeComponent(ref, Invulnerable.getComponentType());
            Msg.send(context, messages.get("commands.god.disabled"));
        } else {
            // Enable god mode - add component
            store.addComponent(ref, Invulnerable.getComponentType(), Invulnerable.INSTANCE);
            Msg.send(context, messages.get("commands.god.enabled"));
        }
    }
}
