package com.lucky.luckyblock.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for editing the items list of a MULTI_ITEM reward.
 *
 * Layout (6 rows):
 *   Rows 0-4 : up to 45 item entry slots
 *              left-click  = edit material / amount
 *              right-click = remove entry
 *              empty slots show a "+" prompt
 *   Row 5    : Back, Add Item, Clear All
 */
public class MultiItemEditorGui extends BaseGui {

    private final RewardEditorGui.EditorState state;
    private final int page;
    private static final int PAGE_SIZE = 36; // rows 0-3

    public MultiItemEditorGui(GuiManager manager, Player player,
                              RewardEditorGui.EditorState state, int page) {
        super(manager, player);
        this.state = state;
        this.page  = page;
    }

    @Override
    public Inventory build() {
        List<RewardEditorGui.ItemEntry> items = state.multiItems;
        int totalPages = Math.max(1, (int) Math.ceil(Math.max(items.size() + 1, 1) / (double) PAGE_SIZE));
        int safePage   = Math.min(page, totalPages - 1);

        Inventory inv = chest(6,
                "&6Multi-Item Editor &8(" + items.size() + " item" + (items.size() == 1 ? "" : "s") + ")");

        // ── Rows 0-3: item entries ────────────────────────────────────────────
        int start = safePage * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx < items.size()) {
                inv.setItem(i, entryItem(items.get(idx), idx));
            } else if (idx == items.size()) {
                // One "add" slot directly after last item
                inv.setItem(i, item(Material.LIME_DYE, "&a+ Add Item",
                        "&7Click to add a new item to the list"));
            }
            // remaining slots stay null (will be filled by fillEmpty below)
        }

        // ── Row 4: summary of current entries ────────────────────────────────
        fillRow(inv, 4, Material.GRAY_STAINED_GLASS_PANE);
        if (!items.isEmpty()) {
            int shown = Math.min(items.size(), 7);
            for (int i = 0; i < shown; i++) {
                RewardEditorGui.ItemEntry e = items.get(i);
                inv.setItem(36 + i, item(safeMat(e.material),
                        "&f" + e.material + " &8x" + e.amount,
                        e.enchantments.isEmpty() ? "&8No enchants"
                                : "&7" + String.join(", ", e.enchantments),
                        "&cRight-click to remove"));
            }
        }

        // ── Row 5: controls ───────────────────────────────────────────────────
        fillRow(inv, 5, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(45, item(Material.ARROW, "&7← Back to Editor"));
        if (safePage > 0)
            inv.setItem(47, item(Material.SPECTRAL_ARROW, "&ePrevious Page"));
        inv.setItem(49, item(Material.LIME_CONCRETE, "&a+ Add Item",
                "&7Adds a new item entry at the end of the list"));
        if (safePage < totalPages - 1)
            inv.setItem(51, item(Material.SPECTRAL_ARROW, "&eNext Page"));
        inv.setItem(53, item(Material.BARRIER, "&cClear All Items"));

        fillEmpty(inv, Material.BLACK_STAINED_GLASS_PANE);
        return inv;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        List<RewardEditorGui.ItemEntry> items = state.multiItems;
        int totalPages = Math.max(1, (int) Math.ceil(Math.max(items.size() + 1, 1) / (double) PAGE_SIZE));
        int safePage   = Math.min(page, totalPages - 1);

        // ── Controls ──────────────────────────────────────────────────────────
        if (slot == 45) { manager.openRewardEditor(player, state); return; }
        if (slot == 47 && safePage > 0) { refresh(safePage - 1); return; }
        if (slot == 49) { promptAddItem(); return; }
        if (slot == 51 && safePage < totalPages - 1) { refresh(safePage + 1); return; }
        if (slot == 53) { items.clear(); refresh(0); return; }

        // ── Summary row: right-click to remove ────────────────────────────────
        if (slot >= 36 && slot <= 42) {
            int idx = slot - 36;
            if (event.getClick().isRightClick() && idx < items.size()) {
                items.remove(idx);
                refresh(safePage);
            }
            return;
        }

        // ── Main grid ────────────────────────────────────────────────────────
        if (slot >= 0 && slot < PAGE_SIZE) {
            int idx = safePage * PAGE_SIZE + slot;

            if (idx == items.size()) {
                // The "+" slot
                promptAddItem();
                return;
            }

            if (idx < items.size()) {
                if (event.getClick().isRightClick()) {
                    items.remove(idx);
                    refresh(safePage);
                } else {
                    // Edit existing entry
                    editEntry(idx);
                }
            }
        }
    }

    // ── Prompt helpers ────────────────────────────────────────────────────────

    private void promptAddItem() {
        manager.startChatInput(player, new ChatInputSession(
                "Enter material name for the new item (e.g. DIAMOND_SWORD):",
                state,
                (mat, s) -> {
                    String matUp = mat.trim().toUpperCase();
                    // Ask for amount next
                    manager.startChatInput(player, new ChatInputSession(
                            "Enter amount for " + matUp + ":",
                            s,
                            (amtStr, s2) -> {
                                int amt = 1;
                                try { amt = Math.max(1, Integer.parseInt(amtStr.trim())); } catch (Exception ignored) {}
                                // Ask for enchants (optional)
                                manager.startChatInput(player, new ChatInputSession(
                                        "Enter enchantments (e.g. SHARPNESS:5 UNBREAKING:3), or 'none' to skip:",
                                        s2,
                                        (enchStr, s3) -> {
                                            List<String> enchs = new ArrayList<>();
                                            if (!enchStr.equalsIgnoreCase("none") && !enchStr.isBlank())
                                                for (String e : enchStr.trim().split("\\s+"))
                                                    if (!e.isBlank()) enchs.add(e.toUpperCase());
                                            s3.multiItems.add(new RewardEditorGui.ItemEntry(matUp, amt, enchs));
                                            manager.openGui(player, new MultiItemEditorGui(manager, player, s3, 0));
                                        }
                                ));
                            }
                    ));
                }
        ));
    }

    private void editEntry(int idx) {
        RewardEditorGui.ItemEntry entry = state.multiItems.get(idx);
        manager.startChatInput(player, new ChatInputSession(
                "Edit material for slot " + (idx + 1) + " (current: " + entry.material + "):",
                state,
                (mat, s) -> {
                    entry.material = mat.trim().toUpperCase();
                    manager.startChatInput(player, new ChatInputSession(
                            "Edit amount (current: " + entry.amount + "):",
                            s,
                            (amtStr, s2) -> {
                                try { entry.amount = Math.max(1, Integer.parseInt(amtStr.trim())); } catch (Exception ignored) {}
                                manager.startChatInput(player, new ChatInputSession(
                                        "Edit enchantments (e.g. SHARPNESS:5), or 'none' to clear:",
                                        s2,
                                        (enchStr, s3) -> {
                                            entry.enchantments.clear();
                                            if (!enchStr.equalsIgnoreCase("none") && !enchStr.isBlank())
                                                for (String e : enchStr.trim().split("\\s+"))
                                                    if (!e.isBlank()) entry.enchantments.add(e.toUpperCase());
                                            manager.openGui(player, new MultiItemEditorGui(manager, player, s3, page));
                                        }
                                ));
                            }
                    ));
                }
        ));
    }

    private void refresh(int p) {
        manager.openGui(player, new MultiItemEditorGui(manager, player, state, p));
    }

    private ItemStack entryItem(RewardEditorGui.ItemEntry entry, int idx) {
        Material mat = safeMat(entry.material);
        return item(mat,
                "&f" + (idx + 1) + ". " + entry.material + " &8×" + entry.amount,
                entry.enchantments.isEmpty() ? "&8No enchantments"
                        : "&7" + String.join("  ", entry.enchantments),
                "",
                "&eLeft-click: §7edit",
                "&cRight-click: §7remove");
    }

    private Material safeMat(String name) {
        Material m = Material.matchMaterial(name);
        return m != null ? m : Material.STONE;
    }
}
