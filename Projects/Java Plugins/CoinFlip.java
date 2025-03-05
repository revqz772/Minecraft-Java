package me.revqz.coinflip;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CoinFlipPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, CoinFlip> coinFlips = new HashMap<>();
    private BankPlugin bankPlugin;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.bankPlugin = (BankPlugin) getServer().getPluginManager().getPlugin("BankPlugin");
        getLogger().info("CoinFlipPlugin has been enabled!");
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("coinflip")) {
            if (args.length == 0) {
                openCoinFlipGUI(player);
                return true;
            }
            
            if (args[0].equalsIgnoreCase("create")) {
                if (args.length < 2) {
                    player.sendMessage("Usage: /coinflip create <amount>");
                    return true;
                }

                if (coinFlips.containsKey(player.getUniqueId())) {
                    player.sendMessage("You already have an active coin flip. Use /coinflip delete to remove it.");
                    return true;
                }

                try {
                    int amount = Integer.parseInt(args[1]);
                    if (amount <= 0) {
                        player.sendMessage("Amount must be greater than zero.");
                        return true;
                    }
                    
                    int balance = bankPlugin.getBalance(player);
                    if (balance < amount) {
                        player.sendMessage("You do not have enough diamonds in the bank to place this bet.");
                        return true;
                    }

                    bankPlugin.withdraw(player, amount);
                    coinFlips.put(player.getUniqueId(), new CoinFlip(player, amount));
                    player.sendMessage("Coin flip created for " + amount + " diamonds!");
                } catch (NumberFormatException e) {
                    player.sendMessage("Invalid amount. Please provide a number.");
                }

                return true;
            }
        }
        return false;
    }

    private void openCoinFlipGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "CoinFlip Arena");
        int slot = 0;
        
        for (CoinFlip coinFlip : coinFlips.values()) {
            if (slot >= 27) break;
            ItemStack skull = createPlayerHead(coinFlip.getOwner());
            gui.setItem(slot, skull);
            slot++;
        }
        
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("CoinFlip Arena")) return;
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() != Material.PLAYER_HEAD) return;
        
        CoinFlip coinFlip = getCoinFlipFromItem(clickedItem);
        if (coinFlip == null || coinFlip.getOwner().equals(player)) return;

        startCoinFlip(coinFlip, player);
    }

    private CoinFlip getCoinFlipFromItem(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return null;
        
        String name = item.getItemMeta().getDisplayName();
        for (CoinFlip coinFlip : coinFlips.values()) {
            if (name.contains(coinFlip.getOwner().getName())) {
                return coinFlip;
            }
        }
        return null;
    }

    private void startCoinFlip(CoinFlip coinFlip, Player challenger) {
        Player owner = coinFlip.getOwner();
        int amount = coinFlip.getAmount();
        
        boolean ownerWins = ThreadLocalRandom.current().nextBoolean();
        Player winner = ownerWins ? owner : challenger;
        Player loser = ownerWins ? challenger : owner;
        
        bankPlugin.deposit(winner, amount * 2);
        coinFlips.remove(owner.getUniqueId());
        
        owner.sendMessage("CoinFlip: You " + (ownerWins ? "won" : "lost") + " " + amount + " diamonds!");
        challenger.sendMessage("CoinFlip: You " + (!ownerWins ? "won" : "lost") + " " + amount + " diamonds!");
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(player.getName());
            head.setItemMeta(meta);
        }
        return head;
    }

    public static class CoinFlip {
        private final Player owner;
        private final int amount;

        public CoinFlip(Player owner, int amount) {
            this.owner = owner;
            this.amount = amount;
        }

        public Player getOwner() {
            return owner;
        }

        public int getAmount() {
            return amount;
        }
    }
}
