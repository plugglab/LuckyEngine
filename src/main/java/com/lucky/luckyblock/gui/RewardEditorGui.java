package com.lucky.luckyblock.gui;

import com.lucky.luckyblock.Reward;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.*;
import java.util.stream.Collectors;

public class RewardEditorGui extends BaseGui {

    // ── Slot map ──────────────────────────────────────────────────────────────
    private static final int SLOT_ID        = 0;
    private static final int SLOT_NAME      = 1;
    private static final int SLOT_TIER      = 2;
    private static final int SLOT_TYPE      = 3;   // → TypeSelectorGui
    private static final int SLOT_WEIGHT    = 9;
    private static final int SLOT_MATERIAL  = 10;
    private static final int SLOT_AMOUNT    = 11;
    private static final int SLOT_ENCHANTS  = 12;  // → EnchantmentEditorGui
    private static final int SLOT_MOB       = 18;  // → MobSelectorGui
    private static final int SLOT_POWER     = 19;
    private static final int SLOT_XP_PTS    = 20;
    private static final int SLOT_XP_LVL    = 21;
    private static final int SLOT_EFFECT    = 27;  // → PotionEffectEditorGui
    private static final int SLOT_CMD       = 28;  // → CommandEditorGui
    private static final int SLOT_BACK      = 45;
    private static final int SLOT_DELETE    = 46;
    private static final int SLOT_SAVE      = 53;

    private static final Reward.Tier[] TIERS = Reward.Tier.values();

    private final EditorState state;

    public RewardEditorGui(GuiManager manager, Player player, EditorState state) {
        super(manager, player);
        this.state = state;
    }

    @Override
    public Inventory build() {
        String title = state.isNew ? "&b+ &eNew Reward" : "&6✎ Edit: &f" + state.id;
        Inventory inv = chest(6, title);

        // ── Row 0 ─────────────────────────────────────────────────────────────
        inv.setItem(SLOT_ID,   field(Material.NAME_TAG,    "&7Reward ID",      state.id,          "&eClick to edit"));
        inv.setItem(SLOT_NAME, field(Material.OAK_SIGN,    "&7Display Name",   state.displayName, "&eClick to edit"));
        inv.setItem(SLOT_TIER, cycleField(Material.COMPARATOR, "&7Tier", state.tier.name(), tierColor(state.tier)));
        inv.setItem(SLOT_TYPE, item(Material.HOPPER, "&7Type: &f" + state.type.name(),
                "&8What this reward does",
                "",
                "&eClick to open Type Selector"));

        // ── Row 1 ─────────────────────────────────────────────────────────────
        inv.setItem(SLOT_WEIGHT,   field(Material.REPEATER,    "&7Weight",   String.valueOf(state.weight),   "&eClick to edit"));
        inv.setItem(SLOT_MATERIAL, field(Material.GRASS_BLOCK, "&7Material", state.material, "&8For ITEM rewards", "&eClick to edit"));
        inv.setItem(SLOT_AMOUNT,   field(Material.CHEST,       "&7Amount",   String.valueOf(state.amount),   "&eClick to edit"));
        inv.setItem(SLOT_ENCHANTS, item(Material.ENCHANTED_BOOK,
                "&7Enchantments &8(" + state.enchantments.size() + ")",
                state.enchantments.isEmpty() ? "&8None" : "&f" + String.join(", ", state.enchantments),
                "",
                "&eClick to open Enchantment Editor"));

        // ── Row 2 ─────────────────────────────────────────────────────────────
        inv.setItem(SLOT_MOB, item(Material.ZOMBIE_HEAD,
                "&7Mob: &f" + state.mob + " &8×" + state.mobCount,
                "&8Powered: " + bool(state.mobPowered) + "  Tamed: " + bool(state.mobTamed),
                "&8Gear: " + bool(state.mobWithGear) + "  Sky: " + bool(state.mobDropFromSky),
                "",
                "&eClick to open Mob Selector"));
        inv.setItem(SLOT_POWER,  field(Material.TNT,               "&7Explosion Power", String.valueOf(state.power), "&eClick to edit"));
        inv.setItem(SLOT_XP_PTS, field(Material.EXPERIENCE_BOTTLE, "&7XP Points",       String.valueOf(state.xpPoints), "&eClick to edit"));
        inv.setItem(SLOT_XP_LVL, field(Material.EMERALD,           "&7XP Levels",       String.valueOf(state.xpLevels), "&eClick to edit"));

        // ── Row 3 ─────────────────────────────────────────────────────────────
        inv.setItem(SLOT_EFFECT, item(Material.POTION,
                "&7Potion Effects &8(" + state.potionEffects.size() + ")",
                state.potionEffects.isEmpty()
                        ? "&8None"
                        : state.potionEffects.stream().map(e -> e.type).collect(Collectors.joining(", ")),
                "",
                "&eClick to open Potion Editor"));
        inv.setItem(SLOT_CMD, item(Material.COMMAND_BLOCK,
                "&7Commands &8(" + state.commands.size() + ")",
                state.commands.isEmpty()
                        ? "&8None"
                        : "&8" + state.commands.get(0) + (state.commands.size() > 1 ? " +" + (state.commands.size()-1) + " more" : ""),
                "",
                "&eClick to open Command Editor"));

        // ── Divider + bottom ──────────────────────────────────────────────────
        fillRow(inv, 4, Material.GRAY_STAINED_GLASS_PANE);
        fillRow(inv, 5, Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(SLOT_BACK,   item(Material.ARROW,        "&7← Back to List"));
        if (!state.isNew)
            inv.setItem(SLOT_DELETE, item(Material.BARRIER,  "&c✗ Delete &8(right-click)"));
        inv.setItem(SLOT_SAVE,   item(Material.LIME_CONCRETE,"&a✔ Save Reward"));

        fillEmpty(inv, Material.BLACK_STAINED_GLASS_PANE);
        return inv;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!player.hasPermission("luckyblock.admin")) { player.sendMessage("§cNo permission."); return; }
        int slot = event.getRawSlot();

        switch (slot) {
            // ── Text fields ───────────────────────────────────────────────────
            case SLOT_ID       -> chat("Enter reward ID (no spaces):",            v -> state.id = v.replace(" ","_"));
            case SLOT_NAME     -> chat("Enter display name (&-colours supported):", v -> state.displayName = v);
            case SLOT_WEIGHT   -> chat("Enter weight (integer ≥ 1):",              v -> { try { state.weight = Math.max(1, Integer.parseInt(v)); } catch (Exception ignored) {} });
            case SLOT_MATERIAL -> chat("Enter material name (e.g. DIAMOND_SWORD):", v -> state.material = v.toUpperCase());
            case SLOT_AMOUNT   -> chat("Enter item amount:",                        v -> { try { state.amount = Math.max(1, Integer.parseInt(v)); } catch (Exception ignored) {} });
            case SLOT_POWER    -> chat("Enter explosion power (e.g. 3.0):",         v -> { try { state.power = Double.parseDouble(v); } catch (Exception ignored) {} });
            case SLOT_XP_PTS   -> chat("Enter XP points to give:",                 v -> { try { state.xpPoints = Math.max(0, Integer.parseInt(v)); } catch (Exception ignored) {} });
            case SLOT_XP_LVL   -> chat("Enter XP levels to give:",                 v -> { try { state.xpLevels = Math.max(0, Integer.parseInt(v)); } catch (Exception ignored) {} });

            // ── Cycle tier ────────────────────────────────────────────────────
            case SLOT_TIER -> {
                int idx = Arrays.asList(TIERS).indexOf(state.tier);
                state.tier = TIERS[event.getClick().isRightClick()
                        ? (idx - 1 + TIERS.length) % TIERS.length
                        : (idx + 1) % TIERS.length];
                reopen();
            }

            // ── Sub-GUIs ──────────────────────────────────────────────────────
            case SLOT_TYPE    -> manager.openTypeSelector(player, state);
            case SLOT_ENCHANTS -> manager.openEnchEditor(player, state);
            case SLOT_MOB     -> manager.openMobSelector(player, state);
            case SLOT_EFFECT  -> manager.openPotionEditor(player, state);
            case SLOT_CMD     -> manager.openCommandEditor(player, state);

            // ── Bottom bar ────────────────────────────────────────────────────
            case SLOT_BACK   -> manager.openRewardList(player, 0);
            case SLOT_DELETE -> {
                if (!state.isNew && event.getClick().isRightClick()) deleteReward();
                else player.sendMessage("§cRight-click the Delete button to confirm.");
            }
            case SLOT_SAVE   -> saveReward();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void chat(String prompt, java.util.function.Consumer<String> apply) {
        manager.startChatInput(player, new ChatInputSession(prompt, state, (v, s) -> { apply.accept(v); reopen(); }));
    }

    private void reopen() { manager.openRewardEditor(player, state); }

    private org.bukkit.inventory.ItemStack field(Material mat, String label, String value, String... extra) {
        List<String> lore = new ArrayList<>();
        lore.add("&fValue: &e" + value);
        lore.add("");
        lore.addAll(Arrays.asList(extra));
        return item(mat, label, lore.toArray(new String[0]));
    }

    private org.bukkit.inventory.ItemStack cycleField(Material mat, String label, String value, String color) {
        return item(mat, label,
                "§fCurrent: " + color + value,
                "",
                "§eLeft/Right click to cycle");
    }

    private String tierColor(Reward.Tier t) {
        return switch (t) { case GREAT -> "§5"; case GOOD -> "§a"; case NEUTRAL -> "§e"; case BAD -> "§c"; };
    }

    private String bool(boolean b) { return b ? "§aON" : "§7OFF"; }

    // ── Persistence ───────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void saveReward() {
        if (state.id.isBlank()) { player.sendMessage("§cID cannot be empty!"); return; }
        List<Object> list = (List<Object>) manager.getPlugin().getConfig().getList("rewards", new ArrayList<>());
        Map<String, Object> map = buildMap();
        if (state.isNew) { list.add(map); }
        else {
            if (state.originalIndex >= 0 && state.originalIndex < list.size())
                list.set(state.originalIndex, map);
            else list.add(map);
        }
        manager.getPlugin().getConfig().set("rewards", list);
        manager.getPlugin().saveConfig();
        manager.getPlugin().getRewardManager().loadRewards();
        player.sendMessage("§a✔ Saved reward: §e" + state.id);
        manager.openRewardList(player, 0);
    }

    @SuppressWarnings("unchecked")
    private void deleteReward() {
        List<Object> list = (List<Object>) manager.getPlugin().getConfig().getList("rewards", new ArrayList<>());
        if (state.originalIndex >= 0 && state.originalIndex < list.size()) {
            list.remove(state.originalIndex);
            manager.getPlugin().getConfig().set("rewards", list);
            manager.getPlugin().saveConfig();
            manager.getPlugin().getRewardManager().loadRewards();
            player.sendMessage("§c✗ Deleted reward: §e" + state.id);
        }
        manager.openRewardList(player, 0);
    }

    private Map<String, Object> buildMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           state.id);
        m.put("tier",         state.tier.name());
        m.put("type",         state.type.name());
        m.put("weight",       state.weight);
        m.put("display-name", state.displayName);

        switch (state.type) {
            case ITEM -> {
                m.put("material", state.material);
                m.put("amount",   state.amount);
                if (!state.enchantments.isEmpty()) m.put("enchantments", new ArrayList<>(state.enchantments));
            }
            case MULTI_ITEM -> m.put("items", List.of());
            case XP -> {
                if (state.xpPoints > 0) m.put("xp-points", state.xpPoints);
                if (state.xpLevels  > 0) m.put("xp-levels",  state.xpLevels);
            }
            case COMMAND -> m.put("commands", new ArrayList<>(state.commands));
            case MOB_SPAWN -> {
                m.put("mob",           state.mob);
                m.put("count",         state.mobCount);
                if (state.mobPowered)    m.put("powered",       true);
                if (state.mobTamed)      m.put("tamed",         true);
                if (state.mobWithGear)   m.put("with-gear",     true);
                if (state.mobDropFromSky)m.put("drop-from-sky", true);
            }
            case EXPLOSION -> {
                m.put("power",        state.power);
                m.put("break-blocks", false);
            }
            case POTION_EFFECT -> {
                List<Map<String,Object>> fx = new ArrayList<>();
                for (EffectEntry e : state.potionEffects) {
                    Map<String,Object> em = new LinkedHashMap<>();
                    em.put("effect",           e.type);
                    em.put("duration-seconds", e.duration);
                    em.put("amplifier",        e.amplifier);
                    fx.add(em);
                }
                m.put("effects", fx);
            }
            case LIGHTNING -> { m.put("count", state.mobCount); m.put("damage", false); }
            case FIREWORKS -> m.put("count", state.mobCount);
            case MESSAGE   -> m.put("message", state.displayName);
            case STRUCTURE -> m.put("structure", state.material);
            case CHEST_LOOT -> m.put("loot-table", "minecraft:chests/simple_dungeon");
            case ENCHANT_HELD -> m.put("levels", 30);
            case TRAP -> m.put("trap", "DROP_HOTBAR");
        }
        return m;
    }

    // ── EditorState ───────────────────────────────────────────────────────────

    public static class EffectEntry {
        public String type;
        public int duration;
        public int amplifier;
        public EffectEntry(String type, int duration, int amplifier) {
            this.type = type; this.duration = duration; this.amplifier = amplifier;
        }
    }

    public static class EditorState {
        public boolean isNew = true;
        public int originalIndex = -1;

        public String id = "my_reward";
        public String displayName = "&aNew Reward!";
        public Reward.Tier tier  = Reward.Tier.NEUTRAL;
        public Reward.Type type  = Reward.Type.ITEM;
        public int weight = 10;

        // ITEM
        public String material = "DIAMOND";
        public int amount = 1;
        public List<String> enchantments = new ArrayList<>();

        // MOB_SPAWN
        public String mob = "ZOMBIE";
        public int mobCount = 1;
        public boolean mobPowered, mobTamed, mobWithGear, mobDropFromSky;

        // EXPLOSION
        public double power = 2.0;

        // XP
        public int xpPoints = 0;
        public int xpLevels = 0;

        // POTION_EFFECT
        public List<EffectEntry> potionEffects = new ArrayList<>();

        // COMMAND
        public List<String> commands = new ArrayList<>();

        public EditorState() {}

        @SuppressWarnings("unchecked")
        public static EditorState fromReward(Reward r, int index) {
            EditorState s = new EditorState();
            s.isNew = false;
            s.originalIndex = index;
            s.id = r.getId();
            s.displayName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', r.getDisplayName()));
            s.tier   = r.getTier();
            s.type   = r.getType();
            s.weight = r.getBaseWeight();
            s.material = r.getString("material", "DIAMOND");
            s.amount   = r.getInt("amount", 1);
            s.enchantments = new ArrayList<>(r.getStringList("enchantments"));
            s.mob      = r.getString("mob", "ZOMBIE");
            s.mobCount = r.getInt("count", 1);
            s.mobPowered     = r.getBoolean("powered", false);
            s.mobTamed       = r.getBoolean("tamed", false);
            s.mobWithGear    = r.getBoolean("with-gear", false);
            s.mobDropFromSky = r.getBoolean("drop-from-sky", false);
            s.power    = r.getDouble("power", 2.0);
            s.xpPoints = r.getInt("xp-points", 0);
            s.xpLevels = r.getInt("xp-levels", 0);
            s.commands = new ArrayList<>(r.getStringList("commands"));

            // Load potion effects list
            List<?> fxList = r.getSection().getList("effects");
            if (fxList != null) {
                for (Object obj : fxList) {
                    if (!(obj instanceof Map<?,?> raw)) continue;
                    @SuppressWarnings("unchecked")
                    Map<Object,Object> map = (Map<Object,Object>) raw;
                    String et = String.valueOf(map.getOrDefault("effect", "SPEED"));
                    int dur = Integer.parseInt(String.valueOf(map.getOrDefault("duration-seconds", 30)));
                    int amp = Integer.parseInt(String.valueOf(map.getOrDefault("amplifier", 0)));
                    s.potionEffects.add(new EffectEntry(et, dur, amp));
                }
            }
            return s;
        }
    }
}
