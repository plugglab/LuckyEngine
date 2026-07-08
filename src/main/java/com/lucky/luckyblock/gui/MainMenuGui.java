package com.lucky.luckyblock.gui;

import com.lucky.luckyblock.LangManager;
import com.lucky.luckyblock.LuckManager;
import com.lucky.luckyblock.LuckyBlockCommand;
import com.lucky.luckyblock.StatsManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class MainMenuGui extends BaseGui {

    public MainMenuGui(GuiManager manager, Player player) {
        super(manager, player);
    }

    @Override
    public Inventory build() {
        LangManager lang = manager.getPlugin().getLangManager();
        LuckManager lm   = manager.getPlugin().getLuckManager();
        int luck = lm.getLuck(player);
        String luckFmt = lm.formatLuck(luck);
        String bar     = luckBar(luck);
        int total      = manager.getPlugin().getRewardManager().getRewards().size();
        boolean hudOn  = manager.getPlugin().getConfig().getBoolean("hud.enabled", true);
        boolean bcastOn= manager.getPlugin().getConfig().getBoolean("broadcast-rewards", true);

        Inventory inv = chest(5, lang.get("gui-main-title"));

        // ── Top border ────────────────────────────────────────────────────────
        fillRow(inv, 0, Material.BLACK_STAINED_GLASS_PANE);

        // ── Luck display (centre top) ─────────────────────────────────────────
        inv.setItem(4, item(Material.NETHER_STAR,
                lang.get("gui-main-luck-item"),
                "&7Value: " + luckFmt,
                "&7" + bar,
                "",
                "&8Tier: " + lm.formatLuckTier(luck, lang)));

        // ── Row 1: main actions ───────────────────────────────────────────────
        inv.setItem(10, item(Material.CHEST,
                lang.get("gui-main-browse"),
                lang.get("gui-main-browse-lore"),
                "&8Total rewards: &e" + total));

        inv.setItem(12, item(Material.WRITABLE_BOOK,
                lang.get("gui-main-create"),
                lang.get("gui-main-create-lore")));

        inv.setItem(14, item(Material.YELLOW_STAINED_GLASS,
                lang.get("gui-main-give"),
                lang.get("gui-main-give-lore"),
                "&8Requires luckyblock.admin"));

        inv.setItem(16, item(Material.COMPARATOR,
                lang.get("gui-main-reload"),
                lang.get("gui-main-reload-lore"),
                "&8Requires luckyblock.admin"));

        // ── Row 2: luck management ────────────────────────────────────────────
        inv.setItem(19, item(Material.CLOCK,
                lang.get("gui-main-setluck"),
                lang.get("gui-main-setluck-lore"),
                "",
                "&eLeft-click:  §7set exact value",
                "&eRight-click: §7reset to default (0)"));

        inv.setItem(20, item(Material.LIME_DYE,
                "&a+ Add Luck",
                "&7Left-click: &f+10 luck",
                "&7Right-click: &f+25 luck",
                "&8Requires luckyblock.admin"));

        inv.setItem(21, item(Material.RED_DYE,
                "&c- Remove Luck",
                "&7Left-click: &f-10 luck",
                "&7Right-click: &f-25 luck",
                "&8Requires luckyblock.admin"));

        inv.setItem(23, item(Material.BOOK,
                "&7Luck Info",
                "&8Your current luck: " + luckFmt,
                "&8Bar: &r" + bar,
                "&8Tier: " + lm.formatLuckTier(luck, lang),
                "",
                "&8Range: &f-100 &8(unlucky) → &f+100 &8(lucky)"));

        // ── Row 3: settings toggles ───────────────────────────────────────────
        inv.setItem(28, item(
                hudOn ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                "&7Break HUD: " + (hudOn ? "&aENABLED" : "&cDISABLED"),
                "&8Shows luck/tier info when a block is broken",
                "", "&eClick to toggle &8(admin only)"));

        inv.setItem(29, item(
                bcastOn ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                "&7Server Broadcast: " + (bcastOn ? "&aENABLED" : "&cDISABLED"),
                "&8Announces LEGENDARY drops to all players",
                "", "&eClick to toggle &8(admin only)"));

        StatsManager stats = manager.getPlugin().getStatsManager();
        java.util.UUID uid = player.getUniqueId();

        inv.setItem(31, item(Material.PAPER,
                lang.get("gui-main-stats"),
                "&8Rewards loaded: &e" + total,
                "&8Your luck: " + luckFmt,
                "&8Language: &e" + manager.getPlugin().getConfig().getString("language","en_US"),
                "",
                lang.get("gui-main-stats-breaks-header"),
                lang.get("gui-main-stats-hourly",  "n", String.valueOf(stats.getInPeriod(uid, StatsManager.HOUR_MS))),
                lang.get("gui-main-stats-daily",   "n", String.valueOf(stats.getInPeriod(uid, StatsManager.DAY_MS))),
                lang.get("gui-main-stats-weekly",  "n", String.valueOf(stats.getInPeriod(uid, StatsManager.WEEK_MS))),
                lang.get("gui-main-stats-monthly", "n", String.valueOf(stats.getInPeriod(uid, StatsManager.MONTH_MS))),
                lang.get("gui-main-stats-total",   "n", String.valueOf(stats.getTotal(uid)))));

        inv.setItem(33, item(Material.CRAFTING_TABLE,
                "&7Crafting Recipe",
                "&8Shape: &f8 Gold Blocks + 1 Dispenser",
                "&8(configurable in config.yml)",
                "",
                "&7G G G",
                "&7G D G   →  &6✦ Lucky Block",
                "&7G G G"));

        // ── Bottom border ─────────────────────────────────────────────────────
        fillRow(inv, 4, Material.BLACK_STAINED_GLASS_PANE);

        fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);
        return inv;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        LangManager lang = manager.getPlugin().getLangManager();

        switch (slot) {
            // ── Browse ────────────────────────────────────────────────────────
            case 10 -> manager.openRewardList(player, 0);

            // ── Create ────────────────────────────────────────────────────────
            case 12 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage(lang.get("gui-main-no-perm")); return;
                }
                manager.openRewardEditor(player, new RewardEditorGui.EditorState());
            }

            // ── Give self ─────────────────────────────────────────────────────
            case 14 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage(lang.get("gui-main-no-perm")); return;
                }
                player.closeInventory();
                player.getInventory().addItem(
                        new LuckyBlockCommand(manager.getPlugin()).buildLuckyBlockItem(1));
                player.sendMessage(lang.get("give-success-target", "amount", "1"));
            }

            // ── Reload ────────────────────────────────────────────────────────
            case 16 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage(lang.get("gui-main-no-perm")); return;
                }
                manager.getPlugin().reload();
                player.sendMessage(lang.get("gui-main-reload-done",
                        "count", String.valueOf(manager.getPlugin().getRewardManager().getRewards().size())));
                manager.openMainMenu(player); // refresh
            }

            // ── Set luck (left = chat input, right = reset) ───────────────────
            case 19 -> {
                if (event.getClick().isRightClick()) {
                    int def = manager.getPlugin().getConfig().getInt("default-luck", 0);
                    manager.getPlugin().getLuckManager().setLuck(player, def);
                    player.sendMessage(lang.get("luck-set-notify",
                            "luck", manager.getPlugin().getLuckManager().formatLuck(def)));
                    manager.openMainMenu(player);
                } else {
                    manager.startChatInput(player, new ChatInputSession(
                            "Enter your new luck value (-100 to 100):", null,
                            (val, s) -> {
                                try {
                                    int v = Integer.parseInt(val.trim());
                                    manager.getPlugin().getLuckManager().setLuck(player, v);
                                    player.sendMessage(lang.get("luck-set-notify",
                                            "luck", manager.getPlugin().getLuckManager().formatLuck(
                                                    manager.getPlugin().getLuckManager().getLuck(player))));
                                } catch (NumberFormatException ignored) {
                                    player.sendMessage(lang.get("invalid-number", "input", val));
                                }
                                manager.openMainMenu(player);
                            }
                    ));
                }
            }

            // ── Add luck ──────────────────────────────────────────────────────
            case 20 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage(lang.get("gui-main-no-perm")); return;
                }
                int delta = event.getClick().isRightClick() ? 25 : 10;
                manager.getPlugin().getLuckManager().addLuck(player, delta);
                player.sendMessage(lang.get("luck-add-notify",
                        "luck", manager.getPlugin().getLuckManager().formatLuck(
                                manager.getPlugin().getLuckManager().getLuck(player))));
                manager.openMainMenu(player);
            }

            // ── Remove luck ───────────────────────────────────────────────────
            case 21 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage(lang.get("gui-main-no-perm")); return;
                }
                int delta = event.getClick().isRightClick() ? 25 : 10;
                manager.getPlugin().getLuckManager().addLuck(player, -delta);
                player.sendMessage(lang.get("luck-add-notify",
                        "luck", manager.getPlugin().getLuckManager().formatLuck(
                                manager.getPlugin().getLuckManager().getLuck(player))));
                manager.openMainMenu(player);
            }

            // ── Toggle HUD ────────────────────────────────────────────────────
            case 28 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage(lang.get("gui-main-no-perm")); return;
                }
                boolean cur = manager.getPlugin().getConfig().getBoolean("hud.enabled", true);
                manager.getPlugin().getConfig().set("hud.enabled", !cur);
                manager.getPlugin().saveConfig();
                manager.openMainMenu(player);
            }

            // ── Toggle broadcast ──────────────────────────────────────────────
            case 29 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage(lang.get("gui-main-no-perm")); return;
                }
                boolean cur = manager.getPlugin().getConfig().getBoolean("broadcast-rewards", true);
                manager.getPlugin().getConfig().set("broadcast-rewards", !cur);
                manager.getPlugin().saveConfig();
                manager.openMainMenu(player);
            }
        }
    }

    private String luckBar(int luck) {
        int filled = (int) Math.round((luck + 100) / 20.0);
        filled = Math.max(0, Math.min(10, filled));
        String col = luck >= 0 ? "§a" : "§c";
        return col + "■".repeat(filled) + "§8" + "□".repeat(10 - filled);
    }
}
