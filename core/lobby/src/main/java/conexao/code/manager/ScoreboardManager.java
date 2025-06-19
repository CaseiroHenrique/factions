// src/main/java/conexao/code/manager/ScoreboardManager.java
package conexao.code.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;

public class ScoreboardManager {
    private final Plugin plugin;
    private BukkitTask task;
    private Scoreboard board;
    private Objective objective;

    public ScoreboardManager(Plugin plugin) {
        this.plugin = plugin;
        reloadBoard();
    }

    public void start() {
        int interval = plugin.getConfig().getInt("scoreboard.update-interval-ticks", 20);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, interval);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(board);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    public void reload() {
        stop();
        reloadBoard();
        start();
    }

    public Scoreboard getBoard() {
        return board;
    }

    private void reloadBoard() {
        org.bukkit.scoreboard.ScoreboardManager bukkitMgr = Bukkit.getScoreboardManager();
        board = bukkitMgr.getNewScoreboard();
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("scoreboard.title", "&cCOMMUNITY"));
        objective = board.registerNewObjective("lobby", "dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    private void updateAll() {
        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }
        int score = lines.size();
        for (String raw : lines) {
            String line = ChatColor.translateAlternateColorCodes('&', applyPlaceholders(raw));
            board.resetScores(line);
            objective.getScore(line).setScore(score--);
        }
    }

    private String applyPlaceholders(String text) {
        if (text.contains("%online%")) {
            text = text.replace("%online%",
                    String.valueOf(Bukkit.getOnlinePlayers().size()));
        }
        if (text.contains("%max%")) {
            text = text.replace("%max%",
                    String.valueOf(plugin.getServer().getMaxPlayers()));
        }
        if (text.contains("%cp%")) {
            text = text.replace("%cp%", "0");
        }
        if (text.contains("%cash%")) {
            text = text.replace("%cash%", "210");
        }
        return text;
    }
}
