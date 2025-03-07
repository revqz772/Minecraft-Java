package me.revqz.playervaults;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerVaultsPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, Map<Integer, Inventory>> playerVaults = new HashMap<>();
    private BankPlugin bankPlugin;
    private final int VAULT_COST = 10; 
    private final int MAX_VAULTS = 10; 
    private File vaultsFile;
    private FileConfiguration vaultsConfig;
    private final Map<UUID, Long> lastVaultAccess = new HashMap<>();
    private final long VAULT_COOLDOWN = 1000; // 1 second cooldown between vault access

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.bankPlugin = (BankPlugin) getServer().getPluginManager().getPlugin("BankPlugin");
        
        // Load vaults data
        loadVaultsData();
        
        // Register commands with tab completion
        getCommand("pv").setExecutor(this);
        getCommand("pvupgrade").setExecutor(this);
        
        // Schedule auto-save task
        Bukkit.getScheduler().runTaskTimer(this, this::saveVaultsData, 6000L, 6000L); // Save every 5 minutes
        
        getLogger().info("PlayerVaults Plugin enabled!");
    }

    @Override
    public void onDisable() {
        saveVaultsData();
        getLogger().info("PlayerVaults Plugin disabled - All vaults saved!");
    }

    private void loadVaultsData() {
        vaultsFile = new File(getDataFolder(), "vaults.yml");
        if (!vaultsFile.exists()) {
            saveResource("vaults.yml", false);
        }
        vaultsConfig = YamlConfiguration.loadConfiguration(vaultsFile);
        
        if (vaultsConfig.contains("vaults")) {
            for (String uuidStr : vaultsConfig.getConfigurationSection("vaults").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Map<Integer, Inventory> playerVaultMap = new HashMap<>();
                
                for (String vaultNumStr : vaultsConfig.getConfigurationSection("vaults." + uuidStr).getKeys(false)) {
                    int vaultNum = Integer.parseInt(vaultNumStr);
                    Inventory vault = Bukkit.createInventory(null, 54, formatTitle("Player Vault " + vaultNum));
                    
                    @SuppressWarnings("unchecked")
                    List<ItemStack> items = (List<ItemStack>) vaultsConfig.getList("vaults." + uuidStr + "." + vaultNumStr);
                    if (items != null) {
                        vault.setContents(items.toArray(new ItemStack[0]));
                    }
                    
                    playerVaultMap.put(vaultNum, vault);
                }
                playerVaults.put(uuid, playerVaultMap);
            }
        }
    }

    private void saveVaultsData() {
        try {
            for (Map.Entry<UUID, Map<Integer, Inventory>> entry : playerVaults.entrySet()) {
                String uuidStr = entry.getKey().toString();
                
                for (Map.Entry<Integer, Inventory> vaultEntry : entry.getValue().entrySet()) {
                    List<ItemStack> items = new ArrayList<>();
                    for (ItemStack item : vaultEntry.getValue().getContents()) {
                        if (item != null && item.getType() != Material.AIR) {
                            items.add(item.clone());
                        }
                    }
                    vaultsConfig.set("vaults." + uuidStr + "." + vaultEntry.getKey(), items);
                }
            }
            
            vaultsConfig.save(vaultsFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save vaults data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String formatTitle(String title) {
        return ChatColor.DARK_PURPLE + title;
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // Check for cooldown
        if (isOnCooldown(player)) {
            player.sendMessage(ChatColor.RED + "Please wait before using vaults again!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("pv")) {
            if (args.length == 1) {
                try {
                    int vaultNumber = Integer.parseInt(args[0]);
                    if (vaultNumber <= 0 || vaultNumber > MAX_VAULTS) {
                        player.sendMessage(ChatColor.RED + "Invalid vault number (1-" + MAX_VAULTS + ")");
                        return true;
                    }
                    openPlayerVault(player, vaultNumber);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid vault number.");
                }
                return true;
            }
            showAvailableVaults(player);
            return true;
        }
        
        if (command.getName().equalsIgnoreCase("pvupgrade")) {
            openUpgradeGUI(player);
            return true;
        }

        return false;
    }

    private boolean isOnCooldown(Player player) {
        long lastAccess = lastVaultAccess.getOrDefault(player.getUniqueId(), 0L);
        if (System.currentTimeMillis() - lastAccess < VAULT_COOLDOWN) {
            return true;
        }
        lastVaultAccess.put(player.getUniqueId(), System.currentTimeMillis());
        return false;
    }

    private void showAvailableVaults(Player player) {
        Map<Integer, Inventory> vaults = playerVaults.getOrDefault(player.getUniqueId(), new HashMap<>());
        player.sendMessage(ChatColor.GOLD + "Your Vaults " + ChatColor.YELLOW + "(" + vaults.size() + "/" + MAX_VAULTS + ")");
        player.sendMessage(ChatColor.GRAY + "Use /pv <number> to access a vault");
        
        for (int vaultNum : vaults.keySet()) {
            player.sendMessage(ChatColor.YELLOW + "➤ Vault " + vaultNum);
        }
        
        if (vaults.size() < MAX_VAULTS) {
            player.sendMessage(ChatColor.GREEN + "Use /pvupgrade to get more vaults!");
        }
    }

    private void openPlayerVault(Player player, int vaultNumber) {
        UUID playerUUID = player.getUniqueId();
        playerVaults.putIfAbsent(playerUUID, new HashMap<>());
        
        Map<Integer, Inventory> vaults = playerVaults.get(playerUUID);
        
        if (!vaults.containsKey(vaultNumber)) {
            player.sendMessage(ChatColor.RED + "You don't have access to this vault!");
            player.sendMessage(ChatColor.YELLOW + "Use /pvupgrade to unlock more vaults.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }
        
        player.openInventory(vaults.get(vaultNumber));
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    private void openUpgradeGUI(Player player) {
        Inventory upgradeGUI = Bukkit.createInventory(null, 27, formatTitle("Vault Upgrades"));
        
        Map<Integer, Inventory> vaults = playerVaults.getOrDefault(player.getUniqueId(), new HashMap<>());
        int currentVaults = vaults.size();
        
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.GOLD + "Vault Information");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Current vaults: " + ChatColor.YELLOW + currentVaults + "/" + MAX_VAULTS);
            lore.add(ChatColor.GRAY + "Cost per vault: " + ChatColor.AQUA + VAULT_COST + " diamonds");
            infoMeta.setLore(lore);
            infoItem.setItemMeta(infoMeta);
        }
        upgradeGUI.setItem(11, infoItem);
        
        if (currentVaults < MAX_VAULTS) {
            ItemStack upgradeItem = new ItemStack(Material.DIAMOND);
            ItemMeta meta = upgradeItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + "Purchase New Vault");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Cost: " + ChatColor.AQUA + VAULT_COST + " diamonds");
                lore.add(ChatColor.GRAY + "Click to purchase!");
                meta.setLore(lore);
                upgradeItem.setItemMeta(meta);
            }
            upgradeGUI.setItem(15, upgradeItem);
        } else {
            ItemStack maxItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = maxItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "Maximum Vaults Reached");
                maxItem.setItemMeta(meta);
            }
            upgradeGUI.setItem(15, maxItem);
        }
        
        player.openInventory(upgradeGUI);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.2f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(formatTitle("Vault Upgrades"))) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.DIAMOND) {
                Map<Integer, Inventory> vaults = playerVaults.getOrDefault(player.getUniqueId(), new HashMap<>());
                
                if (vaults.size() >= MAX_VAULTS) {
                    player.sendMessage(ChatColor.RED + "You have reached the maximum number of vaults!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    return;
                }
                
                int balance = bankPlugin.getBalance(player);
                
                if (balance >= VAULT_COST) {
                    bankPlugin.withdraw(player, VAULT_COST);
                    addVaultToPlayer(player);
                    player.sendMessage(ChatColor.GREEN + "You have unlocked a new vault!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    player.closeInventory();
                } else {
                    player.sendMessage(ChatColor.RED + "You need " + VAULT_COST + " diamonds to upgrade!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("Player Vault")) {
            saveVaultsData();
            Player player = (Player) event.getPlayer();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
        }
    }

    private void addVaultToPlayer(Player player) {
        UUID playerUUID = player.getUniqueId();
        playerVaults.putIfAbsent(playerUUID, new HashMap<>());
        
        int newVaultNumber = playerVaults.get(playerUUID).size() + 1;
        Inventory newVault = Bukkit.createInventory(null, 54, formatTitle("Player Vault " + newVaultNumber));
        playerVaults.get(playerUUID).put(newVaultNumber, newVault);
        saveVaultsData();
    }
}
