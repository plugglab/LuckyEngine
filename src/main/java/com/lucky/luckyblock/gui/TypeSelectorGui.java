package com.lucky.luckyblock.gui;

import com.lucky.luckyblock.Reward;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class TypeSelectorGui extends BaseGui {

    private final RewardEditorGui.EditorState state;

    // Icon representing each reward type
    private static final Object[][] TYPES = {
        { Reward.Type.ITEM,         Material.DIAMOND,           "&bItem Drop",         "&7Give the player one or more items" },
        { Reward.Type.MULTI_ITEM,   Material.CHEST,             "&bMulti-Item Drop",   "&7Give multiple different items at once" },
        { Reward.Type.XP,           Material.EXPERIENCE_BOTTLE, "&aXP Reward",         "&7Give XP points or levels directly" },
        { Reward.Type.COMMAND,      Material.COMMAND_BLOCK,     "&eCommand(s)",        "&7Run console commands on trigger" },
        { Reward.Type.MOB_SPAWN,    Material.ZOMBIE_HEAD,       "&2Mob Spawn",         "&7Spawn mobs around the block" },
        { Reward.Type.EXPLOSION,    Material.TNT,               "&cExplosion",         "&7Create an explosion" },
        { Reward.Type.POTION_EFFECT,Material.POTION,            "&dPotion Effect(s)",  "&7Apply potion effects to the player" },
        { Reward.Type.LIGHTNING,    Material.LIGHTNING_ROD,     "&eLightning",         "&7Strike lightning around the block" },
        { Reward.Type.MESSAGE,      Material.OAK_SIGN,          "&7Message",           "&7Send a private message to the player" },
        { Reward.Type.STRUCTURE,    Material.BRICKS,            "&6Structure",         "&7Build a structure at the block" },
        { Reward.Type.CHEST_LOOT,   Material.BARREL,            "&6Chest Loot",        "&7Drop items from a vanilla loot table" },
        { Reward.Type.ENCHANT_HELD, Material.ENCHANTED_BOOK,    "&5Enchant Held Item", "&7Randomly enchant whatever the player holds" },
        { Reward.Type.FIREWORKS,    Material.FIREWORK_ROCKET,   "&eFireworks",         "&7Launch colourful fireworks" },
        { Reward.Type.TRAP,         Material.TRIPWIRE_HOOK,     "&cTrap",              "&7Trigger a trap effect on the player" },
    };

    public TypeSelectorGui(GuiManager manager, Player player, RewardEditorGui.EditorState state) {
        super(manager, player);
        this.state = state;
    }

    @Override
    public Inventory build() {
        Inventory inv = chest(4, "&8Select Reward Type");
        fillRow(inv, 3, Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < TYPES.length; i++) {
            Object[] row = TYPES[i];
            Reward.Type type = (Reward.Type) row[0];
            Material mat = (Material) row[1];
            String name = (String) row[2];
            String desc = (String) row[3];
            boolean selected = state.type == type;

            inv.setItem(i + 9, item(selected ? Material.LIME_STAINED_GLASS_PANE : mat,
                    (selected ? "&a✔ " : "") + name,
                    desc,
                    selected ? "&a(currently selected)" : "&eClick to select"));
        }

        inv.setItem(27, item(Material.ARROW, "&7← Back to Editor"));
        fillEmpty(inv, Material.BLACK_STAINED_GLASS_PANE);
        return inv;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 27) { manager.openRewardEditor(player, state); return; }

        int idx = slot - 9;
        if (idx >= 0 && idx < TYPES.length) {
            state.type = (Reward.Type) TYPES[idx][0];
            manager.openRewardEditor(player, state);
        }
    }
}
