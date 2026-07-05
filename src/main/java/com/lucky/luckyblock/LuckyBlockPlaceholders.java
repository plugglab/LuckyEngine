package com.lucky.luckyblock;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers %luckyblock_<placeholder>% with PlaceholderAPI.
 *
 * Available placeholders:
 *   %luckyblock_luck%           → raw luck number, e.g. 42
 *   %luckyblock_luck_formatted% → coloured luck string, e.g. §a+42
 *   %luckyblock_luck_bar%       → 10-char progress bar
 *   %luckyblock_luck_tier%      → Very Lucky / Lucky / Neutral / Unlucky / Very Unlucky
 *   %luckyblock_total_rewards%  → how many rewards are loaded
 */
public class LuckyBlockPlaceholders extends PlaceholderExpansion {

    private final LuckyBlockPlugin plugin;

    public LuckyBlockPlaceholders(LuckyBlockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "luckyblock"; }
    @Override public @NotNull String getAuthor()     { return "Claude"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        LuckManager lm = plugin.getLuckManager();
        int luck = lm.getLuck(player);

        return switch (params.toLowerCase()) {
            case "luck"           -> String.valueOf(luck);
            case "luck_formatted" -> lm.formatLuck(luck);
            case "luck_bar"       -> buildBar(luck);
            case "luck_tier"      -> luckTier(luck);
            case "total_rewards"  -> String.valueOf(plugin.getRewardManager().getRewards().size());
            default               -> null;
        };
    }

    /** Builds a 10-segment bar where filled = §a■ (good) or §c■ (bad), empty = §8□ */
    private String buildBar(int luck) {
        // Map -100..100 → 0..10
        int filled = (int) Math.round((luck + 100) / 20.0);
        filled = Math.max(0, Math.min(10, filled));
        String color = luck >= 0 ? "§a" : "§c";
        return color + "■".repeat(filled) + "§8" + "□".repeat(10 - filled);
    }

    private String luckTier(int luck) {
        if (luck >= 75)  return "§5§lVery Lucky";
        if (luck >= 25)  return "§aLucky";
        if (luck >= -24) return "§eNeutral";
        if (luck >= -74) return "§cUnlucky";
        return "§4§lVery Unlucky";
    }
}
