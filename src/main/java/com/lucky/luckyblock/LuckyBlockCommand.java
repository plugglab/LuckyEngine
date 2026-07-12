package com.lucky.luckyblock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.util.*;

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

                // /lb give <player|@a|*> [rarity] [amount]
                LuckyBlockRarity rarity = LuckyBlockRarity.COMMON;
                int amount = 1;
                if (args.length >= 3) {
                    LuckyBlockRarity parsed = tryParseRarity(args[2]);
                    if (parsed != null) { rarity = parsed; }
                    else { try { amount = Math.max(1, Integer.parseInt(args[2])); } catch (NumberFormatException ignored) {} }
                }
                if (args.length >= 4) {
                    try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (NumberFormatException ignored) {}
                }

                ItemStack item = buildLuckyBlockItem(amount, rarity);
                final LuckyBlockRarity finalRarity = rarity;
                final int finalAmount = amount;

                if (args[1].equals("@a") || args[1].equals("*")) {
                    Collection<? extends Player> online = Bukkit.getOnlinePlayers();
                    for (Player t : online) {
                        t.getInventory().addItem(item.clone());
                        t.sendMessage(lang().get("give-success-target", "amount", String.valueOf(finalAmount)));
                    }
                    sender.sendMessage(lang().get("give-success-sender",
                            "amount", String.valueOf(finalAmount),
                            "target", "everyone (" + online.size() + " players)"));
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(lang().get("player-not-found", "player", args[1])); return true; }
                target.getInventory().addItem(item);
                sender.sendMessage(lang().get("give-success-sender",
                        "amount", String.valueOf(finalAmount), "target", target.getName()));
                if (!sender.equals(target))
                    target.sendMessage(lang().get("give-success-target", "amount", String.valueOf(finalAmount)));
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
                } else if (sender instanceof Player p) { target = p; }
                else { sender.sendMessage(lang().get("setluck-usage")); return true; }
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
                for (Reward r : rewards)
                    sender.sendMessage(lang().get("list-entry",
                            "id", r.getId(), "tier", r.getTier().name(),
                            "weight", String.valueOf(r.getBaseWeight()), "type", r.getType().name()));
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    // ── Build lucky block skull item ───────────────────────────────────────────

    public ItemStack buildLuckyBlockItem(int amount, LuckyBlockRarity rarity) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, amount);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        // Apply skull texture
        PlayerProfile profile = createProfile(rarity.getTexture());
        if (profile != null) meta.setOwnerProfile(profile);

        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', rarity.getItemName()));

        List<String> lore = new ArrayList<>();
        for (String line : rarity.getLore())
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(plugin.getLuckyItemKey(), PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(plugin.getRarityKey(), PersistentDataType.STRING, rarity.name());
        item.setItemMeta(meta);
        return item;
    }

    /** Convenience — COMMON rarity default. */
    public ItemStack buildLuckyBlockItem(int amount) {
        return buildLuckyBlockItem(amount, LuckyBlockRarity.COMMON);
    }

    private PlayerProfile createProfile(String base64Texture) {
        try {
            String json = new String(Base64.getDecoder().decode(base64Texture));
            int urlStart = json.indexOf("\"url\":\"") + 7;
            int urlEnd   = json.indexOf("\"", urlStart);
            String url   = json.substring(urlStart, urlEnd);
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "LuckyBlock");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URI(url).toURL());
            profile.setTextures(textures);
            return profile;
        } catch (Exception e) {
            plugin.getLogger().warning("Could not apply skull texture: " + e.getMessage());
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private LuckyBlockRarity tryParseRarity(String s) {
        try { return LuckyBlockRarity.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private int parseAmount(String[] args, int index, int def) {
        if (args.length <= index) return def;
        try { return Math.max(1, Integer.parseInt(args[index])); }
        catch (NumberFormatException e) { return def; }
    }

    private String luckBar(int luck) {
        int filled = (int) Math.round((luck + 100) / 20.0);
        filled = Math.max(0, Math.min(10, filled));
        return (luck >= 0 ? "§a" : "§c") + "■".repeat(filled) + "§8" + "□".repeat(10 - filled);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1)
            out.addAll(List.of("gui","give","setluck","addluck","getluck","list","reload"));
        else if (args.length == 2 && List.of("give","setluck","addluck","getluck").contains(args[0].toLowerCase())) {
            if (args[0].equalsIgnoreCase("give")) out.addAll(List.of("@a","*"));
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give"))
                for (LuckyBlockRarity r : LuckyBlockRarity.values()) out.add(r.name());
            if (args[0].equalsIgnoreCase("setluck")) out.addAll(List.of("-100","-50","0","50","100"));
            if (args[0].equalsIgnoreCase("addluck"))  out.addAll(List.of("-25","-10","10","25"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give"))
            out.addAll(List.of("1","5","10","64"));
        return out;
    }
}
