package com.lucky.luckyblock;

import com.lucky.luckyblock.gui.GuiListener;
import com.lucky.luckyblock.gui.GuiManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class LuckyBlockPlugin extends JavaPlugin {

    private static LuckyBlockPlugin instance;
    private RewardManager rewardManager;
    private LuckManager luckManager;
    private CraftingManager craftingManager;
    private GuiManager guiManager;
    private NamespacedKey luckyItemKey;
    private boolean placeholderApiEnabled = false;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.luckyItemKey = new NamespacedKey(this, "lucky_block_item");

        this.luckManager    = new LuckManager(this);
        this.rewardManager  = new RewardManager(this);
        this.rewardManager.loadRewards();

        LuckyBlockTracker.init(this);

        this.craftingManager = new CraftingManager(this);
        this.craftingManager.register();

        this.guiManager = new GuiManager(this);

        // Event listeners
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(guiManager), this);

        // Commands
        LuckyBlockCommand cmd = new LuckyBlockCommand(this);
        getCommand("luckyblock").setExecutor(cmd);
        getCommand("luckyblock").setTabCompleter(cmd);

        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LuckyBlockPlaceholders(this).register();
            placeholderApiEnabled = true;
            getLogger().info("PlaceholderAPI found — placeholders registered.");
        }

        getLogger().info("LuckyBlock v1.0 enabled with "
                + rewardManager.getRewards().size() + " rewards loaded.");
    }

    @Override
    public void onDisable() {
        luckManager.saveAll();
        getLogger().info("LuckyBlock disabled.");
    }

    public static LuckyBlockPlugin getInstance() { return instance; }
    public RewardManager getRewardManager()       { return rewardManager; }
    public LuckManager getLuckManager()           { return luckManager; }
    public GuiManager getGuiManager()             { return guiManager; }
    public NamespacedKey getLuckyItemKey()         { return luckyItemKey; }
    public boolean isPlaceholderApiEnabled()       { return placeholderApiEnabled; }

    public void reload() {
        reloadConfig();
        rewardManager.loadRewards();
        craftingManager.register();
    }
}
