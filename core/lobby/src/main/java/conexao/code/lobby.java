// src/main/java/conexao/code/LobbyPlugin.java
package conexao.code;

import conexao.code.commands.ReloadCommand;
import conexao.code.commands.SetSpawnCommand;
import conexao.code.commands.ChangePasswordCommand;
import conexao.code.common.DatabaseManager;
import conexao.code.manager.ScoreboardManager;
import conexao.code.manager.TabListManager;
import conexao.code.manager.SpawnManager;
import conexao.code.listeners.LobbySettingsListener;
import conexao.code.menu.ServerSelectorMenu;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class lobby extends JavaPlugin implements Listener {
    private ScoreboardManager scoreboardManager;
    private TabListManager tabListManager;
    private SpawnManager spawnManager;
    private ServerSelectorMenu selectorMenu;

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
        selectorMenu = new ServerSelectorMenu(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new LobbySettingsListener(), this);
        Bukkit.getPluginManager().registerEvents(selectorMenu, this);
        getCommand("recarregar").setExecutor(new ReloadCommand(this, scoreboardManager, tabListManager));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnManager));
        getCommand("trocarsenha").setExecutor(new ChangePasswordCommand(this));
    }

    @Override
    public void onDisable() {
        scoreboardManager.stop();
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
}
