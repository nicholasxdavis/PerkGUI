package com.playpandora.perkshop.managers;

import com.playpandora.perkshop.PerkShop;
import com.playpandora.perkshop.models.Perk;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerkManager {
    
    private final PerkShop plugin;
    private final Map<String, Perk> perks;
    
    public PerkManager(PerkShop plugin) {
        this.plugin = plugin;
        this.perks = new HashMap<>();
        loadPerks();
    }
    
    private void loadPerks() {
        ConfigurationSection perksSection = plugin.getConfig().getConfigurationSection("perks");
        if (perksSection == null) {
            plugin.getLogger().warning("No perks section found in config!");
            return;
        }
        
        for (String key : perksSection.getKeys(false)) {
            ConfigurationSection perkConfig = perksSection.getConfigurationSection(key);
            if (perkConfig == null) {
                continue;
            }
            
            if (!perkConfig.getBoolean("enabled", true)) {
                continue;
            }
            
            String name = perkConfig.getString("name", key);
            List<String> description = perkConfig.getStringList("description");
            // Support both "cost" and "price" for backwards compatibility
            double price = perkConfig.contains("cost") ? 
                perkConfig.getDouble("cost", 0.0) : 
                perkConfig.getDouble("price", 0.0);
            String material = perkConfig.getString("material", "BARRIER");
            String permission = perkConfig.getString("permission", "");
            String requires = perkConfig.getString("requires", null);
            Double multiplier = perkConfig.contains("multiplier") ? 
                perkConfig.getDouble("multiplier") : null;
            int requiredLevel = perkConfig.getInt("required-level", 0);
            
            Perk perk = new Perk(key, name, description, price, material, permission, requires, multiplier, requiredLevel);
            perks.put(key, perk);
        }
        
        plugin.getLogger().info("Loaded " + perks.size() + " perks!");
    }
    
    public Perk getPerk(String key) {
        return perks.get(key);
    }
    
    public Map<String, Perk> getAllPerks() {
        return new HashMap<>(perks);
    }
    
    public void reload() {
        perks.clear();
        plugin.reloadConfig();
        loadPerks();
    }
}

