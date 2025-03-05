package me.revqz.teleporttoplayer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportToPlayerPlugin extends JavaPlugin implements CommandExecutor, Listener {
    private static final int TELEPORT_COOLDOWN = 30; // Cooldown in seconds
    private static final int TELEPORT_DELAY = 3; // Delay in seconds
    
    @Override
    public void onEnable() {
        this.getCommand("tp").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        getLogger().info("TeleportToPlayerPlugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("tp.use")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            sender.sendMessage("§cUsage: /tp <player> [player]");
            return true;
        }

        // Handle /tp <player1> <player2> format
        if (args.length == 2 && player.hasPermission("tp.others")) {
            Player target1 = Bukkit.getPlayer(args[0]);
            Player target2 = Bukkit.getPlayer(args[1]);
            
            if (target1 == null || target2 == null) {
                player.sendMessage("§cOne or both players not found!");
                return true;
            }
            
            teleportPlayer(target1, target2, player);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        
        if (target == null || !target.isOnline()) {
            player.sendMessage("§cPlayer not found or offline!");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§cYou cannot teleport to yourself!");
            return true;
        }

        if (!player.hasPermission("tp.bypass")) {
            if (target.hasPermission("tp.exempt")) {
                player.sendMessage("§cYou cannot teleport to this player!");
                return true;
            }
            
            // Check if player is in combat or recently damaged
            if (player.getLastDamageTick() > 0 && System.currentTimeMillis() - player.getLastDamageTick() < 10000) {
                player.sendMessage("§cCannot teleport while in combat!");
                return true;
            }
        }

        teleportPlayer(player, target, null);
        return true;
    }

    private void teleportPlayer(Player player, Player target, Player initiator) {
        Location destination = target.getLocation();
        
        if (!player.hasPermission("tp.nodelay")) {
            player.sendMessage("§eTeleporting in " + TELEPORT_DELAY + " seconds. Don't move!");
            Location startLoc = player.getLocation();
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.getLocation().equals(startLoc)) {
                        player.sendMessage("§cTeleport cancelled - you moved!");
                        return;
                    }
                    executeTeleport(player, target, destination, initiator);
                }
            }.runTaskLater(this, TELEPORT_DELAY * 20L);
        } else {
            executeTeleport(player, target, destination, initiator);
        }
    }

    private void executeTeleport(Player player, Player target, Location destination, Player initiator) {
        player.teleport(destination, PlayerTeleportEvent.TeleportCause.COMMAND);
        
        if (initiator != null) {
            initiator.sendMessage("§aTeleported " + player.getName() + " to " + target.getName() + "!");
            player.sendMessage("§aYou were teleported to " + target.getName() + " by " + initiator.getName() + "!");
        } else {
            player.sendMessage("§aTeleported to " + target.getName() + "!");
            target.sendMessage("§a" + player.getName() + " has teleported to you!");
        }
        
        // Log the teleport
        if (initiator != null) {
            getLogger().info(initiator.getName() + " teleported " + player.getName() + " to " + target.getName());
        } else {
            getLogger().info(player.getName() + " teleported to " + target.getName());
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
            Player player = event.getPlayer();
            Location to = event.getTo();
            
            // Safety checks
            if (to.getBlock().getType().isSolid() || 
                to.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                Location safe = findSafeLocation(to);
                if (safe != null) {
                    event.setTo(safe);
                }
            }
        }
    }

    private Location findSafeLocation(Location loc) {
        for (int y = 0; y < 5; y++) {
            Location check = loc.clone().add(0, y, 0);
            if (!check.getBlock().getType().isSolid() && 
                !check.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                return check;
            }
        }
        return null;
    }
}
