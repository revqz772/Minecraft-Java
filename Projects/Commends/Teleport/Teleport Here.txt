package me.revqz.teleporthere;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;

public class TeleportHerePlugin extends JavaPlugin implements CommandExecutor, Listener {
    @Override
    public void onEnable() {
        this.getCommand("tphere").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("TeleportHerePlugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("tphere.use")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /tphere <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        
        if (target == null || !target.isOnline()) {
            player.sendMessage("§cPlayer not found or offline!");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§cYou cannot teleport yourself to yourself!");
            return true;
        }

        if (!player.hasPermission("tphere.bypass") && target.hasPermission("tphere.exempt")) {
            player.sendMessage("§cYou cannot teleport this player!");
            return true;
        }
        
        target.teleport(player.getLocation());
        player.sendMessage("§aTeleported " + target.getName() + " to you!");
        target.sendMessage("§aYou have been teleported to " + player.getName() + "!");
        
        // Log the teleport
        getLogger().info(player.getName() + " teleported " + target.getName() + " to their location");
        return true;
    }
}
