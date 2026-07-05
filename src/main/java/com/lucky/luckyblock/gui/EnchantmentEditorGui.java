package com.lucky.luckyblock.gui;

import com.lucky.luckyblock.Reward;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.*;

public class EnchantmentEditorGui extends BaseGui {

    private final RewardEditorGui.EditorState state;
    private int page = 0;

    private static final List<Enchantment> ALL;
    static {
        ALL = new ArrayList<>(Arrays.asList(Enchantment.values()));
        ALL.sort(Comparator.comparing(e -> e.getKey().getKey()));
    }

    private static final int PAGE_SIZE = 36; // 4 rows of 9

    public EnchantmentEditorGui(GuiManager manager, Player player, RewardEditorGui.EditorState state) {
        super(manager, player);
        this.state = state;
    }

    public EnchantmentEditorGui(GuiManager manager, Player player, RewardEditorGui.EditorState state, int page) {
        super(manager, player);
        this.state = state;
        this.page = page;
    }

    @Override
    public Inventory build() {
        int totalPages = Math.max(1, (int) Math.ceil(ALL.size() / (double) PAGE_SIZE));
        Inventory inv = chest(6, "&5Enchantments &8— Page " + (page + 1) + "/" + totalPages);

        // ── Rows 0-3: enchantment browser ────────────────────────────────────
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= ALL.size()) break;
            Enchantment ench = ALL.get(idx);
            String id = ench.getKey().getKey().toUpperCase();
            int curLevel = getCurrentLevel(id);
            boolean applied = curLevel > 0;

            ItemStack icon = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta m = (EnchantmentStorageMeta) icon.getItemMeta();
            m.addStoredEnchant(ench, Math.max(1, curLevel), true);
            m.setDisplayName((applied ? "§a✔ " : "§7") + id);
            m.setLore(List.of(
                    "§7Max level: §f" + ench.getMaxLevel(),
                    applied ? "§aCurrent level: §f" + curLevel : "§8Not applied",
                    "",
                    applied ? "§eLeft-click: §7level +1" : "§eClick: §7add at level 1",
                    applied ? "§eRight-click: §7level -1 / remove" : ""
            ));
            icon.setItemMeta(m);
            inv.setItem(i, icon);
        }

        // ── Row 4: current enchants on this reward ────────────────────────────
        fillRow(inv, 4, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(36, item(Material.PAPER, "&7Applied enchantments:"));

        List<String> applied = new ArrayList<>(state.enchantments);
        for (int i = 0; i < Math.min(applied.size(), 7); i++) {
            String entry = applied.get(i); // e.g. SHARPNESS:5
            inv.setItem(37 + i, item(Material.BOOK, "&a" + entry,
                    "&cRight-click to remove"));
        }

        // ── Row 5: navigation ─────────────────────────────────────────────────
        fillRow(inv, 5, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(45, item(Material.ARROW, "&7← Back to Editor"));
        if (page > 0)
            inv.setItem(48, item(Material.SPECTRAL_ARROW, "&ePrevious Page"));
        inv.setItem(49, item(Material.BARRIER, "&cClear All Enchantments"));
        if (page < totalPages - 1)
            inv.setItem(50, item(Material.SPECTRAL_ARROW, "&eNext Page"));

        return inv;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // ── Navigation row ────────────────────────────────────────────────────
        if (slot == 45) { manager.openRewardEditor(player, state); return; }
        if (slot == 48 && page > 0) { openPage(page - 1); return; }
        if (slot == 49) { state.enchantments.clear(); openPage(page); return; }
        if (slot == 50) { openPage(page + 1); return; }

        // ── Applied enchant row (37-43): right-click to remove ────────────────
        if (slot >= 37 && slot <= 43) {
            int idx = slot - 37;
            if (idx < state.enchantments.size()) {
                if (event.getClick().isRightClick()) {
                    state.enchantments.remove(idx);
                    openPage(page);
                }
            }
            return;
        }

        // ── Browser slots (0-35) ──────────────────────────────────────────────
        if (slot >= 0 && slot < PAGE_SIZE) {
            int enchIdx = page * PAGE_SIZE + slot;
            if (enchIdx >= ALL.size()) return;
            Enchantment ench = ALL.get(enchIdx);
            String id = ench.getKey().getKey().toUpperCase();
            int curLevel = getCurrentLevel(id);

            if (event.getClick().isRightClick()) {
                // Decrement or remove
                if (curLevel > 1) {
                    setLevel(id, curLevel - 1, ench.getMaxLevel());
                } else {
                    state.enchantments.removeIf(e -> e.startsWith(id + ":"));
                }
            } else {
                // Increment or add
                int newLevel = Math.min(curLevel + 1, ench.getMaxLevel() + 2); // allow slight over-cap
                if (newLevel < 1) newLevel = 1;
                setLevel(id, newLevel, ench.getMaxLevel() + 2);
            }
            openPage(page);
        }
    }

    private int getCurrentLevel(String id) {
        for (String e : state.enchantments) {
            String[] parts = e.split(":");
            if (parts.length == 2 && parts[0].equals(id)) {
                try { return Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    private void setLevel(String id, int level, int max) {
        state.enchantments.removeIf(e -> e.startsWith(id + ":"));
        if (level > 0) state.enchantments.add(id + ":" + Math.min(level, max));
    }

    private void openPage(int p) {
        manager.openGui(player, new EnchantmentEditorGui(manager, player, state, p));
    }
}
