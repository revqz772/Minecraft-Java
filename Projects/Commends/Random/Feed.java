package me.revqz.feedplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;

public class FeedPlugin extends JavaPlugin implements CommandExecutor, Listener {
    @Override
    public void onEnable() {
        this.getCommand("feed").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("FeedPlugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("feed.use")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        // Handle feeding other players
        if (args.length == 1) {
            if (!player.hasPermission("feed.others")) {
                player.sendMessage("§cYou don't have permission to feed other players!");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cPlayer not found!");
                return true;
            }

            feedPlayer(target);
            player.sendMessage("§aYou have fed " + target.getName() + "!");
            target.sendMessage("§aYou have been fed by " + player.getName() + "!");
            return true;
        }

        // Feed the command sender
        feedPlayer(player);
        player.sendMessage("§aYou have been fed!");
        return true;
    }

    private void feedPlayer(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20.0f); // Increased saturation for longer lasting effect
        getLogger().info(player.getName() + " has been fed");
    }
}
