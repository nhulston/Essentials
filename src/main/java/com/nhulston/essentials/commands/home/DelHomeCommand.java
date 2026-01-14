package com.nhulston.essentials.commands.home;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.HomeManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;

public class DelHomeCommand extends AbstractPlayerCommand {
    private final HomeManager homeManager;
    private final RequiredArg<String> nameArg;

    public DelHomeCommand(@Nonnull HomeManager homeManager) {
        super("delhome", "Delete a home");
        this.homeManager = homeManager;
        this.nameArg = withRequiredArg("name", "Home name", ArgTypes.STRING);

        requirePermission("essentials.delhome");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String homeName = context.get(nameArg);

        boolean deleted = homeManager.deleteHome(playerRef.getUuid(), homeName);

        if (deleted) {
            Msg.success(context, "Home '" + homeName + "' deleted.");
        } else {
            Msg.fail(context, "Home '" + homeName + "' not found.");
        }
    }
}
