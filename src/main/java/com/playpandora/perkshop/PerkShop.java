package com.playpandora.perkshop;

import com.playpandora.perkshop.commands.ShopCommand;
import com.playpandora.perkshop.gui.ShopGUI;
import com.playpandora.perkshop.listeners.PlayerListener;
import com.playpandora.perkshop.listeners.RespawnListener;
import com.playpandora.perkshop.listeners.RewardListener;
import com.playpandora.perkshop.managers.PerkManager;
import com.playpandora.perkshop.managers.PurchaseManager;
import com.playpandora.perkshop.managers.RewardManager;
import com.playpandora.perkshop.managers.QuestManager;
import com.playpandora.perkshop.storage.DataManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.lang.reflect.Method;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class PerkShop extends JavaPlugin {
    
    private static PerkShop instance;
    private Economy economy;
    private PerkManager perkManager;
    private PurchaseManager purchaseManager;
    private RewardManager rewardManager;
    private QuestManager questManager;
    private DataManager dataManager;
    private ShopGUI shopGUI;
    private Object levelAPI;
    private Method getLevelMethod;
    private boolean levelIntegrationAvailable;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Setup Vault economy
            if (!setupEconomy()) {
                getLogger().severe("Vault or an economy plugin is not installed! Disabling plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Setup LevelPlugin integration
            setupLevelIntegration();
            
            // Initialize managers
            dataManager = new DataManager(this);
            perkManager = new PerkManager(this);
            purchaseManager = new PurchaseManager(this);
            rewardManager = new RewardManager(this);
            questManager = new QuestManager(this);
            shopGUI = new ShopGUI(this);
            
            // DataManager loads data in constructor if needed
            
            // Register commands with null checks
            registerCommands();
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            getServer().getPluginManager().registerEvents(new RewardListener(this), this);
            getServer().getPluginManager().registerEvents(new RespawnListener(this), this);
            
            // Load quest progress for online players
            if (dataManager != null && questManager != null) {
                for (Player player : getServer().getOnlinePlayers()) {
                    try {
                        Map<String, Integer> questProgress = dataManager.getQuestProgress(player.getUniqueId());
                        questManager.loadPlayerProgress(player.getUniqueId(), questProgress);
                    } catch (Exception e) {
                        getLogger().warning("Error loading quest progress for " + player.getName() + ": " + e.getMessage());
                    }
                }
            }
            
            getLogger().info("PerkShop v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable PerkShop! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    private void registerCommands() {
        if (getCommand("shop") != null) {
            getCommand("shop").setExecutor(new ShopCommand(this));
        } else {
            getLogger().warning("Command 'shop' not found in plugin.yml!");
        }
        if (getCommand("perks") != null) {
            getCommand("perks").setExecutor(new com.playpandora.perkshop.commands.PerksCommand(this));
        }
        if (getCommand("heal") != null) {
            getCommand("heal").setExecutor(new com.playpandora.perkshop.commands.HealCommand(this));
        }
        if (getCommand("feed") != null) {
            getCommand("feed").setExecutor(new com.playpandora.perkshop.commands.FeedCommand(this));
        }
        if (getCommand("nv") != null) {
            getCommand("nv").setExecutor(new com.playpandora.perkshop.commands.NightCommand(this));
        }
        if (getCommand("repair") != null) {
            getCommand("repair").setExecutor(new com.playpandora.perkshop.commands.RepairCommand(this));
        }
    }
    
    @Override
    public void onDisable() {
        try {
            if (dataManager != null) {
                dataManager.close();
            }
            
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("PerkShop has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
    
    private void setupLevelIntegration() {
        if (getServer().getPluginManager().getPlugin("LevelPlugin") == null) {
            getLogger().info("LevelPlugin not found. Level requirements will be disabled.");
            levelIntegrationAvailable = false;
            return;
        }
        
        try {
            Class<?> levelPluginClass = Class.forName("com.playpandora.levelplugin.LevelPlugin");
            Method getInstanceMethod = levelPluginClass.getMethod("getInstance");
            Object levelPlugin = getInstanceMethod.invoke(null);
            
            Method getAPIMethod = levelPluginClass.getMethod("getAPI");
            levelAPI = getAPIMethod.invoke(levelPlugin);
            
            getLevelMethod = levelAPI.getClass().getMethod("getLevel", org.bukkit.entity.Player.class);
            
            levelIntegrationAvailable = true;
            getLogger().info("LevelPlugin integration enabled!");
        } catch (Exception e) {
            getLogger().warning("Failed to integrate with LevelPlugin: " + e.getMessage());
            levelIntegrationAvailable = false;
        }
    }
    
    public int getPlayerLevel(Player player) {
        if (!levelIntegrationAvailable || levelAPI == null || getLevelMethod == null) {
            return 0;
        }
        try {
            return (Integer) getLevelMethod.invoke(levelAPI, player);
        } catch (Exception e) {
            getLogger().warning("Error getting player level: " + e.getMessage());
            return 0;
        }
    }
    
    public boolean isLevelIntegrationAvailable() {
        return levelIntegrationAvailable;
    }
    
    public static PerkShop getInstance() {
        return instance;
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    public double getPlayerBalance(org.bukkit.entity.Player player) {
        if (economy == null) {
            return 0.0;
        }
        return economy.getBalance(player);
    }
    
    public boolean withdrawPlayer(org.bukkit.entity.Player player, double amount) {
        if (economy == null) {
            getLogger().warning("Economy is null! Cannot withdraw money for " + player.getName());
            return false;
        }
        
        try {
            net.milkbowl.vault.economy.EconomyResponse response = economy.withdrawPlayer(player, amount);
            if (response != null) {
                if (!response.transactionSuccess()) {
                    getLogger().warning("Failed to withdraw money for " + player.getName() + ": " + response.errorMessage);
                    return false;
                }
                return true;
            }
            return true;
        } catch (Exception e) {
            getLogger().severe("Failed to withdraw money for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public PerkManager getPerkManager() {
        return perkManager;
    }
    
    public PurchaseManager getPurchaseManager() {
        return purchaseManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public ShopGUI getShopGUI() {
        return shopGUI;
    }
    
    public RewardManager getRewardManager() {
        return rewardManager;
    }
    
    public QuestManager getQuestManager() {
        return questManager;
    }
    
    /**
     * Formats money using the economy plugin's format method
     * @param amount The amount to format
     * @return Formatted money string (e.g., "$100.00")
     */
    public String formatMoney(double amount) {
        if (economy != null) {
            return economy.format(amount);
        }
        return String.format("$%.2f", amount);
    }
    
    /**
     * Formats a message from config with placeholder replacement
     * @param path Config path to the message
     * @param defaultMessage Default message if not found in config
     * @return Formatted message with color codes translated
     */
    public String formatMessage(String path, String defaultMessage) {
        String message = getConfig().getString(path, defaultMessage);
        String prefix = getConfig().getString("messages.prefix", "&ePandora");
        message = message.replace("{prefix}", prefix);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}

