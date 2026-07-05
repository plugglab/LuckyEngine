package com.lucky.luckyblock.gui;

import com.lucky.luckyblock.LuckManager;
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
        Inventory inv = chest(4, "&6✦ &eLucky Block &6✦ &8Main Menu");

        // ── Border ───────────────────────────────────────────────────────────
        fillRow(inv, 0, Material.BLACK_STAINED_GLASS_PANE);
        fillRow(inv, 3, Material.BLACK_STAINED_GLASS_PANE);

        // ── Luck bar header ───────────────────────────────────────────────────
        LuckManager lm = manager.getPlugin().getLuckManager();
        int luck = lm.getLuck(player);
        String bar = lm.formatLuck(luck);
        inv.setItem(4, item(Material.NETHER_STAR, "&6Your Luck",
                "&7Value: " + bar,
                "&7" + luckBar(luck),
                "",
                "&8Click to view luck commands"));

        // ── Slot 10: Browse rewards ───────────────────────────────────────────
        inv.setItem(10, item(Material.CHEST, "&aBrowse Rewards",
                "&7View all configured rewards",
                "&7Click to open the reward list"));

        // ── Slot 12: Create reward ─────────────────────────────────────────────
        inv.setItem(12, item(Material.WRITABLE_BOOK, "&bCreate New Reward",
                "&7Open the reward editor",
                "&7to add a new lucky drop"));

        // ── Slot 14: Give lucky block ──────────────────────────────────────────
        inv.setItem(14, item(Material.SPONGE, "&eLucky Block",
                "&7Give yourself a Lucky Block",
                "&8Requires luckyblock.admin"));

        // ── Slot 16: Reload ───────────────────────────────────────────────────
        inv.setItem(16, item(Material.COMPARATOR, "&cReload Config",
                "&7Reloads config.yml from disk",
                "&8Requires luckyblock.admin"));

        // ── Stats footer ──────────────────────────────────────────────────────
        int total = manager.getPlugin().getRewardManager().getRewards().size();
        inv.setItem(31, item(Material.PAPER, "&7Statistics",
                "&8Loaded rewards: &e" + total,
                "&8Your luck: " + bar));

        fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);
        return inv;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        switch (slot) {
            case 10 -> manager.openRewardList(player, 0);

            case 12 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage("§cYou don't have permission to create rewards.");
                    return;
                }
                manager.openRewardEditor(player, new RewardEditorGui.EditorState());
            }

            case 14 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage("§cNo permission.");
                    return;
                }
                player.closeInventory();
                com.lucky.luckyblock.LuckyBlockCommand cmd =
                        new com.lucky.luckyblock.LuckyBlockCommand(manager.getPlugin());
                player.getInventory().addItem(cmd.buildLuckyBlockItem(1));
                player.sendMessage("§a✦ Lucky Block added to your inventory!");
            }

            case 16 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage("§cNo permission.");
                    return;
                }
                manager.getPlugin().reload();
                player.sendMessage("§aConfig reloaded! §e"
                        + manager.getPlugin().getRewardManager().getRewards().size() + " §arewards.");
                player.closeInventory();
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
