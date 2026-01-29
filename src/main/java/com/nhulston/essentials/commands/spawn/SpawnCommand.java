package com.nhulston.essentials.commands.spawn;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
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
import com.nhulston.essentials.util.ConfigManager;
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
        // Check if creative mode spawn blocking is enabled
        ConfigManager configManager = Essentials.getInstance().getConfigManager();

        com.nhulston.essentials.util.Log.info("Creative mode spawn block enabled: " +
            (configManager != null && configManager.isCreativeModeSpawnBlockEnabled()));

        if (configManager != null && configManager.isCreativeModeSpawnBlockEnabled()) {
            // Check if player has bypass permission
            boolean hasBypass = com.hypixel.hytale.server.core.permissions.PermissionsModule.get()
                    .hasPermission(playerRef.getUuid(), "essentials.spawn.creative.bypass");

            com.nhulston.essentials.util.Log.info("Player has bypass permission: " + hasBypass);

            if (!hasBypass) {
                // Check if we're in a blocked world
                java.util.List<String> blockedWorlds = configManager.getCreativeModeSpawnBlockWorlds();
                boolean shouldBlock = blockedWorlds.isEmpty() || blockedWorlds.contains(world.getName());

                com.nhulston.essentials.util.Log.info("Should block in world " + world.getName() + ": " + shouldBlock);
                com.nhulston.essentials.util.Log.info("Blocked worlds: " + blockedWorlds);

                if (shouldBlock) {
                    // Check player's game mode
                    com.hypixel.hytale.server.core.entity.entities.Player player =
                        store.getComponent(ref, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());

                    if (player != null) {
                        com.hypixel.hytale.protocol.GameMode gameMode = player.getGameMode();
                        com.nhulston.essentials.util.Log.info("Player game mode: " + gameMode);

                        if (gameMode == com.hypixel.hytale.protocol.GameMode.Creative) {
                            Msg.send(context, messages.get("commands.spawn.blocked-creative"));
                            com.nhulston.essentials.util.Log.info("BLOCKED spawn command for creative mode player");
                            return;
                        }
                    } else {
                        com.nhulston.essentials.util.Log.warning("Could not get Player component");
                    }
                }
            }
        }

        Spawn spawn = spawnManager.getSpawn();

        if (spawn == null) {
            Msg.send(context, messages.get("commands.spawn.not-set"));
            return;
        }

        // Save current location before teleporting
        Vector3d currentPos = playerRef.getTransform().getPosition();
        Vector3f currentRot = playerRef.getTransform().getRotation();
        backManager.setTeleportLocation(playerRef.getUuid(), world.getName(),
            currentPos.getX(), currentPos.getY(), currentPos.getZ(),
            currentRot.getY(), currentRot.getX());

        Vector3d startPosition = playerRef.getTransform().getPosition();

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
