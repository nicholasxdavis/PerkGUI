package com.playpandora.perkshop.storage;

import com.playpandora.perkshop.PerkShop;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {
    
    private final PerkShop plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, Set<String>> playerPerksCache;
    
    public DataManager(PerkShop plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.playerPerksCache = new HashMap<>();
        loadData();
    }
    
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create data.yml: " + e.getMessage());
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // Load all player data into cache
        if (dataConfig.contains("players")) {
            Set<String> playerKeys = dataConfig.getConfigurationSection("players").getKeys(false);
            for (String uuidString : playerKeys) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    List<String> perks = dataConfig.getStringList("players." + uuidString + ".perks");
                    playerPerksCache.put(uuid, new HashSet<>(perks));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in data.yml: " + uuidString);
                }
            }
        }
        
        plugin.getLogger().info("Loaded data for " + playerPerksCache.size() + " players");
    }
    
    public void addPerk(UUID uuid, String perkKey) {
        Set<String> perks = playerPerksCache.computeIfAbsent(uuid, k -> new HashSet<>());
        perks.add(perkKey);
        
        List<String> perkList = new ArrayList<>(perks);
        dataConfig.set("players." + uuid.toString() + ".perks", perkList);
        saveData();
    }
    
    public boolean hasPerk(UUID uuid, String perkKey) {
        Set<String> perks = playerPerksCache.get(uuid);
        return perks != null && perks.contains(perkKey);
    }
    
    public Set<String> getPlayerPerks(UUID uuid) {
        return new HashSet<>(playerPerksCache.getOrDefault(uuid, new HashSet<>()));
    }
    
    private void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
        }
    }
    
    public void saveQuestProgress(UUID uuid, String questKey) {
        List<String> completedQuests = dataConfig.getStringList("players." + uuid.toString() + ".quests");
        if (!completedQuests.contains(questKey)) {
            completedQuests.add(questKey);
        }
        dataConfig.set("players." + uuid.toString() + ".quests", completedQuests);
        saveData();
    }
    
    public Map<String, Integer> getQuestProgress(UUID uuid) {
        Map<String, Integer> progress = new HashMap<>();
        List<String> completedQuests = dataConfig.getStringList("players." + uuid.toString() + ".quests");
        for (String quest : completedQuests) {
            progress.put(quest, 1);
        }
        return progress;
    }
    
    public long getLastHealUse(UUID uuid) {
        return dataConfig.getLong("players." + uuid.toString() + ".last_heal_use", 0);
    }
    
    public void setLastHealUse(UUID uuid, long timestamp) {
        dataConfig.set("players." + uuid.toString() + ".last_heal_use", timestamp);
        saveData();
    }
    
    public void close() {
        saveData();
    }
}

