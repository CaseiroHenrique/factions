// src/main/java/conexao/code/LobbyPlugin.java
package conexao.code;

import conexao.code.commands.ReloadCommand;
import conexao.code.commands.SetSpawnCommand;
import conexao.code.commands.TagCommand;
import conexao.code.manager.ScoreboardManager;
import conexao.code.manager.TabListManager;
import conexao.code.manager.SpawnManager;
import conexao.code.manager.TagManager;
import conexao.code.manager.TagChannelListener;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class lobby extends JavaPlugin implements Listener {
    private ScoreboardManager scoreboardManager;
    private TabListManager tabListManager;
    private SpawnManager spawnManager;
    private TagManager tagManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        scoreboardManager = new ScoreboardManager(this);
        scoreboardManager.start();
        tabListManager = new TabListManager(this);
        tabListManager.applyAll();
        spawnManager = new SpawnManager(this);
        tagManager = new TagManager(this);
        getServer().getMessenger().registerIncomingPluginChannel(this, TagChannelListener.TAG_CHANNEL,
                new TagChannelListener(this, tagManager));
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("recarregar").setExecutor(new ReloadCommand(this, scoreboardManager, tabListManager));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnManager));
        getCommand("tag").setExecutor(new TagCommand(this, tagManager));
    }

    @Override
    public void onDisable() {
        scoreboardManager.stop();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.getPlayer().setScoreboard(scoreboardManager.getBoard());
        tabListManager.applyAll();
        tagManager.apply(e.getPlayer());
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        tagManager.clear(e.getPlayer().getUniqueId());
    }
}
