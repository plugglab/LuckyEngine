package com.lucky.luckyblock;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages per-player luck values (-100 to +100).
 * Persists to luck.yml in the plugin data folder.
 */
public class LuckManager {

    private final LuckyBlockPlugin plugin;
    private final Map<UUID, Integer> luckMap = new HashMap<>();
    private File dataFile;

    public LuckManager(LuckyBlockPlugin plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "luck.yml");
        load();
    }

    /** Gets a player's luck value, defaulting to config default. */
    public int getLuck(Player player) {
        return luckMap.getOrDefault(player.getUniqueId(),
                plugin.getConfig().getInt("default-luck", 0));
    }

    /** Sets a player's luck, clamped to [-100, 100]. */
    public void setLuck(Player player, int value) {
        luckMap.put(player.getUniqueId(), Math.max(-100, Math.min(100, value)));
        save();
    }

    /** Adds delta to a player's luck, clamped to [-100, 100]. */
    public void addLuck(Player player, int delta) {
        setLuck(player, getLuck(player) + delta);
    }

    /** Returns a colourful formatted luck display, e.g. "§a+45" or "§c-20". */
    public String formatLuck(int luck) {
        if (luck > 50)  return "§a§l+" + luck;
        if (luck > 10)  return "§a+" + luck;
        if (luck > -10) return "§e" + luck;
        if (luck > -50) return "§c" + luck;
        return "§4§l" + luck;
    }

    public void saveAll() { save(); }

    private void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : cfg.getKeys(false)) {
            try {
                luckMap.put(UUID.fromString(key), cfg.getInt(key));
            } catch (Exception ignored) {}
        }
    }

    private void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (var entry : luckMap.entrySet()) {
            cfg.set(entry.getKey().toString(), entry.getValue());
        }
        try { cfg.save(dataFile); }
        catch (IOException e) { plugin.getLogger().log(Level.WARNING, "Failed to save luck.yml", e); }
    }
}
