package com.tntbulldozer.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.tntbulldozer.TNTBulldozerPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;

public class BlockBreakListener implements Listener {

    private final TNTBulldozerPlugin plugin;
    private final boolean worldGuardEnabled;
    
    // Блоки, которые нельзя разрушить мгновенно обычной киркой
    private static final Set<Material> PROTECTED_BLOCKS = new HashSet<>();
    
    static {
        // Индустриальные и особые блоки
        PROTECTED_BLOCKS.add(Material.BEDROCK);
        PROTECTED_BLOCKS.add(Material.COMMAND_BLOCK);
        PROTECTED_BLOCKS.add(Material.CHAIN_COMMAND_BLOCK);
        PROTECTED_BLOCKS.add(Material.REPEATING_COMMAND_BLOCK);
        PROTECTED_BLOCKS.add(Material.ENDER_CHEST);
        PROTECTED_BLOCKS.add(Material.BARRIER);
        PROTECTED_BLOCKS.add(Material.END_PORTAL_FRAME);
        PROTECTED_BLOCKS.add(Material.END_PORTAL);
        PROTECTED_BLOCKS.add(Material.END_GATEWAY);
        PROTECTED_BLOCKS.add(Material.STRUCTURE_BLOCK);
        PROTECTED_BLOCKS.add(Material.JIGSAW);
    }

    public BlockBreakListener(TNTBulldozerPlugin plugin) {
        this.plugin = plugin;
        this.worldGuardEnabled = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Проверка наличия зачарования TNT Bulldozer
        Enchantment tntBulldozer = plugin.getTntBulldozerEnchantment();
        if (item == null || !item.containsEnchantment(tntBulldozer)) {
            return;
        }
        
        Block centerBlock = event.getBlock();
        
        // Проверка WorldGuard
        if (worldGuardEnabled && !canBreakInRegion(player, centerBlock.getLocation())) {
            return;
        }
        
        // Получение радиуса из конфига
        int radius = plugin.getConfig().getInt("tnt-bulldozer.radius", 1);
        
        // Визуальные и звуковые эффекты
        playEffects(centerBlock.getLocation(), radius);
        
        // Разрушение блоков в области 3x3x3
        breakBlocksInArea(player, centerBlock, radius, item);
        
        // Уменьшение прочности инструмента на 1
        damageTool(item, player);
    }

    private void breakBlocksInArea(Player player, Block centerBlock, int radius, ItemStack tool) {
        Location center = centerBlock.getLocation();
        World world = center.getWorld();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // Пропускаем центральный блок (он уже ломается)
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    
                    Block block = world.getBlockAt(
                        center.getBlockX() + x,
                        center.getBlockY() + y,
                        center.getBlockZ() + z
                    );
                    
                    // Проверка, можно ли разрушить блок
                    if (canBreakBlock(block) && canBreakInRegion(player, block.getLocation())) {
                        // Дроп предметов с учетом зачарований (Fortune, Silk Touch)
                        block.breakNaturally(tool);
                    }
                }
            }
        }
    }

    private boolean canBreakBlock(Block block) {
        Material material = block.getType();
        
        // Воздух и защищенные блоки
        if (material == Material.AIR || PROTECTED_BLOCKS.contains(material)) {
            return false;
        }
        
        // Проверка прочности блока
        // Блоки с прочностью > 3.0 обычно не ломаются мгновенно каменной киркой
        float hardness = material.getHardness();
        
        // Разрешаем только блоки с низкой прочностью
        // Камень = 1.5, обсидиан = 50, бедрок = -1
        return hardness >= 0 && hardness <= 3.0;
    }

    private boolean canBreakInRegion(Player player, Location location) {
        if (!worldGuardEnabled) {
            return true;
        }
        
        try {
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);
            
            return query.testState(loc, WorldGuardPlugin.inst().wrapPlayer(player), Flags.BLOCK_BREAK);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка проверки WorldGuard: " + e.getMessage());
            return true;
        }
    }

    private void playEffects(Location location, int radius) {
        World world = location.getWorld();
        if (world == null) return;
        
        boolean enableParticles = plugin.getConfig().getBoolean("tnt-bulldozer.enable-particles", true);
        boolean enableSound = plugin.getConfig().getBoolean("tnt-bulldozer.enable-sound", true);
        
        if (enableParticles) {
            // Частицы взрыва в центре
            world.spawnParticle(Particle.EXPLOSION_HUGE, 
                location.clone().add(0.5, 0.5, 0.5), 
                1, 0, 0, 0, 0);
            
            // Дополнительные частицы по области
            for (int i = 0; i < radius * 3; i++) {
                double offsetX = (Math.random() - 0.5) * radius * 2;
                double offsetY = (Math.random() - 0.5) * radius * 2;
                double offsetZ = (Math.random() - 0.5) * radius * 2;
                
                world.spawnParticle(Particle.EXPLOSION_NORMAL,
                    location.clone().add(offsetX, offsetY, offsetZ),
                    1, 0, 0, 0, 0);
            }
        }
        
        if (enableSound) {
            world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        }
    }

    private void damageTool(ItemStack tool, Player player) {
        if (tool.getItemMeta() instanceof Damageable) {
            Damageable meta = (Damageable) tool.getItemMeta();
            meta.setDamage(meta.getDamage() + 1);
            tool.setItemMeta((ItemMeta) meta);
            
            // Проверка на поломку инструмента
            if (meta.getDamage() >= tool.getType().getMaxDurability()) {
                player.getInventory().setItemInMainHand(null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            }
        }
    }
}
