package com.lucky.luckyblock;

import org.bukkit.configuration.ConfigurationSection;
import java.util.List;

/**
 * Represents a single configurable reward entry.
 */
public class Reward {

    public enum Type {
        ITEM, MULTI_ITEM, COMMAND, MOB_SPAWN, EXPLOSION,
        POTION_EFFECT, LIGHTNING, MESSAGE, STRUCTURE,
        CHEST_LOOT, ENCHANT_HELD, FIREWORKS, TRAP, XP
    }

    public enum Tier {
        GREAT, GOOD, NEUTRAL, BAD;

        public static Tier fromString(String s) {
            try { return valueOf(s.toUpperCase()); }
            catch (Exception e) { return NEUTRAL; }
        }
    }

    private final String id;
    private final Type type;
    private final Tier tier;
    private final int baseWeight;
    private final String displayName;
    private final ConfigurationSection section;

    public Reward(String id, Type type, Tier tier, int baseWeight, String displayName, ConfigurationSection section) {
        this.id = id;
        this.type = type;
        this.tier = tier;
        this.baseWeight = baseWeight;
        this.displayName = displayName;
        this.section = section;
    }

    /**
     * Returns the effective weight after luck adjustment.
     * Luck range: -100 (very unlucky) to +100 (very lucky).
     * At +100 luck: GREAT/GOOD rewards get 3x weight; BAD get 0.1x.
     * At -100 luck: BAD rewards get 3x; GREAT/GOOD get 0.1x.
     */
    public int getEffectiveWeight(int luck) {
        double factor = switch (tier) {
            case GREAT -> 1.0 + (luck / 100.0) * 2.0;   // 0.0 → 3.0
            case GOOD  -> 1.0 + (luck / 100.0) * 1.5;   // 0.0 → 2.5  (but clamped below)
            case BAD   -> 1.0 - (luck / 100.0) * 0.9;   // 1.9 → 0.1
            case NEUTRAL -> 1.0;
        };
        factor = Math.max(0.05, factor);
        return Math.max(1, (int) Math.round(baseWeight * factor));
    }

    public String getId() { return id; }
    public Type getType() { return type; }
    public Tier getTier() { return tier; }
    public int getBaseWeight() { return baseWeight; }
    public String getDisplayName() { return displayName; }
    public ConfigurationSection getSection() { return section; }

    public String getString(String path, String def) { return section.getString(path, def); }
    public int getInt(String path, int def) { return section.getInt(path, def); }
    public double getDouble(String path, double def) { return section.getDouble(path, def); }
    public boolean getBoolean(String path, boolean def) { return section.getBoolean(path, def); }
    public List<String> getStringList(String path) { return section.getStringList(path); }
    public List<?> getList(String path) { return section.getList(path); }
}
