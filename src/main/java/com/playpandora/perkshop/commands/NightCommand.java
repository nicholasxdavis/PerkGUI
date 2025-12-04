package com.playpandora.perkshop.commands;

import com.playpandora.perkshop.PerkShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NightCommand implements CommandExecutor {
    
    private final PerkShop plugin;
    
    public NightCommand(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§e§lPandora §7This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has the night perk
        if (!plugin.getPurchaseManager().hasPerk(player.getUniqueId(), "night")) {
            player.sendMessage("§e§lPandora §7You need to purchase the Night perk from the shop first!");
            return true;
        }
        
        // Give night vision potion effect (duration: 1 hour = 72000 ticks, amplifier: 0 = level 1)
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 72000, 0, true, false));
        player.sendMessage(plugin.formatMessage("messages.night-vision-granted", 
            "{prefix} &7Night vision granted!"));
        
        return true;
    }
}

