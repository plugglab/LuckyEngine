package com.lucky.luckyblock.gui;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class MobSelectorGui extends BaseGui {

    private final RewardEditorGui.EditorState state;

    // { EntityType name, display name, icon material }
    private static final Object[][] MOBS = {
        { "ZOMBIE",            "&2Zombie",            Material.ROTTEN_FLESH },
        { "SKELETON",          "&fSkeleton",          Material.BONE },
        { "CREEPER",           "&aCreeper",           Material.GUNPOWDER },
        { "SPIDER",            "&8Spider",            Material.COBWEB },
        { "CAVE_SPIDER",       "&8Cave Spider",       Material.FERMENTED_SPIDER_EYE },
        { "ENDERMAN",          "&5Enderman",          Material.ENDER_PEARL },
        { "WITCH",             "&dWitch",             Material.GLASS_BOTTLE },
        { "WITHER_SKELETON",   "&8Wither Skeleton",   Material.WITHER_SKELETON_SKULL },
        { "BLAZE",             "&6Blaze",             Material.BLAZE_ROD },
        { "GHAST",             "&fGhast",             Material.GHAST_TEAR },
        { "PHANTOM",           "&5Phantom",           Material.PHANTOM_MEMBRANE },
        { "PILLAGER",          "&7Pillager",          Material.CROSSBOW },
        { "RAVAGER",           "&cRavager",           Material.SADDLE },
        { "VINDICATOR",        "&7Vindicator",        Material.IRON_AXE },
        { "ELDER_GUARDIAN",    "&bElder Guardian",    Material.SPONGE },
        { "ENDER_DRAGON",      "&5Ender Dragon",      Material.DRAGON_EGG },
        { "WITHER",            "&8Wither",            Material.NETHER_STAR },
        { "IRON_GOLEM",        "&7Iron Golem",        Material.IRON_BLOCK },
        { "SNOW_GOLEM",        "&bSnow Golem",        Material.SNOWBALL },
        { "SHULKER",           "&dShulker",           Material.SHULKER_SHELL },
        { "HORSE",             "&6Horse",             Material.SADDLE },
        { "WOLF",              "&fWolf",              Material.BONE },
        { "CAT",               "&eCat",               Material.STRING },
        { "BEE",               "&eHoney Bee",         Material.HONEYCOMB },
        { "DOLPHIN",           "&bDolphin",           Material.TROPICAL_FISH },
        { "SLIME",             "&aSlime",             Material.SLIME_BALL },
        { "MAGMA_CUBE",        "&cMagma Cube",        Material.MAGMA_CREAM },
        { "ZOMBIE_VILLAGER",   "&2Zombie Villager",   Material.ROTTEN_FLESH },
        { "DROWNED",           "&3Drowned",           Material.NAUTILUS_SHELL },
        { "HUSK",              "&6Husk",              Material.SAND },
        { "STRAY",             "&bStray",             Material.ICE },
        { "PIGLIN_BRUTE",      "&6Piglin Brute",      Material.GOLDEN_AXE },
        { "HOGLIN",            "&dHoglin",            Material.CRIMSON_STEM },
        { "ZOGLIN",            "&cZoglin",            Material.CRYING_OBSIDIAN },
        { "VILLAGER",          "&eTrader",            Material.EMERALD },
        { "WANDERING_TRADER",  "&bWandering Trader",  Material.LEAD },
    };

    public MobSelectorGui(GuiManager manager, Player player, RewardEditorGui.EditorState state) {
        super(manager, player);
        this.state = state;
    }

    @Override
    public Inventory build() {
        Inventory inv = chest(6, "&2Select Mob Type");
        fillRow(inv, 5, Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < MOBS.length && i < 45; i++) {
            String mobId   = (String) MOBS[i][0];
            String name    = (String) MOBS[i][1];
            Material icon  = (Material) MOBS[i][2];
            boolean sel    = state.mob.equalsIgnoreCase(mobId);
            inv.setItem(i, item(sel ? Material.LIME_STAINED_GLASS_PANE : icon,
                    (sel ? "&a✔ " : "") + name,
                    "&8" + mobId,
                    sel ? "&a(selected)" : "&eClick to select"));
        }

        inv.setItem(45, item(Material.ARROW, "&7← Back to Editor"));

        // Extra options that affect mob behaviour
        inv.setItem(47, item(
                state.mobPowered ? Material.LIGHTNING_ROD : Material.STONE,
                state.mobPowered ? "&eCharged: &aON" : "&eCharged: &7OFF",
                "&7Powered creepers deal more damage",
                "&eClick to toggle"));
        inv.setItem(48, item(
                state.mobTamed ? Material.LEAD : Material.FEATHER,
                state.mobTamed ? "&eTamed: &aON" : "&eTamed: &7OFF",
                "&7Tame the mob to the player (horses/wolves)",
                "&eClick to toggle"));
        inv.setItem(49, item(
                state.mobWithGear ? Material.IRON_CHESTPLATE : Material.LEATHER_CHESTPLATE,
                state.mobWithGear ? "&eWith Gear: &aON" : "&eWith Gear: &7OFF",
                "&7Equip mobs with basic iron gear",
                "&eClick to toggle"));
        inv.setItem(50, item(
                state.mobDropFromSky ? Material.FEATHER : Material.GRASS_BLOCK,
                state.mobDropFromSky ? "&eDrop From Sky: &aON" : "&eDrop From Sky: &7OFF",
                "&7Mobs fall from the sky above",
                "&eClick to toggle"));
        inv.setItem(52, item(Material.REPEATER, "&7Spawn Count: &f" + state.mobCount,
                "&eLeft-click: +1   Right-click: -1"));

        fillEmpty(inv, Material.BLACK_STAINED_GLASS_PANE);
        return inv;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 45) { manager.openRewardEditor(player, state); return; }
        if (slot == 47) { state.mobPowered    = !state.mobPowered;    manager.openGui(player, new MobSelectorGui(manager, player, state)); return; }
        if (slot == 48) { state.mobTamed      = !state.mobTamed;      manager.openGui(player, new MobSelectorGui(manager, player, state)); return; }
        if (slot == 49) { state.mobWithGear   = !state.mobWithGear;   manager.openGui(player, new MobSelectorGui(manager, player, state)); return; }
        if (slot == 50) { state.mobDropFromSky= !state.mobDropFromSky;manager.openGui(player, new MobSelectorGui(manager, player, state)); return; }

        if (slot == 52) {
            if (event.getClick().isRightClick()) state.mobCount = Math.max(1, state.mobCount - 1);
            else state.mobCount = Math.min(50, state.mobCount + 1);
            manager.openGui(player, new MobSelectorGui(manager, player, state));
            return;
        }

        if (slot >= 0 && slot < 45 && slot < MOBS.length) {
            state.mob = (String) MOBS[slot][0];
            manager.openRewardEditor(player, state);
        }
    }
}
