// src/main/java/conexao/code/LobbyPlugin.java
package conexao.code;

import conexao.code.commands.ReloadCommand;
import conexao.code.manager.ScoreboardManager;
import conexao.code.manager.TabListManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class lobby extends JavaPlugin implements Listener {
    private ScoreboardManager scoreboardManager;
    private TabListManager tabListManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        scoreboardManager = new ScoreboardManager(this);
        scoreboardManager.start();
        tabListManager = new TabListManager(this);
        tabListManager.applyAll();
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("recarregar").setExecutor(new ReloadCommand(this, scoreboardManager, tabListManager));
    }

    @Override
    public void onDisable() {
        scoreboardManager.stop();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.getPlayer().setScoreboard(scoreboardManager.getBoard());
        tabListManager.applyAll();
    }
}
