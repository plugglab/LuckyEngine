package com.lucky.luckyblock;

import com.lucky.luckyblock.gui.GuiListener;
import com.lucky.luckyblock.gui.GuiManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class LuckyBlockPlugin extends JavaPlugin {

    private RewardManager rewardManager;
    private LuckManager luckManager;
    private LangManager langManager;
    private StatsManager statsManager;
    private CraftingManager craftingManager;
    private GuiManager guiManager;
    private NamespacedKey luckyItemKey;
    private NamespacedKey rarityKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.luckyItemKey  = new NamespacedKey(this, "lucky_block_item");
        this.rarityKey     = new NamespacedKey(this, "lucky_block_rarity");
        this.langManager   = new LangManager(this);
        this.langManager.load();
        this.luckManager   = new LuckManager(this);
        this.statsManager  = new StatsManager(this);
        this.statsManager.init();
        this.rewardManager = new RewardManager(this);
        this.rewardManager.loadRewards();

        LuckyBlockTracker.init(this);

        this.craftingManager = new CraftingManager(this);
        this.craftingManager.register();

        this.guiManager = new GuiManager(this);

        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(guiManager), this);

        LuckyBlockCommand cmd = new LuckyBlockCommand(this);
        Objects.requireNonNull(getCommand("luckyblock")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("luckyblock")).setTabCompleter(cmd);
        UpdateChecker updateChecker = new UpdateChecker(this);
        updateChecker.check();
        getServer()
                .getPluginManager()
                .registerEvents(
                        new JoinListener(updateChecker, this),
                        this
                );

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LuckyBlockPlaceholders(this).register();
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

    public RewardManager getRewardManager()       { return rewardManager; }
    public LuckManager getLuckManager()           { return luckManager; }
    public LangManager getLangManager()           { return langManager; }
    public StatsManager getStatsManager()         { return statsManager; }
    public GuiManager getGuiManager()             { return guiManager; }
    public NamespacedKey getLuckyItemKey()         { return luckyItemKey; }
    public NamespacedKey getRarityKey()            { return rarityKey; }

    public void reload() {
        reloadConfig();
        langManager.load();
        rewardManager.loadRewards();
        craftingManager.register();
    }
}
