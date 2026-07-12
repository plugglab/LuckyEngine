package com.lucky.luckyblock.gui;

import com.lucky.luckyblock.LangManager;
import com.lucky.luckyblock.LuckManager;
import com.lucky.luckyblock.LuckyBlockCommand;
import com.lucky.luckyblock.LuckyBlockRarity;
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
        LangManager  lang  = manager.getPlugin().getLangManager();
        LuckManager  lm    = manager.getPlugin().getLuckManager();
        StatsManager stats = manager.getPlugin().getStatsManager();
        int luck   = lm.getLuck(player);
        String luckFmt = lm.formatLuck(luck);
        String bar     = luckBar(luck);
        int total      = manager.getPlugin().getRewardManager().getRewards().size();
        boolean hudOn  = manager.getPlugin().getConfig().getBoolean("hud.enabled", true);
        boolean bcastOn= manager.getPlugin().getConfig().getBoolean("broadcast-rewards", true);
        java.util.UUID uid = player.getUniqueId();

        Inventory inv = chest(6, lang.get("gui-main-title"));

        // ── Row 0: header ─────────────────────────────────────────────────────
        fillRow(inv, 0, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(4, item(Material.NETHER_STAR,
                lang.get("gui-main-luck-item"),
                "&7Value: " + luckFmt,
                "&7" + bar,
                "&8Tier: " + lm.formatLuckTier(luck, lang)));

        // ── Row 1: main navigation ────────────────────────────────────────────
        inv.setItem(10, item(Material.CHEST,
                lang.get("gui-main-browse"),
                lang.get("gui-main-browse-lore"),
                "&8Rewards loaded: &e" + total));

        inv.setItem(12, item(Material.WRITABLE_BOOK,
                lang.get("gui-main-create"),
                lang.get("gui-main-create-lore")));

        inv.setItem(14, item(Material.COMPARATOR,
                lang.get("gui-main-reload"),
                lang.get("gui-main-reload-lore"),
                "&8Requires luckyblock.admin"));

        inv.setItem(16, item(Material.PAPER,
                lang.get("gui-main-stats"),
                "&8" + lang.get("gui-main-stats-breaks-header"),
                lang.get("gui-main-stats-hourly",  "n", String.valueOf(stats.getInPeriod(uid, StatsManager.HOUR_MS))),
                lang.get("gui-main-stats-daily",   "n", String.valueOf(stats.getInPeriod(uid, StatsManager.DAY_MS))),
                lang.get("gui-main-stats-weekly",  "n", String.valueOf(stats.getInPeriod(uid, StatsManager.WEEK_MS))),
                lang.get("gui-main-stats-monthly", "n", String.valueOf(stats.getInPeriod(uid, StatsManager.MONTH_MS))),
                lang.get("gui-main-stats-total",   "n", String.valueOf(stats.getTotal(uid)))));

        // ── Row 2: give by rarity ─────────────────────────────────────────────
        LuckyBlockCommand cmd = new LuckyBlockCommand(manager.getPlugin());
        inv.setItem(19, rarityGiveItem(cmd, LuckyBlockRarity.COMMON));
        inv.setItem(20, rarityGiveItem(cmd, LuckyBlockRarity.RARE));
        inv.setItem(21, rarityGiveItem(cmd, LuckyBlockRarity.EPIC));
        inv.setItem(22, rarityGiveItem(cmd, LuckyBlockRarity.LEGENDARY));
        inv.setItem(24, item(Material.BOOK, lang.get("gui-main-give-rarity-info"),
                lang.get("gui-main-give-rarity-desc"),
                "",
                "&aCommon   &8+0 luck",
                "&9Rare     &8+15 luck",
                "&5Epic     &8+30 luck",
                "&6Legendary &8+50 luck"));

        // ── Row 3: luck controls ──────────────────────────────────────────────
        inv.setItem(28, item(Material.CLOCK,
                lang.get("gui-main-setluck"),
                lang.get("gui-main-setluck-lore"),
                "",
                "&eLeft-click:  &7set exact value",
                "&eRight-click: &7reset to 0"));

        inv.setItem(29, item(Material.LIME_DYE,
                "&a+ Add Luck",
                "&7Left-click: &f+10   Right-click: &f+25",
                "&8Requires luckyblock.admin"));

        inv.setItem(30, item(Material.RED_DYE,
                "&c- Remove Luck",
                "&7Left-click: &f-10   Right-click: &f-25",
                "&8Requires luckyblock.admin"));

        // ── Row 4: settings toggles ───────────────────────────────────────────
        inv.setItem(37, item(
                hudOn ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                "&7Break HUD: " + (hudOn ? "&aENABLED" : "&cDISABLED"),
                "&8Shows luck/tier info on block break",
                "&eClick to toggle &8(admin only)"));

        inv.setItem(38, item(
                bcastOn ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                "&7Server Broadcast: " + (bcastOn ? "&aENABLED" : "&cDISABLED"),
                "&8Announces LEGENDARY drops to all players",
                "&eClick to toggle &8(admin only)"));

        inv.setItem(40, item(Material.CRAFTING_TABLE,
                "&7Crafting Recipe",
                "&88 &6Gold Blocks &7+ &81 &7Dispenser &7→ &aCommon Lucky Block",
                "&8(configurable in config.yml)",
                "",
                "&7G G G",
                "&7G D G   →  &aCommon Lucky Block",
                "&7G G G"));

        // ── Bottom border ─────────────────────────────────────────────────────
        fillRow(inv, 5, Material.BLACK_STAINED_GLASS_PANE);
        fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);
        return inv;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        LangManager lang = manager.getPlugin().getLangManager();
        LuckyBlockCommand cmd = new LuckyBlockCommand(manager.getPlugin());

        switch (slot) {
            case 10 -> manager.openRewardList(player, 0);

            case 12 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage(lang.get("gui-main-no-perm")); return;
                }
                manager.openRewardEditor(player, new RewardEditorGui.EditorState());
            }

            case 14 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage(lang.get("gui-main-no-perm")); return;
                }
                manager.getPlugin().reload();
                player.sendMessage(lang.get("gui-main-reload-done",
                        "count", String.valueOf(manager.getPlugin().getRewardManager().getRewards().size())));
                manager.openMainMenu(player);
            }

            // ── Give by rarity ────────────────────────────────────────────────
            case 19 -> giveRarity(cmd, lang, LuckyBlockRarity.COMMON);
            case 20 -> giveRarity(cmd, lang, LuckyBlockRarity.RARE);
            case 21 -> giveRarity(cmd, lang, LuckyBlockRarity.EPIC);
            case 22 -> giveRarity(cmd, lang, LuckyBlockRarity.LEGENDARY);

            // ── Set / adjust luck ─────────────────────────────────────────────
            case 28 -> {
                if (event.getClick().isRightClick()) {
                    manager.getPlugin().getLuckManager().setLuck(player, 0);
                    player.sendMessage(lang.get("luck-set-notify",
                            "luck", manager.getPlugin().getLuckManager().formatLuck(0)));
                    manager.openMainMenu(player);
                } else {
                    manager.startChatInput(player, new ChatInputSession(
                            "Enter your new luck value (-100 to 100):", null,
                            (val, s) -> {
                                try {
                                    manager.getPlugin().getLuckManager().setLuck(player, Integer.parseInt(val.trim()));
                                    player.sendMessage(lang.get("luck-set-notify", "luck",
                                            manager.getPlugin().getLuckManager().formatLuck(
                                                    manager.getPlugin().getLuckManager().getLuck(player))));
                                } catch (NumberFormatException ignored) {
                                    player.sendMessage(lang.get("invalid-number", "input", val));
                                }
                                manager.openMainMenu(player);
                            }));
                }
            }

            case 29 -> {
                if (!player.hasPermission("luckyblock.admin")) { player.sendMessage(lang.get("gui-main-no-perm")); return; }
                int delta = event.getClick().isRightClick() ? 25 : 10;
                manager.getPlugin().getLuckManager().addLuck(player, delta);
                player.sendMessage(lang.get("luck-add-notify", "luck",
                        manager.getPlugin().getLuckManager().formatLuck(manager.getPlugin().getLuckManager().getLuck(player))));
                manager.openMainMenu(player);
            }

            case 30 -> {
                if (!player.hasPermission("luckyblock.admin")) { player.sendMessage(lang.get("gui-main-no-perm")); return; }
                int delta = event.getClick().isRightClick() ? 25 : 10;
                manager.getPlugin().getLuckManager().addLuck(player, -delta);
                player.sendMessage(lang.get("luck-add-notify", "luck",
                        manager.getPlugin().getLuckManager().formatLuck(manager.getPlugin().getLuckManager().getLuck(player))));
                manager.openMainMenu(player);
            }

            // ── Toggles ───────────────────────────────────────────────────────
            case 37 -> {
                if (!player.hasPermission("luckyblock.admin")) { player.sendMessage(lang.get("gui-main-no-perm")); return; }
                boolean cur = manager.getPlugin().getConfig().getBoolean("hud.enabled", true);
                manager.getPlugin().getConfig().set("hud.enabled", !cur);
                manager.getPlugin().saveConfig();
                manager.openMainMenu(player);
            }

            case 38 -> {
                if (!player.hasPermission("luckyblock.admin")) { player.sendMessage(lang.get("gui-main-no-perm")); return; }
                boolean cur = manager.getPlugin().getConfig().getBoolean("broadcast-rewards", true);
                manager.getPlugin().getConfig().set("broadcast-rewards", !cur);
                manager.getPlugin().saveConfig();
                manager.openMainMenu(player);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private org.bukkit.inventory.ItemStack rarityGiveItem(LuckyBlockCommand cmd, LuckyBlockRarity rarity) {
        org.bukkit.inventory.ItemStack skull = cmd.buildLuckyBlockItem(1, rarity);
        org.bukkit.inventory.meta.ItemMeta meta = skull.getItemMeta();
        // Append "click to give" to lore
        java.util.List<String> lore = meta.hasLore() ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
        lore.add("");
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eClick to give yourself 1"));
        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private void giveRarity(LuckyBlockCommand cmd, LangManager lang, LuckyBlockRarity rarity) {
        if (!player.hasPermission("luckyblock.admin")) {
            player.sendMessage(lang.get("gui-main-no-perm")); return;
        }
        player.closeInventory();
        player.getInventory().addItem(cmd.buildLuckyBlockItem(1, rarity));
        player.sendMessage(lang.get("give-success-target", "amount", "1"));
    }

    private String luckBar(int luck) {
        int filled = (int) Math.round((luck + 100) / 20.0);
        filled = Math.max(0, Math.min(10, filled));
        return (luck >= 0 ? "§a" : "§c") + "■".repeat(filled) + "§8" + "□".repeat(10 - filled);
    }
}
