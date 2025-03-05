package com.example.instantminingzones;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class InstantMiningZones extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<String, Set<Location>> instantMineZones = new HashMap<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadZones();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("instantmine").setExecutor(this);
    }

    @Override
    public void onDisable() {
        saveZones();
    }

    private void loadZones() {
        if (config.contains("zones")) {
            for (String player : config.getConfigurationSection("zones").getKeys(false)) {
                Set<Location> zones = new HashSet<>();
                for (String locStr : config.getStringList("zones." + player)) {
                    String[] parts = locStr.split(",");
                    Location loc = new Location(
                        Bukkit.getWorld(parts[0]),
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3])
                    );
                    zones.add(loc);
                }
                instantMineZones.put(player, zones);
            }
        }
    }

    private void saveZones() {
        for (Map.Entry<String, Set<Location>> entry : instantMineZones.entrySet()) {
            Set<String> locStrings = new HashSet<>();
            for (Location loc : entry.getValue()) {
                locStrings.add(String.format("%s,%f,%f,%f",
                    loc.getWorld().getName(),
                    loc.getX(),
                    loc.getY(),
                    loc.getZ()
                ));
            }
            config.set("zones." + entry.getKey(), locStrings);
        }
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage("§cUsage: /instantmine <create|remove|list>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                return createZone(player);
            case "remove":
                return removeZone(player);
            case "list":
                return listZones(player);
            default:
                player.sendMessage("§cUnknown subcommand. Use create, remove, or list.");
                return true;
        }
    }

    private boolean createZone(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage("§cLook at a block within 5 blocks to set an instant mine zone!");
            return true;
        }

        String playerName = player.getName();
        instantMineZones.computeIfAbsent(playerName, k -> new HashSet<>())
                       .add(targetBlock.getLocation());
        
        player.sendMessage("§aInstant Mine Zone created!");
        return true;
    }

    private boolean removeZone(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        
        if (targetBlock == null) {
            player.sendMessage("§cLook at a block within 5 blocks to remove an instant mine zone!");
            return true;
        }

        String playerName = player.getName();
        Set<Location> playerZones = instantMineZones.get(playerName);
        
        if (playerZones != null && playerZones.remove(targetBlock.getLocation())) {
            player.sendMessage("§aInstant Mine Zone removed!");
        } else {
            player.sendMessage("§cNo Instant Mine Zone found at that location!");
        }
        return true;
    }

    private boolean listZones(Player player) {
        String playerName = player.getName();
        Set<Location> playerZones = instantMineZones.get(playerName);
        
        if (playerZones == null || playerZones.isEmpty()) {
            player.sendMessage("§eYou have no Instant Mine Zones.");
        } else {
            player.sendMessage("§eYour Instant Mine Zones:");
            for (Location loc : playerZones) {
                player.sendMessage(String.format("§7- World: %s, X: %d, Y: %d, Z: %d",
                    loc.getWorld().getName(),
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ()
                ));
            }
        }
        return true;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location blockLoc = event.getBlock().getLocation();
        
        for (Set<Location> zones : instantMineZones.values()) {
            if (zones.contains(blockLoc)) {
                event.setCancelled(true);
                event.getBlock().setType(Material.AIR);
                event.getPlayer().sendMessage("§eInstantly mined block!");
                break;
            }
        }
    }
}
