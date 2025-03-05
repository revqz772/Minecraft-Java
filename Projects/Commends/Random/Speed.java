package me.revqz.speedplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.UUID;

public class SpeedPlugin extends JavaPlugin implements CommandExecutor, Listener {
    private final HashMap<UUID, Float> previousSpeeds = new HashMap<>();
    
    @Override
    public void onEnable() {
        this.getCommand("speed").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("SpeedPlugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("speed.use")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            // Reset speed if no arguments
            resetSpeed(player);
            return true;
        }

        if (args.length > 2) {
            sendUsage(player);
            return true;
        }

        // Check if targeting another player
        Player target = player;
        if (args.length == 2) {
            if (!player.hasPermission("speed.others")) {
                player.sendMessage("§cYou don't have permission to modify others' speed!");
                return true;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cPlayer not found!");
                return true;
            }
        }

        try {
            float speed = Float.parseFloat(args[0]);
            if (speed < 0 || speed > 10) {
                player.sendMessage("§cSpeed value must be between 0 and 10!");
                return true;
            }

            // Store previous speed before changing
            if (!previousSpeeds.containsKey(target.getUniqueId())) {
                previousSpeeds.put(target.getUniqueId(), target.getWalkSpeed());
            }

            target.setWalkSpeed(speed / 10);
            target.sendMessage("§aYour speed has been set to " + speed + "!");
            
            if (target != player) {
                player.sendMessage("§aSet " + target.getName() + "'s speed to " + speed + "!");
            }
        } catch (NumberFormatException e) {
            if (args[0].equalsIgnoreCase("reset")) {
                resetSpeed(target);
                if (target != player) {
                    player.sendMessage("§aReset " + target.getName() + "'s speed!");
                }
            } else {
                sendUsage(player);
            }
        }
        return true;
    }

    private void resetSpeed(Player player) {
        float defaultSpeed = 0.2f;
        float previousSpeed = previousSpeeds.getOrDefault(player.getUniqueId(), defaultSpeed);
        player.setWalkSpeed(previousSpeed);
        previousSpeeds.remove(player.getUniqueId());
        player.sendMessage("§aYour speed has been reset!");
    }

    private void sendUsage(Player player) {
        player.sendMessage("§cUsage: /speed <value (0-10) | reset> [player]");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Reset speed when player leaves
        Player player = event.getPlayer();
        if (previousSpeeds.containsKey(player.getUniqueId())) {
            resetSpeed(player);
        }
    }
}
