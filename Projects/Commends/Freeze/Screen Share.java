package me.revoqz.ss;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SSControl extends JavaPlugin implements Listener {

    private final Map<UUID, Location> frozen = new HashMap<>();
    private final Set<UUID> ssPlayers = new HashSet<>();
    private final Map<UUID, UUID> ssStaff = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ss")) {
            if (args.length != 1) return false;
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) return false;

            frozen.put(target.getUniqueId(), target.getLocation());
            ssPlayers.add(target.getUniqueId());
            if (sender instanceof Player) ssStaff.put(target.getUniqueId(), ((Player) sender).getUniqueId());

            target.sendMessage("§cYou have been frozen.");
            target.sendMessage("§eYou have 1 minute to join one of the Discord calls in the Discord server or you will get banned.");
            target.sendMessage("§eIf you log out you will be banned too.");
            sender.sendMessage("§a" + target.getName() + " has been frozen.");
        }

        if (command.getName().equalsIgnoreCase("sschat")) {
            if (!(sender instanceof Player)) return true;
            Player staff = (Player) sender;
            UUID targetId = null;
            for (Map.Entry<UUID, UUID> entry : ssStaff.entrySet()) {
                if (entry.getValue().equals(staff.getUniqueId())) {
                    targetId = entry.getKey();
                    break;
                }
            }
            if (targetId == null) return true;
            Player frozenPlayer = Bukkit.getPlayer(targetId);
            if (frozenPlayer == null || !frozenPlayer.isOnline()) return true;
            frozenPlayer.sendMessage("§9[SS Chat] " + staff.getName() + ": " + String.join(" ", Arrays.copyOfRange(args, 0, args.length)));
        }

        return true;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (ssPlayers.contains(player.getUniqueId())) {
            Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), "Disconnected while frozen", null, "SSControl");
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!frozen.containsKey(player.getUniqueId())) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
            event.setTo(from);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!ssPlayers.contains(event.getPlayer().getUniqueId())) return;
        UUID staffId = ssStaff.get(event.getPlayer().getUniqueId());
        if (staffId == null) return;
        Player staff = Bukkit.getPlayer(staffId);
        if (staff != null && staff.isOnline()) {
            staff.sendMessage("§9[SS Chat] " + event.getPlayer().getName() + ": " + event.getMessage());
        }
        event.setCancelled(true);
    }
}
