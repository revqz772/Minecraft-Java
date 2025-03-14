package me.revqz.bankplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BankPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, Integer> balances = new HashMap<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadBalances();
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("bank").setExecutor(new BankCommand());
        this.getCommand("balance").setExecutor(new BalanceCommand());
    }

    private void loadBalances() {
        if (config.contains("balances")) {
            for (String key : config.getConfigurationSection("balances").getKeys(false)) {
                balances.put(UUID.fromString(key), config.getInt("balances." + key));
            }
        }
    }

    private void saveBalances() {
        for (Map.Entry<UUID, Integer> entry : balances.entrySet()) {
            config.set("balances." + entry.getKey().toString(), entry.getValue());
        }
        saveConfig();
    }

    public class BankCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            Player player = (Player) sender;
            openBankGUI(player);
            return true;
        }
    }

    public class BalanceCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                sender.sendMessage(ChatColor.GOLD + "Your balance: " + ChatColor.AQUA + balances.getOrDefault(player.getUniqueId(), 0) + " diamonds.");
            } else {
                sender.sendMessage(ChatColor.RED + "Only players can check their own balance.");
            }
            return true;
        }
    }

    private void openBankGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.DARK_BLUE + "Bank Account");

        ItemStack deposit = new ItemStack(Material.EMERALD);
        ItemMeta depositMeta = deposit.getItemMeta();
        depositMeta.setDisplayName(ChatColor.GREEN + "Deposit Diamonds");
        deposit.setItemMeta(depositMeta);

        ItemStack withdraw = new ItemStack(Material.GOLD_INGOT);
        ItemMeta withdrawMeta = withdraw.getItemMeta();
        withdrawMeta.setDisplayName(ChatColor.RED + "Withdraw Diamonds");
        withdraw.setItemMeta(withdrawMeta);

        ItemStack balance = new ItemStack(Material.PAPER);
        ItemMeta balanceMeta = balance.getItemMeta();
        balanceMeta.setDisplayName(ChatColor.YELLOW + "Current Balance: " + ChatColor.AQUA + balances.getOrDefault(player.getUniqueId(), 0));
        balance.setItemMeta(balanceMeta);

        gui.setItem(2, deposit);
        gui.setItem(4, balance);
        gui.setItem(6, withdraw);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_BLUE + "Bank Account")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            UUID uuid = player.getUniqueId();
            
            if (event.getCurrentItem() == null) return;

            switch (event.getCurrentItem().getType()) {
                case EMERALD:
                    int diamonds = countDiamonds(player);
                    if (diamonds > 0) {
                        removeDiamonds(player, diamonds);
                        balances.put(uuid, balances.getOrDefault(uuid, 0) + diamonds);
                        saveBalances();
                        player.sendMessage(ChatColor.GREEN + "Deposited " + diamonds + " diamonds.");
                    } else {
                        player.sendMessage(ChatColor.RED + "You have no diamonds to deposit.");
                    }
                    break;
                case GOLD_INGOT:
                    int balance = balances.getOrDefault(uuid, 0);
                    if (balance > 0) {
                        player.getInventory().addItem(new ItemStack(Material.DIAMOND, balance));
                        balances.put(uuid, 0);
                        saveBalances();
                        player.sendMessage(ChatColor.GREEN + "Withdrew " + balance + " diamonds.");
                    } else {
                        player.sendMessage(ChatColor.RED + "You have no diamonds in your bank.");
                    }
                    break;
            }
        }
    }

    private int countDiamonds(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeDiamonds(Player player, int amount) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) {
                if (item.getAmount() > amount) {
                    item.setAmount(item.getAmount() - amount);
                    return;
                } else {
                    amount -= item.getAmount();
                    item.setAmount(0);
                }
                if (amount <= 0) return;
            }
        }
    }
}
