package me.revqz.koth;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class KOTHPlugin extends JavaPlugin implements CommandExecutor {

    private Location kothLocation;
    private final HashMap<UUID, Integer> playerTime = new HashMap<>();
    private boolean eventActive = false;
    private Player currentContender = null;
    private int requiredTime;
    private double captureRadius;
    private List<String> rewards;
    private BukkitRunnable eventTimer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        
        this.getCommand("kothset").setExecutor(this);
        this.getCommand("kothstart").setExecutor(this);
        this.getCommand("kothstop").setExecutor(this);
        
        getLogger().info("KOTH Plugin has been enabled!");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        config.addDefault("settings.required-time", 120);
        config.addDefault("settings.capture-radius", 3.0);
        
        List<String> defaultRewards = new ArrayList<>();
        defaultRewards.add("DIAMOND 5");
        defaultRewards.add("EMERALD 3");
        defaultRewards.add("GOLDEN_APPLE 2");
        config.addDefault("rewards", defaultRewards);
        
        config.options().copyDefaults(true);
        saveConfig();

        requiredTime = config.getInt("settings.required-time");
        captureRadius = config.getDouble("settings.capture-radius");
        rewards = config.getStringList("rewards");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        
        switch(command.getName().toLowerCase()) {
            case "kothset":
                if (!player.hasPermission("koth.admin")) {
                    player.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                kothLocation = player.getLocation();
                player.sendMessage("§aKOTH location set at your position!");
                return true;
                
            case "kothstart":
                if (!player.hasPermission("koth.admin")) {
                    player.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                if (eventActive) {
                    player.sendMessage("§cKOTH event is already running!");
                    return true;
                }
                startEvent();
                return true;
                
            case "kothstop":
                if (!player.hasPermission("koth.admin")) {
                    player.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                if (!eventActive) {
                    player.sendMessage("§cNo KOTH event is currently running!");
                    return true;
                }
                stopEvent();
                return true;
        }
        return false;
    }

    private void startEvent() {
        if (kothLocation == null) {
            Bukkit.broadcastMessage("§cCannot start KOTH - location not set!");
            return;
        }
        
        eventActive = true;
        Bukkit.broadcastMessage("§6§lKOTH Event has started!");
        startKothChecker();
        
        eventTimer = new BukkitRunnable() {
            @Override
            public void run() {
                stopEvent();
                Bukkit.broadcastMessage("§c§lKOTH Event has ended due to time limit!");
            }
        };
        eventTimer.runTaskLater(this, 72000); 
    }

    private void stopEvent() {
        eventActive = false;
        resetKOTH();
        if (eventTimer != null) {
            eventTimer.cancel();
        }
        Bukkit.broadcastMessage("§c§lKOTH Event has been stopped!");
    }

    private void startKothChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventActive || kothLocation == null) {
                    this.cancel();
                    return;
                }

                kothLocation.getWorld().spawnParticle(Particle.FLAME, kothLocation, 50, 1, 1, 1, 0);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getLocation().distance(kothLocation) < captureRadius) {
                        if (currentContender == null || currentContender.equals(player)) {
                            currentContender = player;
                            playerTime.put(player.getUniqueId(), playerTime.getOrDefault(player.getUniqueId(), 0) + 1);
                            int timeLeft = requiredTime - playerTime.get(player.getUniqueId());
                        
                            String progressBar = getProgressBar(playerTime.get(player.getUniqueId()), requiredTime);
                            player.sendActionBar(ChatColor.GOLD + "Capturing: " + progressBar + " §e" + timeLeft + "s");
                            
                            if (timeLeft <= 0) {
                                rewardPlayer(player);
                                stopEvent();
                                startEvent(); 
                            }
                        }
                    } else if (player.equals(currentContender)) {
                        player.sendMessage("§cYou left the KOTH area! Progress reset.");
                        resetKOTH();
                    }
                }
            }
        }.runTaskTimer(this, 20, 20);
    }

    private String getProgressBar(int current, int max) {
        int bars = 20;
        float percent = (float) current / max;
        int filledBars = (int) (bars * percent);
        
        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < bars; i++) {
            if (i == filledBars) bar.append("§c");
            bar.append("█");
        }
        return bar.toString();
    }

    private void rewardPlayer(Player player) {
        ItemStack kothKey = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta meta = kothKey.getItemMeta();
        meta.setDisplayName("§6§lKOTH Key");
        List<String> lore = new ArrayList<>();
        lore.add("§7A legendary key obtained from");
        lore.add("§7winning the KOTH event!");
        meta.setLore(lore);
        NamespacedKey key = new NamespacedKey(this, "koth_key");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "true");
        kothKey.setItemMeta(meta);
        player.getInventory().addItem(kothKey);

        for (String reward : rewards) {
            String[] parts = reward.split(" ");
            Material material = Material.valueOf(parts[0]);
            int amount = Integer.parseInt(parts[1]);
            player.getInventory().addItem(new ItemStack(material, amount));
        }

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        Bukkit.broadcastMessage("§6§l" + player.getName() + " §ehas won the King of the Hill event!");
    }

    private void resetKOTH() {
        playerTime.clear();
        currentContender = null;
    }
}
