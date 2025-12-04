package com.playpandora.perkshop.commands;

import com.playpandora.perkshop.PerkShop;
import com.playpandora.perkshop.models.Perk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class PerksCommand implements CommandExecutor {
    
    private final PerkShop plugin;
    
    public PerksCommand(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§e§lPandora §7This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("perkshop.reload")) {
                player.sendMessage("§e§lPandora §7You don't have permission to use this command!");
                return true;
            }
            
            // Re-grant all owned perk permissions
            Set<String> ownedPerks = plugin.getDataManager().getPlayerPerks(player.getUniqueId());
            int granted = 0;
            
            for (String perkKey : ownedPerks) {
                Perk perk = plugin.getPerkManager().getPerk(perkKey);
                if (perk != null && perk.getPermission() != null && !perk.getPermission().isEmpty()) {
                    plugin.getPurchaseManager().grantPermission(player, perk.getPermission());
                    granted++;
                }
            }
            
            player.sendMessage("§e§lPandora §7Re-granted permissions for §6" + granted + " §7owned perks!");
            return true;
        }
        
        // Show owned perks
        Set<String> ownedPerks = plugin.getDataManager().getPlayerPerks(player.getUniqueId());
        if (ownedPerks.isEmpty()) {
            player.sendMessage("§e§lPandora §7You don't own any perks yet. Use §6/shop §7to buy some!");
            return true;
        }
        
        player.sendMessage("§6§lYour Perks:");
        for (String perkKey : ownedPerks) {
            Perk perk = plugin.getPerkManager().getPerk(perkKey);
            if (perk != null) {
                String status = player.hasPermission(perk.getPermission()) ? "§e✓" : "§7✗";
                player.sendMessage("  " + status + " §7" + perk.getName());
            }
        }
        
        return true;
    }
}

