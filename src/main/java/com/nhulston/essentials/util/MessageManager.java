package com.nhulston.essentials.util;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    private final Path messagesPath;
    private final Map<String, String> messages = new HashMap<>();

    public MessageManager(@Nonnull Path dataFolder) {
        this.messagesPath = dataFolder.resolve("messages.toml");
        load();
    }

    private void load() {
        if (!Files.exists(messagesPath)) {
            TomlMigrationHelper.createDefault(messagesPath, "messages.toml");
        } else {
            TomlMigrationHelper.migrateToml(messagesPath, "messages.toml");
        }

        try {
            String content = TomlMigrationHelper.readWithBom(messagesPath);
            TomlParseResult toml = Toml.parse(content);
            
            if (toml.hasErrors()) {
                toml.errors().forEach(error -> Log.error("Messages error: " + error.toString()));
                return;
            }

            loadMessagesRecursive(toml, "");
            Log.info("Messages loaded!");
        } catch (Exception e) {
            Log.error("Failed to load messages: " + e.getMessage());
        }
    }

    private void loadMessagesRecursive(@Nonnull TomlParseResult toml, @Nonnull String prefix) {
        for (String key : toml.keySet()) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (toml.isTable(key)) {
                var table = toml.getTable(key);
                if (table != null) loadMessagesFromTable(table, fullKey);
            } else {
                String value = toml.getString(key);
                if (value != null) messages.put(fullKey, value);
            }
        }
    }

    private void loadMessagesFromTable(@Nonnull org.tomlj.TomlTable table, @Nonnull String prefix) {
        for (String key : table.keySet()) {
            String fullKey = prefix + "." + key;
            if (table.isTable(key)) {
                var nested = table.getTable(key);
                if (nested != null) loadMessagesFromTable(nested, fullKey);
            } else {
                String value = table.getString(key);
                if (value != null) messages.put(fullKey, value);
            }
        }
    }

    @Nonnull
    public String get(@Nonnull String key, @Nullable Map<String, String> placeholders) {
        String message = messages.getOrDefault(key, "");
        if (message.isEmpty()) return "";
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    @Nonnull
    public String get(@Nonnull String key) {
        return get(key, null);
    }

    public void reload() {
        Log.info("Reloading messages...");
        messages.clear();
        load();
    }
}
