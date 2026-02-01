package com.nhulston.essentials.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper for TOML file migration - adds missing sections from default resource.
 */
public class TomlMigrationHelper {
    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\[([a-zA-Z0-9_.-]+)]\\s*$");

    public static void migrateToml(@Nonnull Path userPath, @Nonnull String resourceName) {
        String defaultContent = loadFromResources(resourceName);
        if (defaultContent == null) return;

        try {
            String userContent = readWithBom(userPath);
            Set<String> userSections = findSections(userContent);
            Map<String, String> defaultSections = extractSections(defaultContent);
            
            List<String> missing = new ArrayList<>();
            for (String section : defaultSections.keySet()) {
                if (!userSections.contains(section)) missing.add(section);
            }
            
            if (missing.isEmpty()) return;
            
            StringBuilder newContent = new StringBuilder(userContent);
            if (!userContent.endsWith("\n")) newContent.append("\n");
            
            for (String section : missing) {
                newContent.append("\n").append(defaultSections.get(section));
                Log.info("Added missing section: [" + section + "] to " + userPath.getFileName());
            }
            
            Files.writeString(userPath, newContent.toString(), StandardCharsets.UTF_8);
            Log.info("Migrated " + userPath.getFileName() + " with " + missing.size() + " new section(s).");
        } catch (Exception e) {
            Log.warning("Migration skipped for " + userPath.getFileName() + ": " + e.getMessage());
        }
    }

    public static void createDefault(@Nonnull Path path, @Nonnull String resourceName) {
        try {
            Files.createDirectories(path.getParent());
            try (InputStream is = TomlMigrationHelper.class.getClassLoader().getResourceAsStream(resourceName)) {
                if (is != null) {
                    Files.copy(is, path);
                    Log.info("Created default " + path.getFileName());
                }
            }
        } catch (IOException e) {
            Log.error("Failed to create default " + path.getFileName() + ": " + e.getMessage());
        }
    }

    @Nonnull
    public static String readWithBom(@Nonnull Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF)
            ? new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8)
            : new String(bytes, StandardCharsets.UTF_8);
    }

    @Nullable
    private static String loadFromResources(@Nonnull String resourceName) {
        try (InputStream is = TomlMigrationHelper.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private static Set<String> findSections(@Nonnull String content) {
        Set<String> sections = new LinkedHashSet<>();
        for (String line : content.split("\n")) {
            Matcher m = SECTION_PATTERN.matcher(line.trim());
            if (m.matches()) sections.add(m.group(1));
        }
        return sections;
    }

    private static Map<String, String> extractSections(@Nonnull String content) {
        Map<String, String> sections = new LinkedHashMap<>();
        String[] lines = content.split("\n");
        String currentSection = null;
        StringBuilder currentContent = new StringBuilder();
        List<String> pendingComments = new ArrayList<>();
        
        for (String line : lines) {
            Matcher m = SECTION_PATTERN.matcher(line.trim());
            if (m.matches()) {
                if (currentSection != null) sections.put(currentSection, currentContent.toString());
                currentSection = m.group(1);
                currentContent = new StringBuilder();
                for (String comment : pendingComments) currentContent.append(comment).append("\n");
                pendingComments.clear();
                currentContent.append(line).append("\n");
            } else if (currentSection != null) {
                currentContent.append(line).append("\n");
            } else if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                pendingComments.add(line);
            }
        }
        if (currentSection != null) sections.put(currentSection, currentContent.toString());
        return sections;
    }
}
