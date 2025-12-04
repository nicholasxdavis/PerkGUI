package com.playpandora.perkshop.listeners;

import com.playpandora.perkshop.PerkShop;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

public class RespawnListener implements Listener {
    
    private final PerkShop plugin;
    
    public RespawnListener(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location respawnLocation = null;
        
        // First, check if player has a home using Essentials API
        Plugin essPlugin = plugin.getServer().getPluginManager().getPlugin("Essentials");
        if (essPlugin != null) {
            respawnLocation = getHomeLocation(essPlugin, player);
        }
        
        // If no home, use spawn
        if (respawnLocation == null) {
            respawnLocation = getSpawnLocation(essPlugin, player);
            plugin.getLogger().fine("Respawning " + player.getName() + " at spawn (no home set)");
        } else {
            plugin.getLogger().fine("Respawning " + player.getName() + " at their home");
        }
        
        // Set the respawn location (this overrides bed respawn)
        event.setRespawnLocation(respawnLocation);
    }
    
    /**
     * Gets the player's home location from Essentials
     * @param essPlugin The Essentials plugin instance
     * @param player The player
     * @return Home location, or null if not found
     */
    private Location getHomeLocation(Plugin essPlugin, Player player) {
        try {
            Class<?> essentialsClass = Class.forName("net.ess3.api.IEssentials");
            if (!essentialsClass.isInstance(essPlugin)) {
                return null;
            }
            
            // Get user
            java.lang.reflect.Method getUserMethod = essentialsClass.getMethod("getUser", org.bukkit.entity.Player.class);
            Object user = getUserMethod.invoke(essPlugin, player);
            
            if (user != null) {
                // Check if user has a home named "home" (all players limited to 1 home)
                java.lang.reflect.Method getHomeMethod = user.getClass().getMethod("getHome", String.class);
                Location home = (Location) getHomeMethod.invoke(user, "home");
                
                if (home != null) {
                    return home;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Could not get home via Essentials API: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Gets the spawn location (Essentials warp "spawn" or world spawn)
     * @param essPlugin The Essentials plugin instance
     * @param player The player
     * @return Spawn location
     */
    private Location getSpawnLocation(Plugin essPlugin, Player player) {
        // Try to get spawn from Essentials warp
        if (essPlugin != null) {
            try {
                Class<?> essentialsClass = Class.forName("net.ess3.api.IEssentials");
                if (essentialsClass.isInstance(essPlugin)) {
                    java.lang.reflect.Method getWarpsMethod = essentialsClass.getMethod("getWarps");
                    Object warps = getWarpsMethod.invoke(essPlugin);
                    
                    if (warps != null) {
                        java.lang.reflect.Method getWarpMethod = warps.getClass().getMethod("getWarp", String.class);
                        Location spawnWarp = (Location) getWarpMethod.invoke(warps, "spawn");
                        
                        if (spawnWarp != null) {
                            return spawnWarp;
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().fine("Could not get spawn warp: " + e.getMessage());
            }
        }
        
        // Fall back to world spawn
        return player.getWorld().getSpawnLocation();
    }
}

