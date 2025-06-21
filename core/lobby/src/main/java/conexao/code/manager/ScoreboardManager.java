// src/main/java/conexao/code/manager/ScoreboardManager.java
package conexao.code.manager;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreboardManager implements PluginMessageListener {
    private final Plugin plugin;
    private BukkitTask task;
    private Scoreboard board;
    private Objective objective;
    private final Map<String, Integer> playerCounts = new HashMap<>();
    private static final Pattern COUNT_PLACEHOLDER = Pattern.compile("%online_([a-zA-Z0-9_-]+)%");

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

    public String applyPlaceholders(String text) {
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

        Matcher m = COUNT_PLACEHOLDER.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String server = m.group(1);
            requestPlayerCount(server);
            int count = playerCounts.getOrDefault(server, 0);
            m.appendReplacement(sb, String.valueOf(count));
        }
        m.appendTail(sb);

        return sb.toString();
    }

    public int getPlayerCount(String server) {
        return playerCounts.getOrDefault(server, 0);
    }

    public void requestPlayerCount(String server) {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        Player p = Bukkit.getOnlinePlayers().iterator().next();
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF(server);
        p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!"BungeeCord".equals(channel)) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String sub = in.readUTF();
        if ("PlayerCount".equals(sub)) {
            String server = in.readUTF();
            int count = in.readInt();
            playerCounts.put(server, count);
        }
    }
}
