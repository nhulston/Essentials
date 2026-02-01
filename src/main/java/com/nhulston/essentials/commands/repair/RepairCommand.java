package com.nhulston.essentials.commands.repair;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.models.PlayerData;
import com.nhulston.essentials.util.ConfigManager;
import com.nhulston.essentials.util.CooldownUtil;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;
import com.nhulston.essentials.util.SoundUtil;
import com.nhulston.essentials.util.StorageManager;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;

/**
 * Command to repair the item in the player's hand.
 * Usage: /repair
 * Aliases: /fix
 */
public class RepairCommand extends AbstractPlayerCommand {
    private static final String COOLDOWN_BYPASS_PERMISSION = "essentials.repair.cooldown.bypass";

    private final ConfigManager configManager;
    private final StorageManager storageManager;
    private final MessageManager messages;

    public RepairCommand(@Nonnull ConfigManager configManager, @Nonnull StorageManager storageManager) {
        super("repair", "Repair the item in your hand");
        this.configManager = configManager;
        this.storageManager = storageManager;
        this.messages = Essentials.getInstance().getMessageManager();

        addAliases("fix");
        requirePermission("essentials.repair");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        UUID playerUuid = playerRef.getUuid();
        PlayerData data = storageManager.getPlayerData(playerUuid);

        // Check cooldown (skip if player has bypass permission)
        int cooldownSeconds = configManager.getRepairCooldown();
        boolean bypassCooldown = PermissionsModule.get().hasPermission(playerUuid, COOLDOWN_BYPASS_PERMISSION);
        if (cooldownSeconds > 0 && !bypassCooldown) {
            Long lastUse = data.getLastRepairTime();
            if (lastUse != null) {
                long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
                long remaining = cooldownSeconds - elapsed;
                if (remaining > 0) {
                    Msg.send(context, messages.get("commands.repair.cooldown", Map.of("time", CooldownUtil.formatCooldown(remaining))));
                    return;
                }
            }
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            Msg.send(context, messages.get("commands.repair.player-data-error"));
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            Msg.send(context, messages.get("commands.repair.inventory-error"));
            return;
        }

        ItemStack heldItem = inventory.getItemInHand();
        if (heldItem == null || heldItem.isEmpty()) {
            Msg.send(context, messages.get("commands.repair.no-item"));
            return;
        }

        double maxDurability = heldItem.getMaxDurability();
        if (maxDurability <= 0) {
            Msg.send(context, messages.get("commands.repair.cannot-repair"));
            return;
        }

        double currentDurability = heldItem.getDurability();
        if (currentDurability >= maxDurability) {
            Msg.send(context, messages.get("commands.repair.already-full"));
            return;
        }

        // Set cooldown before repair
        data.setLastRepairTime(System.currentTimeMillis());
        storageManager.savePlayerData(playerUuid);

        // Create repaired item and replace in hotbar
        ItemStack repairedItem = heldItem.withDurability(maxDurability);
        ItemContainer hotbar = inventory.getHotbar();
        byte activeSlot = inventory.getActiveHotbarSlot();
        hotbar.setItemStackForSlot(activeSlot, repairedItem);

        // Sync inventory to client
        player.sendInventory();

        SoundUtil.playSound(playerRef, "SFX_Item_Repair");
        Msg.send(context, messages.get("commands.repair.success"));
    }
}
