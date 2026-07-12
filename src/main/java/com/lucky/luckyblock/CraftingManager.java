package com.lucky.luckyblock;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;

public class CraftingManager {

    private final LuckyBlockPlugin plugin;
    private final NamespacedKey recipeKey;

    public CraftingManager(LuckyBlockPlugin plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "lucky_block_craft");
    }

    public void register() {
        plugin.getServer().removeRecipe(recipeKey);
        if (!plugin.getConfig().getBoolean("crafting.enabled", true)) return;

        List<String> shape = plugin.getConfig().getStringList("crafting.shape");
        if (shape.size() != 3) {
            plugin.getLogger().warning("crafting.shape must have exactly 3 rows!");
            return;
        }

        // Result is always a COMMON rarity lucky block (skull item)
        LuckyBlockCommand cmd = new LuckyBlockCommand(plugin);
        int amount = plugin.getConfig().getInt("crafting.result-amount", 1);
        var result = cmd.buildLuckyBlockItem(amount, LuckyBlockRarity.COMMON);

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(shape.get(0), shape.get(1), shape.get(2));

        var ingredientSection = plugin.getConfig().getConfigurationSection("crafting.ingredients");
        if (ingredientSection != null) {
            for (String key : ingredientSection.getKeys(false)) {
                String matName = ingredientSection.getString(key, "AIR");
                Material mat = Material.matchMaterial(matName);
                if (mat == null || mat == Material.AIR) {
                    plugin.getLogger().warning("Unknown crafting ingredient: " + matName);
                    continue;
                }
                recipe.setIngredient(key.charAt(0), mat);
            }
        }

        plugin.getServer().addRecipe(recipe);
        plugin.getLogger().info("Lucky Block crafting recipe registered (COMMON rarity).");
    }
}
