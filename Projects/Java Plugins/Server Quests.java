package com.revoqz.serverquests;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ServerQuests extends JavaPlugin implements Listener {

    private enum QuestType {
        ZOMBIE_KILLS,
        UNDERWATER_TIME
    }

    private QuestType currentQuest;
    private int target;
    private int progress;
    private final Set<UUID> underwaterTracking = new HashSet<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        startQuestScheduler();
    }

    private void startQuestScheduler() {
        startNewQuest();

        new BukkitRunnable() {
            @Override
            public void run() {
                startNewQuest();
            }
        }.runTaskTimer(this, 20 * 300, 20 * 300);
    }

    private void startNewQuest() {
        QuestType[] types = QuestType.values();
        currentQuest = types[new Random().nextInt(types.length)];
        progress = 0;
        target = (currentQuest == QuestType.ZOMBIE_KILLS) ? 10000 : 10000;
        Bukkit.broadcastMessage("§eNew Server Quest: " + getQuestDescription());
    }

    private String getQuestDescription() {
        return switch (currentQuest) {
            case ZOMBIE_KILLS -> "Kill 10,000 Zombies";
            case UNDERWATER_TIME -> "Stay underwater for 10,000 seconds (all players combined)";
        };
    }

    private void checkCompletion() {
        if (progress >= target) {
            Bukkit.broadcastMessage("§aServer Quest Completed! Reward given to all online players.");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.DIAMOND));
            }
            startNewQuest();
        }
    }

    @EventHandler
    public void onZombieKill(EntityDeathEvent event) {
        if (currentQuest != QuestType.ZOMBIE_KILLS) return;
        if (!(event.getEntity() instanceof Zombie)) return;
        if (event.getEntity().getKiller() == null) return;
        progress++;
        if (progress % 100 == 0)
            Bukkit.broadcastMessage("§bQuest Progress: " + progress + " / " + target);
        checkCompletion();
    }

    @EventHandler
    public void onUnderwater(PlayerMoveEvent event) {
        if (currentQuest != QuestType.UNDERWATER_TIME) return;
        Player player = event.getPlayer();
        Material blockType = player.getLocation().getBlock().getType();
        boolean inWater = blockType == Material.WATER || blockType == Material.BUBBLE_COLUMN;

        if (inWater) {
            if (!underwaterTracking.contains(player.getUniqueId())) {
                underwaterTracking.add(player.getUniqueId());
                new BukkitRunnable() {
                    int time = 0;
                    public void run() {
                        if (!player.isOnline() || !underwaterTracking.contains(player.getUniqueId())) {
                            cancel();
                            return;
                        }
                        Material current = player.getLocation().getBlock().getType();
                        if (current == Material.WATER || current == Material.BUBBLE_COLUMN) {
                            time++;
                            progress++;
                            if (progress % 100 == 0)
                                Bukkit.broadcastMessage("§bQuest Progress: " + progress + " / " + target);
                            checkCompletion();
                        } else {
                            underwaterTracking.remove(player.getUniqueId());
                            cancel();
                        }
                    }
                }.runTaskTimer(this, 20, 20);
            }
        } else {
            underwaterTracking.remove(player.getUniqueId());
        }
    }
}
