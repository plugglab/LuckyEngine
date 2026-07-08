package com.lucky.luckyblock;

import org.bukkit.*;
import org.bukkit.block.Block;
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

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        if (!hand.hasItemMeta()) return;
        var meta = hand.getItemMeta();
        if (meta == null) return;
        if (!meta.getPersistentDataContainer().has(plugin.getLuckyItemKey(), PersistentDataType.BYTE)) return;

        Block block = event.getBlock();
        block.setType(Material.YELLOW_STAINED_GLASS);

        BlockDisplay inner = spawnDisplay(block.getLocation(), Material.SPONGE,
                new Vector3f(0.25f, 0.25f, 0.25f),
                new Vector3f(0.5f, 0.5f, 0.5f));

        LuckyBlockTracker.mark(block.getLocation(), inner.getUniqueId());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        if (!LuckyBlockTracker.isMarked(loc)) return;

        LuckyBlockTracker.unmark(loc);
        event.setDropItems(false);
        block.setType(Material.AIR);

        Player player = event.getPlayer();
        LangManager lang = plugin.getLangManager();
        int luck = plugin.getLuckManager().getLuck(player);

        Reward reward = plugin.getRewardManager().pickRandomReward(luck);
        if (reward == null) {
            player.sendMessage(lang.get("no-rewards-configured"));
            return;
        }

        spawnBreakParticles(loc);
        playBreakSound(loc);
        sendLuckHUD(player, luck, reward);
        plugin.getStatsManager().recordBreak(player.getUniqueId());
        plugin.getRewardManager().execute(reward, player, loc.clone());
    }

    private BlockDisplay spawnDisplay(Location loc, Material mat, Vector3f translation, Vector3f scale) {
        BlockDisplay display = (BlockDisplay) loc.getWorld().spawnEntity(loc, EntityType.BLOCK_DISPLAY);
        display.setBlock(Bukkit.createBlockData(mat));
        display.setPersistent(true);
        display.setGravity(false);
        display.setTransformation(new Transformation(
                translation,
                new AxisAngle4f(0, 0, 1, 0),
                scale,
                new AxisAngle4f(0, 0, 1, 0)));
        display.setGlowing(false);
        display.setShadowRadius(0.5f);
        display.setShadowStrength(0.8f);
        return display;
    }

    private void spawnBreakParticles(Location loc) {
        try {
            Particle p = Particle.valueOf(plugin.getConfig().getString("effects.break-particle", "TOTEM_OF_UNDYING"));
            int count = plugin.getConfig().getInt("effects.particle-count", 60);
            loc.getWorld().spawnParticle(p, loc.clone().add(0.5, 0.5, 0.5), count, 0.6, 0.6, 0.6, 0.1);
        } catch (IllegalArgumentException ignored) {}
    }

    private void playBreakSound(Location loc) {
        try {
            Sound s = Sound.valueOf(plugin.getConfig().getString("effects.break-sound", "ENTITY_PLAYER_LEVELUP"));
            float vol = (float) plugin.getConfig().getDouble("effects.sound-volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("effects.sound-pitch", 1.0);
            loc.getWorld().playSound(loc, s, vol, pitch);
        } catch (IllegalArgumentException ignored) {}
    }

    private void sendLuckHUD(Player player, int luck, Reward reward) {
        if (!plugin.getConfig().getBoolean("hud.enabled", true)) return;
        LangManager lang = plugin.getLangManager();
        String luckStr = plugin.getLuckManager().formatLuck(luck);
        String tier = lang.get("tier-" + reward.getTier().name().toLowerCase());
        player.sendMessage(lang.get("hud-line-1"));
        player.sendMessage(lang.get("hud-line-2", "luck", luckStr, "tier", tier));
        player.sendMessage(lang.get("hud-line-3"));

        // Private notify for GOOD drops (GREAT ones broadcast to server via broadcastReward)
        if (reward.getTier() == Reward.Tier.GOOD) {
            player.sendMessage(lang.get("reward-good-notify", "reward", reward.getDisplayName()));
        }
    }
}
