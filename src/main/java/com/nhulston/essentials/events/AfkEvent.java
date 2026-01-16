package com.nhulston.essentials.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.event.events.ShutdownEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.models.PlayerData;
import com.nhulston.essentials.util.StorageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AfkEvent {

    private final StorageManager storageManager;
    private final PlayerMovementSystem playerMovementSystem;

    public AfkEvent(StorageManager storageManager) {
        this.storageManager = storageManager;
        playerMovementSystem = new PlayerMovementSystem(storageManager);
    }

    public void registerEvents(@NotNull EventRegistry eventRegistry) {
        eventRegistry.registerGlobal(PlayerSetupConnectEvent.class, event -> {
            UUID playerUuid = event.getUuid();
            PlayerData playerData = storageManager.getPlayerData(playerUuid);

            playerData.updateActivity();
        });

        eventRegistry.registerAsyncGlobal(PlayerChatEvent.class, future ->
                future.thenApply(event -> {
                    PlayerRef sender = event.getSender();
                    PlayerData playerData = storageManager.getPlayerData(sender.getUuid());

                    playerData.updateActivity();
                    return event;
                })
        );

        eventRegistry.registerGlobal(PlayerDisconnectEvent.class, event -> {
            UUID playerUuid = event.getPlayerRef().getUuid();
            playerMovementSystem.removePlayer(playerUuid);
        });

        eventRegistry.registerGlobal(ShutdownEvent.class, _ -> playerMovementSystem.clear());
    }

    public void registerSystems(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        registry.registerSystem(new PlayerDamageEventSystem(storageManager));
        registry.registerSystem(playerMovementSystem);
    }

    private static class PlayerDamageEventSystem extends DamageEventSystem {
        private final StorageManager storageManager;

        public PlayerDamageEventSystem(StorageManager storageManager) {
            this.storageManager = storageManager;
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void handle(int i, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer, @NotNull Damage damage) {
            if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) {
                return;
            }

            PlayerRef playerRef = store.getComponent(entitySource.getRef(), PlayerRef.getComponentType());
            if (playerRef == null) return;

            PlayerData playerData = storageManager.getPlayerData(playerRef.getUuid());
            playerData.updateActivity();
        }
    }

    private static class PlayerMovementSystem extends EntityTickingSystem<EntityStore> {
        private final Map<UUID, Vector3d> previousPositions = new HashMap<>();
        @Nonnull
        private final ComponentType<EntityStore, TransformComponent> TRANSFORM_COMPONENT_TYPE = TransformComponent.getComponentType();

        private final StorageManager storageManager;

        public PlayerMovementSystem(StorageManager storageManager) {
            this.storageManager = storageManager;
        }

        @Nonnull
        public Query<EntityStore> getQuery() {
            return Query.and(PlayerRef.getComponentType(), TRANSFORM_COMPONENT_TYPE);
        }

        public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            PlayerRef player = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            if (player == null) return;

            UUID playerUuid = player.getUuid();
            TransformComponent transform = archetypeChunk.getComponent(index, TRANSFORM_COMPONENT_TYPE);
            if (transform != null) {
                Vector3d currentPosition = transform.getPosition();
                Vector3d previousPosition = this.previousPositions.get(playerUuid);

                if (previousPosition != null) {
                    double distance = currentPosition.distanceSquaredTo(previousPosition);

                    // Player has moved
                    if (distance > 0.01) {
                        PlayerData playerData = storageManager.getPlayerData(player.getUuid());
                        playerData.updateActivity();
                    }
                }

                this.previousPositions.put(playerUuid, new Vector3d(currentPosition));
            }
        }

        public void removePlayer(@Nonnull UUID playerUuid) {
            this.previousPositions.remove(playerUuid);
        }

        public void clear() {
            this.previousPositions.clear();
        }
    }

}
