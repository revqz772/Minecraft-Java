package me.revoqz.heartstealer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class HeartStealer extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("giveheartstealer") && sender instanceof Player) {
            Player player = (Player) sender;
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            ItemMeta meta = sword.getItemMeta();
            meta.setDisplayName("§4Heart Stealer");
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
        if (!weapon.getItemMeta().getDisplayName().equals("§4Heart Stealer")) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity target = (LivingEntity) event.getEntity();
        double hearts = target.getHealth() / 10.0;
        attacker.setAbsorptionAmount(Math.min(attacker.getAbsorptionAmount() + hearts * 2.0, 20.0));
    }

    @EventHandler
    public void onKill(PlayerDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) return;
        Player killer = event.getEntity().getKiller();
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon.getType() != Material.NETHERITE_SWORD) return;
        if (weapon.getItemMeta() == null) return;
        if (!weapon.getItemMeta().getDisplayName().equals("§4Heart Stealer")) return;

        Player victim = event.getEntity();
        AttributeInstance killerHealth = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance victimHealth = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (killerHealth.getBaseValue() < 40.0)
            killerHealth.setBaseValue(killerHealth.getBaseValue() + 2.0);
        if (victimHealth.getBaseValue() > 4.0)
            victimHealth.setBaseValue(victimHealth.getBaseValue() - 2.0);
    }
}
