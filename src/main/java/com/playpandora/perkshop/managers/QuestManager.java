package com.playpandora.perkshop.managers;

import com.playpandora.perkshop.PerkShop;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestManager {
    
    private final PerkShop plugin;
    private final Map<UUID, Map<String, Integer>> playerProgress = new HashMap<>();
    
    public QuestManager(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    public void completeQuest(Player player, String questKey) {
        ConfigurationSection questsSection = plugin.getConfig().getConfigurationSection("quests");
        if (questsSection == null) {
            return;
        }
        
        ConfigurationSection questConfig = questsSection.getConfigurationSection(questKey);
        if (questConfig == null || !questConfig.getBoolean("enabled", true)) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        
        // Check if already completed
        if (hasCompletedQuest(uuid, questKey)) {
            return;
        }
        
        // Mark as completed
        Map<String, Integer> progress = playerProgress.computeIfAbsent(uuid, k -> new HashMap<>());
        progress.put(questKey, 1);
        
        // Give reward
        double reward = questConfig.getDouble("reward", 0.0);
        if (reward > 0) {
            plugin.getEconomy().depositPlayer(player, reward);
            
            // Send Hook notification
            sendQuestCompleteNotification(player, questConfig.getString("name", questKey), reward);
            
            String message = plugin.getConfig().getString("quests.completion-message", 
                "&ePandora &7Quest completed! You received &6%reward%");
            message = message.replace("%reward%", plugin.formatMoney(reward))
                           .replace("%quest%", questConfig.getString("name", questKey));
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        }
        
        // Save progress
        plugin.getDataManager().saveQuestProgress(uuid, questKey);
    }
    
    public boolean hasCompletedQuest(UUID uuid, String questKey) {
        Map<String, Integer> progress = playerProgress.get(uuid);
        return progress != null && progress.containsKey(questKey) && progress.get(questKey) > 0;
    }
    
    public void loadPlayerProgress(UUID uuid, Map<String, Integer> progress) {
        playerProgress.put(uuid, progress);
    }
    
    private void sendQuestCompleteNotification(Player player, String questName, double reward) {
        try {
            org.bukkit.plugin.Plugin hookPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("Hook");
            if (hookPlugin != null && hookPlugin.isEnabled()) {
                Object hookInstance = hookPlugin.getClass().getMethod("getInstance").invoke(null);
                Object hookAPI = hookInstance.getClass().getMethod("getAPI").invoke(hookInstance);
                String formattedReward = String.format("%.2f", reward);
                hookAPI.getClass().getMethod("sendQuestComplete", org.bukkit.entity.Player.class, 
                    String.class, double.class, double.class)
                    .invoke(hookAPI, player, questName, reward, 0.0);
            }
        } catch (Exception e) {
            // Silently fail if Hook is not available
        }
    }
}


