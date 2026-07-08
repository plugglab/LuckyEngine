package com.lucky.luckyblock;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers %luckyblock_<placeholder>% with PlaceholderAPI.
 *
 * ── Luck ──────────────────────────────────────────────────────────────
 *   %luckyblock_luck%                  raw luck value, e.g. 42
 *   %luckyblock_luck_formatted%        coloured string, e.g. §a+42
 *   %luckyblock_luck_bar%              10-char §a■■■■□□□□□□ bar
 *   %luckyblock_luck_tier%             tier label, e.g. Lucky
 *
 * ── Blocks broken — personal ──────────────────────────────────────────
 *   %luckyblock_breaks_total%          all-time total for this player
 *   %luckyblock_breaks_hourly%         breaks in the last 60 minutes
 *   %luckyblock_breaks_daily%          breaks in the last 24 hours
 *   %luckyblock_breaks_weekly%         breaks in the last 7 days
 *   %luckyblock_breaks_monthly%        breaks in the last 30 days
 *
 * ── Blocks broken — global (all players combined) ─────────────────────
 *   %luckyblock_breaks_global_total%
 *   %luckyblock_breaks_global_hourly%
 *   %luckyblock_breaks_global_daily%
 *   %luckyblock_breaks_global_weekly%
 *   %luckyblock_breaks_global_monthly%
 *
 * ── Misc ──────────────────────────────────────────────────────────────
 *   %luckyblock_total_rewards%         number of loaded rewards
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
        LuckManager  lm    = plugin.getLuckManager();
        StatsManager stats = plugin.getStatsManager();

        // Player-context placeholders require an online player
        if (player != null) {
            int luck = lm.getLuck(player);
            java.util.UUID uuid = player.getUniqueId();

            switch (params.toLowerCase()) {
                // ── Luck ──────────────────────────────────────────────────────
                case "luck"              -> { return String.valueOf(luck); }
                case "luck_formatted"    -> { return lm.formatLuck(luck); }
                case "luck_bar"          -> { return buildBar(luck); }
                case "luck_tier"         -> { return lm.formatLuckTier(luck, plugin.getLangManager()); }

                // ── Personal breaks ───────────────────────────────────────────
                case "breaks_total"      -> { return String.valueOf(stats.getTotal(uuid)); }
                case "breaks_hourly"     -> { return String.valueOf(stats.getInPeriod(uuid, StatsManager.HOUR_MS)); }
                case "breaks_daily"      -> { return String.valueOf(stats.getInPeriod(uuid, StatsManager.DAY_MS)); }
                case "breaks_weekly"     -> { return String.valueOf(stats.getInPeriod(uuid, StatsManager.WEEK_MS)); }
                case "breaks_monthly"    -> { return String.valueOf(stats.getInPeriod(uuid, StatsManager.MONTH_MS)); }
            }
        }

        // ── Global breaks (no player context needed) ──────────────────────────
        switch (params.toLowerCase()) {
            case "breaks_global_total"   -> { return String.valueOf(stats.getGlobalTotal()); }
            case "breaks_global_hourly"  -> { return String.valueOf(stats.getGlobalInPeriod(StatsManager.HOUR_MS)); }
            case "breaks_global_daily"   -> { return String.valueOf(stats.getGlobalInPeriod(StatsManager.DAY_MS)); }
            case "breaks_global_weekly"  -> { return String.valueOf(stats.getGlobalInPeriod(StatsManager.WEEK_MS)); }
            case "breaks_global_monthly" -> { return String.valueOf(stats.getGlobalInPeriod(StatsManager.MONTH_MS)); }
            case "total_rewards"         -> { return String.valueOf(plugin.getRewardManager().getRewards().size()); }
        }

        return null; // unknown placeholder
    }

    private String buildBar(int luck) {
        int filled = (int) Math.round((luck + 100) / 20.0);
        filled = Math.max(0, Math.min(10, filled));
        String color = luck >= 0 ? "§a" : "§c";
        return color + "■".repeat(filled) + "§8" + "□".repeat(10 - filled);
    }
}
