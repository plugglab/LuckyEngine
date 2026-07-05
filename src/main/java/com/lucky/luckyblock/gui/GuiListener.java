package com.lucky.luckyblock.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class GuiListener implements Listener {

    private final GuiManager manager;

    public GuiListener(GuiManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        BaseGui gui = manager.getOpen(player);
        if (gui == null) return;

        if (event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }

        gui.handleClick(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        // Skip: player is navigating to another GUI screen
        if (manager.isTransitioning(player)) return;
        // Skip: player closed the GUI to type in chat — session handles re-open
        if (manager.hasChatSession(player)) return;
        manager.clearOpen(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!manager.hasChatSession(player)) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();
        ChatInputSession session = manager.getChatSession(player);
        manager.clearChatSession(player);

        player.getServer().getScheduler().runTask(
                manager.getPlugin(),
                () -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        player.sendMessage("§7Input cancelled.");
                        manager.openRewardEditor(player, session.getState());
                    } else {
                        session.complete(input);
                    }
                }
        );
    }
}
