package com.playpandora.perkshop.gui;

import com.playpandora.perkshop.PerkShop;
import com.playpandora.perkshop.models.Perk;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ShopGUI {
    
    private final PerkShop plugin;
    private final java.util.Map<java.util.UUID, java.util.Map<Integer, String>> playerSlotMappings = new java.util.HashMap<>();
    
    public ShopGUI(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    public String getPerkBySlot(org.bukkit.entity.Player player, int slot) {
        java.util.Map<Integer, String> mapping = playerSlotMappings.get(player.getUniqueId());
        if (mapping != null) {
            return mapping.get(slot);
        }
        return null;
    }
    
    public void clearPlayerMapping(org.bukkit.entity.Player player) {
        playerSlotMappings.remove(player.getUniqueId());
    }
    
    public void openShop(Player player) {
        String title = "&8Perk Shop"; // Force &8 style (no bold) - single row GUI
        ConfigurationSection perksSection = plugin.getConfig().getConfigurationSection("perks");
        if (perksSection == null) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&8Pandora &7Error: No perks configured!"));
            return;
        }
        
        // Count enabled perks
        int perkCount = 0;
        for (String perkKey : perksSection.getKeys(false)) {
            ConfigurationSection perkConfig = perksSection.getConfigurationSection(perkKey);
            if (perkConfig != null && perkConfig.getBoolean("enabled", true)) {
                Perk perk = plugin.getPerkManager().getPerk(perkKey);
                if (perk != null) {
                    perkCount++;
                }
            }
        }
        
        // Always use size 9 (1 row) for PerkShop
        int size = 9;
        
        Inventory inv = Bukkit.createInventory(null, size, 
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        
        // Fill borders first
        fillBorders(inv, size, true, true, true);
        
        // Add close button (bottom right - slot 8)
        int closeSlot = size - 1;
        inv.setItem(closeSlot, createCloseButton());
        
        // Add perk items in a single row - start at slot 1, end at slot 7 (slots 0 and 8 are borders/close)
        int currentSlot = 1;
        int maxSlot = 7; // Last valid slot before close button
        
        // Clear and create slot mapping for this player
        playerSlotMappings.remove(player.getUniqueId());
        java.util.Map<Integer, String> slotMapping = new java.util.HashMap<>();
        
        for (String perkKey : perksSection.getKeys(false)) {
            ConfigurationSection perkConfig = perksSection.getConfigurationSection(perkKey);
            if (perkConfig == null || !perkConfig.getBoolean("enabled", true)) {
                continue;
            }
            
            Perk perk = plugin.getPerkManager().getPerk(perkKey);
            if (perk == null) {
                continue;
            }
            
            if (currentSlot > maxSlot) {
                break; // No more space in single row
            }
            
            ItemStack item = createPerkItem(perk, player);
            inv.setItem(currentSlot, item);
            
            // Map slot to perk key
            slotMapping.put(currentSlot, perkKey);
            
            currentSlot++;
        }
        
        // Store slot mapping for this player
        playerSlotMappings.put(player.getUniqueId(), slotMapping);
        
        // Fill remaining empty slots with borders
        fillEmptySlots(inv, size);
        
        player.openInventory(inv);
    }
    
    public void updatePerkItem(Player player, String perkKey, int slot) {
        // Check if player has the shop open
        if (player.getOpenInventory() == null || 
            !player.getOpenInventory().getTitle().equals(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("shop.title", "&8Perk Shop")))) {
            return;
        }
        
        Perk perk = plugin.getPerkManager().getPerk(perkKey);
        if (perk == null) {
            return;
        }
        
        ItemStack item = createPerkItem(perk, player);
        // Update the top inventory (the shop GUI), not the player's inventory
        player.getOpenInventory().getTopInventory().setItem(slot, item);
    }
    
    
    private ItemStack createPerkItem(Perk perk, Player player) {
        String materialName = perk.getMaterial();
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            material = Material.BARRIER;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            return item;
        }
        
        // Set display name - ensure it uses &6 for color
        String displayName = perk.getName();
        // Remove &l if present, but keep &6
        displayName = displayName.replace("&l", "").replace("&L", "");
        if (!displayName.contains("&6") && !displayName.contains("&e")) {
            displayName = "&6" + displayName;
        }
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
        
        // Build lore with matching style (same as FarmShop/PetShop)
        List<String> lore = new ArrayList<>();
        
        // Add price and level requirement at the top (configurable)
        boolean showPriceInLore = plugin.getConfig().getBoolean("shop.show-price-in-lore", true);
        boolean showLevelInLore = plugin.getConfig().getBoolean("shop.show-level-in-lore", true);
        
        if (showPriceInLore) {
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Price: &6" + plugin.formatMoney(perk.getPrice())));
        }
        
        if (showLevelInLore && perk.getRequiredLevel() > 0) {
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Required Level: &6" + perk.getRequiredLevel()));
        }
        
        if (showPriceInLore || (showLevelInLore && perk.getRequiredLevel() > 0)) {
            lore.add(""); // Empty line after price/level
        }
        
        // Add description with placeholder replacement
        for (String line : perk.getDescription()) {
            // Replace placeholders like {cooldown}
            String processed = line;
            if (line.contains("{cooldown}")) {
                int cooldown = plugin.getConfig().getInt("perks." + perk.getKey() + ".cooldown", 0);
                processed = processed.replace("{cooldown}", String.valueOf(cooldown));
            }
            if (line.contains("{price}")) {
                processed = processed.replace("{price}", plugin.formatMoney(perk.getPrice()));
            }
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', processed));
        }
        
        lore.add(""); // Empty line
        
        // Check if player already owns this perk
        boolean ownsPerk = plugin.getPurchaseManager().hasPerk(player.getUniqueId(), perk.getKey());
        
        if (ownsPerk) {
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a✓ OWNED"));
        } else {
            boolean canPurchase = true;
            
            // Check level requirement (matching pet plugin style)
            if (perk.getRequiredLevel() > 0) {
                int playerLevel = plugin.getPlayerLevel(player);
                if (playerLevel < perk.getRequiredLevel()) {
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                        "&7Requires Level: &6" + perk.getRequiredLevel()));
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                        "&7Your Level: &c" + playerLevel));
                    lore.add("");
                    canPurchase = false;
                }
            }
            
            // Check if player has required perk
            if (perk.getRequires() != null && !perk.getRequires().isEmpty()) {
                boolean hasRequired = plugin.getPurchaseManager().hasPerk(player.getUniqueId(), perk.getRequires());
                if (!hasRequired) {
                    Perk requiredPerk = plugin.getPerkManager().getPerk(perk.getRequires());
                    if (requiredPerk != null) {
                        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                            "&7Requires: &6" + requiredPerk.getName().replace("&l", "").replace("&L", "")));
                        lore.add("");
                    }
                    canPurchase = false;
                }
            }
            
            lore.add("");
            
            // Check if player can afford it
            double balance = plugin.getPlayerBalance(player);
            if (balance >= perk.getPrice() && canPurchase) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to purchase"));
            } else {
                if (balance < perk.getPrice()) {
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c✗ INSUFFICIENT FUNDS"));
                    double needed = perk.getPrice() - balance;
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                        "&7You need: &6" + plugin.formatMoney(needed) + " &7more"));
                } else {
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c✗ REQUIREMENTS NOT MET"));
                }
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    // Removed duplicate getPlayerLevel - use plugin.getPlayerLevel() instead (already using it above)
    
    // GUI utility methods (Hypixel standard)
    private int calculateOptimalSizeWithNav(int itemCount) {
        int neededSlots = itemCount + 1; // +1 for close button
        return calculateOptimalSize(neededSlots);
    }
    
    private int calculateOptimalSize(int itemCount) {
        if (itemCount <= 9) return 9;
        if (itemCount <= 18) return 18;
        if (itemCount <= 27) return 27;
        if (itemCount <= 36) return 36;
        if (itemCount <= 45) return 45;
        return 54;
    }
    
    private void fillBorders(Inventory inv, int size, boolean fillTop, boolean fillBottom, boolean fillSides) {
        ItemStack border = createBorder();
        int rows = size / 9;
        
        if (fillTop && rows > 0) {
            for (int i = 0; i < 9; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, border);
                }
            }
        }
        
        if (fillBottom && rows > 0) {
            int startBottom = (rows - 1) * 9;
            for (int i = startBottom; i < size; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, border);
                }
            }
        }
        
        if (fillSides && rows > 0) {
            for (int row = 0; row < rows; row++) {
                int leftSlot = row * 9;
                int rightSlot = row * 9 + 8;
                if (inv.getItem(leftSlot) == null) {
                    inv.setItem(leftSlot, border);
                }
                if (inv.getItem(rightSlot) == null) {
                    inv.setItem(rightSlot, border);
                }
            }
        }
    }
    
    private void fillEmptySlots(Inventory inv, int size) {
        ItemStack border = createBorder();
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }
    }
    
    private ItemStack createBorder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c✖ Close"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to close this menu"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

