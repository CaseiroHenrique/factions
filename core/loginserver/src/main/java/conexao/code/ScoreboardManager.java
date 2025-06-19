// src/main/java/conexao/code/ScoreboardManager.java
package conexao.code;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;

public class ScoreboardManager {
    private final JavaPlugin plugin;
    private final String objectiveName = "sidebar";

    public ScoreboardManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player) {
        // qualifica o tipo para o Bukkit.ScoreboardManager, não sua classe
        org.bukkit.scoreboard.ScoreboardManager bukkitMgr = Bukkit.getScoreboardManager();
        Scoreboard board = bukkitMgr.getNewScoreboard();

        // título configurado em config.yml: "scoreboard.title"
        String rawTitle = plugin.getConfig().getString("scoreboard.title", "&6Scoreboard");
        String title = ChatColor.translateAlternateColorCodes('&', rawTitle);
        Objective obj = board.registerNewObjective(objectiveName, "dummy", title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // linhas configuradas em config.yml: "scoreboard.lines"
        List<String> rawLines = plugin.getConfig().getStringList("scoreboard.lines");
        int score = rawLines.size();

        for (String rawLine : rawLines) {
            String line = ChatColor.translateAlternateColorCodes('&', rawLine);

            // linha em branco: gera entry única
            if (line.trim().isEmpty()) {
                line = ChatColor.COLOR_CHAR + Integer.toHexString(score);
            }

            obj.getScore(line).setScore(score);
            score--;
        }

        player.setScoreboard(board);
    }
}
