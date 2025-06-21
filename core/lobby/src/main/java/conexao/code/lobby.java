// src/main/java/conexao/code/LobbyPlugin.java
package conexao.code;

import conexao.code.commands.ReloadCommand;
import conexao.code.commands.SetSpawnCommand;
import conexao.code.commands.ChangePasswordCommand;
import conexao.code.commands.SetHologramCommand;
import conexao.code.common.DatabaseManager;
import conexao.code.manager.ScoreboardManager;
import conexao.code.manager.TabListManager;
import conexao.code.manager.SpawnManager;
import conexao.code.listeners.LobbySettingsListener;
import conexao.code.menu.ServerSelectorMenu;
import conexao.code.hologram.HologramManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class lobby extends JavaPlugin implements Listener {
    private ScoreboardManager scoreboardManager;
    private TabListManager tabListManager;
    private SpawnManager spawnManager;
    private ServerSelectorMenu selectorMenu;
    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String host     = getConfig().getString("mysql.host");
        int    port     = getConfig().getInt("mysql.port");
        String database = getConfig().getString("mysql.database");
        String user     = getConfig().getString("mysql.user");
        String pass     = getConfig().getString("mysql.password");
        DatabaseManager.init(host, port, database, user, pass);
        scoreboardManager = new ScoreboardManager(this);
        scoreboardManager.start();
        tabListManager = new TabListManager(this);
        tabListManager.applyAll();
        spawnManager = new SpawnManager(this);
        selectorMenu = new ServerSelectorMenu(this, scoreboardManager);
        if (getConfig().getBoolean("hologram.enabled", false)) {
            String serverName = getConfig().getString("hologram.server", "factions");
            String itemName = getConfig().getString("hologram.item", "DIAMOND_SWORD");
            double offset = getConfig().getDouble("hologram.offset-y", 2.0);
            boolean smallItem = getConfig().getBoolean("hologram.small-item", false);
            String nameColor = getConfig().getString("hologram.name-color", "&e");
            String countColor = getConfig().getString("hologram.count-color", "&e");
            org.bukkit.Material mat = org.bukkit.Material.matchMaterial(itemName);
            org.bukkit.inventory.ItemStack stack = mat != null ? new org.bukkit.inventory.ItemStack(mat) : new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD);
            org.bukkit.Location holoLoc = null;
            if (getConfig().contains("hologram.world")) {
                org.bukkit.World w = org.bukkit.Bukkit.getWorld(getConfig().getString("hologram.world"));
                if (w != null) {
                    double hx = getConfig().getDouble("hologram.x");
                    double hy = getConfig().getDouble("hologram.y");
                    double hz = getConfig().getDouble("hologram.z");
                    holoLoc = new org.bukkit.Location(w, hx, hy, hz);
                }
            }
            hologramManager = new HologramManager(this, scoreboardManager, stack, serverName, offset, smallItem, nameColor, countColor, holoLoc);
        }
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", scoreboardManager);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new LobbySettingsListener(), this);
        Bukkit.getPluginManager().registerEvents(selectorMenu, this);
        getCommand("recarregar").setExecutor(new ReloadCommand(this, scoreboardManager, tabListManager));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnManager));
        getCommand("trocarsenha").setExecutor(new ChangePasswordCommand(this));
        getCommand("sethologram").setExecutor(new SetHologramCommand(this, hologramManager));
    }

    @Override
    public void onDisable() {
        scoreboardManager.stop();
        if (hologramManager != null) {
            hologramManager.remove();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
        e.getPlayer().setScoreboard(scoreboardManager.getBoard());
        tabListManager.applyAll();
        if (spawnManager.getSpawn() != null) {
            e.getPlayer().teleport(spawnManager.getSpawn());
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if (spawnManager.getSpawn() != null) {
            e.setRespawnLocation(spawnManager.getSpawn());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        String message = e.getMessage();
        String lower = message.toLowerCase();
        if (lower.startsWith("/trocarsenha")) {
            e.setCancelled(true);

            String[] split = message.substring(1).split(" ");
            String label = split[0];
            String[] args = java.util.Arrays.copyOfRange(split, 1, split.length);

            PluginCommand cmd = getCommand(label);
            if (cmd != null && cmd.getExecutor() != null) {
                cmd.getExecutor().onCommand(e.getPlayer(), cmd, label, args);
            }
        }
    }
}
