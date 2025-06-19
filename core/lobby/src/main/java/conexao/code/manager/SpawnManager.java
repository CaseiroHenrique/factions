package conexao.code.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public class SpawnManager {
    private static final String PATH = "spawn";

    private final Plugin plugin;
    private Location spawn;

    public SpawnManager(Plugin plugin) {
        this.plugin = plugin;
        loadSpawn();
    }

    private void loadSpawn() {
        if (plugin.getConfig().contains(PATH + ".world")) {
            String worldName = plugin.getConfig().getString(PATH + ".world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = plugin.getConfig().getDouble(PATH + ".x");
                double y = plugin.getConfig().getDouble(PATH + ".y");
                double z = plugin.getConfig().getDouble(PATH + ".z");
                float yaw = (float) plugin.getConfig().getDouble(PATH + ".yaw");
                float pitch = (float) plugin.getConfig().getDouble(PATH + ".pitch");
                spawn = new Location(world, x, y, z, yaw, pitch);
            }
        }
    }

    public void setSpawn(Location location) {
        spawn = location;
        plugin.getConfig().set(PATH + ".world", location.getWorld().getName());
        plugin.getConfig().set(PATH + ".x", location.getX());
        plugin.getConfig().set(PATH + ".y", location.getY());
        plugin.getConfig().set(PATH + ".z", location.getZ());
        plugin.getConfig().set(PATH + ".yaw", location.getYaw());
        plugin.getConfig().set(PATH + ".pitch", location.getPitch());
        plugin.saveConfig();
    }

    public Location getSpawn() {
        return spawn;
    }
}
