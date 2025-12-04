package com.playpandora.perkshop.commands;

import com.playpandora.perkshop.PerkShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealCommand implements CommandExecutor {
    
    private final PerkShop plugin;
    private static final long HEAL_COOLDOWN_MS = 3600000; // 1 hour in milliseconds
    
    public HealCommand(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§e§lPandora §7This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has the heal perk
        if (!plugin.getPurchaseManager().hasPerk(player.getUniqueId(), "heal")) {
            player.sendMessage("§e§lPandora §7You need to purchase the Heal perk from the shop first!");
            return true;
        }
        
        // Check cooldown
        long lastUsed = plugin.getDataManager().getLastHealUse(player.getUniqueId());
        long currentTime = System.currentTimeMillis();
        long timeRemaining = (lastUsed + HEAL_COOLDOWN_MS) - currentTime;
        
        if (timeRemaining > 0) {
            long minutes = timeRemaining / 60000;
            long seconds = (timeRemaining % 60000) / 1000;
            player.sendMessage("§e§lPandora §7You must wait §6" + minutes + "m " + seconds + "s §7before using heal again!");
            return true;
        }
        
        // Heal the player
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setFireTicks(0);
        
        // Update cooldown
        plugin.getDataManager().setLastHealUse(player.getUniqueId(), currentTime);
        
        player.sendMessage("§e§lPandora §7You have been healed!");
        
        return true;
    }
}


