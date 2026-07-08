package com.lucky.luckyblock;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.logging.Level;

public class RewardManager {

    private final LuckyBlockPlugin plugin;
    private final List<Reward> rewards = new ArrayList<>();
    private final Random random = new Random();

    public RewardManager(LuckyBlockPlugin plugin) { this.plugin = plugin; }
    public List<Reward> getRewards() { return rewards; }

    public void loadRewards() {
        rewards.clear();
        List<?> list = plugin.getConfig().getList("rewards");
        if (list == null) { plugin.getLogger().warning("No 'rewards' list found in config.yml!"); return; }

        for (Object obj : list) {
            ConfigurationSection section;
            if (obj instanceof ConfigurationSection cs) {
                section = cs;
            } else if (obj instanceof Map<?,?> map) {
                MemoryConfiguration mc = new MemoryConfiguration();
                for (var entry : map.entrySet()) mc.set(entry.getKey().toString(), entry.getValue());
                section = mc;
            } else continue;

            try {
                String id = section.getString("id", "reward_" + rewards.size());
                Reward.Type type = Reward.Type.valueOf(section.getString("type", "ITEM").toUpperCase());
                Reward.Tier tier = Reward.Tier.fromString(section.getString("tier", "NEUTRAL"));
                int weight = section.getInt("weight", 10);
                String displayName = ChatColor.translateAlternateColorCodes('&',
                        section.getString("display-name", id));
                rewards.add(new Reward(id, type, tier, weight, displayName, section));
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to load reward: " + section.getString("id", "?"), ex);
            }
        }
        plugin.getLogger().info("Loaded " + rewards.size() + " rewards.");
    }

    public Reward pickRandomReward(int luck) {
        if (rewards.isEmpty()) return null;
        int totalWeight = rewards.stream().mapToInt(r -> r.getEffectiveWeight(luck)).sum();
        if (totalWeight <= 0) return rewards.get(random.nextInt(rewards.size()));
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (Reward r : rewards) {
            cumulative += r.getEffectiveWeight(luck);
            if (roll < cumulative) return r;
        }
        return rewards.get(rewards.size() - 1);
    }

    public void execute(Reward reward, Player player, Location loc) {
        switch (reward.getType()) {
            case ITEM        -> giveItem(reward, player);
            case MULTI_ITEM  -> giveMultiItem(reward, player);
            case COMMAND     -> runCommands(reward, player);
            case MOB_SPAWN   -> spawnMobs(reward, loc, player);
            case EXPLOSION   -> explode(reward, loc);
            case POTION_EFFECT -> applyPotions(reward, player);
            case LIGHTNING   -> strikeLightning(reward, loc);
            case MESSAGE     -> sendMessage(reward, player);
            case STRUCTURE   -> buildStructure(reward, loc, player);
            case CHEST_LOOT  -> dropLootTable(reward, loc, player);
            case ENCHANT_HELD -> enchantHeld(reward, player);
            case FIREWORKS   -> launchFireworks(reward, loc);
            case TRAP        -> executeTrap(reward, player, loc);
            case XP          -> giveXp(reward, player);
        }
        broadcastReward(reward, player);

        // Luck drift
        int drift = plugin.getConfig().getInt("luck-drift-after-break", 0);
        if (drift != 0) plugin.getLuckManager().addLuck(player, drift);
    }

    // ── Item rewards ───────────────────────────────────────────────────────────
    private void giveItem(Reward reward, Player player) {
        ItemStack item = buildItem(reward.getSection());
        give(player, item);
        player.sendMessage(plugin.getLangManager().get("reward-item-received", "reward", reward.getDisplayName()));
    }

    private void giveMultiItem(Reward reward, Player player) {
        List<?> items = reward.getSection().getList("items");
        if (items == null || items.isEmpty()) {
            player.sendMessage("§c[LuckyBlock] Multi-item reward '" + reward.getId() + "' has no items configured.");
            return;
        }
        int given = 0;
        for (Object obj : items) {
            if (!(obj instanceof Map<?,?> rawMap)) continue;
            @SuppressWarnings("unchecked")
            Map<Object,Object> map = (Map<Object,Object>) rawMap;
            MemoryConfiguration mc = new MemoryConfiguration();
            for (var e : map.entrySet()) mc.set(e.getKey().toString(), e.getValue());
            give(player, buildItem(mc));
            given++;
        }
        if (given > 0)
            player.sendMessage(plugin.getLangManager().get("reward-item-received", "reward", reward.getDisplayName()));
    }

    /** Builds an ItemStack from a ConfigurationSection with material/amount/enchantments/custom-name/lore fields. */
    @SuppressWarnings("unchecked")
    private ItemStack buildItem(ConfigurationSection sec) {
        Material mat = Material.matchMaterial(sec.getString("material", "DIRT"));
        if (mat == null) mat = Material.DIRT;
        int amount = sec.getInt("amount", 1);
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String customName = sec.getString("custom-name", null);
            if (customName != null)
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', customName));
            List<String> lore = sec.getStringList("lore");
            if (!lore.isEmpty()) {
                lore.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s));
                meta.setLore(lore);
            }
            List<String> enchants = sec.getStringList("enchantments");
            for (String entry : enchants) {
                String[] parts = entry.split(":");
                if (parts.length != 2) continue;
                Enchantment ench = Enchantment.getByName(parts[0].trim().toUpperCase());
                if (ench == null) continue;
                try { meta.addEnchant(ench, Integer.parseInt(parts[1].trim()), true); }
                catch (NumberFormatException ignored) {}
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void give(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        for (ItemStack extra : leftover.values())
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
    }

    // ── Commands ──────────────────────────────────────────────────────────────
    private void runCommands(Reward reward, Player player) {
        for (String cmd : reward.getStringList("commands"))
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
    }

    // ── Mob spawns ────────────────────────────────────────────────────────────
    private void spawnMobs(Reward reward, Location loc, Player player) {
        String mobName = reward.getString("mob", "ZOMBIE").toUpperCase();
        EntityType type;
        try { type = EntityType.valueOf(mobName); }
        catch (IllegalArgumentException e) { type = EntityType.ZOMBIE; }

        int count = reward.getInt("count", 1);
        boolean powered = reward.getBoolean("powered", false);
        boolean tamed = reward.getBoolean("tamed", false);
        boolean dropFromSky = reward.getBoolean("drop-from-sky", false);
        boolean withGear = reward.getBoolean("with-gear", false);

        World world = loc.getWorld();
        if (world == null) return;

        for (int i = 0; i < count; i++) {
            Location spawnLoc = dropFromSky
                    ? loc.clone().add(random.nextInt(7) - 3, 20 + random.nextInt(10), random.nextInt(7) - 3)
                    : loc.clone().add(random.nextInt(5) - 2, 1, random.nextInt(5) - 2);

            Entity e = world.spawnEntity(spawnLoc, type);

            if (powered && e instanceof Creeper c) c.setPowered(true);
            if (tamed && e instanceof Horse h) {
                h.setTamed(true);
                h.setOwner(player);
            }
            if (withGear && e instanceof Zombie z) {
                z.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                z.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            }
        }
    }

    // ── Explosion ─────────────────────────────────────────────────────────────
    private void explode(Reward reward, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        float power = (float) reward.getDouble("power", 2.0);
        boolean breakBlocks = reward.getBoolean("break-blocks", false);
        world.createExplosion(loc.clone().add(0.5, 0.5, 0.5), power, false, breakBlocks);
    }

    // ── Potion effects (supports multiple) ───────────────────────────────────
    private void applyPotions(Reward reward, Player player) {
        // Support both a single effect and a list of effects
        List<?> effectList = reward.getSection().getList("effects");
        if (effectList != null && !effectList.isEmpty()) {
            for (Object obj : effectList) {
                if (!(obj instanceof Map<?,?> raw)) continue;
                @SuppressWarnings("unchecked")
                Map<Object, Object> map = (Map<Object, Object>) raw;
                String eName = String.valueOf(map.getOrDefault("effect", "SPEED")).toUpperCase();
                int dur = Integer.parseInt(String.valueOf(map.getOrDefault("duration-seconds", 30))) * 20;
                int amp = Integer.parseInt(String.valueOf(map.getOrDefault("amplifier", 0)));
                PotionEffectType pet = PotionEffectType.getByName(eName);
                if (pet != null) player.addPotionEffect(new PotionEffect(pet, dur, amp));
            }
        } else {
            // Legacy single-effect format
            PotionEffectType pet = PotionEffectType.getByName(reward.getString("effect", "SPEED").toUpperCase());
            if (pet != null) {
                int dur = reward.getInt("duration-seconds", 30) * 20;
                int amp = reward.getInt("amplifier", 0);
                player.addPotionEffect(new PotionEffect(pet, dur, amp));
            }
        }
    }

    // ── Lightning ─────────────────────────────────────────────────────────────
    private void strikeLightning(Reward reward, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        int count = reward.getInt("count", 1);
        boolean damage = reward.getBoolean("damage", false);
        for (int i = 0; i < count; i++) {
            Location target = loc.clone().add(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
            if (damage) world.strikeLightning(target);
            else world.strikeLightningEffect(target);
        }
    }

    // ── Message ───────────────────────────────────────────────────────────────
    private void sendMessage(Reward reward, Player player) {
        String msg = reward.getString("message", "");
        if (!msg.isEmpty()) player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    // ── Structures ────────────────────────────────────────────────────────────
    private void buildStructure(Reward reward, Location loc, Player player) {
        String structure = reward.getString("structure", "").toUpperCase();
        World world = loc.getWorld();
        if (world == null) return;

        switch (structure) {
            case "OBSIDIAN_CAGE" -> buildObsidianCage(loc, world, player);
            case "COBWEB_TRAP" -> buildCobwebTrap(loc, world);
            case "TREASURE_VAULT" -> buildTreasureVault(loc, world);
            case "ORE_VEIN_DIAMOND" -> buildOreVein(loc, world, Material.DIAMOND_ORE);
            default -> plugin.getLogger().warning("Unknown structure: " + structure);
        }
    }

    /**
     * Fixed obsidian cage — spawns the player inside before sealing.
     * A 3×5×3 cage with air inside, obsidian walls/roof/floor, and glass
     * windows so the trapped player can see out (but not escape easily).
     */
    private void buildObsidianCage(Location loc, World world, Player player) {
        // Centre the cage one block above the break location
        Location base = loc.clone().add(-1, 0, -1);

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 3; z++) {
                    boolean isFloor = (y == 0);
                    boolean isRoof  = (y == 4);
                    boolean isEdge  = (x == 0 && z == 0) || (x == 0 && z == 2)
                                   || (x == 2 && z == 0) || (x == 2 && z == 2);
                    boolean isWall  = (x == 0 || x == 2 || z == 0 || z == 2);
                    boolean isInner = !isWall;

                    Material mat;
                    if (isFloor || isRoof || isEdge) {
                        mat = Material.OBSIDIAN;
                    } else if (isWall) {
                        // Glass panes on middle rows of each face so player can see
                        mat = (y == 1 || y == 2 || y == 3) ? Material.GLASS : Material.OBSIDIAN;
                    } else {
                        mat = Material.AIR;
                    }
                    world.getBlockAt(base.clone().add(x, y, z)).setType(mat);
                }
            }
        }

        // Teleport player into the centre of the cage after it's built
        Location cageCenter = loc.clone().add(0.5, 1, 0.5);
        cageCenter.setYaw(player.getLocation().getYaw());
        cageCenter.setPitch(player.getLocation().getPitch());
        player.teleport(cageCenter);
    }

    private void buildCobwebTrap(Location loc, World world) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location target = loc.clone().add(x, y, z);
                    if (world.getBlockAt(target).getType() == Material.AIR)
                        world.getBlockAt(target).setType(Material.COBWEB);
                }
            }
        }
    }

    private void buildTreasureVault(Location loc, World world) {
        Location base = loc.clone().add(-2, 0, -2);

        // Build 5×4×5 stone brick shell
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 5; z++) {
                    boolean isShell = (x == 0 || x == 4 || y == 0 || y == 3 || z == 0 || z == 4);
                    world.getBlockAt(base.clone().add(x, y, z))
                            .setType(isShell ? Material.STONE_BRICKS : Material.AIR, false);
                }
            }
        }

        // 2-tall entrance in front face
        world.getBlockAt(base.clone().add(2, 1, 0)).setType(Material.AIR, false);
        world.getBlockAt(base.clone().add(2, 2, 0)).setType(Material.AIR, false);

        // Torches
        world.getBlockAt(base.clone().add(1, 2, 1)).setType(Material.TORCH, false);
        world.getBlockAt(base.clone().add(3, 2, 3)).setType(Material.TORCH, false);

        // Chest — set block first, then fill via the LIVE block state (not a snapshot)
        Location chestLoc = base.clone().add(2, 1, 2);
        world.getBlockAt(chestLoc).setType(Material.CHEST, false);

        // Schedule one tick later so the block finishes placing before we access its inventory
        Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.block.BlockState state = world.getBlockAt(chestLoc).getState();
            if (state instanceof org.bukkit.block.Chest chest) {
                org.bukkit.inventory.Inventory inv = chest.getInventory();
                inv.addItem(new ItemStack(Material.DIAMOND, 5));
                inv.addItem(new ItemStack(Material.GOLD_INGOT, 10));
                inv.addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
                inv.addItem(new ItemStack(Material.NETHERITE_INGOT, 1));
                inv.addItem(new ItemStack(Material.EMERALD, 8));
                inv.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
                inv.addItem(new ItemStack(Material.DIAMOND_SWORD, 1));
                inv.addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, 16));
            }
        });
    }

    private void buildOreVein(Location loc, World world, Material oreMat) {
        Random r = new Random();
        for (int i = 0; i < 12; i++) {
            Location ore = loc.clone().add(r.nextInt(5) - 2, r.nextInt(3) - 1, r.nextInt(5) - 2);
            if (world.getBlockAt(ore).getType().isSolid())
                world.getBlockAt(ore).setType(oreMat);
        }
    }

    // ── Loot table ────────────────────────────────────────────────────────────
    private void dropLootTable(Reward reward, Location loc, Player player) {
        String tableStr = reward.getString("loot-table", "minecraft:chests/simple_dungeon");
        try {
            NamespacedKey key = NamespacedKey.fromString(tableStr);
            if (key == null) return;
            LootTable table = Bukkit.getLootTable(key);
            if (table == null) return;
            LootContext ctx = new LootContext.Builder(loc).killer(player).lootedEntity(player).build();
            Collection<ItemStack> items = table.populateLoot(random, ctx);
            for (ItemStack item : items) loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 1, 0.5), item);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to populate loot table: " + tableStr + " — " + e.getMessage());
        }
    }

    // ── Enchant held item ─────────────────────────────────────────────────────
    private void enchantHeld(Reward reward, Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Nothing to enchant — hold an item!");
            return;
        }
        int levels = reward.getInt("levels", 30);
        Enchantment[] all = Enchantment.values();
        Random r = new Random();
        int added = 0;
        // Try up to 20 times to find compatible enchantments
        ItemMeta meta = item.getItemMeta();
        for (int attempt = 0; attempt < 20 && added < 3; attempt++) {
            Enchantment ench = all[r.nextInt(all.length)];
            if (!ench.canEnchantItem(item) && !meta.hasEnchant(ench)) continue;
            int maxLevel = Math.min(ench.getMaxLevel(), Math.max(1, levels / 10));
            int level = 1 + r.nextInt(maxLevel);
            meta.addEnchant(ench, level, true);
            added++;
        }
        item.setItemMeta(meta);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Your " + item.getType().name().replace('_', ' ') + " has been enchanted!");
    }

    // ── Fireworks ─────────────────────────────────────────────────────────────
    private void launchFireworks(Reward reward, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        int count = reward.getInt("count", 3);
        Random r = new Random();
        org.bukkit.FireworkEffect.Type[] types = org.bukkit.FireworkEffect.Type.values();
        for (int i = 0; i < count; i++) {
            Firework fw = (Firework) world.spawnEntity(loc.clone().add(r.nextInt(5) - 2, 1, r.nextInt(5) - 2), EntityType.FIREWORK_ROCKET);
            var meta = fw.getFireworkMeta();
            var effect = org.bukkit.FireworkEffect.builder()
                    .with(types[r.nextInt(types.length)])
                    .withColor(Color.fromRGB(r.nextInt(0xFFFFFF)))
                    .withFade(Color.fromRGB(r.nextInt(0xFFFFFF)))
                    .trail(r.nextBoolean())
                    .flicker(r.nextBoolean())
                    .build();
            meta.addEffect(effect);
            meta.setPower(1 + r.nextInt(2));
            fw.setFireworkMeta(meta);
        }
    }

    // ── Traps ─────────────────────────────────────────────────────────────────
    private void executeTrap(Reward reward, Player player, Location loc) {
        String trap = reward.getString("trap", "").toUpperCase();
        if (trap.equals("DROP_HOTBAR")) {
            // Drop everything in hotbar slots on the ground
            for (int i = 0; i < 9; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                    player.getInventory().setItem(i, null);
                }
            }
            player.sendMessage(ChatColor.RED + "Your hotbar exploded with bad luck!");
        }
    }

    // ── XP ────────────────────────────────────────────────────────────────────
    private void giveXp(Reward reward, Player player) {
        int points = reward.getInt("xp-points", 0);
        int levels = reward.getInt("xp-levels", 0);
        if (points > 0) player.giveExp(points);
        if (levels > 0) player.giveExpLevels(levels);
        LangManager lang = plugin.getLangManager();
        if (points > 0 && levels > 0)
            player.sendMessage(lang.get("reward-xp-and-levels", "points", String.valueOf(points), "levels", String.valueOf(levels)));
        else if (points > 0)
            player.sendMessage(lang.get("reward-xp-received", "points", String.valueOf(points)));
        else if (levels > 0)
            player.sendMessage(lang.get("reward-levels-received", "levels", String.valueOf(levels)));
    }

    // ── Broadcast ─────────────────────────────────────────────────────────────
    private void broadcastReward(Reward reward, Player player) {
        if (!plugin.getConfig().getBoolean("broadcast-rewards", true)) return;
        // Only LEGENDARY (GREAT) tier announces to the whole server
        if (reward.getTier() != Reward.Tier.GREAT) return;
        String msg = plugin.getLangManager().get("broadcast-format",
                "player", player.getName(), "reward", reward.getDisplayName());
        Bukkit.broadcastMessage(msg);
    }
}
