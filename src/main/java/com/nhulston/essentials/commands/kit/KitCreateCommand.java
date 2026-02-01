package com.nhulston.essentials.commands.kit;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.nhulston.essentials.Essentials;
import com.nhulston.essentials.managers.KitManager;
import com.nhulston.essentials.models.KitItem;
import com.nhulston.essentials.util.MessageManager;
import com.nhulston.essentials.util.Msg;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Subcommand to create a kit from the player's current inventory.
 * Usage: /kit create <name>
 */
public class KitCreateCommand extends AbstractPlayerCommand {
    private final KitManager kitManager;
    private final MessageManager messages;
    private final RequiredArg<String> nameArg;

    public KitCreateCommand(@Nonnull KitManager kitManager) {
        super("create", "Create a kit from your current inventory");
        this.kitManager = kitManager;
        this.messages = Essentials.getInstance().getMessageManager();

        requirePermission("essentials.kit.create");
        this.nameArg = withRequiredArg("name", "Kit name", ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String kitName = context.get(nameArg);

        // Validate kit name
        if (!kitName.matches("^[a-zA-Z0-9_-]+$")) {
            Msg.send(context, messages.get("commands.kit.create.invalid-name"));
            return;
        }

        // Prevent reserved names
        if (kitName.equalsIgnoreCase("create") || kitName.equalsIgnoreCase("delete")) {
            Msg.send(context, messages.get("commands.kit.create.reserved-name", Map.of("name", kitName)));
            return;
        }

        // Check if kit already exists
        if (kitManager.getKit(kitName) != null) {
            Msg.send(context, messages.get("commands.kit.create.already-exists", Map.of("name", kitName)));
            return;
        }

        // Get player's inventory
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            Msg.send(context, messages.get("commands.kit.create.inventory-error"));
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            Msg.send(context, messages.get("commands.kit.create.inventory-error"));
            return;
        }

        List<KitItem> items = new ArrayList<>();

        // Collect items from hotbar
        collectItems(inventory.getHotbar(), "hotbar", items);

        // Collect items from storage
        collectItems(inventory.getStorage(), "storage", items);

        // Collect items from armor
        collectItems(inventory.getArmor(), "armor", items);

        // Collect items from utility
        collectItems(inventory.getUtility(), "utility", items);

        // Collect items from tools
        collectItems(inventory.getTools(), "tools", items);

        if (items.isEmpty()) {
            Msg.send(context, messages.get("commands.kit.create.empty-inventory"));
            return;
        }

        // Create the kit
        kitManager.createKit(kitName, items);

        Msg.send(context, messages.get("commands.kit.create.success", Map.of("name", kitName, "count", String.valueOf(items.size()))));
        Msg.send(context, messages.get("commands.kit.create.config-info"));
    }

    /**
     * Collects non-empty items from an item container, filtering out editor tools
     */
    private void collectItems(@Nonnull ItemContainer container, @Nonnull String section, @Nonnull List<KitItem> items) {
        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack itemStack = container.getItemStack(slot);
            if (itemStack != null && !itemStack.isEmpty()) {
                String itemId = itemStack.getItemId();
                
                // Skip editor tools and other creative-only items
                if (isEditorItem(itemId)) {
                    continue;
                }
                
                items.add(new KitItem(
                        itemId,
                        itemStack.getQuantity(),
                        section,
                        slot
                ));
            }
        }
    }

    /**
     * Checks if an item is an editor/creative-only item that should be filtered out
     */
    private boolean isEditorItem(@Nonnull String itemId) {
        return itemId.startsWith("EditorTool_") ||
               itemId.startsWith("Editor_") ||
               itemId.startsWith("Debug_") ||
               itemId.startsWith("Admin_");
    }
}
