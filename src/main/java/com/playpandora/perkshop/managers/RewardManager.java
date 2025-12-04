package com.playpandora.perkshop.managers;

import com.playpandora.perkshop.PerkShop;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

public class RewardManager {
    
    private final PerkShop plugin;
    
    public RewardManager(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    public void giveReward(Player player, String rewardType, String key) {
        ConfigurationSection rewardsSection = plugin.getConfig().getConfigurationSection("rewards." + rewardType);
        if (rewardsSection == null) {
            return;
        }
        
        ConfigurationSection rewardConfig = rewardsSection.getConfigurationSection(key);
        if (rewardConfig == null) {
            // Try direct value
            double amount = rewardsSection.getDouble(key, 0.0);
            if (amount > 0) {
                giveMoney(player, amount, rewardType);
            }
            return;
        }
        
        if (!rewardConfig.getBoolean("enabled", true)) {
            return;
        }
        
        double amount = rewardConfig.getDouble("amount", 0.0);
        if (amount > 0) {
            giveMoney(player, amount, rewardType);
        }
    }
    
    public double getReward(String rewardType, String key) {
        ConfigurationSection rewardsSection = plugin.getConfig().getConfigurationSection("rewards." + rewardType);
        if (rewardsSection == null) {
            return 0.0;
        }
        
        ConfigurationSection rewardConfig = rewardsSection.getConfigurationSection(key);
        if (rewardConfig != null) {
            return rewardConfig.getDouble("amount", 0.0);
        }
        
        return rewardsSection.getDouble(key, 0.0);
    }
    
    private void giveMoney(Player player, double amount, String source) {
        if (amount <= 0) {
            return;
        }
        
        plugin.getEconomy().depositPlayer(player, amount);
        
        // Send message if enabled
        if (plugin.getConfig().getBoolean("rewards.show-messages", true)) {
            String message = plugin.getConfig().getString("rewards.message-format", 
                "&ePandora &7+&6%amount% &7(%source%)");
            message = message.replace("%amount%", plugin.formatMoney(amount))
                           .replace("%source%", source);
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        }
    }
    
    public boolean isRewardEnabled(String rewardType, String key) {
        ConfigurationSection rewardsSection = plugin.getConfig().getConfigurationSection("rewards." + rewardType);
        if (rewardsSection == null) {
            return false;
        }
        
        ConfigurationSection rewardConfig = rewardsSection.getConfigurationSection(key);
        if (rewardConfig != null) {
            return rewardConfig.getBoolean("enabled", true);
        }
        
        return rewardsSection.getDouble(key, 0.0) > 0;
    }
}


