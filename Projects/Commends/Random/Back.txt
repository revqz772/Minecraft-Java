package me.revqz.backplugin;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class BackPlugin extends JavaPlugin implements CommandExecutor {
    private final HashMap<UUID, Location> lastLocations = new HashMap<>();

    @Override
    public void onEnable() {
        this.getCommand("back").setExecutor(this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        
        if (lastLocations.containsKey(uuid)) {
            player.teleport(lastLocations.get(uuid));
            player.sendMessage("§aTeleported back to your last location!");
        } else {
            player.sendMessage("§cNo previous location found!");
        }
        return true;
    }

    public void setLastLocation(Player player, Location location) {
        lastLocations.put(player.getUniqueId(), location);
    }
}
