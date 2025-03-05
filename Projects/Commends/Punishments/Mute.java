package me.revqz.muteplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.UUID;

public class MutePlugin extends JavaPlugin implements CommandExecutor, Listener {
    private final HashSet<UUID> mutedPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        this.getCommand("mute").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§cUsage: /mute <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cPlayer not found or offline!");
            return true;
        }

        UUID targetUUID = target.getUniqueId();
        if (mutedPlayers.contains(targetUUID)) {
            mutedPlayers.remove(targetUUID);
            sender.sendMessage("§aUnmuted " + target.getName() + "!");
            target.sendMessage("§aYou have been unmuted!");
        } else {
            mutedPlayers.add(targetUUID);
            sender.sendMessage("§cMuted " + target.getName() + "!");
            target.sendMessage("§cYou have been muted! You cannot chat!");
        }
        return true;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (mutedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage("§cYou are muted and cannot chat!");
            event.setCancelled(true);
        }
    }
}
