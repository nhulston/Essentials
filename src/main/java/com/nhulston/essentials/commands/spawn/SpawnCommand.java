package com.nhulston.essentials.commands.spawn;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.SpawnManager;
import com.nhulston.essentials.models.Spawn;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.TeleportUtil;

import javax.annotation.Nonnull;

public class SpawnCommand extends AbstractPlayerCommand {
    private final SpawnManager spawnManager;

    public SpawnCommand(@Nonnull SpawnManager spawnManager) {
        super("spawn", "Teleport to the server spawn");
        this.spawnManager = spawnManager;

        requirePermission("essentials.spawn");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Spawn spawn = spawnManager.getSpawn();

        if (spawn == null) {
            Msg.fail(context, "Spawn has not been set.");
            return;
        }

        String error = TeleportUtil.teleportToSpawn(playerRef, spawn);
        if (error != null) {
            Msg.fail(context, error);
            return;
        }

        Msg.success(context, "Teleported to spawn.");
    }
}
