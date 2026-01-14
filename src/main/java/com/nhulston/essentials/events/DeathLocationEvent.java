package com.nhulston.essentials.events;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.managers.BackManager;
import com.nhulston.essentials.util.Log;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * Tracks player death locations for the /back command.
 */
public class DeathLocationEvent {
    private final BackManager backManager;

    public DeathLocationEvent(@Nonnull BackManager backManager) {
        this.backManager = backManager;
    }

    public void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        registry.registerSystem(new DeathLocationTrackingSystem(backManager));
        Log.info("Death location tracking enabled for /back command.");
    }

    /**
     * System that records player death locations when DeathComponent is added.
     */
    private static class DeathLocationTrackingSystem extends RefChangeSystem<EntityStore, DeathComponent> {
        private final BackManager backManager;

        DeathLocationTrackingSystem(BackManager backManager) {
            this.backManager = backManager;
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
            // Player died - record their death location
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }

            Vector3d position = transform.getPosition();
            Vector3f rotation = transform.getRotation();
            
            // Get the world name
            EntityStore entityStore = store.getExternalData();
            World world = entityStore.getWorld();
            String worldName = world.getName();

            backManager.setDeathLocation(
                playerRef.getUuid(),
                worldName,
                position.getX(),
                position.getY(),
                position.getZ(),
                rotation.getY(), // yaw
                rotation.getX()  // pitch
            );
        }

        @Override
        public void onComponentSet(@NotNull Ref<EntityStore> ref, DeathComponent oldComponent, @NotNull DeathComponent newComponent,
                                   @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> buffer) {
            // Do nothing on component update
        }

        @Override
        public void onComponentRemoved(@NotNull Ref<EntityStore> ref, @NotNull DeathComponent component,
                                       @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> buffer) {
            // Do nothing when player respawns - we keep the death location
        }
    }
}
