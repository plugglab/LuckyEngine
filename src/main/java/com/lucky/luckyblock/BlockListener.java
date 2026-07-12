package com.lucky.luckyblock;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.util.Base64;
import java.util.UUID;

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

        // Read rarity from NBT
        String rarityStr = meta.getPersistentDataContainer()
                .getOrDefault(plugin.getRarityKey(), PersistentDataType.STRING, "COMMON");
        LuckyBlockRarity rarity = LuckyBlockRarity.fromString(rarityStr);

        Block block = event.getBlock();
        block.setType(Material.PLAYER_HEAD, false);

        // Apply skull texture
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!(block.getState() instanceof Skull skull)) return;
            PlayerProfile profile = createProfile(rarity.getTexture());
            if (profile != null) {
                skull.setOwnerProfile(profile);
                skull.update(true, false);
            }
        });

        LuckyBlockTracker.mark(block.getLocation(), rarity);
    }

    // ── Break ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        if (!LuckyBlockTracker.isMarked(loc)) return;

        LuckyBlockRarity rarity = LuckyBlockTracker.getRarity(loc);
        LuckyBlockTracker.unmark(loc);
        event.setDropItems(false);
        block.setType(Material.AIR, false);

        Player player = event.getPlayer();
        LangManager lang = plugin.getLangManager();

        // Effective luck = player's base luck + rarity bonus (capped at max)
        int baseLuck    = plugin.getLuckManager().getLuck(player);
        int luckBonus   = plugin.getConfig().getInt("rarity." + rarity.name().toLowerCase() + ".luck-bonus", rarity.getLuckBonus());
        int effectiveLuck = Math.min(
                plugin.getConfig().getInt("luck-max", 100),
                baseLuck + luckBonus);

        Reward reward = plugin.getRewardManager().pickRandomReward(effectiveLuck);
        if (reward == null) {
            player.sendMessage(lang.get("no-rewards-configured"));
            return;
        }

        spawnBreakParticles(loc, rarity);
        playBreakSound(loc);
        sendLuckHUD(player, baseLuck, luckBonus, reward, rarity);
        plugin.getStatsManager().recordBreak(player.getUniqueId());
        plugin.getRewardManager().execute(reward, player, loc.clone());
    }

    // ── Skull profile builder ─────────────────────────────────────────────────

    private PlayerProfile createProfile(String base64Texture) {
        try {
            // Decode the base64 to extract the texture URL
            String json = new String(Base64.getDecoder().decode(base64Texture));
            // Extract URL from JSON: {"textures":{"SKIN":{"url":"<URL>"}}}
            int urlStart = json.indexOf("\"url\":\"") + 7;
            int urlEnd   = json.indexOf("\"", urlStart);
            String url   = json.substring(urlStart, urlEnd);

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "LuckyBlock");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URI(url).toURL());
            profile.setTextures(textures);
            return profile;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create skull profile: " + e.getMessage());
            return null;
        }
    }

    // ── Effects ───────────────────────────────────────────────────────────────

    private void spawnBreakParticles(Location loc, LuckyBlockRarity rarity) {
        try {
            String particleName = rarity.getParticleName();
            Particle p = Particle.valueOf(particleName);
            int count = plugin.getConfig().getInt("effects.particle-count", 60);
            loc.getWorld().spawnParticle(p, loc.clone().add(0.5, 0.5, 0.5), count, 0.5, 0.5, 0.5, 0.1);
        } catch (IllegalArgumentException e) {
            // Fallback to totem particles
            try {
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                        loc.clone().add(0.5, 0.5, 0.5), 40, 0.5, 0.5, 0.5, 0.1);
            } catch (Exception ignored) {}
        }
    }

    private void playBreakSound(Location loc) {
        try {
            Sound s = Sound.valueOf(plugin.getConfig().getString("effects.break-sound", "ENTITY_PLAYER_LEVELUP"));
            float vol   = (float) plugin.getConfig().getDouble("effects.sound-volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("effects.sound-pitch", 1.0);
            loc.getWorld().playSound(loc, s, vol, pitch);
        } catch (IllegalArgumentException ignored) {}
    }

    private void sendLuckHUD(Player player, int baseLuck, int bonus, Reward reward, LuckyBlockRarity rarity) {
        if (!plugin.getConfig().getBoolean("hud.enabled", true)) return;
        LangManager lang = plugin.getLangManager();
        String luckStr = plugin.getLuckManager().formatLuck(baseLuck)
                + (bonus > 0 ? " &8(+" + bonus + " " + rarity.getShortName() + "&8)" : "");
        String tier = lang.get("tier-" + reward.getTier().name().toLowerCase());
        player.sendMessage(lang.get("hud-line-1"));
        player.sendMessage(lang.get("hud-line-2", "luck", luckStr, "tier", tier));
        player.sendMessage(lang.get("hud-line-3"));
        if (reward.getTier() == Reward.Tier.GOOD)
            player.sendMessage(lang.get("reward-good-notify", "reward", reward.getDisplayName()));
    }
}
