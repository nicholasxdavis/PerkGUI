package com.playpandora.perkshop.commands;

import com.playpandora.perkshop.PerkShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FeedCommand implements CommandExecutor {
    
    private final PerkShop plugin;
    
    public FeedCommand(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§e§lPandora §7This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has the feed perk
        if (!plugin.getPurchaseManager().hasPerk(player.getUniqueId(), "feed")) {
            player.sendMessage("§e§lPandora §7You need to purchase the Feed perk from the shop first!");
            return true;
        }
        
        // Feed the player
        player.setFoodLevel(20);
        player.setSaturation(20);
        
        player.sendMessage("§e§lPandora §7You have been fed!");
        
        return true;
    }
}


