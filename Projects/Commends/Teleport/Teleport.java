package me.revqz.teleportplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;

public class TeleportPlugin extends JavaPlugin implements CommandExecutor, Listener {
    @Override
    public void onEnable() {
        this.getCommand("teleport").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("TeleportPlugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("teleport.use")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        // Handle teleporting to coordinates
        if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                
                Location loc = new Location(player.getWorld(), x, y, z);
                if (!isLocationSafe(loc)) {
                    player.sendMessage("§cWarning: Destination may be unsafe!");
                }
                
                player.teleport(loc);
                player.sendMessage("§aTeleported to: " + x + ", " + y + ", " + z);
                return true;
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid coordinates!");
                return true;
            }
        }
        
        // Handle teleporting to players
        else if (args.length == 1) {
            if (!player.hasPermission("teleport.player")) {
                player.sendMessage("§cYou don't have permission to teleport to players!");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§cPlayer not found or offline!");
                return true;
            }
            
            player.teleport(target.getLocation());
            player.sendMessage("§aTeleported to " + target.getName());
            return true;
        }

        player.sendMessage("§cUsage: /teleport <x> <y> <z> OR /teleport <player>");
        return true;
    }

    private boolean isLocationSafe(Location location) {
        return location.getBlock().getType().isTransparent() && 
               location.add(0, 1, 0).getBlock().getType().isTransparent() &&
               !location.subtract(0, 2, 0).getBlock().getType().isTransparent();
    }
}
