package me.revqz.reportPlugin;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class ReportPlugin extends JavaPlugin implements Listener {
    private File reportsFile;
    private FileConfiguration reportsConfig;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("report").setExecutor(new ReportCommand());
        getCommand("reportlist").setExecutor(new ReportListCommand());
        loadReports();
        cleanOldReports();
    }

    private void loadReports() {
        reportsFile = new File(getDataFolder(), "reports.yml");
        if (!reportsFile.exists()) {
            saveResource("reports.yml", false);
        }
        reportsConfig = YamlConfiguration.loadConfiguration(reportsFile);
    }

    private void saveReports() {
        try {
            reportsConfig.save(reportsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cleanOldReports() {
        long cutoff = Instant.now().getEpochSecond() - 86400; // 24 hours ago
        List<String> toRemove = new ArrayList<>();
        
        for (String key : reportsConfig.getKeys(false)) {
            if (reportsConfig.getLong(key + ".timestamp") < cutoff) {
                toRemove.add(key);
            }
        }
        
        for (String key : toRemove) {
            reportsConfig.set(key, null);
        }
        saveReports();
    }

    private class ReportCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /report <player>");
                return true;
            }

            Player reportedPlayer = Bukkit.getPlayer(args[0]);
            if (reportedPlayer == null || !reportedPlayer.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Player not found or offline.");
                return true;
            }

            openReportGUI((Player) sender, reportedPlayer.getName());
            return true;
        }
    }

    private void openReportGUI(Player player, String reportedPlayer) {
        Inventory gui = Bukkit.createInventory(player, 27, "Report " + reportedPlayer);
        String[] categories = {"Cheating", "Spamming", "Discrimination", "Boosting", "Bug Abuse", "Filter Bypass", "Death Wishes"};
        for (int i = 0; i < categories.length; i++) {
            gui.setItem(i, createEnchantedBook(categories[i]));
        }
        player.openInventory(gui);
    }

    private ItemStack createEnchantedBook(String category) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + category);
            meta.addEnchant(Enchantment.LURE, 1, false);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Report reason:", ChatColor.GREEN + category));
            book.setItemMeta(meta);
        }
        return book;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Report ")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() != Material.ENCHANTED_BOOK) return;
            
            String category = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            Player reporter = (Player) event.getWhoClicked();
            String reportedPlayer = event.getView().getTitle().replace("Report ", "");
            
            long timestamp = Instant.now().getEpochSecond();
            reportsConfig.set(UUID.randomUUID().toString(), Map.of(
                "reporter", reporter.getName(),
                "reported", reportedPlayer,
                "reason", category,
                "timestamp", timestamp
            ));
            saveReports();
            
            reporter.sendMessage(ChatColor.GREEN + "Report submitted: " + reportedPlayer + " for " + category);
            Bukkit.getLogger().info(reporter.getName() + " reported " + reportedPlayer + " for " + category);
            reporter.closeInventory();
        }
    }

    private class ReportListCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            Player player = (Player) sender;
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null || !user.getCachedData().getPermissionData().checkPermission("report.view").asBoolean()) {
                player.sendMessage(ChatColor.RED + "You do not have permission to view reports.");
                return true;
            }
            
            player.sendMessage(ChatColor.GOLD + "Recent Reports (Last 24 Hours):");
            long cutoff = Instant.now().getEpochSecond() - 86400;
            for (String key : reportsConfig.getKeys(false)) {
                long timestamp = reportsConfig.getLong(key + ".timestamp");
                if (timestamp >= cutoff) {
                    String reporter = reportsConfig.getString(key + ".reporter");
                    String reported = reportsConfig.getString(key + ".reported");
                    String reason = reportsConfig.getString(key + ".reason");
                    player.sendMessage(ChatColor.YELLOW + "- " + reported + " reported by " + reporter + " for " + reason);
                }
            }
            return true;
        }
    }
}
