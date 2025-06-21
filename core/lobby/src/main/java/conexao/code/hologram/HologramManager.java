package conexao.code.hologram;

import conexao.code.manager.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class HologramManager implements Listener {
    private final Plugin plugin;
    private final ScoreboardManager scoreboardManager;
    private final ItemStack item;
    private final String server;
    private final double offsetY;
    private final Map<Player, Hologram> active = new HashMap<>();

    public HologramManager(Plugin plugin, ScoreboardManager scoreboardManager, ItemStack item, String server, double offsetY) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
        this.item = item;
        this.server = server;
        this.offsetY = offsetY;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void removeAll() {
        for (Hologram h : active.values()) {
            h.destroy();
        }
        active.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        spawnFor(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        remove(e.getPlayer());
    }

    private void spawnFor(Player player) {
        Hologram h = new Hologram(player);
        active.put(player, h);
    }

    private void remove(Player player) {
        Hologram h = active.remove(player);
        if (h != null) {
            h.destroy();
        }
    }

    private class Hologram {
        private final Player player;
        private final ArmorStand itemStand;
        private final ArmorStand nameStand;
        private final ArmorStand countStand;
        private final BukkitTask task;

        Hologram(Player p) {
            this.player = p;
            Location loc = p.getLocation().add(0, offsetY, 0);
            itemStand = spawn(loc, false);
            itemStand.getEquipment().setItem(EquipmentSlot.HEAD, item);

            nameStand = spawn(loc.clone().add(0, 0.4, 0), true);
            nameStand.setCustomName(ChatColor.translateAlternateColorCodes('&', server));

            countStand = spawn(loc.clone().add(0, -0.4, 0), true);

            task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0L, 2L);
        }

        private ArmorStand spawn(Location loc, boolean customName) {
            ArmorStand as = loc.getWorld().spawn(loc, ArmorStand.class);
            as.setInvisible(true);
            as.setMarker(true);
            as.setGravity(false);
            as.setCustomNameVisible(customName);
            return as;
        }

        private void update() {
            Location base = player.getLocation().add(0, offsetY, 0);
            itemStand.teleport(base);
            itemStand.setRotation(itemStand.getLocation().getYaw() + 5f, 0f);
            nameStand.teleport(base.clone().add(0, 0.4, 0));
            countStand.teleport(base.clone().add(0, -0.4, 0));
            int count = scoreboardManager.getPlayerCount(server);
            scoreboardManager.requestPlayerCount(server);
            countStand.setCustomName(ChatColor.YELLOW.toString() + count + " jogadores");
        }

        private void destroy() {
            task.cancel();
            itemStand.remove();
            nameStand.remove();
            countStand.remove();
        }
    }
}
