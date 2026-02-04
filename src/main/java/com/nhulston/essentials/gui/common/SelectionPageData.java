package com.nhulston.essentials.gui.common;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Shared event data class for grid selection pages.
 * Used by WarpPage and HomePage to receive selection events from the UI.
 * Note: KitPage uses its own KitPageData with "Kit" as the codec key.
 */
public class SelectionPageData {
    
    /**
     * Codec for deserializing selection events from the UI.
     * Expects a "Selection" key with a string value containing the selected item's ID.
     */
    public static final BuilderCodec<SelectionPageData> CODEC = BuilderCodec
            .builder(SelectionPageData.class, SelectionPageData::new)
            .append(new KeyedCodec<>("Selection", Codec.STRING), 
                    (data, s) -> data.selection = s, 
                    data -> data.selection)
            .add()
            .build();

    private String selection;

    /**
     * Gets the ID of the selected item.
     * @return The selection ID, or null if no selection was made
     */
    public String getSelection() {
        return selection;
    }
    
    /**
     * Checks if a valid selection was made.
     * @return true if selection is non-null and non-empty
     */
    public boolean hasSelection() {
        return selection != null && !selection.isEmpty();
    }
}
