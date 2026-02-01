package com.nhulston.essentials.commands.spawn;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.managers.SpawnManager;
import com.nhulston.essentials.managers.TeleportManager;
import com.nhulston.essentials.models.Spawn;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.TeleportUtil;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Teleport to spawn command.
 * Usage: /spawn - Teleport yourself to spawn (with delay)
 * Usage: /spawn <player> - Teleport another player to spawn instantly (requires essentials.spawn.others or console)
 */
public class SpawnCommand extends AbstractPlayerCommand {
    private final SpawnManager spawnManager;
    private final TeleportManager teleportManager;
    private final BackManager backManager;
    private final MessageManager messages;

    public SpawnCommand(@Nonnull SpawnManager spawnManager, @Nonnull TeleportManager teleportManager,
                       @Nonnull BackManager backManager) {
        super("spawn", "Teleport to the server spawn");
        this.spawnManager = spawnManager;
        this.teleportManager = teleportManager;
        this.backManager = backManager;
        this.messages = Essentials.getInstance().getMessageManager();

        addAliases("s");
        requirePermission("essentials.spawn");
        
        // Add variant for teleporting other players
        addUsageVariant(new SpawnOtherCommand(spawnManager, backManager));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Spawn spawn = spawnManager.getSpawn();

        if (spawn == null) {
            Msg.send(context, messages.get("commands.spawn.not-set"));
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
            spawn.getWorld(), spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw(), spawn.getPitch(),
            messages.get("commands.spawn.teleported")
        );
    }

    /**
     * Variant command for teleporting other players to spawn (console or admin).
     * Usage: /spawn <player>
     */
    private static class SpawnOtherCommand extends AbstractCommand {
        private final SpawnManager spawnManager;
        private final BackManager backManager;
        private final RequiredArg<PlayerRef> targetArg;

        SpawnOtherCommand(@Nonnull SpawnManager spawnManager, @Nonnull BackManager backManager) {
            super("Teleport another player to spawn");
            this.spawnManager = spawnManager;
            this.backManager = backManager;
            this.targetArg = withRequiredArg("player", "Player to teleport", ArgTypes.PLAYER_REF);
            requirePermission("essentials.spawn.others");
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            MessageManager messages = Essentials.getInstance().getMessageManager();
            Spawn spawn = spawnManager.getSpawn();

            if (spawn == null) {
                Msg.send(context, messages.get("commands.spawn.not-set"));
                return CompletableFuture.completedFuture(null);
            }

            PlayerRef targetPlayer = context.get(targetArg);
            
            // Execute teleport on player's world thread with validation
            TeleportUtil.executeOnPlayerWorld(targetPlayer, context, () -> {
                TeleportUtil.saveLocationAndTeleportToSpawn(targetPlayer, backManager, spawn);

                String senderName = "Console";
                Msg.send(context, messages.get("commands.spawn.teleported-other", Map.of("player", targetPlayer.getUsername())));
                Msg.send(targetPlayer, messages.get("commands.spawn.teleported-by", Map.of("sender", senderName)));
            });

            return CompletableFuture.completedFuture(null);
        }
    }
}
