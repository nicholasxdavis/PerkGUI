package com.playpandora.perkshop.listeners;

import com.playpandora.perkshop.PerkShop;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RewardListener implements Listener {
    
    private final PerkShop plugin;
    private final Map<UUID, Long> lastRewardTime = new HashMap<>();
    private static final long COOLDOWN_MS = 100; // 100ms cooldown to prevent spam
    
    // Important blocks that give rewards when placed
    private static final Material[] IMPORTANT_BLOCKS = {
        Material.BEACON,
        Material.ENCHANTING_TABLE,
        Material.ANVIL,
        Material.CRAFTING_TABLE,
        Material.FURNACE,
        Material.BLAST_FURNACE,
        Material.SMOKER,
        Material.CAMPFIRE,
        Material.SOUL_CAMPFIRE
    };
    
    public RewardListener(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        EntityType entityType = event.getEntityType();
        
        // Check cooldown
        if (!checkCooldown(killer.getUniqueId())) {
            return;
        }
        
        String entityName = entityType.name().toLowerCase();
        
        // Check if this mob type has a reward
        if (plugin.getRewardManager().isRewardEnabled("mobs", entityName)) {
            plugin.getRewardManager().giveReward(killer, "mobs", entityName);
        } else {
            // Use default mob reward if configured
            double defaultReward = plugin.getRewardManager().getReward("mobs", "default");
            if (defaultReward > 0) {
                plugin.getRewardManager().giveReward(killer, "mobs", "default");
            }
        }
        
        // Check for first kill quest
        if (!plugin.getQuestManager().hasCompletedQuest(killer.getUniqueId(), "first_kill")) {
            plugin.getQuestManager().completeQuest(killer, "first_kill");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check cooldown
        if (!checkCooldown(player.getUniqueId())) {
            return;
        }
        
        if (event.getCaught() == null) {
            return;
        }
        
        // Get the caught entity/item
        org.bukkit.entity.Entity caughtEntity = event.getCaught();
        Material fishType = Material.COD; // Default
        
        // Try to get item from entity if it's an item
        if (caughtEntity instanceof org.bukkit.entity.Item) {
            org.bukkit.entity.Item itemEntity = (org.bukkit.entity.Item) caughtEntity;
            ItemStack itemStack = itemEntity.getItemStack();
            if (itemStack != null) {
                fishType = itemStack.getType();
            }
        }
        String fishName = fishType.name().toLowerCase();
        
        // Check if this fish type has a reward
        if (plugin.getRewardManager().isRewardEnabled("fishing", fishName)) {
            plugin.getRewardManager().giveReward(player, "fishing", fishName);
        } else {
            // Use default fishing reward
            double defaultReward = plugin.getRewardManager().getReward("fishing", "default");
            if (defaultReward > 0) {
                plugin.getRewardManager().giveReward(player, "fishing", "default");
            }
        }
        
        // Check for first fish quest
        if (!plugin.getQuestManager().hasCompletedQuest(player.getUniqueId(), "first_fish")) {
            plugin.getQuestManager().completeQuest(player, "first_fish");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();
        
        // Check cooldown
        if (!checkCooldown(player.getUniqueId())) {
            return;
        }
        
        // Check if it's an ore
        if (isOre(type)) {
            String oreName = type.name().toLowerCase();
            
            // Check if this ore has a reward
            if (plugin.getRewardManager().isRewardEnabled("ores", oreName)) {
                plugin.getRewardManager().giveReward(player, "ores", oreName);
            } else {
                // Use default ore reward
                double defaultReward = plugin.getRewardManager().getReward("ores", "default");
                if (defaultReward > 0) {
                    plugin.getRewardManager().giveReward(player, "ores", "default");
                }
            }
            
            // Check for first diamond quest
            if (type == org.bukkit.Material.DIAMOND_ORE || type == org.bukkit.Material.DEEPSLATE_DIAMOND_ORE) {
                if (!plugin.getQuestManager().hasCompletedQuest(player.getUniqueId(), "first_diamond")) {
                    plugin.getQuestManager().completeQuest(player, "first_diamond");
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();
        
        // Check cooldown
        if (!checkCooldown(player.getUniqueId())) {
            return;
        }
        
        // Check if it's an important block
        for (Material importantBlock : IMPORTANT_BLOCKS) {
            if (type == importantBlock) {
                String blockName = type.name().toLowerCase();
                
                // Check if this block has a reward
                if (plugin.getRewardManager().isRewardEnabled("placing", blockName)) {
                    plugin.getRewardManager().giveReward(player, "placing", blockName);
                } else {
                    // Use default placing reward
                    double defaultReward = plugin.getRewardManager().getReward("placing", "default");
                    if (defaultReward > 0) {
                        plugin.getRewardManager().giveReward(player, "placing", "default");
                    }
                }
                
                // Check for beacon quest
                if (type == Material.BEACON) {
                    if (!plugin.getQuestManager().hasCompletedQuest(player.getUniqueId(), "build_beacon")) {
                        plugin.getQuestManager().completeQuest(player, "build_beacon");
                    }
                }
                break;
            }
        }
    }
    
    private boolean checkCooldown(UUID uuid) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastRewardTime.get(uuid);
        
        if (lastTime != null && (currentTime - lastTime) < COOLDOWN_MS) {
            return false;
        }
        
        lastRewardTime.put(uuid, currentTime);
        return true;
    }
    
    private boolean isOre(Material type) {
        return type.name().contains("_ORE") || 
               type == Material.DEEPSLATE_COAL_ORE ||
               type == Material.DEEPSLATE_COPPER_ORE ||
               type == Material.DEEPSLATE_DIAMOND_ORE ||
               type == Material.DEEPSLATE_EMERALD_ORE ||
               type == Material.DEEPSLATE_GOLD_ORE ||
               type == Material.DEEPSLATE_IRON_ORE ||
               type == Material.DEEPSLATE_LAPIS_ORE ||
               type == Material.DEEPSLATE_REDSTONE_ORE ||
               type == Material.NETHER_GOLD_ORE ||
               type == Material.NETHER_QUARTZ_ORE ||
               type == Material.ANCIENT_DEBRIS;
    }
}

