package com.lucky.luckyblock.gui;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.*;

public class PotionEffectEditorGui extends BaseGui {

    private final RewardEditorGui.EditorState state;

    // All available potion effect types with friendly names
    private static final String[][] EFFECTS = {
        {"SPEED","Speed"},{"SLOWNESS","Slowness"},{"HASTE","Haste"},
        {"MINING_FATIGUE","Mining Fatigue"},{"STRENGTH","Strength"},
        {"INSTANT_HEALTH","Instant Health"},{"INSTANT_DAMAGE","Instant Damage"},
        {"JUMP_BOOST","Jump Boost"},{"NAUSEA","Nausea"},{"REGENERATION","Regeneration"},
        {"RESISTANCE","Resistance"},{"FIRE_RESISTANCE","Fire Resistance"},
        {"WATER_BREATHING","Water Breathing"},{"INVISIBILITY","Invisibility"},
        {"BLINDNESS","Blindness"},{"NIGHT_VISION","Night Vision"},
        {"HUNGER","Hunger"},{"WEAKNESS","Weakness"},{"POISON","Poison"},
        {"WITHER","Wither"},{"HEALTH_BOOST","Health Boost"},
        {"ABSORPTION","Absorption"},{"SATURATION","Saturation"},
        {"GLOWING","Glowing"},{"LEVITATION","Levitation"},{"LUCK","Luck"},
        {"UNLUCK","Bad Luck"},{"SLOW_FALLING","Slow Falling"},
        {"CONDUIT_POWER","Conduit Power"},{"DOLPHINS_GRACE","Dolphins Grace"},
        {"BAD_OMEN","Bad Omen"},{"HERO_OF_THE_VILLAGE","Hero of the Village"},
        {"DARKNESS","Darkness"},
    };

    public PotionEffectEditorGui(GuiManager manager, Player player, RewardEditorGui.EditorState state) {
        super(manager, player);
        this.state = state;
    }

    @Override
    public Inventory build() {
        Inventory inv = chest(6, "&dPotion Effects Editor");

        // ── Rows 0-3: effect browser ─────────────────────────────────────────
        for (int i = 0; i < EFFECTS.length && i < 36; i++) {
            String effectId   = EFFECTS[i][0];
            String effectName = EFFECTS[i][1];
            RewardEditorGui.EffectEntry existing = findEffect(effectId);
            boolean applied = existing != null;

            ItemStack potion = new ItemStack(applied ? Material.POTION : Material.GLASS_BOTTLE);
            PotionMeta pm = (PotionMeta) potion.getItemMeta();
            pm.setDisplayName((applied ? "§a✔ " : "§7") + effectName);

            List<String> lore = new ArrayList<>();
            if (applied) {
                lore.add("§7Duration: §f" + existing.duration + "s");
                lore.add("§7Amplifier: §f" + existing.amplifier + " (level " + (existing.amplifier + 1) + ")");
                lore.add("");
                lore.add("§eLeft-click: §7edit duration/amplifier");
                lore.add("§cRight-click: §7remove effect");
            } else {
                lore.add("§8Not applied");
                lore.add("");
                lore.add("§eClick to add with defaults");
                lore.add("§8(30s, level 1)");
            }
            pm.setLore(lore);
            pm.setColor(applied ? Color.GREEN : Color.GRAY);
            potion.setItemMeta(pm);
            inv.setItem(i, potion);
        }

        // ── Row 4: current applied effects summary ────────────────────────────
        fillRow(inv, 4, Material.GRAY_STAINED_GLASS_PANE);
        List<RewardEditorGui.EffectEntry> effects = state.potionEffects;
        inv.setItem(36, item(Material.PAPER,
                "&7Applied: &f" + effects.size() + " effect(s)"));

        for (int i = 0; i < Math.min(effects.size(), 7); i++) {
            RewardEditorGui.EffectEntry e = effects.get(i);
            inv.setItem(37 + i, item(Material.POTION,
                    "&a" + e.type,
                    "&7" + e.duration + "s  Amp: " + e.amplifier,
                    "&cRight-click to remove"));
        }

        // ── Row 5: controls ───────────────────────────────────────────────────
        fillRow(inv, 5, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(45, item(Material.ARROW, "&7← Back to Editor"));
        inv.setItem(49, item(Material.BARRIER, "&cClear All Effects"));

        return inv;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 45) { manager.openRewardEditor(player, state); return; }
        if (slot == 49) { state.potionEffects.clear(); refresh(); return; }

        // Applied effects row — right-click to remove
        if (slot >= 37 && slot <= 43) {
            int idx = slot - 37;
            if (event.getClick().isRightClick() && idx < state.potionEffects.size()) {
                state.potionEffects.remove(idx);
                refresh();
            }
            return;
        }

        // Effect browser
        if (slot >= 0 && slot < 36 && slot < EFFECTS.length) {
            String effectId = EFFECTS[slot][0];
            RewardEditorGui.EffectEntry existing = findEffect(effectId);

            if (event.getClick().isRightClick() && existing != null) {
                state.potionEffects.remove(existing);
                refresh();
                return;
            }

            if (existing != null) {
                // Edit duration + amplifier via chat
                manager.startChatInput(player, new ChatInputSession(
                        "Enter duration (seconds) and amplifier for " + effectId + " — e.g. '60 1':",
                        state,
                        (val, s) -> {
                            String[] parts = val.trim().split("\\s+");
                            try { existing.duration  = Integer.parseInt(parts[0]); } catch (Exception ignored) {}
                            try { existing.amplifier = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                            manager.openGui(player, new PotionEffectEditorGui(manager, player, s));
                        }
                ));
            } else {
                // Add with defaults
                state.potionEffects.add(new RewardEditorGui.EffectEntry(effectId, 30, 0));
                refresh();
            }
        }
    }

    private RewardEditorGui.EffectEntry findEffect(String id) {
        return state.potionEffects.stream()
                .filter(e -> e.type.equalsIgnoreCase(id))
                .findFirst().orElse(null);
    }

    private void refresh() {
        manager.openGui(player, new PotionEffectEditorGui(manager, player, state));
    }
}
