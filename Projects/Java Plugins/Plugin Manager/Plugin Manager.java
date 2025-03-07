package me.revqz.plugins.manager;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PluginManager extends JavaPlugin implements CommandExecutor {
    private static PluginManager instance;
    private final Map<String, Plugin> managedPlugins = new HashMap<>();
    private FileConfiguration config;
    private File configFile;
    private final Logger logger = getLogger();

    @Override
    public void onEnable() {
        instance = this;
        loadConfig();
        registerCommands();
        registerPlugins();
        logger.info("Plugin Manager has been enabled!");
    }

    @Override
    public void onDisable() {
        disableAllPlugins();
        saveConfig();
        logger.info("Plugin Manager has been disabled!");
    }

    private void loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void registerCommands() {
        getCommand("pm").setExecutor(this);
        getCommand("pluginmanager").setExecutor(this);
    }

    private void registerPlugins() {
        registerPlugin("AutoMine");
        registerPlugin("Bank");
        registerPlugin("CoinFlip");
        registerPlugin("Home");
        registerPlugin("HomingArrows");
        registerPlugin("InstantMiningZones");
        registerPlugin("MysteryBox");
        registerPlugin("PlayerVaults");
        registerPlugin("Report");
        registerPlugin("RocketBoots");
    }

    private void registerPlugin(String pluginName) {
        try {
            String className = "com.revqz772.plugins." + pluginName.toLowerCase() + "." + pluginName + "Plugin";
            Class<?> pluginClass = Class.forName(className);
            Plugin plugin = (Plugin) pluginClass.newInstance();
            managedPlugins.put(pluginName, plugin);
            if (config.getBoolean("plugins." + pluginName + ".enabled", true)) {
                Bukkit.getPluginManager().enablePlugin(plugin);
                logger.info("Successfully registered and enabled " + pluginName);
            }
        } catch (Exception e) {
            logger.warning("Failed to register plugin: " + pluginName);
            e.printStackTrace();
        }
    }

    private void disableAllPlugins() {
        managedPlugins.values().forEach(Bukkit.getPluginManager()::disablePlugin);
        managedPlugins.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        if (!hasPermission(player, "pluginmanager.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                listPlugins(player);
                break;
            case "enable":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /pm enable <plugin>");
                    return true;
                }
                enablePlugin(player, args[1]);
                break;
            case "disable":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /pm disable <plugin>");
                    return true;
                }
                disablePlugin(player, args[1]);
                break;
            case "reload":
                if (args.length < 2) {
                    reloadAllPlugins(player);
                } else {
                    reloadPlugin(player, args[1]);
                }
                break;
            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private boolean hasPermission(Player player, String permission) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        return luckPerms.getUserManager().getUser(player.getUniqueId())
                .getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "Plugin Manager Commands:");
        player.sendMessage(ChatColor.YELLOW + "/pm list " + ChatColor.GRAY + "- List all plugins");
        player.sendMessage(ChatColor.YELLOW + "/pm enable <plugin> " + ChatColor.GRAY + "- Enable a plugin");
        player.sendMessage(ChatColor.YELLOW + "/pm disable <plugin> " + ChatColor.GRAY + "- Disable a plugin");
        player.sendMessage(ChatColor.YELLOW + "/pm reload [plugin] " + ChatColor.GRAY + "- Reload plugin(s)");
    }

    private void listPlugins(Player player) {
        player.sendMessage(ChatColor.GOLD + "Managed Plugins:");
        for (Map.Entry<String, Plugin> entry : managedPlugins.entrySet()) {
            String status = entry.getValue().isEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
            player.sendMessage(ChatColor.YELLOW + "- " + entry.getKey() + ": " + status);
        }
    }

    private void enablePlugin(Player player, String pluginName) {
        Plugin plugin = managedPlugins.get(pluginName);
        if (plugin == null) {
            player.sendMessage(ChatColor.RED + "Plugin not found: " + pluginName);
            return;
        }

        if (plugin.isEnabled()) {
            player.sendMessage(ChatColor.YELLOW + pluginName + " is already enabled!");
            return;
        }

        Bukkit.getPluginManager().enablePlugin(plugin);
        config.set("plugins." + pluginName + ".enabled", true);
        saveConfig();
        player.sendMessage(ChatColor.GREEN + "Successfully enabled " + pluginName);
    }

    private void disablePlugin(Player player, String pluginName) {
        Plugin plugin = managedPlugins.get(pluginName);
        if (plugin == null) {
            player.sendMessage(ChatColor.RED + "Plugin not found: " + pluginName);
            return;
        }

        if (!plugin.isEnabled()) {
            player.sendMessage(ChatColor.YELLOW + pluginName + " is already disabled!");
            return;
        }

        Bukkit.getPluginManager().disablePlugin(plugin);
        config.set("plugins." + pluginName + ".enabled", false);
        saveConfig();
        player.sendMessage(ChatColor.GREEN + "Successfully disabled " + pluginName);
    }

    private void reloadPlugin(Player player, String pluginName) {
        Plugin plugin = managedPlugins.get(pluginName);
        if (plugin == null) {
            player.sendMessage(ChatColor.RED + "Plugin not found: " + pluginName);
            return;
        }

        Bukkit.getPluginManager().disablePlugin(plugin);
        Bukkit.getPluginManager().enablePlugin(plugin);
        player.sendMessage(ChatColor.GREEN + "Successfully reloaded " + pluginName);
    }

    private void reloadAllPlugins(Player player) {
        managedPlugins.forEach((name, plugin) -> {
            Bukkit.getPluginManager().disablePlugin(plugin);
            Bukkit.getPluginManager().enablePlugin(plugin);
        });
        player.sendMessage(ChatColor.GREEN + "Successfully reloaded all plugins!");
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            logger.severe("Could not save config to " + configFile);
            e.printStackTrace();
        }
    }

    public static PluginManager getInstance() {
        return instance;
    }
}
