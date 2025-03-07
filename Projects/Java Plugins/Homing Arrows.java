package me.revqz.homingarrows;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class HomingArrows extends JavaPlugin implements Listener {
    private final Map<Arrow, LivingEntity> trackingArrows = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onArrowLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Arrow arrow = (Arrow) event.getEntity();
        Player shooter = (Player) arrow.getShooter();

        LivingEntity nearest = null;
        double nearestDistance = 10.0;
        for (LivingEntity entity : shooter.getWorld().getLivingEntities()) {
            if (entity == shooter) continue;
            double distance = entity.getLocation().distance(shooter.getLocation());
            if (distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        
        if (nearest != null) {
            trackingArrows.put(arrow, nearest);
            shooter.sendMessage("§aHoming arrow activated!");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (arrow.isDead() || !trackingArrows.containsKey(arrow)) {
                        cancel();
                        return;
                    }
                    
                    Location targetLoc = nearest.getLocation().add(0, 1, 0);
                    arrow.setVelocity(targetLoc.toVector().subtract(arrow.getLocation().toVector()).normalize().multiply(1.5));
                    arrow.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, arrow.getLocation(), 5);
                }
            }.runTaskTimer(this, 1L, 1L);
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            trackingArrows.remove(event.getEntity());
            event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);
        }
    }
}
