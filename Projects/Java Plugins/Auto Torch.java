package me.revqz.autotorch;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.UUID;

public class AutoTorchPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private final HashSet<UUID> autoTorchEnabled = new HashSet<>();
    private final HashSet<UUID> cooldowns = new HashSet<>();
    private int minLightLevel;
    private int torchCooldown;
    private boolean notifyOnPlace;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getCommand("autotorch").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("autotorch.use")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        UUID uuid = player.getUniqueId();
        
        if (autoTorchEnabled.contains(uuid)) {
            autoTorchEnabled.remove(uuid);
            player.sendMessage("§cAuto Torch disabled.");
        } else {
            autoTorchEnabled.add(uuid);
            player.sendMessage("§aAuto Torch enabled. Walking in dark areas will place torches.");
        }
        return true;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (!autoTorchEnabled.contains(uuid)) return;
        if (cooldowns.contains(uuid)) return;
        
        Block block = player.getLocation().getBlock();
        Block floor = block.getRelative(BlockFace.DOWN);
        
        if (block.getLightLevel() < minLightLevel && isValidTorchLocation(floor)) {
            ItemStack torch = new ItemStack(Material.TORCH);
            if (player.getInventory().containsAtLeast(torch, 1)) {
                floor.setType(Material.TORCH);
                player.getInventory().removeItem(torch);
                
                if (notifyOnPlace) {
                    player.sendMessage("§eTorch placed automatically!");
                }
                
                cooldowns.add(uuid);
                getServer().getScheduler().runTaskLater(this, () -> cooldowns.remove(uuid), torchCooldown * 20L);
            }
        }
    }

    private boolean isValidTorchLocation(Block block) {
        return block.getType().isSolid() && 
               block.getType() != Material.ICE && 
               block.getType() != Material.PACKED_ICE &&
               block.getType() != Material.BARRIER &&
               block.getRelative(BlockFace.UP).getType() == Material.AIR;
    }
}
