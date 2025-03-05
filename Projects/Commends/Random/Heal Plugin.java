package me.revqz.healplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;

public class HealPlugin extends JavaPlugin implements CommandExecutor, Listener {
    @Override
    public void onEnable() {
        this.getCommand("heal").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("HealPlugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("heal.use")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        // Handle healing other players
        if (args.length == 1) {
            if (!player.hasPermission("heal.others")) {
                player.sendMessage("§cYou don't have permission to heal other players!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cPlayer not found!");
                return true;
            }

            healPlayer(target);
            player.sendMessage("§aYou have healed " + target.getName() + "!");
            target.sendMessage("§aYou have been healed by " + player.getName() + "!");
            return true;
        }

        // Self heal
        healPlayer(player);
        player.sendMessage("§aYou have been healed!");
        return true;
    }

    private void healPlayer(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20); // Also restore food level
        player.setFireTicks(0); // Extinguish fire if player is burning
        player.getActivePotionEffects().clear(); // Remove negative potion effects
    }
}
