package com.nhulston.essentials.commands.kit;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.gui.KitPage;
import com.nhulston.essentials.managers.KitManager;
import com.nhulston.essentials.models.Kit;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.CooldownUtil;
import com.nhulston.essentials.util.Log;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Command to open the kit selection GUI.
 * Usage: /kit - Opens kit GUI
 * Usage: /kit <kitname> - Claims a specific kit
 * Usage: /kit <kitname> <player> - Gives a kit to another player (requires essentials.kit.other permission)
 * Usage: /kit create <name> - Creates a kit from current inventory
 * Usage: /kit delete <name> - Deletes a kit
 */
public class KitCommand extends AbstractPlayerCommand {
    private static final String COOLDOWN_BYPASS_PERMISSION = "essentials.kit.cooldown.bypass";
    
    private final KitManager kitManager;
    private final ConfigManager configManager;
    private final MessageManager messages;

    public KitCommand(@Nonnull KitManager kitManager, @Nonnull ConfigManager configManager) {
        super("kit", "Open the kit selection menu");
        this.addAliases("kits");
        this.kitManager = kitManager;
        this.configManager = configManager;
        this.messages = Essentials.getInstance().getMessageManager();

        requirePermission("essentials.kit");
        
        // Add usage variants
        addUsageVariant(new KitClaimCommand(kitManager, configManager));
        addUsageVariant(new KitGiveCommand(kitManager));
        
        // Add subcommands
        addSubCommand(new KitCreateCommand(kitManager));
        addSubCommand(new KitDeleteCommand(kitManager));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        // Get the Player component to access PageManager
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            Msg.send(context, messages.get("commands.kit.player-error"));
            return;
        }

        // Create and open the kit selection page
        KitPage kitPage = new KitPage(playerRef, kitManager, configManager);
        player.getPageManager().openCustomPage(ref, store, kitPage);
    }
    
    /**
     * Usage variant for /kit <kitname> - Player claims a specific kit
     */
    private static class KitClaimCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> kitNameArg;
        private final KitManager kitManager;
        private final ConfigManager configManager;
        
        KitClaimCommand(@Nonnull KitManager kitManager, @Nonnull ConfigManager configManager) {
            super("Claim a specific kit");
            this.kitManager = kitManager;
            this.configManager = configManager;
            this.kitNameArg = withRequiredArg("kitname", "Kit to claim", ArgTypes.STRING);
        }
        
        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                               @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            MessageManager messages = Essentials.getInstance().getMessageManager();
            String kitName = context.get(kitNameArg);
            
            // Get kit (case-insensitive)
            Kit kit = kitManager.getKit(kitName.toLowerCase());
            if (kit == null) {
                Msg.send(context, messages.get("commands.kit.not-found", Map.of("kit", kitName)));
                return;
            }
            
            // Check if this is the starter kit - disallow claiming via command
            if (configManager.isStarterKitEnabled()) {
                String starterKitName = configManager.getStarterKitName();
                if (!starterKitName.isEmpty() && kit.getId().equalsIgnoreCase(starterKitName)) {
                    Msg.send(context, messages.get("commands.kit.not-found", Map.of("kit", kitName)));
                    return;
                }
            }
            
            // Check permission
            String permission = "essentials.kit." + kit.getId();
            if (!PermissionsModule.get().hasPermission(playerRef.getUuid(), permission)) {
                Msg.send(context, messages.get("commands.kit.no-permission"));
                return;
            }
            
            // Check cooldown (unless player has bypass permission)
            boolean canBypassCooldown = PermissionsModule.get().hasPermission(playerRef.getUuid(), COOLDOWN_BYPASS_PERMISSION);
            if (!canBypassCooldown) {
                long remainingCooldown = kitManager.getRemainingCooldown(playerRef.getUuid(), kit.getId());
                if (remainingCooldown > 0) {
                    Msg.send(context, messages.get("commands.kit.cooldown", 
                        Map.of("time", CooldownUtil.formatCooldown(remainingCooldown))));
                    return;
                }
            }
            
            // Set cooldown before applying kit
            if (kit.getCooldown() > 0 && !canBypassCooldown) {
                kitManager.setKitUsed(playerRef.getUuid(), kit.getId());
            }
            
            // Apply kit on world thread
            applyKitToPlayer(kit, playerRef, ref, store, world, () -> Msg.send(context, messages.get("commands.kit.received", Map.of("kit", kit.getDisplayName()))));
        }
    }
    
    /**
     * Helper method to apply a kit to a player on their world thread.
     * Used by both KitClaimCommand and KitGiveCommand.
     * 
     * @param onSuccess Callback executed after kit is successfully applied (runs on world thread)
     */
    private static void applyKitToPlayer(@Nonnull Kit kit, @Nonnull PlayerRef playerRef,
                                         @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                         @Nonnull World world, @Nonnull Runnable onSuccess) {
        world.execute(() -> {
            if (!ref.isValid()) {
                return;
            }
            
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                Log.error("Failed to get Player component for " + playerRef.getUsername());
                return;
            }
            
            Inventory inventory = player.getInventory();
            if (inventory == null) {
                Log.error("Failed to get inventory for " + playerRef.getUsername());
                return;
            }
            
            KitManager.applyKit(kit, inventory, ref, store);
            player.sendInventory();

            onSuccess.run();
        });
    }
    
    /**
     * Usage variant for /kit <kitname> <player> - Give kit to another player
     * Can be executed by players or console.
     * Does not check target player's permissions or cooldowns.
     * Does not modify any cooldowns.
     * Requires essentials.kit.other permission.
     */
    private static class KitGiveCommand extends AbstractCommand {
        private final RequiredArg<String> kitNameArg;
        private final RequiredArg<PlayerRef> targetPlayerArg;
        private final KitManager kitManager;
        
        KitGiveCommand(@Nonnull KitManager kitManager) {
            super("Give a kit to another player");  // Description only, no command name!
            this.kitManager = kitManager;
            this.kitNameArg = withRequiredArg("kitname", "Kit to give", ArgTypes.STRING);
            this.targetPlayerArg = withRequiredArg("player", "Player to give kit to", ArgTypes.PLAYER_REF);
            requirePermission("essentials.kit.other");
        }
        
        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            MessageManager messages = Essentials.getInstance().getMessageManager();
            String kitName = context.get(kitNameArg);
            PlayerRef targetPlayer = context.get(targetPlayerArg);
            
            // Get kit (case-insensitive)
            Kit kit = kitManager.getKit(kitName.toLowerCase());
            if (kit == null) {
                Msg.send(context, messages.get("commands.kit.not-found", Map.of("kit", kitName)));
                return CompletableFuture.completedFuture(null);
            }
            
            // Validate target player
            if (targetPlayer == null) {
                Msg.send(context, messages.get("commands.kit.player-not-found", Map.of("player", kitName)));
                return CompletableFuture.completedFuture(null);
            }
            
            // Get target player's ref and store
            Ref<EntityStore> targetRef = targetPlayer.getReference();
            if (targetRef == null || !targetRef.isValid()) {
                Msg.send(context, messages.get("commands.kit.player-not-found", Map.of("player", targetPlayer.getUsername())));
                return CompletableFuture.completedFuture(null);
            }
            
            Store<EntityStore> targetStore = targetRef.getStore();
            
            // Get target player's world and execute on their thread
            World targetWorld = targetStore.getExternalData().getWorld();

            // Apply kit on target player's world thread (no permission or cooldown checks)
            applyKitToPlayer(kit, targetPlayer, targetRef, targetStore, targetWorld, () -> {
                // Send messages to both players
                Msg.send(targetPlayer, messages.get("commands.kit.received", Map.of("kit", kit.getDisplayName())));
                Msg.send(context, messages.get("commands.kit.given", 
                    Map.of("kit", kit.getDisplayName(), "player", targetPlayer.getUsername())));
            });
            
            return CompletableFuture.completedFuture(null);
        }
    }
}
