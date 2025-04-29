package me.revoqz.bloodsword;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class BloodthirstSword extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("givebloodsword") && sender instanceof Player) {
            Player player = (Player) sender;
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            ItemMeta meta = sword.getItemMeta();
            meta.setDisplayName("§cBloodthirst Sword");
            meta.addEnchant(Enchantment.DAMAGE_ALL, 5, true);
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
        if (weapon.getType() != Material.NETHERITE_SWORD) return;
        if (weapon.getItemMeta() == null) return;
        if (!weapon.getItemMeta().getDisplayName().equals("§cBloodthirst Sword")) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        double damage = event.getDamage();
        double healAmount = damage * 0.2;
        double newHealth = Math.min(attacker.getHealth() + healAmount, attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        attacker.setHealth(newHealth);
    }
}
