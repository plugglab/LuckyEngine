package com.lucky.luckyblock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Tracks placed Lucky Blocks (yellow glass + inner sponge BlockDisplay).
 * Stores one display entity UUID per location; removes it on break.
 */
public final class LuckyBlockTracker {

    // loc key → inner sponge display UUID string
    private static final Map<String, String> entries = new HashMap<>();
    private static File dataFile;

    private LuckyBlockTracker() {}

    public static void init(LuckyBlockPlugin plugin) {
        dataFile = new File(plugin.getDataFolder(), "lucky_blocks.yml");
        load();
    }

    private static String key(Location loc) {
        return loc.getWorld().getName()
                + ";" + loc.getBlockX()
                + ";" + loc.getBlockY()
                + ";" + loc.getBlockZ();
    }

    public static void mark(Location loc, UUID displayUUID) {
        entries.put(key(loc), displayUUID.toString());
        save();
    }

    public static void unmark(Location loc) {
        String uuid = entries.remove(key(loc));
        if (uuid != null) removeEntity(uuid);
        save();
    }

    public static boolean isMarked(Location loc) {
        return entries.containsKey(key(loc));
    }

    private static void removeEntity(String uuidStr) {
        try {
            Entity e = Bukkit.getEntity(UUID.fromString(uuidStr));
            if (e != null) e.remove();
        } catch (Exception ignored) {}
    }

    private static void load() {
        if (dataFile == null || !dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        entries.clear();
        for (String k : cfg.getKeys(false)) {
            String uuid = cfg.getString(k);
            if (uuid != null) entries.put(k, uuid);
        }
    }

    private static void save() {
        if (dataFile == null) return;
        YamlConfiguration cfg = new YamlConfiguration();
        entries.forEach(cfg::set);
        try { cfg.save(dataFile); }
        catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to save lucky_blocks.yml", e);
        }
    }
}
