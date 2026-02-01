package com.nhulston.essentials.commands.heal;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;

/**
 * Command to heal the player to full health.
 * Usage: /heal
 */
public class HealCommand extends AbstractPlayerCommand {
    private final MessageManager messages;

    public HealCommand() {
        super("heal", "Restore your health to full");
        this.messages = Essentials.getInstance().getMessageManager();
        requirePermission("essentials.heal");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            Msg.send(context, messages.get("commands.heal.stats-error"));
            return;
        }

        int healthStatIndex = DefaultEntityStatTypes.getHealth();
        statMap.maximizeStatValue(healthStatIndex);
        
        Msg.send(context, messages.get("commands.heal.success"));
    }
}
