package com.lucky.luckyblock;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class JoinListener implements Listener {

    private final UpdateChecker checker;
    private final JavaPlugin plugin;


    public JoinListener(UpdateChecker checker, JavaPlugin plugin) {
        this.checker = checker;
        this.plugin = plugin;
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> checker.notifyPlayer(event.getPlayer()),
                40L
        );

    }

}