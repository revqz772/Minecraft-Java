package me.revqz.godmodeplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.UUID;

public class GodModePlugin extends JavaPlugin implements CommandExecutor, Listener {
    private final HashSet<UUID> godModePlayers = new HashSet<>();

    @Override
    public void onEnable() {
        this.getCommand("godmode").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("GodModePlugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("godmode.use")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        // Handle /godmode <player> format
        if (args.length == 1) {
            if (!player.hasPermission("godmode.others")) {
                player.sendMessage("§cYou don't have permission to toggle god mode for others!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cPlayer not found!");
                return true;
            }

            toggleGodMode(target);
            player.sendMessage("§aToggled god mode for " + target.getName());
            return true;
        }

        toggleGodMode(player);
        return true;
    }

    private void toggleGodMode(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (godModePlayers.contains(uuid)) {
            godModePlayers.remove(uuid);
            player.setInvulnerable(false);
            player.sendMessage("§cGod mode disabled!");
        } else {
            godModePlayers.add(uuid);
            player.setInvulnerable(true);
            player.sendMessage("§aGod mode enabled!");
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (godModePlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (godModePlayers.contains(player.getUniqueId())) {
            player.setInvulnerable(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (godModePlayers.contains(player.getUniqueId())) {
            player.setInvulnerable(false);
        }
    }
}
