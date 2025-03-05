package me.revqz.spawnplugin;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class SpawnPlugin extends JavaPlugin implements CommandExecutor, Listener {
    private Location spawnLocation;
    private boolean teleportOnJoin;
    private int teleportDelay;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Register commands
        this.getCommand("setspawn").setExecutor(this);
        this.getCommand("spawn").setExecutor(this);
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        
        // Load config values
        loadConfig();
        loadSpawnLocation();
        
        getLogger().info("SpawnPlugin has been enabled!");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        // Set defaults if they don't exist
        config.addDefault("settings.teleport-on-join", true);
        config.addDefault("settings.teleport-delay", 3);
        config.options().copyDefaults(true);
        saveConfig();
        
        teleportOnJoin = config.getBoolean("settings.teleport-on-join");
        teleportDelay = config.getInt("settings.teleport-delay");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (teleportOnJoin && spawnLocation != null) {
            Player player = event.getPlayer();
            if (!player.hasPlayedBefore()) {
                teleportToSpawn(player);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (command.getName().equalsIgnoreCase("setspawn")) {
            if (!player.hasPermission("spawn.set")) {
                player.sendMessage("§cYou don't have permission to set spawn!");
                return true;
            }
            
            spawnLocation = player.getLocation();
            saveSpawnLocation();
            player.sendMessage("§aSpawn location has been set!");
            return true;
        }
        
        if (command.getName().equalsIgnoreCase("spawn")) {
            if (!player.hasPermission("spawn.teleport")) {
                player.sendMessage("§cYou don't have permission to teleport to spawn!");
                return true;
            }
            
            if (spawnLocation == null) {
                player.sendMessage("§cSpawn location has not been set!");
                return true;
            }

            player.sendMessage("§aTeleporting to spawn in " + teleportDelay + " seconds...");
            teleportToSpawn(player);
            return true;
        }
        
        return false;
    }

    private void teleportToSpawn(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                player.teleport(spawnLocation);
                player.sendMessage("§aWelcome to spawn!");
            }
        }.runTaskLater(this, teleportDelay * 20L);
    }

    private void saveSpawnLocation() {
        FileConfiguration config = this.getConfig();
        config.set("spawn.world", spawnLocation.getWorld().getName());
        config.set("spawn.x", spawnLocation.getX());
        config.set("spawn.y", spawnLocation.getY());
        config.set("spawn.z", spawnLocation.getZ());
        config.set("spawn.yaw", spawnLocation.getYaw());
        config.set("spawn.pitch", spawnLocation.getPitch());
        saveConfig();
    }

    private void loadSpawnLocation() {
        FileConfiguration config = this.getConfig();
        if (config.contains("spawn.world")) {
            spawnLocation = new Location(
                getServer().getWorld(config.getString("spawn.world")),
                config.getDouble("spawn.x"),
                config.getDouble("spawn.y"),
                config.getDouble("spawn.z"),
                (float) config.getDouble("spawn.yaw"),
                (float) config.getDouble("spawn.pitch")
            );
        }
    }
}
