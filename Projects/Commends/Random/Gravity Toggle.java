package me.revqz.gravitytoggle;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import java.util.HashSet;
import java.util.UUID;

public class GravityTogglePlugin extends JavaPlugin implements CommandExecutor, Listener {
    private final HashSet<UUID> noGravityPlayers = new HashSet<>();
    private boolean resetOnQuit;
    private String noPermissionMessage;
    private String gravityEnabledMessage;
    private String gravityDisabledMessage;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        loadConfig();

        // Register command and events
        this.getCommand("gravity").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        // Register permissions
        getServer().getPluginManager().addPermission(new Permission("gravitytoggle.use"));

        getLogger().info("GravityToggle has been enabled!");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        config.addDefault("settings.reset-on-quit", true);
        config.addDefault("messages.no-permission", "§cYou don't have permission to use this command!");
        config.addDefault("messages.gravity-enabled", "§aGravity enabled! You can now fall.");
        config.addDefault("messages.gravity-disabled", "§cGravity disabled! You are now floating.");
        config.options().copyDefaults(true);
        saveConfig();

        resetOnQuit = config.getBoolean("settings.reset-on-quit");
        noPermissionMessage = config.getString("messages.no-permission");
        gravityEnabledMessage = config.getString("messages.gravity-enabled");
        gravityDisabledMessage = config.getString("messages.gravity-disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;

        if (!player.hasPermission("gravitytoggle.use")) {
            player.sendMessage(noPermissionMessage);
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (noGravityPlayers.contains(uuid)) {
            noGravityPlayers.remove(uuid);
            player.setGravity(true);
            player.sendMessage(gravityEnabledMessage);
        } else {
            noGravityPlayers.add(uuid);
            player.setGravity(false);
            player.sendMessage(gravityDisabledMessage);
        }
        return true;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (resetOnQuit) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            if (noGravityPlayers.contains(uuid)) {
                noGravityPlayers.remove(uuid);
                player.setGravity(true);
            }
        }
    }
}
