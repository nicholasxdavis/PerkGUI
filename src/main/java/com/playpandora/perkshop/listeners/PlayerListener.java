package com.playpandora.perkshop.listeners;

import com.playpandora.perkshop.PerkShop;
import com.playpandora.perkshop.models.Perk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {
    
    private final PerkShop plugin;
    
    public PlayerListener(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (!title.equals(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("shop.title", "&8Perk Shop")))) {
            return;
        }
        
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null) {
            return;
        }
        
        int slot = event.getSlot();
        
        // Check if close button was clicked
        int inventorySize = event.getInventory().getSize();
        if (slot == inventorySize - 1) {
            player.closeInventory();
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked.getType() == Material.AIR) {
            return;
        }
        
        // Find which perk was clicked using slot mapping
        String perkKey = plugin.getShopGUI().getPerkBySlot(player, slot);
        if (perkKey == null) {
            return;
        }
        
        // Attempt purchase
        boolean success = plugin.getPurchaseManager().purchasePerk(player, perkKey);
        
        // Update the specific item in the inventory without closing it
        if (success) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                // Update the item in place to reflect the purchase
                plugin.getShopGUI().updatePerkItem(player, perkKey, slot);
            }, 1L);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        
        if (title.equals(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("shop.title", "&8Perk Shop")))) {
            // Clear slot mapping when shop closes
            plugin.getShopGUI().clearPlayerMapping(player);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Check for mining speed perks
        double multiplier = getMiningSpeedMultiplier(uuid);
        if (multiplier > 1.0) {
            // Apply speed boost by reducing break time
            // Note: This is a simplified implementation
            // For a more accurate implementation, you'd need to use NMS or a plugin like ProtocolLib
            Block block = event.getBlock();
            Material type = block.getType();
            
            // Only apply to blocks that can be mined
            if (type.getHardness() > 0 && !type.isAir()) {
                // The multiplier effect is conceptual - actual implementation would require
                // modifying block break speed, which is complex in Bukkit API
                // This is a placeholder for the concept
            }
        }
        
        // Check for double drops
        if (plugin.getPurchaseManager().hasPerk(uuid, "double_drops")) {
            // Drop additional items
            Block block = event.getBlock();
            ItemStack drop = new ItemStack(block.getType());
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Load quest progress first
        Map<String, Integer> questProgress = plugin.getDataManager().getQuestProgress(uuid);
        plugin.getQuestManager().loadPlayerProgress(uuid, questProgress);
        
        // Re-grant permissions for owned perks (delay to ensure permissions are loaded)
        // Use multiple delays to ensure permissions are granted even if first attempt fails
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            grantOwnedPerkPermissions(player, uuid);
        }, 40L); // 2 second delay to ensure permissions are loaded
        
        // Also try again after a longer delay as backup
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                grantOwnedPerkPermissions(player, uuid);
            }
        }, 100L); // 5 second delay as backup
        
        // Apply permanent effects
        applyPermanentEffects(player, uuid);
    }
    
    private void grantOwnedPerkPermissions(Player player, UUID uuid) {
        // Get all owned perks
        var ownedPerks = plugin.getDataManager().getPlayerPerks(uuid);
        
        if (ownedPerks.isEmpty()) {
            return;
        }
        
        plugin.getLogger().info("Re-granting permissions for " + player.getName() + " - " + ownedPerks.size() + " perks");
        
        int granted = 0;
        for (String perkKey : ownedPerks) {
            var perk = plugin.getPerkManager().getPerk(perkKey);
            if (perk != null && perk.getPermission() != null && !perk.getPermission().isEmpty()) {
                // Re-grant the permission
                plugin.getPurchaseManager().grantPermission(player, perk.getPermission());
                granted++;
            }
        }
        
        plugin.getLogger().info("Re-granted " + granted + " permissions for " + player.getName());
        
        // Recalculate permissions after granting all
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.recalculatePermissions();
                plugin.getLogger().info("Recalculated permissions for " + player.getName());
            }
        }, 10L);
    }
    
    private void applyPermanentEffects(Player player, UUID uuid) {
        // Speed boost
        if (plugin.getPurchaseManager().hasPerk(uuid, "speed_boost")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
        }
        
        // Jump boost
        if (plugin.getPurchaseManager().hasPerk(uuid, "jump_boost")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 0, true, false));
        }
        
        // Note: Night perk is for /night command, not night vision effect
        // If you want a night vision perk, create a separate "night_vision" perk
        
        // Water breathing
        if (plugin.getPurchaseManager().hasPerk(uuid, "water_breathing")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, true, false));
        }
        
        // Extra hearts (health boost)
        if (plugin.getPurchaseManager().hasPerk(uuid, "extra_hearts")) {
            // Apply health boost effect (level 0 = +4 hearts, but we want +6, so we'll use attribute)
            // Actually, health boost gives +4 per level, so we need level 1 for +4, but we want +6
            // Let's use level 1 which gives +4 hearts, and we can adjust
            // Actually, Health Boost level 0 = +4 HP, level 1 = +8 HP
            // We want +6 HP (3 hearts), so we'll use a custom approach
            applyExtraHearts(player);
        }
    }
    
    private void applyExtraHearts(Player player) {
        // Use attribute modifier to add 6 health (3 hearts)
        org.bukkit.attribute.AttributeInstance healthAttribute = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (healthAttribute != null) {
            // Remove existing modifier if any
            healthAttribute.getModifiers().stream()
                .filter(mod -> mod.getName().equals("PerkShop_ExtraHearts"))
                .forEach(healthAttribute::removeModifier);
            
            // Add new modifier for +6 health
            org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                "PerkShop_ExtraHearts",
                6.0,
                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
            );
            healthAttribute.addModifier(modifier);
            
            // Ensure player's current health doesn't exceed new max
            if (player.getHealth() > healthAttribute.getValue()) {
                player.setHealth(healthAttribute.getValue());
            }
        }
    }
    
    
    private double getMiningSpeedMultiplier(UUID uuid) {
        // Check for highest level mining speed perk
        if (plugin.getPurchaseManager().hasPerk(uuid, "mining_speed_3")) {
            Perk perk = plugin.getPerkManager().getPerk("mining_speed_3");
            return perk != null && perk.getMultiplier() != null ? perk.getMultiplier() : 1.0;
        } else if (plugin.getPurchaseManager().hasPerk(uuid, "mining_speed_2")) {
            Perk perk = plugin.getPerkManager().getPerk("mining_speed_2");
            return perk != null && perk.getMultiplier() != null ? perk.getMultiplier() : 1.0;
        } else if (plugin.getPurchaseManager().hasPerk(uuid, "mining_speed_1")) {
            Perk perk = plugin.getPerkManager().getPerk("mining_speed_1");
            return perk != null && perk.getMultiplier() != null ? perk.getMultiplier() : 1.0;
        }
        
        return 1.0;
    }
}

