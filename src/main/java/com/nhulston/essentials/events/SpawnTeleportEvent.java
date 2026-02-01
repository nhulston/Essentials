package com.nhulston.essentials.events;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.SpawnManager;
import com.nhulston.essentials.models.Spawn;
import com.nhulston.essentials.util.ConfigManager;
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
        // Use PlayerConnectEvent - fires BEFORE player joins a world
        // This allows us to set which world they join and their spawn position
        // Avoids the jarring teleport after spawn that happens with PlayerReadyEvent
        eventRegistry.registerGlobal(PlayerConnectEvent.class, event -> {
            boolean firstJoin = configManager.isFirstJoinSpawnEnabled();
            boolean everyJoin = configManager.isEveryJoinSpawnEnabled();
            
            if (!firstJoin && !everyJoin) {
                return;
            }
            
            PlayerRef playerRef = event.getPlayerRef();
            UUID uuid = playerRef.getUuid();
            
            boolean hasJoined = storageManager.hasPlayerJoined(uuid);
            
            // Mark player as joined if this is their first time
            if (!hasJoined) {
                storageManager.markPlayerJoined(uuid);
            }
            
            // Check if we should teleport to spawn
            if (everyJoin || !hasJoined) {
                Spawn spawn = spawnManager.getSpawn();
                if (spawn != null) {
                    World targetWorld = Universe.get().getWorld(spawn.getWorld());
                    if (targetWorld != null) {
                        // Set the world they'll join directly
                        event.setWorld(targetWorld);
                        
                        // Set their spawn position and rotation via the Holder
                        Holder<EntityStore> holder = event.getHolder();
                        Vector3d position = new Vector3d(spawn.getX(), spawn.getY(), spawn.getZ());
                        float yaw = TeleportUtil.roundToCardinalYaw(spawn.getYaw());
                        Vector3f bodyRotation = new Vector3f(0, yaw, 0);
                        
                        // Set body transform (position + body rotation)
                        TransformComponent transformComponent = new TransformComponent(position, bodyRotation);
                        holder.putComponent(TransformComponent.getComponentType(), transformComponent);
                        
                        // Set head rotation to match body (so player looks in the right direction)
                        HeadRotation headRotation = holder.ensureAndGetComponent(HeadRotation.getComponentType());
                        headRotation.teleportRotation(bodyRotation);
                    }
                }
            }
        });
    }

    public void registerSystems(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        registry.registerSystem(new RespawnTeleportSystem(spawnManager, configManager));
    }

    /**
     * System that teleports players to spawn when they respawn (DeathComponent removed).
     */
    private static class RespawnTeleportSystem extends RefChangeSystem<EntityStore, DeathComponent> {
        private final SpawnManager spawnManager;
        private final ConfigManager configManager;

        RespawnTeleportSystem(SpawnManager spawnManager, ConfigManager configManager) {
            this.spawnManager = spawnManager;
            this.configManager = configManager;
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
            if (!configManager.isDeathSpawnEnabled()) {
                return;
            }
            
            Spawn spawn = spawnManager.getSpawn();
            if (spawn != null) {
                TeleportUtil.teleportToSpawnBuffered(ref, buffer, spawn);
            }
        }
    }
}
