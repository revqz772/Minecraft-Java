package me.revoqz.gamemode;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class GamemodePlugin extends JavaPlugin {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        switch (command.getName().toLowerCase()) {
            case "gmc":
                player.setGameMode(GameMode.CREATIVE);
                break;
            case "gmsp":
                player.setGameMode(GameMode.SPECTATOR);
                break;
            case "gms":
                player.setGameMode(GameMode.SURVIVAL);
                break;
        }
        return true;
    }
}
