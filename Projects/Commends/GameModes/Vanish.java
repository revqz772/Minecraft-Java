package me.revqz.vanishplugin;

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

public class VanishPlugin extends JavaPlugin implements CommandExecutor, Listener {
    private final HashSet<UUID> vanishedPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        this.getCommand("vanish").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("VanishPlugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("vanish.use")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        Player target;
        if (args.length == 1) {
            if (!player.hasPermission("vanish.others")) {
                player.sendMessage("§cYou don't have permission to vanish other players!");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cPlayer not found!");
                return true;
            }
        } else if (args.length == 0) {
            target = player;
        } else {
            player.sendMessage("§cUsage: /vanish [player]");
            return true;
        }

        UUID uuid = target.getUniqueId();

        if (vanishedPlayers.contains(uuid)) {
            vanishedPlayers.remove(uuid);
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(this, target);
            }
            target.sendMessage("§aYou are now visible!");
            if (player != target) {
                player.sendMessage("§aMade " + target.getName() + " visible!");
            }
        } else {
            vanishedPlayers.add(uuid);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("vanish.see")) {
                    online.hidePlayer(this, target);
                }
            }
            target.sendMessage("§aYou are now vanished!");
            if (player != target) {
                player.sendMessage("§aMade " + target.getName() + " vanish!");
            }
        }
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();
        
        // Hide vanished players from joining player
        for (UUID uuid : vanishedPlayers) {
            Player vanishedPlayer = Bukkit.getPlayer(uuid);
            if (vanishedPlayer != null && !joiningPlayer.hasPermission("vanish.see")) {
                joiningPlayer.hidePlayer(this, vanishedPlayer);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remove player from vanished set when they leave
        vanishedPlayers.remove(event.getPlayer().getUniqueId());
    }
}
