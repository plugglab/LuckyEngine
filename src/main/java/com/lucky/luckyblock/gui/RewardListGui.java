package com.lucky.luckyblock.gui;

import com.lucky.luckyblock.Reward;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated grid of all loaded rewards (45 slots per page).
 * Click a reward to edit it. Bottom row has navigation + back.
 */
public class RewardListGui extends BaseGui {

    private static final int PAGE_SIZE = 45; // 5 rows × 9
    private final int page;

    public RewardListGui(GuiManager manager, Player player, int page) {
        super(manager, player);
        this.page = page;
    }

    @Override
    public Inventory build() {
        List<Reward> rewards = manager.getPlugin().getRewardManager().getRewards();
        int totalPages = Math.max(1, (int) Math.ceil(rewards.size() / (double) PAGE_SIZE));
        int safePage = Math.min(page, totalPages - 1);

        Inventory inv = chest(6, "&6✦ &eRewards &8— Page " + (safePage + 1) + "/" + totalPages);

        // ── Reward slots (0‥44) ──────────────────────────────────────────────
        int start = safePage * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= rewards.size()) break;
            Reward r = rewards.get(idx);
            inv.setItem(i, rewardItem(r, idx));
        }

        // ── Bottom bar ────────────────────────────────────────────────────────
        fillRow(inv, 5, Material.BLACK_STAINED_GLASS_PANE);

        // Back
        inv.setItem(45, item(Material.ARROW, "&7← Back to Menu"));

        // Prev page
        if (safePage > 0)
            inv.setItem(48, item(Material.SPECTRAL_ARROW, "&ePrevious Page",
                    "&8Page " + safePage + "/" + totalPages));

        // Page indicator
        inv.setItem(49, item(Material.PAPER, "&7Page " + (safePage + 1) + " of " + totalPages,
                "&8Total rewards: &e" + rewards.size()));

        // Next page
        if (safePage < totalPages - 1)
            inv.setItem(50, item(Material.SPECTRAL_ARROW, "&eNext Page",
                    "&8Page " + (safePage + 2) + "/" + totalPages));

        // New reward
        inv.setItem(53, item(Material.LIME_DYE, "&a+ Create New Reward"));

        return inv;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        List<Reward> rewards = manager.getPlugin().getRewardManager().getRewards();
        int totalPages = Math.max(1, (int) Math.ceil(rewards.size() / (double) PAGE_SIZE));
        int safePage = Math.min(page, totalPages - 1);

        if (slot < 45) {
            int idx = safePage * PAGE_SIZE + slot;
            if (idx >= rewards.size()) return;
            Reward r = rewards.get(idx);

            if (event.getClick().isRightClick() && player.hasPermission("luckyblock.admin")) {
                // Inline delete with confirmation via second right-click
                deleteReward(idx);
            } else {
                RewardEditorGui.EditorState state = RewardEditorGui.EditorState.fromReward(r, idx);
                manager.openRewardEditor(player, state);
            }
            return;
        }

        switch (slot) {
            case 45 -> manager.openMainMenu(player);
            case 48 -> { if (safePage > 0) manager.openRewardList(player, safePage - 1); }
            case 50 -> { if (safePage < totalPages - 1) manager.openRewardList(player, safePage + 1); }
            case 53 -> {
                if (!player.hasPermission("luckyblock.admin")) {
                    player.sendMessage("§cNo permission."); return;
                }
                manager.openRewardEditor(player, new RewardEditorGui.EditorState());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void deleteReward(int index) {
        java.util.List<Object> list = (java.util.List<Object>) manager.getPlugin()
                .getRewardManager().getRewardsConfig().getList("rewards", new java.util.ArrayList<>());
        if (index < 0 || index >= list.size()) return;
        list.remove(index);
        manager.getPlugin().getRewardManager().getRewardsConfig().set("rewards", list);
        manager.getPlugin().getRewardManager().saveRewardsConfig();
        manager.getPlugin().getRewardManager().loadRewards();
        player.sendMessage("§c✗ Reward deleted.");
        // Re-open on same page (clamped if it was the last item)
        int newTotal = manager.getPlugin().getRewardManager().getRewards().size();
        int newMaxPage = Math.max(0, (int) Math.ceil(newTotal / (double) PAGE_SIZE) - 1);
        manager.openRewardList(player, Math.min(page, newMaxPage));
    }

    private ItemStack rewardItem(Reward reward, int index) {
        Material icon = tierIcon(reward.getTier());
        String tierColor = tierColor(reward.getTier());

        List<String> lore = new ArrayList<>();
        lore.add("&8ID: &7" + reward.getId());
        lore.add(tierColor + "Tier: &f" + reward.getTier().name());
        lore.add("&7Type: &f" + reward.getType().name());
        lore.add("&7Weight: &f" + reward.getBaseWeight());
        lore.add("");
        lore.add("&eClick to edit");
        if (player.hasPermission("luckyblock.admin"))
            lore.add("&cRight-click to delete");

        return item(icon, reward.getDisplayName(), lore.toArray(new String[0]));
    }

    private Material tierIcon(Reward.Tier tier) {
        return switch (tier) {
            case GREAT   -> Material.DIAMOND;
            case GOOD    -> Material.GOLD_INGOT;
            case NEUTRAL -> Material.IRON_INGOT;
            case BAD     -> Material.COAL;
        };
    }

    private String tierColor(Reward.Tier tier) {
        return switch (tier) {
            case GREAT   -> "&5";
            case GOOD    -> "&a";
            case NEUTRAL -> "&e";
            case BAD     -> "&c";
        };
    }
}
