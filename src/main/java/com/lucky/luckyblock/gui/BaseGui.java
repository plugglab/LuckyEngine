package com.lucky.luckyblock.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Abstract base for every GUI screen.
 * Subclasses implement build() to populate the inventory
 * and handleClick() to respond to slot clicks.
 */
public abstract class BaseGui {

    protected final GuiManager manager;
    protected final Player player;

    protected BaseGui(GuiManager manager, Player player) {
        this.manager = manager;
        this.player = player;
    }

    /** Build and return the inventory to open. */
    public abstract Inventory build();

    /** Called by GuiListener when this player clicks inside the inventory. */
    public abstract void handleClick(InventoryClickEvent event);

    // ── Item building helpers ────────────────────────────────────────────────

    protected ItemStack item(Material mat, String name, String... lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        if (lore.length > 0) {
            m.setLore(Arrays.stream(lore)
                    .map(l -> ChatColor.translateAlternateColorCodes('&', l))
                    .toList());
        }
        i.setItemMeta(m);
        return i;
    }

    protected ItemStack item(Material mat, int amount, String name, String... lore) {
        ItemStack i = item(mat, name, lore);
        i.setAmount(amount);
        return i;
    }

    /** A glass pane filler — used as a border/spacer. */
    protected ItemStack filler(Material mat) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(" ");
        i.setItemMeta(m);
        return i;
    }

    protected Inventory chest(int rows, String title) {
        return Bukkit.createInventory(null, rows * 9,
                ChatColor.translateAlternateColorCodes('&', title));
    }

    /** Fill a row with filler panes. */
    protected void fillRow(Inventory inv, int row, Material mat) {
        ItemStack f = filler(mat);
        for (int col = 0; col < 9; col++) inv.setItem(row * 9 + col, f);
    }

    /** Fill all empty slots with filler. */
    protected void fillEmpty(Inventory inv, Material mat) {
        ItemStack f = filler(mat);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, f);
        }
    }
}
