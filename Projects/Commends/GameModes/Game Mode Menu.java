package me.revoqz.gamemodemenur;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class GamemodeEnhancer extends JavaPlugin implements Listener {

    private final Map<String, GameMode> worldGamemodes = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadGamemodeConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void loadGamemodeConfig() {
        FileConfiguration config = getConfig();
        if (config.isConfigurationSection("world-gamemodes")) {
            for (String world : config.getConfigurationSection("world-gamemodes").getKeys(false)) {
                try {
                    GameMode mode = GameMode.valueOf(config.getString("world-gamemodes." + world).toUpperCase());
                    worldGamemodes.put(world, mode);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gamemenu") && sender instanceof Player) {
            Player player = (Player) sender;
            Inventory gui = Bukkit.createInventory(null, InventoryType.HOPPER, "§6Gamemode Selector");

            gui.setItem(0, createItem(Material.IRON_SWORD, "§aSurvival"));
            gui.setItem(1, createItem(Material.GRASS_BLOCK, "§bCreative"));
            gui.setItem(2, createItem(Material.MAP, "§eAdventure"));
            gui.setItem(3, createItem(Material.ENDER_EYE, "§cSpectator"));

            player.openInventory(gui);
        }
        return true;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String world = player.getWorld().getName();
        if (worldGamemodes.containsKey(world)) {
            player.setGameMode(worldGamemodes.get(world));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals("§6Gamemode Selector")) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String name = clicked.getItemMeta().getDisplayName();
        switch (name) {
            case "§aSurvival" -> player.setGameMode(GameMode.SURVIVAL);
            case "§bCreative" -> player.setGameMode(GameMode.CREATIVE);
            case "§eAdventure" -> player.setGameMode(GameMode.ADVENTURE);
            case "§cSpectator" -> player.setGameMode(GameMode.SPECTATOR);
        }
        player.closeInventory();
    }
}
