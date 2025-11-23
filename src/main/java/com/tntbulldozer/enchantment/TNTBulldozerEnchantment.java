package com.tntbulldozer.enchantment;

import com.tntbulldozer.TNTBulldozerPlugin;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;

public class TNTBulldozerEnchantment extends Enchantment {

    private static final String KEY = "tnt_bulldozer";

    public TNTBulldozerEnchantment() {
        super(NamespacedKey.minecraft(KEY));
    }

    @Override
    public String getName() {
        String displayName = TNTBulldozerPlugin.getInstance().getConfig().getString("tnt-bulldozer.display-name", "&6TNT Бульдозер");
        return ChatColor.translateAlternateColorCodes('&', displayName);
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getStartLevel() {
        return 1;
    }

    @Override
    public EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.TOOL;
    }

    @Override
    public boolean isTreasure() {
        return true;
    }

    @Override
    public boolean isCursed() {
        return false;
    }

    @Override
    public boolean conflictsWith(Enchantment other) {
        // Совместимо со всеми зачарованиями
        return false;
    }

    @Override
    public boolean canEnchantItem(ItemStack item) {
        // Только кирки
        return item.getType().name().endsWith("_PICKAXE");
    }
}
