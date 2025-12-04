package com.playpandora.perkshop.commands;

import com.playpandora.perkshop.PerkShop;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class RepairCommand implements CommandExecutor {
    
    private final PerkShop plugin;
    
    public RepairCommand(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§e§lPandora §7This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has the repair perk
        if (!plugin.getPurchaseManager().hasPerk(player.getUniqueId(), "repair")) {
            player.sendMessage("§e§lPandora §7You need to purchase the Repair perk from the shop first!");
            return true;
        }
        
        // Get the item in player's hand
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§e§lPandora §7You must be holding an item to repair!");
            return true;
        }
        
        // Check if item is repairable
        if (!(item.getItemMeta() instanceof Damageable)) {
            player.sendMessage("§e§lPandora §7This item cannot be repaired!");
            return true;
        }
        
        Damageable damageable = (Damageable) item.getItemMeta();
        
        // Check if item is already at full durability
        if (damageable.getDamage() == 0) {
            player.sendMessage("§e§lPandora §7This item is already at full durability!");
            return true;
        }
        
        // Repair the item
        damageable.setDamage(0);
        item.setItemMeta((ItemMeta) damageable);
        
        player.sendMessage("§e§lPandora §7Item repaired!");
        
        return true;
    }
}


