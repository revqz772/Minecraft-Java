package me.revqz.bountyplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

public class BountyPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, Double> bounties = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        this.getCommand("setbounty").setExecutor(new SetBountyCommand());
        this.getCommand("bountylist").setExecutor(new BountyListCommand());
    }

    public class SetBountyCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /setbounty <player> <amount>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount.");
                return true;
            }

            bounties.put(target.getUniqueId(), amount);
            sender.sendMessage(ChatColor.GREEN + "Bounty set on " + target.getName() + " for $" + amount);
            return true;
        }
    }

    public class BountyListCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            openBountyListGUI((Player) sender);
            return true;
        }
    }

    private void openBountyListGUI(Player player) {
        int size = ((bounties.size() - 1) / 9 + 1) * 9; 
        Inventory gui = Bukkit.createInventory(null, size, "Active Bounties");

        for (Map.Entry<UUID, Double> entry : bounties.entrySet()) {
            Player target = Bukkit.getPlayer(entry.getKey());
            if (target != null) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(target);
                    meta.setDisplayName(ChatColor.WHITE + target.getName());
                    meta.setLore(Collections.singletonList(ChatColor.GREEN + "Bounty: $" + entry.getValue()));
                    skull.setItemMeta(meta);
                }
                gui.addItem(skull);
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Active Bounties")) {
            event.setCancelled(true); // Prevent item interaction
        }
    }
}
