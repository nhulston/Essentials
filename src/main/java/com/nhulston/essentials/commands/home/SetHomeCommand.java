package com.nhulston.essentials.commands.home;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.managers.HomeManager;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import java.util.Map;

public class SetHomeCommand extends AbstractPlayerCommand {
    private final HomeManager homeManager;
    private final MessageManager messages;

    public SetHomeCommand(@Nonnull HomeManager homeManager) {
        super("sethome", "Set your home location");
        this.homeManager = homeManager;
        this.messages = Essentials.getInstance().getMessageManager();

        requirePermission("essentials.sethome");
        addUsageVariant(new SetHomeNamedCommand(homeManager));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // /sethome (no args) - use default name
        doSetHome(context, store, ref, playerRef, world, homeManager.getDefaultHomeName(), homeManager, messages);
    }

    private static void doSetHome(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                                  @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef,
                                  @Nonnull World world, @Nonnull String homeName, @Nonnull HomeManager homeManager,
                                  @Nonnull MessageManager messages) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            Msg.send(context, messages.get("commands.sethome.position-error"));
            return;
        }

        Vector3d position = transform.getPosition();
        HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());
        Vector3f rotation = (headRotation != null) ? headRotation.getRotation() : new Vector3f(0.0F, 0.0F, 0.0F);

        String error = homeManager.setHome(
                playerRef.getUuid(),
                homeName,
                world.getName(),
                position.getX(),
                position.getY(),
                position.getZ(),
                rotation.getY(),
                rotation.getX()
        );

        if (error != null) {
            Msg.send(context, error);
            return;
        }

        Msg.send(context, messages.get("commands.sethome.success", Map.of("home", homeName)));
    }

    // Inner class for /sethome <name> variant
    private static class SetHomeNamedCommand extends AbstractPlayerCommand {
        private final HomeManager homeManager;
        private final RequiredArg<String> nameArg;

        SetHomeNamedCommand(@Nonnull HomeManager homeManager) {
            super("Set your home location with a name");
            this.homeManager = homeManager;
            this.nameArg = withRequiredArg("name", "Home name", ArgTypes.STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            String homeName = context.get(nameArg);
            doSetHome(context, store, ref, playerRef, world, homeName, homeManager, Essentials.getInstance().getMessageManager());
        }
    }
}
