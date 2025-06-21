package conexao.code;

import conexao.code.listeners.SpawnListener;
import conexao.code.listeners.HungerListener;
import conexao.code.manager.TabListManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class factions extends JavaPlugin implements Listener {
    private TabListManager tabListManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        tabListManager = new TabListManager(this);
        tabListManager.applyAll();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new SpawnListener(), this);
        Bukkit.getPluginManager().registerEvents(new HungerListener(), this);
    }

    @Override
    public void onDisable() {
        // nothing needed
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        tabListManager.applyAll();
        event.getPlayer().setFoodLevel(20);
    }
}
