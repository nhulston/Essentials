package com.nhulston.essentials.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * A GUI page for selecting kits.
 */
public class KitPage extends InteractiveCustomUIPage<KitPage.KitPageData> {

    private static final Kit[] KITS = {
        new Kit("soldier", "Soldier", "A balanced fighter with sword and shield", "Weapon_Axe_Mithril"),
        new Kit("brute", "Brute", "Heavy hitter with massive damage", "Weapon_Axe_Mithril"),
        new Kit("ninja", "Ninja", "Fast and stealthy assassin", "Weapon_Axe_Mithril"),
        new Kit("archer", "Archer", "Ranged specialist with bow and arrows", "Weapon_Axe_Mithril")
    };

    public KitPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, KitPageData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/Essentials_KitPage.ui");

        for (int i = 0; i < KITS.length; i++) {
            Kit kit = KITS[i];
            String selector = "#KitCards[" + i + "]";
            
            commandBuilder.append("#KitCards", "Pages/Essentials_KitEntry.ui");
            commandBuilder.set(selector + " #Name.Text", kit.displayName);
            commandBuilder.set(selector + " #Description.Text", kit.description);
            commandBuilder.set(selector + " #Icon.ItemId", kit.iconItemId);
            
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, 
                selector, 
                EventData.of("Kit", kit.id)
            );
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull KitPageData data) {
        if (data.kit != null && !data.kit.isEmpty()) {
            // Execute kill command as proof of concept
            String command = "kill " + playerRef.getUsername();
            CommandManager.get().handleCommand(ConsoleSender.INSTANCE, command);
            
            // Close the page
            this.close();
        }
    }

    /**
     * Kit definition.
     */
    private record Kit(String id, String displayName, String description, String iconItemId) {}

    /**
     * Event data for kit selection.
     */
    public static class KitPageData {
        public static final BuilderCodec<KitPageData> CODEC = BuilderCodec.builder(KitPageData.class, KitPageData::new)
            .append(new KeyedCodec<>("Kit", Codec.STRING), (data, s) -> data.kit = s, data -> data.kit)
            .add()
            .build();

        private String kit;

        public String getKit() {
            return kit;
        }
    }
}
