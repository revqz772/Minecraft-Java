package me.revqz.repairplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;

public class RepairPlugin extends JavaPlugin implements CommandExecutor, Listener {
    @Override
    public void onEnable() {
        this.getCommand("repair").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("RepairPlugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("repair.use")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        // Handle repairing other players' items
        if (args.length == 1) {
            if (!player.hasPermission("repair.others")) {
                player.sendMessage("§cYou don't have permission to repair other players' items!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cPlayer not found!");
                return true;
            }

            repairItem(target);
            player.sendMessage("§aYou have repaired " + target.getName() + "'s item!");
            target.sendMessage("§aYour item has been repaired by " + player.getName() + "!");
            return true;
        }

        // Handle repairing own item
        repairItem(player);
        return true;
    }

    private void repairItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType().isAir()) {
            player.sendMessage("§cYou must hold an item to repair!");
            return;
        }
        
        if (item.getItemMeta() instanceof Damageable) {
            Damageable damageable = (Damageable) item.getItemMeta();
            if (damageable.getDamage() == 0) {
                player.sendMessage("§cThis item is already fully repaired!");
                return;
            }
            damageable.setDamage(0);
            item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) damageable);
            player.sendMessage("§aYour item has been repaired!");
            // Log the repair
            getLogger().info(player.getName() + " repaired their " + item.getType().name());
        } else {
            player.sendMessage("§cThis item cannot be repaired!");
        }
    }
}
