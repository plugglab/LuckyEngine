package com.lucky.luckyblock;

import org.bukkit.ChatColor;

/**
 * Defines the four Lucky Block rarities.
 * Each has a skull texture (base64), display colour, and a luck bonus
 * applied temporarily when the block is broken.
 *
 * Textures can be replaced — they are standard Minecraft skull base64 values.
 * Use https://namemc.com/ or https://minecraft-heads.com/ to find new ones.
 */
public enum LuckyBlockRarity {

    COMMON(
        "COMMON",
        "&a✦ Common Lucky Block",
        "&aCommon",
        0,   // no luck bonus — pure base luck
        // Yellow star / lucky block classic look
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTE5ZDI4YTg2MzJmYTRkODdjYTE5OWJiYzJlODhjZjM2OGRlZGQ1NTc0NzAxN2FlMzQ4NDM1NjlmN2E2MzRjNSJ9fX0=",
        ChatColor.GREEN,
        "TOTEM_OF_UNDYING"
    ),

    RARE(
        "RARE",
        "&9✦ Rare Lucky Block",
        "&9Rare",
        15,  // +15 luck on break
        // Blue orb / water block
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzY2ZDZjMTllNGY1MDUxODgyODUxYTRhZWFkNzlmMGYxZjM4YWE2ODk3MTliNmIzMzAzMTdlYTJiOGIwZTUwMCJ9fX0=",
        ChatColor.BLUE,
        "SOUL_FLAME"
    ),

    EPIC(
        "EPIC",
        "&5✦ Epic Lucky Block",
        "&5Epic",
        30,  // +30 luck on break
        // Purple / amethyst themed
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODk1NDFhZWI1YjQwMmRjMjUzMzcyZWI0MGRjYWRhZjMyZWY5MmNjYTgzMWJkMzVkMzJiNDE5OTcxMzBjYjJlZSJ9fX0=",
        ChatColor.DARK_PURPLE,
        "WITCH"
    ),

    LEGENDARY(
        "LEGENDARY",
        "&6&l✦ Legendary Lucky Block",
        "&6&lLegendary",
        50,  // +50 luck on break — near-guaranteed GREAT rewards
        // Red / fire / nether themed
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzE1NDRiZDcyNjA1ODI3YjlhODgwMjNlNzgzNjhiNDFlOGY4M2FjNWM5ZTQ3YzgyZmIxOTZmYzY3MmIyMmE3NiJ9fX0=",
        ChatColor.GOLD,
        "FIREWORKS_SPARK"
    );

    private final String id;
    private final String itemName;       // coloured item display name
    private final String shortName;      // coloured short name for HUD/chat
    private final int luckBonus;         // added to player luck for this single roll
    private final String texture;        // base64 skull texture value
    private final ChatColor color;       // for particles
    private final String particleName;   // Particle enum name for break effect

    LuckyBlockRarity(String id, String itemName, String shortName,
                     int luckBonus, String texture,
                     ChatColor color, String particleName) {
        this.id          = id;
        this.itemName    = itemName;
        this.shortName   = shortName;
        this.luckBonus   = luckBonus;
        this.texture     = texture;
        this.color       = color;
        this.particleName = particleName;
    }

    public String getId()           { return id; }
    public String getItemName()     { return itemName; }
    public String getShortName()    { return shortName; }
    public int getLuckBonus()       { return luckBonus; }
    public String getTexture()      { return texture; }
    public ChatColor getColor()     { return color; }
    public String getParticleName() { return particleName; }

    public static LuckyBlockRarity fromString(String s) {
        if (s == null) return COMMON;
        try { return valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return COMMON; }
    }

    /** Lore lines shown on the item. */
    public String[] getLore() {
        return new String[]{
            "&8Rarity: " + shortName,
            "&8Luck bonus: &f+" + luckBonus,
            "",
            "&7Place and break for a random reward.",
            "&8Higher rarity = better luck roll."
        };
    }
}
