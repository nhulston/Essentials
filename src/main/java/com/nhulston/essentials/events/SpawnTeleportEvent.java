package com.nhulston.essentials.events;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.SpawnManager;
import com.nhulston.essentials.models.Spawn;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.Log;
import com.nhulston.essentials.util.StorageManager;
import com.nhulston.essentials.util.TeleportUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.UUID;

public class SpawnTeleportEvent {
    private final SpawnManager spawnManager;
    private final ConfigManager configManager;
    private final StorageManager storageManager;

    public SpawnTeleportEvent(@Nonnull SpawnManager spawnManager, @Nonnull ConfigManager configManager,
                              @Nonnull StorageManager storageManager) {
        this.spawnManager = spawnManager;
        this.configManager = configManager;
        this.storageManager = storageManager;
    }

    public void registerEvents(@Nonnull EventRegistry eventRegistry) {
        boolean firstJoin = configManager.isFirstJoinSpawnEnabled();
        boolean everyJoin = configManager.isEveryJoinSpawnEnabled();
        
        if (!firstJoin && !everyJoin) {
            return;
        }
        
        // Use PlayerReadyEvent - fires after client is ready and player is fully in world
        eventRegistry.registerGlobal(PlayerReadyEvent.class, event -> {
            Ref<EntityStore> ref = event.getPlayerRef();
            if (!ref.isValid()) {
                return;
            }
            
            Store<EntityStore> store = ref.getStore();
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }
            
            UUID uuid = playerRef.getUuid();
            boolean hasJoined = storageManager.hasPlayerJoined(uuid);
            
            // Mark player as joined if this is their first time
            if (!hasJoined) {
                storageManager.markPlayerJoined(uuid);
            }
            
            // Check if we should teleport
            if (everyJoin || !hasJoined) {
                Spawn spawn = spawnManager.getSpawn();
                if (spawn != null) {
                    TeleportUtil.teleportToSpawn(playerRef, spawn);
                }
            }
        });
        
        if (everyJoin) {
            Log.info("Every-join spawn teleport enabled.");
        } else {
            Log.info("First-join spawn teleport enabled.");
        }
    }

    public void registerSystems(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        if (configManager.isDeathSpawnEnabled()) {
            registry.registerSystem(new RespawnTeleportSystem(spawnManager));
            Log.info("Death spawn teleport enabled.");
        }
    }

    /**
     * System that teleports players to spawn when they respawn (DeathComponent removed).
     */
    private static class RespawnTeleportSystem extends RefChangeSystem<EntityStore, DeathComponent> {
        private final SpawnManager spawnManager;

        RespawnTeleportSystem(SpawnManager spawnManager) {
            this.spawnManager = spawnManager;
        }

        @Override
        public @NotNull ComponentType<EntityStore, DeathComponent> componentType() {
            return DeathComponent.getComponentType();
        }

        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void onComponentAdded(@NotNull Ref<EntityStore> ref, @NotNull DeathComponent component,
                                     @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> buffer) {
            // Do nothing when player dies
        }

        @Override
        public void onComponentSet(@NotNull Ref<EntityStore> ref, DeathComponent oldComponent, @NotNull DeathComponent newComponent,
                                   @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> buffer) {
            // Do nothing on component update
        }

        @Override
        public void onComponentRemoved(@NotNull Ref<EntityStore> ref, @NotNull DeathComponent component,
                                       @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> buffer) {
            Spawn spawn = spawnManager.getSpawn();
            if (spawn != null) {
                TeleportUtil.teleportToSpawnBuffered(ref, buffer, spawn);
            }
        }
    }
}
