package conexao.code.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TabListManager {
    private final Plugin plugin;

    public TabListManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void applyAll() {
        String header = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("tablist.header", ""));
        String footer = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("tablist.footer", ""));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListHeaderFooter(header, footer);
        }
    }

    public void reload() {
        applyAll();
    }
}
