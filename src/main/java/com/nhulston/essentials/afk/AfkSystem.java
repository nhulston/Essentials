package com.nhulston.essentials.afk;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.util.ConfigManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AfkSystem {

    private static ComponentType<EntityStore, AfkComponent> AFK_COMPONENT;

    private final ConfigManager configManager;
    private static final String BYPASS_PERMISSION = "essentials.afk.bypass";

    public AfkSystem(final @NotNull ConfigManager configManager) {
        this.configManager = configManager;
    }

    public static void registerComponents(@NotNull ComponentRegistryProxy<EntityStore> registry) {
        AFK_COMPONENT = registry.registerComponent(AfkComponent.class, AfkComponent::new);
    }

    public void registerSystems(@NotNull ComponentRegistryProxy<EntityStore> registry) {
        registry.registerSystem(new PlayerDamageSystem(configManager));
        registry.registerSystem(new PlayerTickerSystem(configManager));
    }

    public void registerEvents(@NotNull EventRegistry eventRegistry) {
        eventRegistry.registerGlobal(PlayerReadyEvent.class, event -> {
            Ref<EntityStore> ref = event.getPlayerRef();
            if (!ref.isValid()) return;

            Store<EntityStore> store = ref.getStore();
            store.addComponent(ref, AFK_COMPONENT, new AfkComponent());
        });

        eventRegistry.registerAsyncGlobal(PlayerChatEvent.class, future ->
                future.thenApply(event -> {
                    PlayerRef playerRef = event.getSender();
                    if (!playerRef.isValid() || playerRef.getReference() == null) return event;

                    Store<EntityStore> store = playerRef.getReference().getStore();
                    World world = store.getExternalData().getWorld();

                    world.execute(() -> {
                        AfkComponent afkComponent = store.getComponent(playerRef.getReference(), AFK_COMPONENT);
                        if (afkComponent != null) {
                            afkComponent.setSecondsSinceLastMoved(0);
                        }
                    });
                    return event;
                })
        );
    }

    private static class PlayerDamageSystem extends DamageEventSystem {
        private final ConfigManager configManager;

        public PlayerDamageSystem(final @NotNull ConfigManager configManager) {
            this.configManager = configManager;
        }

        @Override
        public void handle(int index, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer, @NotNull Damage damage) {
            if (configManager.getAfkKickTime() <= 0) return;

            AfkComponent afk = archetypeChunk.getComponent(index, AFK_COMPONENT);
            if (afk == null) return;

            afk.setSecondsSinceLastMoved(0);
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return Query.and(Player.getComponentType(), AFK_COMPONENT);
        }
    }

    private static class PlayerTickerSystem extends DelayedEntitySystem<EntityStore> {

        private static final double EPSILON = 0.0000001;

        private final ConfigManager configManager;

        public PlayerTickerSystem(final @NotNull ConfigManager configManager) {
            super(1.0f);
            this.configManager = configManager;
        }

        @Override
        public void tick(float dt, int index, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer) {
            if (configManager.getAfkKickTime() <= 0) return;

            PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;

            Player player = archetypeChunk.getComponent(index, Player.getComponentType());
            if (player == null) return;

            AfkComponent afk = archetypeChunk.getComponent(index, AFK_COMPONENT);
            if (afk == null) return;

            TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
            if (transform == null) return;

            if (player.hasPermission(BYPASS_PERMISSION)) return;

            // Check if player has moved
            if (afk.getLastLocation().distanceSquaredTo(transform.getPosition()) > EPSILON) {
                afk.setLastLocation(transform.getPosition().clone());
                afk.setSecondsSinceLastMoved(0);
                return;
            }

            afk.setSecondsSinceLastMoved(afk.getSecondsSinceLastMoved() + 1);

            if (afk.getSecondsSinceLastMoved() < configManager.getAfkKickTime()) return;

            // Use commandBuffer to defer disconnect until after tick completes
            // (cannot modify store while it's processing)
            commandBuffer.run(_ -> playerRef.getPacketHandler().disconnect(configManager.getAfkKickMessage()));
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return Query.and(TransformComponent.getComponentType(),
                    Player.getComponentType(),
                    AFK_COMPONENT);
        }
    }
}
