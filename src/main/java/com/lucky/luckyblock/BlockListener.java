package com.lucky.luckyblock;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class BlockListener implements Listener {

    private final LuckyBlockPlugin plugin;

    public BlockListener(LuckyBlockPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Place ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        if (!hand.hasItemMeta()) return;
        var meta = hand.getItemMeta();
        if (meta == null) return;
        if (!meta.getPersistentDataContainer().has(plugin.getLuckyItemKey(), PersistentDataType.BYTE)) return;

        Block block = event.getBlock();
        // Real block is yellow stained glass — visible, breakable by players normally
        block.setType(Material.YELLOW_STAINED_GLASS);

        // Spawn display at block corner (0,0,0 of the block).
        // Translation (0.25, 0.25, 0.25) + scale (0.5, 0.5, 0.5) centres
        // the sponge perfectly inside the 1×1×1 glass shell.
        Location corner = block.getLocation();

        BlockDisplay inner = spawnDisplay(corner, Material.SPONGE,
                new Vector3f(0.25f, 0.25f, 0.25f),
                new Vector3f(0.5f, 0.5f, 0.5f));

        LuckyBlockTracker.mark(block.getLocation(), inner.getUniqueId());
    }

    // ── Break ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();

        if (!LuckyBlockTracker.isMarked(loc)) return;

        // Remove display entity and untrack
        LuckyBlockTracker.unmark(loc);
        event.setDropItems(false);
        block.setType(Material.AIR);

        Player player = event.getPlayer();
        int luck = plugin.getLuckManager().getLuck(player);

        Reward reward = plugin.getRewardManager().pickRandomReward(luck);
        if (reward == null) {
            player.sendMessage(ChatColor.RED + "No rewards configured!");
            return;
        }

        spawnBreakParticles(loc);
        playBreakSound(loc);
        sendLuckHUD(player, luck, reward);

        plugin.getRewardManager().execute(reward, player, loc.clone());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private BlockDisplay spawnDisplay(Location loc, Material mat,
                                      Vector3f translation, Vector3f scale) {
        BlockDisplay display = (BlockDisplay) loc.getWorld().spawnEntity(loc, EntityType.BLOCK_DISPLAY);
        display.setBlock(Bukkit.createBlockData(mat));
        display.setPersistent(true);
        display.setGravity(false);

        Transformation tf = new Transformation(
                translation,
                new AxisAngle4f(0, 0, 1, 0),
                scale,
                new AxisAngle4f(0, 0, 1, 0)
        );
        display.setTransformation(tf);

        display.setGlowing(false);
        display.setShadowRadius(0.5f);
        display.setShadowStrength(0.8f);

        return display;
    }

    private void spawnBreakParticles(Location loc) {
        try {
            String name = plugin.getConfig().getString("effects.break-particle", "TOTEM_OF_UNDYING");
            Particle p = Particle.valueOf(name);
            loc.getWorld().spawnParticle(p, loc.clone().add(0.5, 0.5, 0.5), 60, 0.6, 0.6, 0.6, 0.1);
        } catch (IllegalArgumentException ignored) {}
    }

    private void playBreakSound(Location loc) {
        try {
            String name = plugin.getConfig().getString("effects.break-sound", "ENTITY_PLAYER_LEVELUP");
            Sound s = Sound.valueOf(name);
            loc.getWorld().playSound(loc, s, 1f, 1f);
        } catch (IllegalArgumentException ignored) {}
    }

    private void sendLuckHUD(Player player, int luck, Reward reward) {
        String luckStr = plugin.getLuckManager().formatLuck(luck);
        String tier = switch (reward.getTier()) {
            case GREAT   -> "§5§lLEGENDARY";
            case GOOD    -> "§a§lGOOD";
            case NEUTRAL -> "§e§lNEUTRAL";
            case BAD     -> "§c§lBAD";
        };
        player.sendMessage("§8┌──────────────────────────────┐");
        player.sendMessage("§8│ §6✦ Lucky Block §8| §7Luck: " + luckStr);
        player.sendMessage("§8│ §7Tier: " + tier);
        player.sendMessage("§8└──────────────────────────────┘");
    }
}
