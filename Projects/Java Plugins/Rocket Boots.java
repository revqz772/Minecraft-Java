package me.revqz.rocketboots;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class RocketBoots extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<Player, Integer> flyingPlayers = new HashMap<>();
    private FileConfiguration config;
    private int flightDuration;
    private double boostSpeed;
    private int cooldown;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("rocketboots").setExecutor(this);
    }

    private void loadConfig() {
        flightDuration = config.getInt("flight-duration", 100);
        boostSpeed = config.getDouble("boost-speed", 1.0);
        cooldown = config.getInt("cooldown", 200);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (!sender.hasPermission("rocketboots.give")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        Player player = (Player) sender;
        ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
        ItemMeta meta = boots.getItemMeta();
        meta.setDisplayName("§bRocket Boots");
        meta.setLore(java.util.Arrays.asList(
            "§7Grants temporary flight when sneaking!",
            "§7Duration: " + (flightDuration/20) + " seconds",
            "§7Cooldown: " + (cooldown/20) + " seconds"
        ));
        boots.setItemMeta(meta);

        player.getInventory().addItem(boots);
        player.sendMessage("§aYou received Rocket Boots!");
        return true;
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();
        
        if (boots == null || !boots.hasItemMeta() || !"§bRocket Boots".equals(boots.getItemMeta().getDisplayName())) {
            return;
        }

        if (!flyingPlayers.containsKey(player)) {
            if (player.hasPermission("rocketboots.use")) {
                activateRocketBoots(player);
            } else {
                player.sendMessage("§cYou don't have permission to use Rocket Boots!");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (flyingPlayers.containsKey(player) && player.isFlying()) {
            Vector direction = player.getLocation().getDirection();
            player.setVelocity(direction.multiply(boostSpeed));
        }
    }

    private void activateRocketBoots(Player player) {
        flyingPlayers.put(player, flightDuration);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage("§6Rocket Boots activated!");
        
        new BukkitRunnable() {
            @Override
            public void run() {
                int timeLeft = flyingPlayers.get(player);
                if (timeLeft <= 0 || !player.isOnline()) {
                    deactivateRocketBoots(player);
                    cancel();
                    return;
                }
                
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 10);
                player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation(), 5);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 1);
                
                flyingPlayers.put(player, timeLeft - 1);
            }
        }.runTaskTimer(this, 0, 1);
    }

    private void deactivateRocketBoots(Player player) {
        player.setAllowFlight(false);
        player.setFlying(false);
        flyingPlayers.remove(player);
        player.sendMessage("§cRocket Boots deactivated!");


        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage("§aRocket Boots are ready to use again!");
            }
        }.runTaskLater(this, cooldown);
    }
}
