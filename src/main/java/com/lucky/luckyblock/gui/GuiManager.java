package com.lucky.luckyblock.gui;

import com.lucky.luckyblock.LuckyBlockPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GuiManager {

    private final LuckyBlockPlugin plugin;
    private final Map<UUID, BaseGui> openGuis    = new HashMap<>();
    private final Map<UUID, ChatInputSession> chatSessions = new HashMap<>();
    private final Set<UUID> transitioning        = new HashSet<>();

    public GuiManager(LuckyBlockPlugin plugin) { this.plugin = plugin; }
    public LuckyBlockPlugin getPlugin()          { return plugin; }

    // ── Named openers ─────────────────────────────────────────────────────────
    public void openMainMenu(Player player)                           { openGui(player, new MainMenuGui(this, player)); }
    public void openRewardList(Player player, int page)              { openGui(player, new RewardListGui(this, player, page)); }
    public void openRewardEditor(Player player, RewardEditorGui.EditorState state) { openGui(player, new RewardEditorGui(this, player, state)); }
    public void openTypeSelector(Player player, RewardEditorGui.EditorState state) { openGui(player, new TypeSelectorGui(this, player, state)); }
    public void openEnchEditor(Player player, RewardEditorGui.EditorState state)   { openGui(player, new EnchantmentEditorGui(this, player, state)); }
    public void openMobSelector(Player player, RewardEditorGui.EditorState state)  { openGui(player, new MobSelectorGui(this, player, state)); }
    public void openPotionEditor(Player player, RewardEditorGui.EditorState state) { openGui(player, new PotionEffectEditorGui(this, player, state)); }
    public void openCommandEditor(Player player, RewardEditorGui.EditorState state){ openGui(player, new CommandEditorGui(this, player, state)); }

    /** Generic opener — used by sub-GUIs that instantiate themselves. */
    public void openGui(Player player, BaseGui gui) {
        UUID uid = player.getUniqueId();
        transitioning.add(uid);
        openGuis.put(uid, gui);
        player.openInventory(gui.build());
        Bukkit.getScheduler().runTask(plugin, () -> transitioning.remove(uid));
    }

    // ── Lookup ────────────────────────────────────────────────────────────────
    public BaseGui getOpen(Player player)       { return openGuis.get(player.getUniqueId()); }
    public void clearOpen(Player player)        { openGuis.remove(player.getUniqueId()); }
    public boolean isTransitioning(Player p)   { return transitioning.contains(p.getUniqueId()); }

    // ── Chat input sessions ───────────────────────────────────────────────────
    public void startChatInput(Player player, ChatInputSession session) {
        chatSessions.put(player.getUniqueId(), session);
        player.closeInventory();
        player.sendMessage("§6[LuckyBlock] §7" + session.getPrompt());
        player.sendMessage("§8Type your answer in chat, or type §ccancel §8to abort.");
    }

    public boolean hasChatSession(Player p)     { return chatSessions.containsKey(p.getUniqueId()); }
    public ChatInputSession getChatSession(Player p) { return chatSessions.get(p.getUniqueId()); }
    public void clearChatSession(Player p)      { chatSessions.remove(p.getUniqueId()); }
}
