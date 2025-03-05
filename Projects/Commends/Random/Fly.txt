package me.revqz.flyplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.UUID;

public class FlyPlugin extends JavaPlugin implements CommandExecutor, Listener {
    private final HashSet<UUID> flyingPlayers = new HashSet<>();
    
    @Override
    public void onEnable() {
        this.getCommand("fly").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("FlyPlugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (!sender.hasPermission("fly.use")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length > 1) {
            sender.sendMessage("§cUsage: /fly [player]");
            return true;
        }

        Player target;
        if (args.length == 1) {
            if (!sender.hasPermission("fly.others")) {
                sender.sendMessage("§cYou don't have permission to toggle flight for others!");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }
        } else {
            target = (Player) sender;
        }
        
        UUID targetUUID = target.getUniqueId();
        boolean isFlying = flyingPlayers.contains(targetUUID);
        
        if (isFlying) {
            flyingPlayers.remove(targetUUID);
            target.setAllowFlight(false);
            target.setFlying(false);
            target.sendMessage("§cFlight disabled!");
            if (sender != target) {
                sender.sendMessage("§cDisabled flight for " + target.getName());
            }
        } else {
            flyingPlayers.add(targetUUID);
            target.setAllowFlight(true);
            target.setFlying(true);
            target.sendMessage("§aFlight enabled!");
            if (sender != target) {
                sender.sendMessage("§aEnabled flight for " + target.getName());
            }
        }
        return true;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        flyingPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("fly.reconnect") && flyingPlayers.contains(player.getUniqueId())) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
    }
}
