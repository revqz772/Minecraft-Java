package me.revqz.clearchat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ClearChatPlugin extends JavaPlugin implements CommandExecutor {
    @Override
    public void onEnable() {
        this.getCommand("clearchat").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        
        for (int i = 0; i < 100; i++) {
            getServer().broadcastMessage(" ");
        }
        
        getServer().broadcastMessage("§aChat has been cleared!");
        return true;
    }
}
