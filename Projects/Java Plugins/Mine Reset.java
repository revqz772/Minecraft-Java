package me.revqz.miningplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.Location;
import java.util.Random;

public class MineRegenerator extends JavaPlugin implements CommandExecutor {

    private Location mineStart;
    private int mineSize;
    private final Material[] mineBlocks = {Material.STONE, Material.COAL_ORE, Material.IRON_ORE, Material.DIAMOND_ORE};
    private final Random random = new Random();

    @Override
    public void onEnable() {
        this.getCommand("minecreate").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length != 1) {
            player.sendMessage("Usage: /minecreate <size>");
            return true;
        }
        
        try {
            mineSize = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("Invalid size! Use a number.");
            return true;
        }
        
        mineStart = player.getLocation();
        generateMine();
        startMineRegeneration();
        
        player.sendMessage("Mine created at your location! It will regenerate every 30 minutes.");
        return true;
    }

    private void generateMine() {
        World world = mineStart.getWorld();
        
        for (int x = 0; x < mineSize; x++) {
            for (int y = 0; y < mineSize / 2; y++) {
                for (int z = 0; z < mineSize; z++) {
                    Block block = world.getBlockAt(mineStart.clone().add(x, -y, z));
                    block.setType(mineBlocks[random.nextInt(mineBlocks.length)]);
                }
            }
        }
    }
    
    private void startMineRegeneration() {
        new BukkitRunnable() {
            @Override
            public void run() {
                generateMine();
                Bukkit.broadcastMessage("The mine has been regenerated!");
            }
        }.runTaskTimer(this, 20L * 60L * 30L, 20L * 60L * 30L); 
    }
}
