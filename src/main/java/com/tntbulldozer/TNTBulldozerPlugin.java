package com.tntbulldozer;

import com.tntbulldozer.enchantment.TNTBulldozerEnchantment;
import com.tntbulldozer.listener.BlockBreakListener;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public class TNTBulldozerPlugin extends JavaPlugin {

    private static TNTBulldozerPlugin instance;
    private TNTBulldozerEnchantment tntBulldozerEnchantment;

    @Override
    public void onEnable() {
        instance = this;
        
        // Сохранить конфиг по умолчанию
        saveDefaultConfig();
        
        // Регистрация зачарования
        tntBulldozerEnchantment = new TNTBulldozerEnchantment();
        registerEnchantment(tntBulldozerEnchantment);
        
        // Регистрация слушателей
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        
        getLogger().info("TNT Bulldozer плагин успешно загружен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TNT Bulldozer плагин выгружен!");
    }

    public static TNTBulldozerPlugin getInstance() {
        return instance;
    }

    public TNTBulldozerEnchantment getTntBulldozerEnchantment() {
        return tntBulldozerEnchantment;
    }

    private void registerEnchantment(Enchantment enchantment) {
        try {
            Field acceptingNewField = Enchantment.class.getDeclaredField("acceptingNew");
            acceptingNewField.setAccessible(true);
            acceptingNewField.set(null, true);
            
            Enchantment.registerEnchantment(enchantment);
            
            acceptingNewField.set(null, false);
        } catch (Exception e) {
            getLogger().warning("Не удалось зарегистрировать зачарование: " + e.getMessage());
        }
    }
}
