package com.lucky.luckyblock.gui;

import com.lucky.luckyblock.LuckyBlockPlugin;
import com.lucky.luckyblock.Reward;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reward editor that shows ONLY fields relevant to the selected type.
 *
 * Fixed slots (always present):
 *   0  ID          1  Display Name    2  Tier (cycle)   3  Type → TypeSelectorGui
 *   9  Weight
 *  45  Back        46  Delete        53  Save
 *
 * Type-specific slots start at 10 and fill left-to-right.
 */
public class RewardEditorGui extends BaseGui {

    // ── Always-fixed slots ────────────────────────────────────────────────────
    private static final int S_ID     = 0;
    private static final int S_NAME   = 1;
    private static final int S_TIER   = 2;
    private static final int S_TYPE   = 3;
    private static final int S_WEIGHT = 9;
    private static final int S_BACK   = 45;
    private static final int S_DELETE = 46;
    private static final int S_SAVE   = 53;

    // ── Type-specific slot assignments (set during build) ─────────────────────
    // We store what each slot does so handleClick can route correctly.
    private final Map<Integer, String> slotActions = new HashMap<>();

    private static final Reward.Tier[] TIERS = Reward.Tier.values();
    private final EditorState state;

    public RewardEditorGui(GuiManager manager, Player player, EditorState state) {
        super(manager, player);
        this.state = state;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public Inventory build() {
        slotActions.clear();
        String title = state.isNew
                ? "&b+ &eNew Reward &8(" + state.type.name() + ")"
                : "&6✎ &f" + state.id + " &8(" + state.type.name() + ")";
        Inventory inv = chest(6, title);

        // ── Row 0: always ─────────────────────────────────────────────────────
        inv.setItem(S_ID,     field(Material.NAME_TAG,    "&7ID",           state.id,           "S_ID"));
        inv.setItem(S_NAME,   nameField());
        inv.setItem(S_TIER,   tierItem());
        inv.setItem(S_TYPE,   item(Material.HOPPER,
                "&7Type: &b" + state.type.name(),
                typeDesc(state.type),
                "",
                "&eClick to change type"));
        slotActions.put(S_TYPE, "TYPE");

        // ── Row 1: weight (always) + type-specific ────────────────────────────
        inv.setItem(S_WEIGHT, field(Material.REPEATER, "&7Weight", String.valueOf(state.weight), "S_WEIGHT"));

        // ── Type-specific fields starting at slot 10 ──────────────────────────
        buildTypeFields(inv);

        // ── Divider + bottom ──────────────────────────────────────────────────
        fillRow(inv, 4, Material.GRAY_STAINED_GLASS_PANE);
        fillRow(inv, 5, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(S_BACK,   item(Material.ARROW,        "&7← Back to List"));
        if (!state.isNew)
            inv.setItem(S_DELETE, item(Material.BARRIER,  "&c✗ Delete &8(right-click)"));
        inv.setItem(S_SAVE,   item(Material.LIME_CONCRETE,"&a✔ Save Reward"));

        fillEmpty(inv, Material.BLACK_STAINED_GLASS_PANE);
        return inv;
    }

    // ── Type-specific layout ──────────────────────────────────────────────────
    private void buildTypeFields(Inventory inv) {
        switch (state.type) {

            case ITEM -> {
                place(inv, 10, field(Material.GRASS_BLOCK, "&7Material", state.material,
                        "&8Any Material enum name, e.g. DIAMOND_SWORD", "&eClick to edit"), "MATERIAL");
                place(inv, 11, field(Material.CHEST, "&7Amount", String.valueOf(state.amount),
                        "&8Stack size (1–64)", "&eClick to edit"), "AMOUNT");
                place(inv, 12, item(Material.ENCHANTED_BOOK,
                        "&7Enchantments &8(" + state.enchantments.size() + ")",
                        state.enchantments.isEmpty() ? "&8None applied" : "&f" + String.join("  ", state.enchantments),
                        "", "&eClick to open Enchantment Editor"), "ENCHANTS");
                place(inv, 13, field(Material.OAK_SIGN, "&7Custom Name", state.customName,
                        "&8Shown as the item's display name (&-colours)", "&eClick to edit"), "CUSTOM_NAME");
                place(inv, 14, loreField(), "LORE");
            }

            case MULTI_ITEM -> {
                int count = state.multiItems.size();
                place(inv, 10, item(Material.CHEST,
                        "&7Items &8(" + count + " configured)",
                        count == 0 ? "&cNo items added yet! Click to add." : "",
                        state.multiItems.stream().limit(5)
                                .map(e -> "&8• &f" + e.material + " x" + e.amount)
                                .collect(Collectors.joining("\n")),
                        count > 5 ? "&8... and " + (count - 5) + " more" : "",
                        "", "&eClick to open Item List Editor"), "MULTI_ITEMS");
            }

            case XP -> {
                place(inv, 10, field(Material.EXPERIENCE_BOTTLE, "&7XP Points", String.valueOf(state.xpPoints),
                        "&8Raw XP added to the player's bar", "&eClick to edit (0 = disabled)"), "XP_POINTS");
                place(inv, 11, field(Material.EMERALD, "&7XP Levels", String.valueOf(state.xpLevels),
                        "&8Full levels added (1 level ≠ fixed XP)", "&eClick to edit (0 = disabled)"), "XP_LEVELS");
                place(inv, 12, item(Material.PAPER, "&7Preview",
                        "&8Player will receive:",
                        (state.xpPoints > 0 ? "&a+ " + state.xpPoints + " XP points" : "&8  (no points)"),
                        (state.xpLevels > 0 ? "&a+ " + state.xpLevels + " levels" : "&8  (no levels)")), "INFO");
            }

            case COMMAND -> {
                int count = state.commands.size();
                place(inv, 10, item(Material.COMMAND_BLOCK,
                        "&7Commands &8(" + count + " configured)",
                        count == 0 ? "&cNo commands added yet!" : "",
                        count > 0 ? "&8First: &7" + truncate(state.commands.get(0), 35) : "",
                        count > 1 ? "&8... and " + (count - 1) + " more" : "",
                        "", "&eClick to open Command Editor"), "COMMANDS");
                place(inv, 11, item(Material.PAPER, "&7About Commands",
                        "&7Commands are run by the console.",
                        "&7Use &f%player% &7for the player's name.",
                        "&7Do &cnot &7include the leading &f/",
                        "", "&8Example: give %player% diamond 1"), "INFO");
            }

            case MOB_SPAWN -> {
                place(inv, 10, item(Material.ZOMBIE_HEAD,
                        "&7Mob: &f" + state.mob,
                        "&7Count: &f" + state.mobCount,
                        "&8Powered: " + boolStr(state.mobPowered),
                        "&8Tamed: "   + boolStr(state.mobTamed),
                        "&8Gear: "    + boolStr(state.mobWithGear),
                        "&8Sky drop: "+ boolStr(state.mobDropFromSky),
                        "", "&eClick to open Mob Selector"), "MOB");
            }

            case EXPLOSION -> {
                place(inv, 10, field(Material.TNT, "&7Power", String.valueOf(state.power),
                        "&8Explosion radius (e.g. 2.0 = small, 5.0 = large)", "&eClick to edit"), "POWER");
                place(inv, 11, toggleItem(Material.GRASS_BLOCK, "&7Break Blocks", state.explodeBreakBlocks,
                        "&8Whether the explosion destroys blocks"), "BREAK_BLOCKS");
                place(inv, 12, item(Material.PAPER, "&7Preview",
                        "&8Power: &f" + state.power,
                        "&8Breaks blocks: " + boolStr(state.explodeBreakBlocks)), "INFO");
            }

            case POTION_EFFECT -> {
                int count = state.potionEffects.size();
                place(inv, 10, item(Material.POTION,
                        "&7Potion Effects &8(" + count + " applied)",
                        count == 0 ? "&cNo effects added yet!" : "",
                        state.potionEffects.stream()
                                .map(e -> "&a" + e.type + " &7" + e.duration + "s amp" + e.amplifier)
                                .collect(Collectors.joining("\n")),
                        "", "&eClick to open Potion Editor"), "POTION");
            }

            case LIGHTNING -> {
                place(inv, 10, field(Material.LIGHTNING_ROD, "&7Strike Count", String.valueOf(state.lightningCount),
                        "&8How many lightning bolts to scatter", "&eClick to edit"), "LIGHTNING_COUNT");
                place(inv, 11, toggleItem(Material.FLINT_AND_STEEL, "&7Deal Damage", state.lightningDamage,
                        "&8Whether the lightning hurts the player"), "LIGHTNING_DAMAGE");
            }

            case MESSAGE -> {
                place(inv, 10, item(Material.PAPER, "&7About Message Type",
                        "&7The Display Name field above IS the message",
                        "&7that will be sent to the player.",
                        "&7Supports & colour codes.",
                        "", "&8Edit the Display Name field to change it."), "INFO");
            }

            case FIREWORKS -> {
                place(inv, 10, field(Material.FIREWORK_ROCKET, "&7Firework Count", String.valueOf(state.fireworkCount),
                        "&8How many fireworks to launch", "&eClick to edit"), "FIREWORK_COUNT");
                place(inv, 11, item(Material.PAPER, "&7Preview",
                        "&8Launches &f" + state.fireworkCount + " &8random coloured fireworks",
                        "&8scattered near the block position."), "INFO");
            }

            case STRUCTURE -> {
                String[] structures = {"OBSIDIAN_CAGE","COBWEB_TRAP","TREASURE_VAULT","ORE_VEIN_DIAMOND"};
                place(inv, 10, item(Material.BRICKS,
                        "&7Structure: &f" + state.structure,
                        "&8Click to cycle through available structures",
                        "", "&8OBSIDIAN_CAGE   &7— traps player in a cage",
                        "&8COBWEB_TRAP     &7— fills area with cobwebs",
                        "&8TREASURE_VAULT  &7— spawns a room with a chest",
                        "&8ORE_VEIN_DIAMOND &7— spawns diamond ore nearby",
                        "", "&eLeft-click: next   Right-click: previous"), "STRUCTURE");
            }

            case CHEST_LOOT -> {
                place(inv, 10, field(Material.BARREL, "&7Loot Table", state.lootTable,
                        "&8Any vanilla loot table key",
                        "&8e.g. minecraft:chests/simple_dungeon",
                        "&8     minecraft:chests/stronghold_crossing",
                        "&8     minecraft:chests/end_city_treasure",
                        "&eClick to edit"), "LOOT_TABLE");
            }

            case ENCHANT_HELD -> {
                place(inv, 10, field(Material.ENCHANTED_BOOK, "&7Enchant Levels", String.valueOf(state.enchantLevels),
                        "&8How many enchantment levels to simulate",
                        "&8Higher = stronger/rarer enchantments", "&eClick to edit"), "ENCHANT_LEVELS");
            }

            case TRAP -> {
                String[] traps = {"DROP_HOTBAR"};
                place(inv, 10, item(Material.TRIPWIRE_HOOK,
                        "&7Trap: &f" + state.trapType,
                        "", "&8DROP_HOTBAR — drops all hotbar items on the ground",
                        "", "&eClick to cycle"), "TRAP_TYPE");
                place(inv, 11, item(Material.PAPER, "&7Preview",
                        state.trapType.equals("DROP_HOTBAR")
                                ? "&8All 9 hotbar slots are dropped at the player's feet."
                                : "&8Unknown trap type."), "INFO");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!player.hasPermission("luckyblock.admin")) {
            player.sendMessage(plugin().getLangManager().get("gui-editor-no-perm")); return;
        }
        int slot = event.getRawSlot();
        String action = slotActions.getOrDefault(slot, "");

        switch (slot) {
            case S_ID     -> chat("Enter reward ID (no spaces):", v -> state.id = v.replace(" ","_"));
            case S_NAME   -> chat(namePrompt(), v -> state.displayName = v);
            case S_WEIGHT -> chat("Enter weight (integer ≥ 1):", v -> { try { state.weight = Math.max(1,Integer.parseInt(v)); } catch(Exception ignored){} });
            case S_TYPE   -> manager.openTypeSelector(player, state);
            case S_TIER   -> {
                int idx = Arrays.asList(TIERS).indexOf(state.tier);
                state.tier = TIERS[event.getClick().isRightClick()
                        ? (idx - 1 + TIERS.length) % TIERS.length
                        : (idx + 1) % TIERS.length];
                reopen();
            }
            case S_BACK   -> manager.openRewardList(player, 0);
            case S_SAVE   -> saveReward();
            case S_DELETE -> {
                if (!state.isNew && event.getClick().isRightClick()) deleteReward();
                else player.sendMessage(plugin().getLangManager().get("gui-editor-delete-confirm"));
            }
            default -> handleTypeSlot(action, event);
        }
    }

    private void handleTypeSlot(String action, InventoryClickEvent event) {
        switch (action) {
            case "MULTI_ITEMS"     -> manager.openGui(player, new MultiItemEditorGui(manager, player, state, 0));
            case "AMOUNT"         -> chat("Enter amount (1–64):", v -> { try { state.amount = Math.max(1,Math.min(64,Integer.parseInt(v))); } catch(Exception ignored){} });
            case "ENCHANTS"       -> manager.openEnchEditor(player, state);
            case "CUSTOM_NAME"    -> chat("Enter custom item name (&-colours):", v -> state.customName = v);
            case "LORE"           -> chat("Enter lore lines separated by | (e.g. &7Line 1|&8Line 2):", v -> {
                                        state.loreLines.clear();
                                        state.loreLines.addAll(Arrays.asList(v.split("\\|")));
                                    });
            case "XP_POINTS"      -> chat("Enter XP points (0 = disabled):", v -> { try { state.xpPoints = Math.max(0,Integer.parseInt(v)); } catch(Exception ignored){} });
            case "XP_LEVELS"      -> chat("Enter XP levels (0 = disabled):", v -> { try { state.xpLevels = Math.max(0,Integer.parseInt(v)); } catch(Exception ignored){} });
            case "COMMANDS"       -> manager.openCommandEditor(player, state);
            case "MOB"            -> manager.openMobSelector(player, state);
            case "POWER"          -> chat("Enter explosion power (e.g. 2.5):", v -> { try { state.power = Math.max(0.1,Double.parseDouble(v)); } catch(Exception ignored){} });
            case "BREAK_BLOCKS"   -> { state.explodeBreakBlocks = !state.explodeBreakBlocks; reopen(); }
            case "POTION"         -> manager.openPotionEditor(player, state);
            case "LIGHTNING_COUNT"-> chat("Enter lightning count:", v -> { try { state.lightningCount = Math.max(1,Integer.parseInt(v)); } catch(Exception ignored){} });
            case "LIGHTNING_DAMAGE"->{ state.lightningDamage = !state.lightningDamage; reopen(); }
            case "FIREWORK_COUNT" -> chat("Enter firework count:", v -> { try { state.fireworkCount = Math.max(1,Integer.parseInt(v)); } catch(Exception ignored){} });
            case "STRUCTURE"      -> cycleStructure(event.getClick().isRightClick());
            case "LOOT_TABLE"     -> chat("Enter loot table key (e.g. minecraft:chests/simple_dungeon):", v -> state.lootTable = v);
            case "ENCHANT_LEVELS" -> chat("Enter enchant level simulation (1–100):", v -> { try { state.enchantLevels = Math.max(1,Integer.parseInt(v)); } catch(Exception ignored){} });
            case "TRAP_TYPE"      -> cycleTrap();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void place(Inventory inv, int slot, org.bukkit.inventory.ItemStack item, String action) {
        inv.setItem(slot, item);
        slotActions.put(slot, action);
    }

    private void chat(String prompt, java.util.function.Consumer<String> apply) {
        manager.startChatInput(player, new ChatInputSession(prompt, state, (v, s) -> { apply.accept(v); reopen(); }));
    }

    private void reopen() { manager.openRewardEditor(player, state); }

    private org.bukkit.inventory.ItemStack field(Material mat, String label, String value, String... lore) {
        List<String> l = new ArrayList<>();
        l.add("&fValue: &e" + value);
        l.add("");
        l.addAll(Arrays.asList(lore));
        return item(mat, label, l.toArray(new String[0]));
    }

    private org.bukkit.inventory.ItemStack tierItem() {
        String color = tierColor(state.tier);
        return item(Material.COMPARATOR, "&7Tier: " + color + state.tier.name(),
                "&8Affects luck-weighted chance",
                "", "&eLeft/Right click to cycle",
                "  &5GREAT &8> &aGOOD &8> &eNEUTRAL &8> &cBAD");
    }

    private org.bukkit.inventory.ItemStack nameField() {
        String label = state.type == Reward.Type.MESSAGE
                ? "&7Message &8(IS the display name)"
                : "&7Display Name";
        return field(Material.OAK_SIGN, label, state.displayName,
                state.type == Reward.Type.MESSAGE
                        ? "&8This text will be sent to the player"
                        : "&8Shown in broadcast and reward list",
                "&eClick to edit");
    }

    private org.bukkit.inventory.ItemStack loreField() {
        return item(Material.WRITABLE_BOOK, "&7Item Lore &8(" + state.loreLines.size() + " lines)",
                state.loreLines.isEmpty() ? "&8None" : state.loreLines.stream()
                        .map(l -> "&7" + l).collect(Collectors.joining("\n")),
                "", "&eClick to edit (separate lines with |)");
    }

    private org.bukkit.inventory.ItemStack toggleItem(Material mat, String label, boolean value, String desc) {
        return item(mat, label + ": " + (value ? "&aON" : "&7OFF"),
                desc, "", "&eClick to toggle");
    }

    private String namePrompt() {
        return state.type == Reward.Type.MESSAGE
                ? "Enter the message to send to the player (&-colours supported):"
                : "Enter display name (&-colours supported):";
    }

    private String boolStr(boolean b) { return b ? "§aON" : "§7OFF"; }

    private String tierColor(Reward.Tier t) {
        return switch (t) { case GREAT -> "§5"; case GOOD -> "§a"; case NEUTRAL -> "§e"; case BAD -> "§c"; };
    }

    private String typeDesc(Reward.Type t) {
        return switch (t) {
            case ITEM          -> "&8Drop one item (with optional enchants)";
            case MULTI_ITEM    -> "&8Drop multiple different items";
            case XP            -> "&8Give XP points or levels directly";
            case COMMAND       -> "&8Run console commands";
            case MOB_SPAWN     -> "&8Spawn mobs around the block";
            case EXPLOSION     -> "&8Create an explosion";
            case POTION_EFFECT -> "&8Apply potion effects to the player";
            case LIGHTNING     -> "&8Strike lightning around the block";
            case MESSAGE       -> "&8Send a private message to the player";
            case STRUCTURE     -> "&8Build a structure at the location";
            case CHEST_LOOT    -> "&8Drop items from a vanilla loot table";
            case ENCHANT_HELD  -> "&8Randomly enchant the player's held item";
            case FIREWORKS     -> "&8Launch colourful random fireworks";
            case TRAP          -> "&8Trigger a trap on the player";
        };
    }

    private void cycleStructure(boolean reverse) {
        String[] all = {"OBSIDIAN_CAGE","COBWEB_TRAP","TREASURE_VAULT","ORE_VEIN_DIAMOND"};
        int idx = Arrays.asList(all).indexOf(state.structure);
        if (idx < 0) idx = 0;
        idx = reverse ? (idx - 1 + all.length) % all.length : (idx + 1) % all.length;
        state.structure = all[idx];
        reopen();
    }

    private void cycleTrap() {
        String[] all = {"DROP_HOTBAR"};
        int idx = Arrays.asList(all).indexOf(state.trapType);
        if (idx < 0) idx = 0;
        state.trapType = all[(idx + 1) % all.length];
        reopen();
    }

    private LuckyBlockPlugin plugin() { return manager.getPlugin(); }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ── Persistence ───────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void saveReward() {
        if (state.id.isBlank()) { player.sendMessage(plugin().getLangManager().get("gui-editor-id-empty")); return; }
        List<Object> list = (List<Object>) plugin().getRewardManager().getRewardsConfig().getList("rewards", new ArrayList<>());
        Map<String,Object> map = buildMap();
        if (state.isNew) { list.add(map); }
        else {
            if (state.originalIndex >= 0 && state.originalIndex < list.size()) list.set(state.originalIndex, map);
            else list.add(map);
        }
        plugin().getRewardManager().getRewardsConfig().set("rewards", list);
        plugin().getRewardManager().saveRewardsConfig();
        plugin().getRewardManager().loadRewards();
        player.sendMessage(plugin().getLangManager().get("gui-editor-save-ok", "id", state.id));
        manager.openRewardList(player, 0);
    }

    @SuppressWarnings("unchecked")
    private void deleteReward() {
        List<Object> list = (List<Object>) plugin().getRewardManager().getRewardsConfig().getList("rewards", new ArrayList<>());
        if (state.originalIndex >= 0 && state.originalIndex < list.size()) {
            list.remove(state.originalIndex);
            plugin().getRewardManager().getRewardsConfig().set("rewards", list);
            plugin().getRewardManager().saveRewardsConfig();
            plugin().getRewardManager().loadRewards();
            player.sendMessage(plugin().getLangManager().get("gui-editor-delete-ok", "id", state.id));
        }
        manager.openRewardList(player, 0);
    }

    private Map<String,Object> buildMap() {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",           state.id);
        m.put("tier",         state.tier.name());
        m.put("type",         state.type.name());
        m.put("weight",       state.weight);
        m.put("display-name", state.displayName);
        switch (state.type) {
            case ITEM -> {
                m.put("material", state.material);
                m.put("amount",   state.amount);
                if (!state.customName.isBlank()) m.put("custom-name", state.customName);
                if (!state.loreLines.isEmpty())  m.put("lore", new ArrayList<>(state.loreLines));
                if (!state.enchantments.isEmpty()) m.put("enchantments", new ArrayList<>(state.enchantments));
            }
            case XP -> {
                if (state.xpPoints > 0) m.put("xp-points", state.xpPoints);
                if (state.xpLevels  > 0) m.put("xp-levels",  state.xpLevels);
            }
            case COMMAND    -> m.put("commands", new ArrayList<>(state.commands));
            case MOB_SPAWN  -> {
                m.put("mob", state.mob); m.put("count", state.mobCount);
                if (state.mobPowered)    m.put("powered",       true);
                if (state.mobTamed)      m.put("tamed",         true);
                if (state.mobWithGear)   m.put("with-gear",     true);
                if (state.mobDropFromSky)m.put("drop-from-sky", true);
            }
            case EXPLOSION  -> { m.put("power", state.power); m.put("break-blocks", state.explodeBreakBlocks); }
            case POTION_EFFECT -> {
                List<Map<String,Object>> fx = new ArrayList<>();
                for (EffectEntry e : state.potionEffects) {
                    Map<String,Object> em = new LinkedHashMap<>();
                    em.put("effect", e.type); em.put("duration-seconds", e.duration); em.put("amplifier", e.amplifier);
                    fx.add(em);
                }
                m.put("effects", fx);
            }
            case LIGHTNING  -> { m.put("count", state.lightningCount); m.put("damage", state.lightningDamage); }
            case FIREWORKS  -> m.put("count", state.fireworkCount);
            case STRUCTURE  -> m.put("structure", state.structure);
            case CHEST_LOOT -> m.put("loot-table", state.lootTable);
            case ENCHANT_HELD->m.put("levels", state.enchantLevels);
            case TRAP       -> m.put("trap", state.trapType);
            case MESSAGE    -> {} // display-name IS the message, already set above
            case MULTI_ITEM -> {
                List<Map<String,Object>> itemMaps = new ArrayList<>();
                for (ItemEntry e : state.multiItems) {
                    Map<String,Object> em = new LinkedHashMap<>();
                    em.put("material", e.material);
                    em.put("amount",   e.amount);
                    if (!e.enchantments.isEmpty()) em.put("enchantments", new ArrayList<>(e.enchantments));
                    itemMaps.add(em);
                }
                m.put("items", itemMaps);
            }
        }
        return m;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EditorState
    // ─────────────────────────────────────────────────────────────────────────
    public static class EffectEntry {
        public String type; public int duration; public int amplifier;
        public EffectEntry(String type, int duration, int amplifier) {
            this.type = type; this.duration = duration; this.amplifier = amplifier;
        }
    }

    public static class ItemEntry {
        public String material;
        public int amount;
        public List<String> enchantments;
        public ItemEntry(String material, int amount, List<String> enchantments) {
            this.material = material;
            this.amount   = amount;
            this.enchantments = enchantments != null ? enchantments : new ArrayList<>();
        }
    }

    public static class EditorState {
        public boolean isNew = true;
        public int originalIndex = -1;

        // Core
        public String id = "my_reward";
        public String displayName = "&aNew Reward!";
        public Reward.Tier tier = Reward.Tier.NEUTRAL;
        public Reward.Type type = Reward.Type.ITEM;
        public int weight = 10;

        // ITEM
        public String material = "DIAMOND";
        public int amount = 1;
        public String customName = "";
        public List<String> loreLines = new ArrayList<>();
        public List<String> enchantments = new ArrayList<>();

        // MOB_SPAWN
        public String mob = "ZOMBIE";
        public int mobCount = 1;
        public boolean mobPowered, mobTamed, mobWithGear, mobDropFromSky;

        // EXPLOSION
        public double power = 2.0;
        public boolean explodeBreakBlocks = false;

        // XP
        public int xpPoints = 0;
        public int xpLevels = 0;

        // POTION_EFFECT
        public List<EffectEntry> potionEffects = new ArrayList<>();

        // COMMAND
        public List<String> commands = new ArrayList<>();

        // MULTI_ITEM
        public List<ItemEntry> multiItems = new ArrayList<>();

        // LIGHTNING
        public int lightningCount = 3;
        public boolean lightningDamage = false;

        // FIREWORKS
        public int fireworkCount = 5;

        // STRUCTURE
        public String structure = "OBSIDIAN_CAGE";

        // CHEST_LOOT
        public String lootTable = "minecraft:chests/simple_dungeon";

        // ENCHANT_HELD
        public int enchantLevels = 30;

        // TRAP
        public String trapType = "DROP_HOTBAR";

        public EditorState() {}

        @SuppressWarnings("unchecked")
        public static EditorState fromReward(com.lucky.luckyblock.Reward r, int index) {
            EditorState s = new EditorState();
            s.isNew = false;
            s.originalIndex = index;
            s.id = r.getId();
            s.displayName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', r.getDisplayName()));
            s.tier = r.getTier(); s.type = r.getType(); s.weight = r.getBaseWeight();
            s.material = r.getString("material","DIAMOND"); s.amount = r.getInt("amount",1);
            s.customName = r.getString("custom-name","");
            s.loreLines = new ArrayList<>(r.getStringList("lore"));
            s.enchantments = new ArrayList<>(r.getStringList("enchantments"));
            s.mob = r.getString("mob","ZOMBIE"); s.mobCount = r.getInt("count",1);
            s.mobPowered = r.getBoolean("powered",false); s.mobTamed = r.getBoolean("tamed",false);
            s.mobWithGear = r.getBoolean("with-gear",false); s.mobDropFromSky = r.getBoolean("drop-from-sky",false);
            s.power = r.getDouble("power",2.0); s.explodeBreakBlocks = r.getBoolean("break-blocks",false);
            s.xpPoints = r.getInt("xp-points",0); s.xpLevels = r.getInt("xp-levels",0);
            s.commands = new ArrayList<>(r.getStringList("commands"));
            s.lightningCount = r.getInt("count",3); s.lightningDamage = r.getBoolean("damage",false);
            s.fireworkCount = r.getInt("count",5);
            s.structure = r.getString("structure","OBSIDIAN_CAGE");
            s.lootTable = r.getString("loot-table","minecraft:chests/simple_dungeon");
            s.enchantLevels = r.getInt("levels",30);
            s.trapType = r.getString("trap","DROP_HOTBAR");
            List<?> multiList = r.getSection().getList("items");
            if (multiList != null) for (Object obj : multiList) {
                if (!(obj instanceof Map<?,?> raw)) continue;
                @SuppressWarnings("unchecked") Map<Object,Object> map = (Map<Object,Object>) raw;
                String mat = String.valueOf(map.getOrDefault("material","DIRT"));
                int amt    = Integer.parseInt(String.valueOf(map.getOrDefault("amount",1)));
                List<String> enchs = new ArrayList<>();
                Object enchObj = map.get("enchantments");
                if (enchObj instanceof List<?> el) el.forEach(x -> enchs.add(String.valueOf(x)));
                s.multiItems.add(new ItemEntry(mat, amt, enchs));
            }
            List<?> fxList = r.getSection().getList("effects");
            if (fxList != null) for (Object obj : fxList) {
                if (!(obj instanceof Map<?,?> raw)) continue;
                Map<Object,Object> map = (Map<Object,Object>) raw;
                s.potionEffects.add(new EffectEntry(
                        String.valueOf(map.getOrDefault("effect","SPEED")),
                        Integer.parseInt(String.valueOf(map.getOrDefault("duration-seconds",30))),
                        Integer.parseInt(String.valueOf(map.getOrDefault("amplifier",0)))));
            }
            return s;
        }
    }
}
