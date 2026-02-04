package com.nhulston.essentials.gui.common;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Builder utility for constructing grid-based selection pages.
 * Provides a consistent layout for kits, warps, homes, and other grid UIs.
 * 
 * This class handles:
 * - Grid layout with configurable columns
 * - Event binding for item selection
 */
public final class GridPageBuilder {
    
    /** Default number of items per row */
    public static final int DEFAULT_COLUMNS = 3;
    
    /** Default row height in pixels */
    private static final int ROW_HEIGHT = 128;
    
    /** Default codec key used for selection events */
    public static final String DEFAULT_SELECTION_KEY = "Selection";
    
    /** Default entry UI file */
    public static final String DEFAULT_ENTRY_UI = "Pages/Essentials_GridEntry.ui";
    
    private GridPageBuilder() {
        // Utility class
    }
    
    /**
     * Builds a grid page with full customization options.
     *
     * @param commandBuilder The UI command builder
     * @param eventBuilder   The UI event builder
     * @param entries        The entries to display
     * @param columns        Number of columns per row
     * @param pageUiFile     The .ui file for the page layout (e.g., "Pages/Essentials_GridPage.ui")
     * @param entryUiFile    The .ui file for each entry (e.g., "Pages/Essentials_GridEntry.ui")
     * @param selectionKey   The codec key for selection events (e.g., "Selection", "Kit")
     * @param rowsSelector   The selector for the rows container (e.g., "#Rows", "#KitRows")
     */
    public static void build(@Nonnull UICommandBuilder commandBuilder,
                             @Nonnull UIEventBuilder eventBuilder,
                             @Nonnull List<GridEntry> entries,
                             int columns,
                             @Nonnull String pageUiFile,
                             @Nonnull String entryUiFile,
                             @Nonnull String selectionKey,
                             @Nonnull String rowsSelector) {
        // Append the main page layout
        commandBuilder.append(pageUiFile);
        
        if (entries.isEmpty()) {
            return;
        }
        
        // Calculate total rows needed
        int totalRows = (int) Math.ceil((double) entries.size() / columns);
        
        for (int row = 0; row < totalRows; row++) {
            // Create a row group
            commandBuilder.appendInline(rowsSelector,
                    "Group { LayoutMode: Left; Anchor: (Height: " + ROW_HEIGHT + "); Padding: (Horizontal: 4); }");
            
            String rowSelector = rowsSelector + "[" + row + "]";
            
            // Calculate indices for this row
            int startIdx = row * columns;
            int endIdx = Math.min(startIdx + columns, entries.size());
            
            for (int col = 0; col < (endIdx - startIdx); col++) {
                int entryIdx = startIdx + col;
                GridEntry entry = entries.get(entryIdx);
                
                // Append entry to this row
                commandBuilder.append(rowSelector, entryUiFile);
                
                // Select the entry card within the row
                String cardSelector = rowSelector + "[" + col + "]";
                
                // Set entry content
                commandBuilder.set(cardSelector + " #Name.Text", entry.displayName());
                commandBuilder.set(cardSelector + " #Status.Text", entry.status());
                
                // Bind click event
                eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        cardSelector,
                        EventData.of(selectionKey, entry.id())
                );
            }
        }
    }
    

}
