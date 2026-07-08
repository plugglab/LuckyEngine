package com.lucky.luckyblock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LuckyBlockCommand implements CommandExecutor, TabCompleter {

    private final LuckyBlockPlugin plugin;

    public LuckyBlockCommand(LuckyBlockPlugin plugin) {
        this.plugin = plugin;
    }

    private LangManager lang() { return plugin.getLangManager(); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {

            case "gui", "menu" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage(lang().get("console-only")); return true; }
                plugin.getGuiManager().openMainMenu(player);
            }

            case "give" -> {
                if (!sender.hasPermission("luckyblock.admin")) { sender.sendMessage(lang().get("no-permission")); return true; }
                if (args.length < 2) { sender.sendMessage(lang().get("give-usage")); return true; }
                int amount = parseAmount(args, 2, 1);
                ItemStack item = buildLuckyBlockItem(amount);

                // @a or * = give to all online players
                if (args[1].equals("@a") || args[1].equals("*")) {
                    Collection<? extends Player> online = Bukkit.getOnlinePlayers();
                    for (Player target : online) {
                        target.getInventory().addItem(item.clone());
                        target.sendMessage(lang().get("give-success-target", "amount", String.valueOf(amount)));
                    }
                    sender.sendMessage(lang().get("give-success-sender",
                            "amount", String.valueOf(amount),
                            "target", "everyone (" + online.size() + " players)"));
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(lang().get("player-not-found", "player", args[1])); return true; }
                target.getInventory().addItem(item);
                sender.sendMessage(lang().get("give-success-sender", "amount", String.valueOf(amount), "target", target.getName()));
                if (!sender.equals(target))
                    target.sendMessage(lang().get("give-success-target", "amount", String.valueOf(amount)));
            }

            case "setluck" -> {
                if (!sender.hasPermission("luckyblock.admin")) { sender.sendMessage(lang().get("no-permission")); return true; }
                if (args.length < 3) { sender.sendMessage(lang().get("setluck-usage")); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(lang().get("player-not-found", "player", args[1])); return true; }
                try {
                    plugin.getLuckManager().setLuck(target, Integer.parseInt(args[2]));
                    String fmt = plugin.getLuckManager().formatLuck(plugin.getLuckManager().getLuck(target));
                    sender.sendMessage(lang().get("luck-set", "target", target.getName(), "luck", fmt));
                    target.sendMessage(lang().get("luck-set-notify", "luck", fmt));
                } catch (NumberFormatException e) {
                    sender.sendMessage(lang().get("invalid-number", "input", args[2]));
                }
            }

            case "addluck" -> {
                if (!sender.hasPermission("luckyblock.admin")) { sender.sendMessage(lang().get("no-permission")); return true; }
                if (args.length < 3) { sender.sendMessage(lang().get("addluck-usage")); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(lang().get("player-not-found", "player", args[1])); return true; }
                try {
                    plugin.getLuckManager().addLuck(target, Integer.parseInt(args[2]));
                    String fmt = plugin.getLuckManager().formatLuck(plugin.getLuckManager().getLuck(target));
                    sender.sendMessage(lang().get("luck-adjusted", "target", target.getName(), "luck", fmt));
                    target.sendMessage(lang().get("luck-add-notify", "luck", fmt));
                } catch (NumberFormatException e) {
                    sender.sendMessage(lang().get("invalid-number", "input", args[2]));
                }
            }

            case "getluck" -> {
                if (!sender.hasPermission("luckyblock.use")) { sender.sendMessage(lang().get("no-permission")); return true; }
                Player target;
                if (args.length >= 2 && sender.hasPermission("luckyblock.admin")) {
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) { sender.sendMessage(lang().get("player-not-found", "player", args[1])); return true; }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    sender.sendMessage(lang().get("setluck-usage")); return true;
                }
                int luck = plugin.getLuckManager().getLuck(target);
                sender.sendMessage(lang().get("luck-display",
                        "target", target.getName(),
                        "luck",   plugin.getLuckManager().formatLuck(luck),
                        "bar",    luckBar(luck)));
            }

            case "reload" -> {
                if (!sender.hasPermission("luckyblock.admin")) { sender.sendMessage(lang().get("no-permission")); return true; }
                plugin.reload();
                sender.sendMessage(lang().get("reload-success",
                        "count", String.valueOf(plugin.getRewardManager().getRewards().size())));
            }

            case "list" -> {
                if (!sender.hasPermission("luckyblock.use")) { sender.sendMessage(lang().get("no-permission")); return true; }
                List<Reward> rewards = plugin.getRewardManager().getRewards();
                sender.sendMessage(lang().get("list-header", "count", String.valueOf(rewards.size())));
                for (Reward r : rewards) {
                    sender.sendMessage(lang().get("list-entry",
                            "id",     r.getId(),
                            "tier",   r.getTier().name(),
                            "weight", String.valueOf(r.getBaseWeight()),
                            "type",   r.getType().name()));
                }
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(lang().get("help-header"));
        sender.sendMessage(lang().get("help-title"));
        sender.sendMessage(lang().get("help-gui"));
        sender.sendMessage(lang().get("help-give"));
        sender.sendMessage(lang().get("help-setluck"));
        sender.sendMessage(lang().get("help-addluck"));
        sender.sendMessage(lang().get("help-getluck"));
        sender.sendMessage(lang().get("help-list"));
        sender.sendMessage(lang().get("help-reload"));
        sender.sendMessage(lang().get("help-header"));
        sender.sendMessage(lang().get("help-craft"));
    }

    public ItemStack buildLuckyBlockItem(int amount) {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("lucky-block-material", "YELLOW_STAINED_GLASS"));
        if (mat == null) mat = Material.YELLOW_STAINED_GLASS;
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(lang().get("lucky-block-item-name"));
        meta.setLore(List.of(
                lang().get("lucky-block-item-lore-1"),
                lang().get("lucky-block-item-lore-2")));
        meta.getPersistentDataContainer().set(plugin.getLuckyItemKey(), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private int parseAmount(String[] args, int index, int def) {
        if (args.length <= index) return def;
        try { return Math.max(1, Integer.parseInt(args[index])); }
        catch (NumberFormatException e) { return def; }
    }

    private String luckBar(int luck) {
        int filled = (int) Math.round((luck + 100) / 20.0);
        filled = Math.max(0, Math.min(10, filled));
        String col = luck >= 0 ? "§a" : "§c";
        return col + "■".repeat(filled) + "§8" + "□".repeat(10 - filled);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) out.addAll(List.of("gui", "give", "setluck", "addluck", "getluck", "list", "reload"));
        else if (args.length == 2 && List.of("give","setluck","addluck","getluck").contains(args[0].toLowerCase())) {
            if (args[0].equalsIgnoreCase("give")) out.addAll(List.of("@a", "*"));
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setluck")) out.addAll(List.of("-100","-50","0","50","100"));
            if (args[0].equalsIgnoreCase("addluck"))  out.addAll(List.of("-25","-10","10","25"));
            if (args[0].equalsIgnoreCase("give"))     out.addAll(List.of("1","5","10","64"));
        }
        return out;
    }
}
