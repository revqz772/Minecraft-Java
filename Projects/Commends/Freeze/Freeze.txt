package me.revqz.freezeplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashSet;
import java.util.UUID;

public class FreezePlugin extends JavaPlugin implements CommandExecutor, Listener {
    private final HashSet<UUID> frozenPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        this.getCommand("freeze").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("FreezePlugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("freeze.use")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§cUsage: /freeze <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cPlayer not found or offline!");
            return true;
        }

        if (target.hasPermission("freeze.exempt") && !sender.hasPermission("freeze.override")) {
            sender.sendMessage("§cYou cannot freeze this player!");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        if (frozenPlayers.contains(targetUUID)) {
            frozenPlayers.remove(targetUUID);
            Bukkit.broadcastMessage("§a" + target.getName() + " has been unfrozen by " + (sender instanceof Player ? ((Player)sender).getName() : "Console") + "!");
            target.sendMessage("§aYou have been unfrozen! You can now move freely.");
        } else {
            frozenPlayers.add(targetUUID);
            Bukkit.broadcastMessage("§c" + target.getName() + " has been frozen by " + (sender instanceof Player ? ((Player)sender).getName() : "Console") + "!");
            target.sendMessage("§cYou have been frozen! You cannot move until an admin unfreezes you!");
        }
        return true;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (frozenPlayers.contains(player.getUniqueId())) {
            // Allow head movement but prevent position changes
            if (event.getTo().getX() != event.getFrom().getX() ||
                event.getTo().getY() != event.getFrom().getY() ||
                event.getTo().getZ() != event.getFrom().getZ()) {
                event.setTo(event.getFrom());
                player.sendMessage("§cYou are frozen and cannot move!");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (frozenPlayers.contains(player.getUniqueId())) {
            Bukkit.broadcastMessage("§c" + player.getName() + " logged out while frozen!");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (frozenPlayers.contains(player.getUniqueId())) {
            player.sendMessage("§cYou are still frozen!");
            Bukkit.broadcastMessage("§c" + player.getName() + " logged back in while frozen!");
        }
    }
}
