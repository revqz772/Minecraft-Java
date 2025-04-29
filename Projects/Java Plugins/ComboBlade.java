package me.revoqz.comboblade;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class ComboBlade extends JavaPlugin implements Listener {

    private final HashMap<UUID, Integer> comboMap = new HashMap<>();
    private final HashMap<UUID, BukkitRunnable> resetTasks = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("givecomboblade") && sender instanceof Player) {
            Player player = (Player) sender;
            ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
            ItemMeta meta = sword.getItemMeta();
            meta.setDisplayName("§eCombo Blade");
            meta.addEnchant(Enchantment.DAMAGE_ALL, 3, true);
            meta.setUnbreakable(true);
            sword.setItemMeta(meta);
            player.getInventory().addItem(sword);
        }
        return true;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon.getType() != Material.DIAMOND_SWORD) return;
        if (weapon.getItemMeta() == null) return;
        if (!weapon.getItemMeta().getDisplayName().equals("§eCombo Blade")) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        UUID id = attacker.getUniqueId();
        int combo = comboMap.getOrDefault(id, 0) + 1;
        comboMap.put(id, combo);
        event.setDamage(event.getDamage() + combo);

        if (resetTasks.containsKey(id)) resetTasks.get(id).cancel();

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                comboMap.remove(id);
                resetTasks.remove(id);
            }
        };
        task.runTaskLater(this, 40L);
        resetTasks.put(id, task);
    }
}
