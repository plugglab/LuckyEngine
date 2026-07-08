package com.lucky.luckyblock;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * Loads the active language file from plugins/LuckyBlock/lang/<locale>.yml
 * and provides colour-translated message lookups with placeholder replacement.
 *
 * Usage:
 *   lang.get("prefix")
 *   lang.get("give-success-sender", "amount", "5", "target", "Steve")
 */
public class LangManager {

    private final LuckyBlockPlugin plugin;
    private YamlConfiguration lang;
    private YamlConfiguration fallback; // en_US always loaded as safety net

    public LangManager(LuckyBlockPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String locale = plugin.getConfig().getString("language", "en_US");
        saveDefault("en_US");
        saveDefault("pl_PL");

        fallback = loadFile("en_US");
        lang     = loadFile(locale);

        if (lang == null) {
            plugin.getLogger().warning("Lang file not found for '" + locale + "', falling back to en_US.");
            lang = fallback;
        }
        plugin.getLogger().info("Language loaded: " + locale);
    }

    /** Returns a colour-translated message by key, applying optional %key%→value replacements. */
    public String get(String key, String... pairs) {
        String raw = lang.getString(key);
        if (raw == null && fallback != null) raw = fallback.getString(key);
        if (raw == null) raw = "&c[missing: " + key + "]";

        // Apply replacements: pairs = ["placeholder", "value", ...]
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            raw = raw.replace("%" + pairs[i] + "%", pairs[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /** Convenience — prefix prepended to message. */
    public String prefixed(String key, String... pairs) {
        return get("prefix") + get(key, pairs);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void saveDefault(String locale) {
        File out = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        if (out.exists()) return;
        out.getParentFile().mkdirs();
        try (InputStream in = plugin.getResource("lang/" + locale + ".yml")) {
            if (in == null) {
                plugin.getLogger().warning("No bundled lang file for: " + locale);
                return;
            }
            Files.copy(in, out.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save lang/" + locale + ".yml", e);
        }
    }

    private YamlConfiguration loadFile(String locale) {
        File file = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        if (!file.exists()) return null;
        return YamlConfiguration.loadConfiguration(file);
    }
}
