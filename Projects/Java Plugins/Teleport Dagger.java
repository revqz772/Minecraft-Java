package com.revoqz.teleportdagger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class TeleportDagger extends JavaPlugin implements Listener {

    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("givetpdagger") && sender instanceof Player) {
            Player player = (Player) sender;
            ItemStack sword = new ItemStack(Material.IRON_SWORD);
            ItemMeta meta = sword.getItemMeta();
            meta.setDisplayName("§bTeleport Dagger");
            meta.addEnchant(Enchantment.DAMAGE_ALL, 4, true);
            meta.setUnbreakable(true);
            sword.setItemMeta(meta);
            player.getInventory().addItem(sword);
        }
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon.getType() != Material.IRON_SWORD) return;
        if (weapon.getItemMeta() == null) return;
        if (!weapon.getItemMeta().getDisplayName().equals("§bTeleport Dagger")) return;

        UUID id = player.getUniqueId();
        if (cooldowns.containsKey(id) && System.currentTimeMillis() < cooldowns.get(id)) return;

        Entity target = null;
        double closest = 10.0;

        for (Entity e : player.getNearbyEntities(10, 10, 10)) {
            if (!(e instanceof LivingEntity) || e == player) continue;
            double distance = player.getLocation().distance(e.getLocation());
            if (distance < closest) {
                closest = distance;
                target = e;
            }
        }

        if (target != null) {
            player.teleport(target.getLocation().add(target.getLocation().getDirection().multiply(-1)).setDirection(player.getLocation().getDirection()));
            cooldowns.put(id, System.currentTimeMillis() + 15000);
        }
    }
}
