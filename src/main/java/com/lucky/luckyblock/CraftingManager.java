package com.lucky.luckyblock;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Registers the Lucky Block crafting recipe from config.yml under the
 * crafting.shape + crafting.ingredients keys.
 */
public class CraftingManager {

    private final LuckyBlockPlugin plugin;
    private NamespacedKey recipeKey;

    public CraftingManager(LuckyBlockPlugin plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "lucky_block_craft");
    }

    public void register() {
        // Remove old recipe if reloading
        plugin.getServer().removeRecipe(recipeKey);

        if (!plugin.getConfig().getBoolean("crafting.enabled", true)) return;

        List<String> shape = plugin.getConfig().getStringList("crafting.shape");
        if (shape.size() != 3) {
            plugin.getLogger().warning("crafting.shape must have exactly 3 rows!");
            return;
        }

        ItemStack result = buildLuckyBlockItem(plugin.getConfig().getInt("crafting.result-amount", 1));
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(shape.get(0), shape.get(1), shape.get(2));

        var ingredientSection = plugin.getConfig().getConfigurationSection("crafting.ingredients");
        if (ingredientSection != null) {
            for (String key : ingredientSection.getKeys(false)) {
                String matName = ingredientSection.getString(key, "AIR");
                Material mat = Material.matchMaterial(matName);
                if (mat == null || mat == Material.AIR) {
                    plugin.getLogger().warning("Unknown crafting ingredient material: " + matName);
                    continue;
                }
                recipe.setIngredient(key.charAt(0), mat);
            }
        }

        plugin.getServer().addRecipe(recipe);
        plugin.getLogger().info("Lucky Block crafting recipe registered.");
    }

    private ItemStack buildLuckyBlockItem(int amount) {
        Material mat = Material.matchMaterial(
                plugin.getConfig().getString("lucky-block-material", "GOLD_BLOCK"));
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
}
