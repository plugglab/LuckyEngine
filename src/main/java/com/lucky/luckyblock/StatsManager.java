package com.lucky.luckyblock;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Tracks per-player Lucky Block break counts.
 *
 * Stores:
 *  - A cumulative all-time total per player
 *  - A rolling list of break timestamps (epoch ms) for the last 31 days,
 *    used to derive hourly / daily / weekly / monthly counts on demand.
 *
 * Persisted to plugins/LuckyBlock/stats.yml.
 */
public class StatsManager {

    // Keep timestamps for 31 days max; anything older is pruned on load/record
    private static final long PRUNE_AGE_MS = 31L * 24 * 60 * 60 * 1000;

    public static final long HOUR_MS    =       60L * 60 * 1000;
    public static final long DAY_MS     =  24L * 60 * 60 * 1000;
    public static final long WEEK_MS    =   7L * 24 * 60 * 60 * 1000;
    public static final long MONTH_MS   =  30L * 24 * 60 * 60 * 1000;

    private final LuckyBlockPlugin plugin;
    private File dataFile;

    // uuid → all-time total (never decremented)
    private final Map<UUID, Integer> totals  = new HashMap<>();
    // uuid → list of recent break timestamps (millis), pruned to 31 days
    private final Map<UUID, List<Long>> recent = new HashMap<>();

    public StatsManager(LuckyBlockPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        dataFile = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    // ── Record ────────────────────────────────────────────────────────────────

    public void recordBreak(UUID uuid) {
        long now = System.currentTimeMillis();
        totals.merge(uuid, 1, Integer::sum);
        recent.computeIfAbsent(uuid, k -> new ArrayList<>()).add(now);
        prune(uuid, now);
        save();
    }

    // ── Query — per player ────────────────────────────────────────────────────

    public int getTotal(UUID uuid) {
        return totals.getOrDefault(uuid, 0);
    }

    public int getInPeriod(UUID uuid, long periodMs) {
        long cutoff = System.currentTimeMillis() - periodMs;
        List<Long> times = recent.getOrDefault(uuid, Collections.emptyList());
        int count = 0;
        for (long t : times) if (t >= cutoff) count++;
        return count;
    }

    // ── Query — global (all players combined) ─────────────────────────────────

    public int getGlobalTotal() {
        return totals.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getGlobalInPeriod(long periodMs) {
        long cutoff = System.currentTimeMillis() - periodMs;
        int count = 0;
        for (List<Long> times : recent.values())
            for (long t : times) if (t >= cutoff) count++;
        return count;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void prune(UUID uuid, long now) {
        List<Long> list = recent.get(uuid);
        if (list == null) return;
        list.removeIf(t -> (now - t) > PRUNE_AGE_MS);
    }

    private void load() {
        if (dataFile == null || !dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        long now = System.currentTimeMillis();

        // totals section
        if (cfg.isConfigurationSection("total")) {
            for (String key : cfg.getConfigurationSection("total").getKeys(false)) {
                try { totals.put(UUID.fromString(key), cfg.getInt("total." + key)); }
                catch (Exception ignored) {}
            }
        }

        // recent section
        if (cfg.isConfigurationSection("recent")) {
            for (String key : cfg.getConfigurationSection("recent").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    List<Long> times = new ArrayList<>();
                    for (long t : cfg.getLongList("recent." + key))
                        if ((now - t) <= PRUNE_AGE_MS) times.add(t);
                    if (!times.isEmpty()) recent.put(uuid, times);
                } catch (Exception ignored) {}
            }
        }
    }

    private void save() {
        if (dataFile == null) return;
        YamlConfiguration cfg = new YamlConfiguration();
        totals.forEach((uuid, n) -> cfg.set("total." + uuid, n));
        recent.forEach((uuid, times) -> cfg.set("recent." + uuid, times));
        try { cfg.save(dataFile); }
        catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save stats.yml", e);
        }
    }
}
