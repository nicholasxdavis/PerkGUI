package com.playpandora.perkshop.managers;

import com.playpandora.perkshop.PerkShop;
import com.playpandora.perkshop.models.Perk;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PurchaseManager {
    
    private final PerkShop plugin;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    
    public PurchaseManager(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    public boolean purchasePerk(Player player, String perkKey) {
        Perk perk = plugin.getPerkManager().getPerk(perkKey);
        if (perk == null) {
            player.sendMessage(plugin.formatMessage("messages.perk-not-found", 
                "{prefix} &7Perk not found!"));
            return false;
        }
        
        UUID uuid = player.getUniqueId();
        
        // Check if already owns perk
        if (hasPerk(uuid, perkKey)) {
            player.sendMessage(plugin.formatMessage("messages.already-purchased", 
                "{prefix} &7You already own this perk!"));
            return false;
        }
        
        // Check if has required perk
        if (perk.getRequires() != null && !perk.getRequires().isEmpty()) {
            if (!hasPerk(uuid, perk.getRequires())) {
                Perk requiredPerk = plugin.getPerkManager().getPerk(perk.getRequires());
                String requiredName = requiredPerk != null ? requiredPerk.getName().replace("&l", "").replace("&L", "") : perk.getRequires();
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "&ePandora") + 
                    " &7You need to purchase &6" + requiredName + " &7first!"));
                return false;
            }
        }
        
        // Check level requirement
        if (perk.getRequiredLevel() > 0) {
            if (!plugin.isLevelIntegrationAvailable()) {
                player.sendMessage(plugin.formatMessage("messages.level-plugin-unavailable",
                    "{prefix} &cLevel plugin is not available. Cannot check level requirements."));
                return false;
            }
            int playerLevel = plugin.getPlayerLevel(player);
            if (playerLevel < perk.getRequiredLevel()) {
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "&ePandora") + 
                    " &7You need to be level &6" + perk.getRequiredLevel() + 
                    " &7to purchase this perk! You are level &6" + playerLevel + "&7."));
                return false;
            }
        }
        
        // Check balance
        double balance = plugin.getPlayerBalance(player);
        if (balance < perk.getPrice()) {
            double needed = perk.getPrice() - balance;
            player.sendMessage(plugin.formatMessage("messages.insufficient-funds", 
                "{prefix} &7You don't have enough money! You need &6{cost}&7.")
                .replace("{cost}", plugin.formatMoney(needed)));
            return false;
        }
        
        // Withdraw money
        boolean withdrawn = plugin.withdrawPlayer(player, perk.getPrice());
        if (!withdrawn) {
            player.sendMessage(plugin.formatMessage("messages.purchase-failed",
                "{prefix} &7Failed to process payment! Please try again."));
            return false;
        }
        
        // Save to database FIRST to ensure it's persisted
        plugin.getDataManager().addPerk(uuid, perkKey);
        
        // Grant perk (permissions and effects)
        grantPerk(player, perk);
        
        // Send success message
        String perkName = perk.getName().replace("&l", "").replace("&L", "");
        player.sendMessage(plugin.formatMessage("messages.purchased", 
            "{prefix} &7You purchased &6{perk} &7for &6{cost}&7!")
            .replace("{perk}", perkName)
            .replace("{cost}", plugin.formatMoney(perk.getPrice())));
        
        return true;
    }
    
    public boolean hasPerk(UUID uuid, String perkKey) {
        return plugin.getDataManager().hasPerk(uuid, perkKey);
    }
    
    private void grantPerk(Player player, Perk perk) {
        // Grant permission if specified
        if (perk.getPermission() != null && !perk.getPermission().isEmpty()) {
            grantPermission(player, perk.getPermission());
        }
        
        // Apply perk-specific effects
        applyPerkEffects(player, perk);
    }
    
    public void grantPermission(Player player, String permission) {
        // Try LuckPerms first using reflection (to avoid NoClassDefFoundError if not installed)
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                // Use reflection to access LuckPerms API
                Class<?> luckPermsProviderClass = Class.forName("net.luckperms.api.LuckPermsProvider");
                Method getMethod = luckPermsProviderClass.getMethod("get");
                Object api = getMethod.invoke(null);
                
                Method getUserManagerMethod = api.getClass().getMethod("getUserManager");
                Object userManager = getUserManagerMethod.invoke(api);
                
                Method getUserMethod = userManager.getClass().getMethod("getUser", java.util.UUID.class);
                Object user = getUserMethod.invoke(userManager, player.getUniqueId());
                
                if (user != null) {
                    // Create permission node using Node.builder()
                    Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
                    Method builderMethod = nodeClass.getMethod("builder");
                    Object nodeBuilder = builderMethod.invoke(null);
                    
                    // Set permission
                    Method permissionMethod = nodeBuilder.getClass().getMethod("permission", String.class);
                    nodeBuilder = permissionMethod.invoke(nodeBuilder, permission);
                    
                    // Set value to true
                    Method valueMethod = nodeBuilder.getClass().getMethod("value", boolean.class);
                    nodeBuilder = valueMethod.invoke(nodeBuilder, true);
                    
                    // Build the node
                    Method buildMethod = nodeBuilder.getClass().getMethod("build");
                    Object node = buildMethod.invoke(nodeBuilder);
                    
                    // Get user data
                    Method dataMethod = user.getClass().getMethod("data");
                    Object data = dataMethod.invoke(user);
                    
                    // Add node
                    Method addMethod = data.getClass().getMethod("add", nodeClass);
                    addMethod.invoke(data, node);
                    
                    // Save user data to ensure persistence
                    try {
                        Method saveUserMethod = userManager.getClass().getMethod("saveUser", user.getClass());
                        saveUserMethod.invoke(userManager, user);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Could not save user via saveUser: " + e.getMessage());
                        // Try alternative save method
                        try {
                            Method getDataManagerMethod = api.getClass().getMethod("getUserManager");
                            Object dataManager = getDataManagerMethod.invoke(api);
                            Method saveUserDataMethod = dataManager.getClass().getMethod("saveUser", user.getClass());
                            saveUserDataMethod.invoke(dataManager, user);
                        } catch (Exception e2) {
                            plugin.getLogger().warning("Could not save user via alternative method: " + e2.getMessage());
                        }
                    }
                    
                    // Recalculate permissions to ensure they're applied immediately
                    try {
                        Method getPermissionManagerMethod = api.getClass().getMethod("getPermissionManager");
                        Object permissionManager = getPermissionManagerMethod.invoke(api);
                        Method refreshUserMethod = permissionManager.getClass().getMethod("refreshUser", user.getClass());
                        refreshUserMethod.invoke(permissionManager, user);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Could not refresh user permissions: " + e.getMessage());
                    }
                    
                    // Also recalculate permissions on the player side
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            player.recalculatePermissions();
                        }
                    });
                    
                    plugin.getLogger().info("Granted permission " + permission + " to " + player.getName() + " via LuckPerms");
                    
                    // Verify permission was granted after a delay
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            if (player.hasPermission(permission)) {
                                plugin.getLogger().info("Verified: " + player.getName() + " has permission " + permission);
                            } else {
                                plugin.getLogger().warning("WARNING: " + player.getName() + " does NOT have permission " + permission + " after granting! Re-granting...");
                                // Try to re-grant if verification fails
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    grantPermission(player, permission);
                                });
                            }
                        }
                    }, 20L); // Check after 1 second
                    
                    return;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to grant permission via LuckPerms: " + e.getMessage());
                // Fall through to PermissionAttachment
            }
        }
        
        // Fallback to Bukkit PermissionAttachment
        UUID uuid = player.getUniqueId();
        PermissionAttachment attachment = attachments.get(uuid);
        if (attachment == null) {
            attachment = player.addAttachment(plugin);
            attachments.put(uuid, attachment);
        }
        attachment.setPermission(permission, true);
        
        // Recalculate permissions
        player.recalculatePermissions();
        
        plugin.getLogger().info("Granted permission " + permission + " to " + player.getName() + " via PermissionAttachment");
        
        // Verify permission was granted
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                if (player.hasPermission(permission)) {
                    plugin.getLogger().info("Verified: " + player.getName() + " has permission " + permission);
                } else {
                    plugin.getLogger().warning("WARNING: " + player.getName() + " does NOT have permission " + permission + " after granting!");
                }
            }
        }, 5L); // Check after 0.25 seconds
        
        player.sendMessage("§e§lPandora §7Permission granted: §6" + permission);
    }
    
    private void applyPerkEffects(Player player, Perk perk) {
        String key = perk.getKey();
        
        // Apply effects based on perk type
        if (key.startsWith("mining_speed")) {
            // Mining speed is handled by the block break listener
            player.sendMessage("§e§lPandora §7Mining speed boost activated!");
        } else if (key.equals("speed_boost")) {
            // Speed effect is handled by the player listener
            player.sendMessage("§e§lPandora §7Speed boost activated!");
        } else if (key.equals("jump_boost")) {
            player.sendMessage("§e§lPandora §7Jump boost activated!");
        } else if (key.equals("night_vision")) {
            player.sendMessage("§e§lPandora §7Night vision activated!");
        } else if (key.equals("water_breathing")) {
            player.sendMessage("§e§lPandora §7Water breathing activated!");
        } else if (key.equals("extra_hearts")) {
            applyExtraHearts(player);
            player.sendMessage("§e§lPandora §7Extra hearts activated! You now have &c+6 health&7!");
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
    
    // Removed duplicate getPlayerLevel - use plugin.getPlayerLevel() instead
}

