package conexao.code.hologram;

import conexao.code.manager.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class HologramManager implements Listener {
    private final Plugin plugin;
    private final ScoreboardManager scoreboardManager;
    private final ItemStack item;
    private final String server;
    private final double offsetY;
    private final boolean smallItem;
    private final String nameColor;
    private final String countColor;
    private Location location;

    private ArmorStand itemStand;
    private ArmorStand nameStand;
    private ArmorStand countStand;
    private BukkitTask task;

    public HologramManager(Plugin plugin, ScoreboardManager scoreboardManager, ItemStack item,
                           String server, double offsetY, boolean smallItem,
                           String nameColor, String countColor, Location loc) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
        this.item = item;
        this.server = server;
        this.offsetY = offsetY;
        this.smallItem = smallItem;
        this.nameColor = nameColor;
        this.countColor = countColor;
        this.location = loc;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (loc != null) spawn();
    }

    public void setLocation(Location loc) {
        remove();
        this.location = loc;
        if (loc != null) spawn();
    }

    public void remove() {
        if (task != null) task.cancel();
        if (itemStand != null) itemStand.remove();
        if (nameStand != null) nameStand.remove();
        if (countStand != null) countStand.remove();
        task = null;
        itemStand = nameStand = countStand = null;
    }

    private void spawn() {
        Location base = location.clone().add(0, offsetY, 0);
        itemStand = spawnStand(base, false);
        itemStand.setSmall(smallItem);
        itemStand.getEquipment().setItem(EquipmentSlot.HEAD, item);

        nameStand = spawnStand(base.clone().add(0, 0.4, 0), true);
        nameStand.setCustomName(ChatColor.translateAlternateColorCodes('&', nameColor + server));

        countStand = spawnStand(base.clone().add(0, -0.4, 0), true);

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0L, 20L);
    }

    private ArmorStand spawnStand(Location loc, boolean customName) {
        World world = loc.getWorld();
        ArmorStand as = world.spawn(loc, ArmorStand.class);
        as.setInvisible(true);
        as.setMarker(true);
        as.setGravity(false);
        as.setCustomNameVisible(customName);
        return as;
    }

    private void update() {
        Location base = location.clone().add(0, offsetY, 0);
        itemStand.teleport(base);
        itemStand.setRotation(itemStand.getLocation().getYaw() + 5f, 0f);
        nameStand.teleport(base.clone().add(0, 0.4, 0));
        countStand.teleport(base.clone().add(0, -0.4, 0));
        int count = scoreboardManager.getPlayerCount(server);
        scoreboardManager.requestPlayerCount(server);
        countStand.setCustomName(ChatColor.translateAlternateColorCodes('&', countColor + count + " jogadores"));
    }
}
