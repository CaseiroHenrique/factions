package conexao.code.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

public class TabListManager {
    private final Plugin plugin;

    public TabListManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void applyAll() {
        List<String> headerLines = plugin.getConfig().getStringList("tablist.headerLines");
        List<String> footerLines = plugin.getConfig().getStringList("tablist.footerLines");
        String header = headerLines.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.joining("\n"));
        String footer = footerLines.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.joining("\n"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListHeaderFooter(header, footer);
        }
    }

    public void reload() {
        applyAll();
    }
}
