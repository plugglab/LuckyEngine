package com.lucky.luckyblock.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * Dedicated command editor GUI.
 *
 * Layout (6 rows):
 *   Row 0-2  : command list (up to 18 slots, left-click edit, right-click delete)
 *   Row 3    : placeholder buttons — click to start a new command pre-filled with that placeholder
 *   Row 4    : template command buttons
 *   Row 5    : Add, Clear All, Back
 */
public class CommandEditorGui extends BaseGui {

    private final RewardEditorGui.EditorState state;

    // Placeholders shown in row 3
    private static final String[][] PLACEHOLDERS = {
        {"%player%",    "Player Name",    "The name of the player who broke the block"},
        {"%uuid%",      "Player UUID",    "The UUID of the player"},
        {"%world%",     "World Name",     "The world the block was broken in"},
        {"%x%",         "Block X",        "X coordinate of the lucky block"},
        {"%y%",         "Block Y",        "Y coordinate of the lucky block"},
        {"%z%",         "Block Z",        "Z coordinate of the lucky block"},
        {"%luck%",      "Luck Value",     "The player's current luck value"},
        {"%online%",    "Online Count",   "Number of online players"},
    };

    // Templates shown in row 4: { label, icon, template command }
    private static final Object[][] TEMPLATES = {
        { "&bGive Item",       Material.DIAMOND,         "give %player% minecraft:diamond 1" },
        { "&aEffect",          Material.POTION,           "effect give %player% minecraft:speed 60 1 true" },
        { "&eTeleport",        Material.ENDER_PEARL,      "tp %player% 0 64 0" },
        { "&6Title",           Material.OAK_SIGN,         "title %player% title {\"text\":\"Lucky!\",\"color\":\"gold\"}" },
        { "&dBroadcast",       Material.PAPER,            "say %player% got lucky!" },
        { "&cKill",            Material.SKELETON_SKULL,   "kill %player%" },
        { "&bSound",           Material.NOTE_BLOCK,       "playsound minecraft:entity.player.levelup master %player% ~ ~ ~ 1 1" },
        { "&eSummon",          Material.ZOMBIE_HEAD,      "summon minecraft:zombie ~ ~ ~ {CustomName:'\"Lucky Mob\"'}" },
        { "&aHeal",            Material.RED_DYE,          "effect give %player% minecraft:instant_health 1 4 true" },
        { "&6XP",              Material.EXPERIENCE_BOTTLE,"xp add %player% 100 points" },
        { "&7Game Mode",       Material.GRASS_BLOCK,      "gamemode survival %player%" },
        { "&cWeather",         Material.WATER_BUCKET,     "weather thunder 300" },
    };

    public CommandEditorGui(GuiManager manager, Player player, RewardEditorGui.EditorState state) {
        super(manager, player);
        this.state = state;
    }

    @Override
    public Inventory build() {
        Inventory inv = chest(6, "&eCommand Editor &8(" + state.commands.size() + " cmd" + (state.commands.size() == 1 ? "" : "s") + ")");

        // ── Rows 0-2: command list (up to 18) ────────────────────────────────
        for (int i = 0; i < 18; i++) {
            if (i < state.commands.size()) {
                String cmd = state.commands.get(i);
                inv.setItem(i, item(Material.LIME_DYE,
                        "&f" + (i + 1) + ". &7" + truncate(cmd, 30),
                        "&8" + cmd,
                        "",
                        "&eLeft-click: §7edit",
                        "&cRight-click: §7delete"));
            } else {
                inv.setItem(i, item(Material.GRAY_DYE,
                        "&8Slot " + (i + 1) + " — empty",
                        "&eClick to add a command here"));
            }
        }

        // ── Row 3: placeholder buttons ────────────────────────────────────────
        fillRow(inv, 3, Material.BLUE_STAINED_GLASS_PANE);
        inv.setItem(27, item(Material.KNOWLEDGE_BOOK, "&b✦ Placeholders",
                "&7Click any placeholder below to start",
                "&7a new command containing it"));
        for (int i = 0; i < PLACEHOLDERS.length; i++) {
            inv.setItem(28 + i, item(Material.CYAN_DYE,
                    "&b" + PLACEHOLDERS[i][0],
                    "&7" + PLACEHOLDERS[i][1],
                    "&8" + PLACEHOLDERS[i][2],
                    "&eClick to use in a new command"));
        }

        // ── Row 4: template commands ──────────────────────────────────────────
        fillRow(inv, 4, Material.ORANGE_STAINED_GLASS_PANE);
        inv.setItem(36, item(Material.COMMAND_BLOCK, "&6✦ Templates",
                "&7Click a template to add it as a command",
                "&7(you can edit it after adding)"));
        for (int i = 0; i < TEMPLATES.length && i < 8; i++) {
            inv.setItem(37 + i, item(
                    (Material) TEMPLATES[i][1],
                    (String) TEMPLATES[i][0],
                    "&8" + truncate((String) TEMPLATES[i][2], 35),
                    "&eClick to add this template"));
        }

        // ── Row 5: controls ───────────────────────────────────────────────────
        fillRow(inv, 5, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(45, item(Material.ARROW,        "&7← Back to Editor"));
        inv.setItem(47, item(Material.LIME_CONCRETE,"&a+ Add New Command",
                "&7Type a custom command in chat"));
        inv.setItem(49, item(Material.BARRIER,      "&cClear All Commands"));
        inv.setItem(51, item(Material.PAPER,        "&7Note: commands run as console",
                "&7Use %player% for the player's name",
                "&7Do NOT include the leading /"));

        return inv;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        List<String> cmds = state.commands;

        // ── Back ──────────────────────────────────────────────────────────────
        if (slot == 45) { manager.openRewardEditor(player, state); return; }

        // ── Add new command ───────────────────────────────────────────────────
        if (slot == 47) {
            promptCommand("Type your command (no leading /, use %player% for the player):", "");
            return;
        }

        // ── Clear all ─────────────────────────────────────────────────────────
        if (slot == 49) { cmds.clear(); refresh(); return; }

        // ── Command list slots (0-17) ─────────────────────────────────────────
        if (slot >= 0 && slot < 18) {
            if (slot < cmds.size()) {
                if (event.getClick().isRightClick()) {
                    cmds.remove(slot);
                    refresh();
                } else {
                    final int idx = slot;
                    final String current = cmds.get(idx);
                    manager.startChatInput(player, new ChatInputSession(
                            "Edit command " + (idx + 1) + " (current: " + current + "):",
                            state,
                            (val, s) -> {
                                s.commands.set(idx, val);
                                manager.openGui(player, new CommandEditorGui(manager, player, s));
                            }
                    ));
                }
            } else {
                // Empty slot — add new command here
                promptCommand("Type command for slot " + (slot + 1) + " (no leading /):", "");
            }
            return;
        }

        // ── Placeholder row (28-35): start new command pre-seeded ────────────
        if (slot >= 28 && slot <= 35) {
            int idx = slot - 28;
            if (idx < PLACEHOLDERS.length) {
                String ph = PLACEHOLDERS[idx][0];
                promptCommand("Type your command — " + ph + " is included as a hint:", ph);
            }
            return;
        }

        // ── Template row (37-44) ──────────────────────────────────────────────
        if (slot >= 37 && slot <= 44) {
            int idx = slot - 37;
            if (idx < TEMPLATES.length) {
                String template = (String) TEMPLATES[idx][2];
                cmds.add(template);
                refresh();
            }
        }
    }

    private void promptCommand(String prompt, String prefill) {
        String fullPrompt = prefill.isEmpty()
                ? prompt
                : prompt + " (suggestion: " + prefill + ")";
        manager.startChatInput(player, new ChatInputSession(
                fullPrompt,
                state,
                (val, s) -> {
                    s.commands.add(val);
                    manager.openGui(player, new CommandEditorGui(manager, player, s));
                }
        ));
    }

    private void refresh() {
        manager.openGui(player, new CommandEditorGui(manager, player, state));
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
