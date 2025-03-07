package me.revqz.mysteryboxes;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MysteryBoxes extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<Location, MysteryBox> mysteryBoxes = new HashMap<>();
    private final Random random = new Random();
    private List<LootItem> lootTable;
    private FileConfiguration config;
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private static final long PLAYER_COOLDOWN = 60000; 
    private final NamespacedKey mysteryBoxKey;

    public MysteryBoxes() {
        this.mysteryBoxKey = new NamespacedKey(this, "mysterybox");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadLootTable();
        loadMysteryBoxes();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("mysterybox").setExecutor(this);
        getLogger().info("MysteryBoxes Plugin enabled!");
        
        // Start periodic save and cleanup task
        new BukkitRunnable() {
            @Override
            public void run() {
                saveMysteryBoxes();
                cleanupExpiredBoxes();
                getLogger().info("Auto-saving mystery boxes and cleaning up expired ones...");
            }
        }.runTaskTimer(this, 6000L, 6000L); // Run every 5 minutes
    }

    private void cleanupExpiredBoxes() {
        Iterator<Map.Entry<Location, MysteryBox>> it = mysteryBoxes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, MysteryBox> entry = it.next();
            Location loc = entry.getKey();
            MysteryBox box = entry.getValue();
            
            // Remove boxes that have been on cooldown for over 24 hours
            if (System.currentTimeMillis() - box.getCreationTime() > 86400000) {
                loc.getBlock().setType(Material.AIR);
                it.remove();
                getLogger().info("Removed expired mystery box at " + loc.toString());
            }
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "place":
                return handlePlaceCommand(player);
            case "reload":
                return handleReloadCommand(player);
            case "stats":
                return showStats(player);
            case "clear":
                return clearExpiredBoxes(player);
            default:
                showHelp(player);
                return true;
        }
    }

    private boolean showStats(Player player) {
        if (!player.hasPermission("mysterybox.stats")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to view stats!");
            return true;
        }

        int totalBoxes = mysteryBoxes.size();
        long expiredBoxes = mysteryBoxes.values().stream()
            .filter(box -> System.currentTimeMillis() - box.getCreationTime() > MysteryBox.COOLDOWN)
            .count();

        player.sendMessage(ChatColor.GOLD + "=== MysteryBox Stats ===");
        player.sendMessage(ChatColor.YELLOW + "Total boxes: " + ChatColor.WHITE + totalBoxes);
        player.sendMessage(ChatColor.YELLOW + "Boxes on cooldown: " + ChatColor.WHITE + expiredBoxes);
        return true;
    }

    private boolean clearExpiredBoxes(Player player) {
        if (!player.hasPermission("mysterybox.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to clear expired boxes!");
            return true;
        }

        int removed = 0;
        Iterator<Map.Entry<Location, MysteryBox>> it = mysteryBoxes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Location, MysteryBox> entry = it.next();
            if (entry.getValue().isOnCooldown()) {
                entry.getKey().getBlock().setType(Material.AIR);
                it.remove();
                removed++;
            }
        }

        player.sendMessage(ChatColor.GREEN + "Cleared " + removed + " expired mystery boxes!");
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== MysteryBox Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/mysterybox place " + ChatColor.GRAY + "- Place a mystery box");
        player.sendMessage(ChatColor.YELLOW + "/mysterybox stats " + ChatColor.GRAY + "- View mystery box statistics");
        if (player.hasPermission("mysterybox.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/mysterybox reload " + ChatColor.GRAY + "- Reload configuration");
            player.sendMessage(ChatColor.YELLOW + "/mysterybox clear " + ChatColor.GRAY + "- Clear expired boxes");
        }
    }


    private void spawnHologram(Location loc) {
        new BukkitRunnable() {
            double y = 0;
            int ticks = 0;
            final double[] particleOffsets = {0.3, -0.3, 0.3, -0.3};
            
            @Override
            public void run() {
                Location baseLoc = loc.clone().subtract(0.5, 1.5, 0.5);
                if (!mysteryBoxes.containsKey(baseLoc)) {
                    this.cancel();
                    return;
                }
                
                if (ticks >= 200) {
                    this.cancel();
                    return;
                }
                
                y += 0.05;
                if (y > 2) y = 0;
                
                // Create a rotating particle effect
                for (int i = 0; i < 4; i++) {
                    double angle = (ticks + (i * Math.PI/2)) % (2 * Math.PI);
                    double x = Math.cos(angle) * 0.5;
                    double z = Math.sin(angle) * 0.5;
                    loc.getWorld().spawnParticle(Particle.SPELL_WITCH, 
                        loc.clone().add(x, y, z), 1, 0, 0, 0, 0);
                }
                
                if (ticks % 20 == 0) { // Every second
                    loc.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE,
                        loc.clone(), 15, 0.5, 0.5, 0.5, 0);
                }
                    
                ticks++;
            }
        }.runTaskTimer(this, 0L, 1L);
    }


    private static class MysteryBox {
        private final long creationTime;
        private static final long COOLDOWN = 300000; // 5 minutes in milliseconds
        private final UUID owner;

        public MysteryBox(UUID owner) {
            this.creationTime = System.currentTimeMillis();
            this.owner = owner;
        }

        public MysteryBox(long creationTime, UUID owner) {
            this.creationTime = creationTime;
            this.owner = owner;
        }

        public boolean isOnCooldown() {
            return System.currentTimeMillis() - creationTime < COOLDOWN;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public UUID getOwner() {
            return owner;
        }
    }

}
