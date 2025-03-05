package me.revqz.clearinventory;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ClearInventoryPlugin extends JavaPlugin implements CommandExecutor {
    @Override
    public void onEnable() {
        this.getCommand("clearinv").setExecutor(this);
        this.getCommand("clear").setExecutor(this);
        this.getCommand("ci").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        player.getInventory().clear();
        player.sendMessage("§aYour inventory has been cleared!");
        return true;
    }
}
