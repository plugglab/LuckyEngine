package com.lucky.luckyblock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Tracks placed Lucky Block locations → rarity.
 * Only blocks placed from a Lucky Block item are tracked;
 * naturally-placed player heads are untouched.
 */
public final class LuckyBlockTracker {

    /** loc key → rarity name */
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

    public static void mark(Location loc, LuckyBlockRarity rarity) {
        entries.put(key(loc), rarity.name());
        save();
    }

    public static void unmark(Location loc) {
        entries.remove(key(loc));
        save();
    }

    public static boolean isMarked(Location loc) {
        return entries.containsKey(key(loc));
    }

    public static LuckyBlockRarity getRarity(Location loc) {
        return LuckyBlockRarity.fromString(entries.get(key(loc)));
    }

    private static void load() {
        if (dataFile == null || !dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        entries.clear();
        for (String k : cfg.getKeys(false)) {
            String val = cfg.getString(k);
            if (val != null) entries.put(k, val);
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
