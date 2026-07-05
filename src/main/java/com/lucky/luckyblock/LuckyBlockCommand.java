package com.lucky.luckyblock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class LuckyBlockCommand implements CommandExecutor, TabCompleter {

    private final LuckyBlockPlugin plugin;

    public LuckyBlockCommand(LuckyBlockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {

            case "gui", "menu" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(pre() + "§cThis command can only be used by players.");
                    return true;
                }
                plugin.getGuiManager().openMainMenu(player);
            }

            case "give" -> {
                if (!sender.hasPermission("luckyblock.admin")) { noPerms(sender); return true; }
                if (args.length < 2) { sender.sendMessage(pre() + "§cUsage: /luckyblock give <player> [amount]"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(pre() + "§cPlayer not found: " + args[1]); return true; }
                int amount = parseAmount(args, 2, 1);
                ItemStack item = buildLuckyBlockItem(amount);
                target.getInventory().addItem(item);
                sender.sendMessage(pre() + "§aGave §e" + amount + " §aLucky Block(s) to §e" + target.getName());
                if (!sender.equals(target))
                    target.sendMessage(pre() + "§aYou received §e" + amount + " §aLucky Block(s)!");
            }

            case "setluck" -> {
                if (!sender.hasPermission("luckyblock.admin")) { noPerms(sender); return true; }
                if (args.length < 3) { sender.sendMessage(pre() + "§cUsage: /luckyblock setluck <player> <-100..100>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(pre() + "§cPlayer not found: " + args[1]); return true; }
                try {
                    int val = Integer.parseInt(args[2]);
                    plugin.getLuckManager().setLuck(target, val);
                    int clamped = plugin.getLuckManager().getLuck(target);
                    sender.sendMessage(pre() + "§aSet §e" + target.getName() + "§a's luck to "
                            + plugin.getLuckManager().formatLuck(clamped));
                    target.sendMessage(pre() + "§7Your luck was set to "
                            + plugin.getLuckManager().formatLuck(clamped));
                } catch (NumberFormatException e) {
                    sender.sendMessage(pre() + "§cInvalid number: " + args[2]);
                }
            }

            case "addluck" -> {
                if (!sender.hasPermission("luckyblock.admin")) { noPerms(sender); return true; }
                if (args.length < 3) { sender.sendMessage(pre() + "§cUsage: /luckyblock addluck <player> <amount>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(pre() + "§cPlayer not found: " + args[1]); return true; }
                try {
                    int delta = Integer.parseInt(args[2]);
                    plugin.getLuckManager().addLuck(target, delta);
                    int newLuck = plugin.getLuckManager().getLuck(target);
                    sender.sendMessage(pre() + "§aAdjusted luck. §e" + target.getName()
                            + " §7is now at " + plugin.getLuckManager().formatLuck(newLuck));
                    target.sendMessage(pre() + "§7Your luck changed! Now: "
                            + plugin.getLuckManager().formatLuck(newLuck));
                } catch (NumberFormatException e) {
                    sender.sendMessage(pre() + "§cInvalid number: " + args[2]);
                }
            }

            case "getluck" -> {
                if (!sender.hasPermission("luckyblock.use")) { noPerms(sender); return true; }
                Player target;
                if (args.length >= 2 && sender.hasPermission("luckyblock.admin")) {
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) { sender.sendMessage(pre() + "§cPlayer not found: " + args[1]); return true; }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    sender.sendMessage(pre() + "§cSpecify a player when running from console."); return true;
                }
                int luck = plugin.getLuckManager().getLuck(target);
                sender.sendMessage(pre() + "§7" + target.getName() + "'s luck: "
                        + plugin.getLuckManager().formatLuck(luck)
                        + " §8(" + luckBar(luck) + "§8)");
            }

            case "reload" -> {
                if (!sender.hasPermission("luckyblock.admin")) { noPerms(sender); return true; }
                plugin.reload();
                sender.sendMessage(pre() + "§aConfig reloaded. §e"
                        + plugin.getRewardManager().getRewards().size() + " §arewards loaded.");
            }

            case "list" -> {
                if (!sender.hasPermission("luckyblock.use")) { noPerms(sender); return true; }
                List<Reward> rewards = plugin.getRewardManager().getRewards();
                sender.sendMessage(pre() + "§7Loaded rewards §8(" + rewards.size() + ")§7:");
                for (Reward r : rewards) {
                    String tierColor = switch (r.getTier()) {
                        case GREAT   -> "§5";
                        case GOOD    -> "§a";
                        case NEUTRAL -> "§e";
                        case BAD     -> "§c";
                    };
                    sender.sendMessage("§8 › §7" + r.getId() + " §8| " + tierColor + r.getTier()
                            + " §8| §7weight " + r.getBaseWeight() + " §8| §7" + r.getType());
                }
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m──────────────────────────────");
        sender.sendMessage("§6✦ LuckyBlock §7v1.0  §8Commands:");
        sender.sendMessage("§e/lb gui                    §7- open the GUI menu");
        sender.sendMessage("§e/lb give <player> [amt]    §7- give Lucky Blocks");
        sender.sendMessage("§e/lb setluck <player> <n>   §7- set luck (-100→100)");
        sender.sendMessage("§e/lb addluck <player> <n>   §7- add/subtract luck");
        sender.sendMessage("§e/lb getluck [player]       §7- check luck value");
        sender.sendMessage("§e/lb list                   §7- list all rewards");
        sender.sendMessage("§e/lb reload                 §7- reload config");
        sender.sendMessage("§8§m──────────────────────────────");
        sender.sendMessage("§7Craft: §88 §fGlass §7+ §81 §eWet Sponge §7in a ring");
        sender.sendMessage("§8§m──────────────────────────────");
    }

    private void noPerms(CommandSender sender) {
        sender.sendMessage(pre() + "§cYou don't have permission to do that.");
    }

    private String pre() {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("prefix", "&6[&eLucky&6] &r"));
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

    public ItemStack buildLuckyBlockItem(int amount) {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("lucky-block-material", "GOLD_BLOCK"));
        if (mat == null) mat = Material.GOLD_BLOCK;
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ Lucky Block");
        meta.setLore(List.of(
                ChatColor.GRAY + "Place and break for a random reward!",
                ChatColor.DARK_GRAY + "Your luck determines what you get."
        ));
        meta.getPersistentDataContainer().set(plugin.getLuckyItemKey(), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("gui", "give", "setluck", "addluck", "getluck", "list", "reload"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give") || sub.equals("setluck") || sub.equals("addluck") || sub.equals("getluck")) {
                for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setluck")) out.addAll(List.of("-100", "-50", "0", "50", "100"));
            if (sub.equals("addluck")) out.addAll(List.of("-25", "-10", "10", "25"));
            if (sub.equals("give")) out.addAll(List.of("1", "5", "10", "64"));
        }
        return out;
    }
}
