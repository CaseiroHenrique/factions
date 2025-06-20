package conexao.code;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import conexao.code.common.DatabaseManager;
import conexao.code.common.factions.FactionMemberDAO;
import conexao.code.common.factions.FactionRank;
import java.util.Optional;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class ChatPlugin extends JavaPlugin implements Listener {
    private double localRadius;
    private List<String> globalTargets;
    private String serverName;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        localRadius = cfg.getDouble("local-radius", 60.0);
        globalTargets = cfg.getStringList("global-servers");
        serverName = cfg.getString("server-name", "");

        String host = cfg.getString("mysql.host");
        int port = cfg.getInt("mysql.port");
        String database = cfg.getString("mysql.database");
        String user = cfg.getString("mysql.user");
        String pass = cfg.getString("mysql.password");
        DatabaseManager.init(host, port, database, user, pass);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "core:chat");

        if (!serverName.equalsIgnoreCase("lobby")) {
            getCommand("g").setExecutor(new GlobalChatCommand(this));
        } else {
            getCommand("g").setExecutor((sender, cmd, label, args) -> {
                sender.sendMessage(ChatColor.RED + "Chat global indispon\u00edvel no lobby.");
                return true;
            });
        }

        getCommand("tell").setExecutor(new TellCommand(this));
        getCommand("r").setExecutor(new ReplyCommand(this));
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        e.setCancelled(true);
        Player sender = e.getPlayer();
        String message = e.getMessage();
        String formatted;
        if (serverName.equalsIgnoreCase("lobby")) {
            if (!sender.hasPermission("lobby.chat")) {
                sender.sendMessage(ChatColor.RED + "Voc\u00ea n\u00e3o tem permiss\u00e3o para falar no lobby.");
                return;
            }
            formatted = ChatColor.WHITE + "[Lobby #1] " + sender.getDisplayName() + ChatColor.GRAY + ": " + ChatColor.GRAY + message;
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(formatted);
            }
            return;
        }

        String factionTag = null;
        String icon = "";
        try {
            Optional<String> tagOpt = FactionMemberDAO.getFactionTag(sender.getUniqueId());
            if (tagOpt.isPresent()) {
                factionTag = tagOpt.get();
                FactionRank rank = FactionMemberDAO.getRank(sender.getUniqueId()).orElse(null);
                if (rank != null) icon = rank.getIcon();
            }
        } catch (Exception ignored) {}
        if (factionTag != null) {
            formatted = ChatColor.GRAY + "[L] " + ChatColor.GRAY + "[" + icon + " " + factionTag + "] " + sender.getDisplayName() + ChatColor.GRAY + ": " + ChatColor.GRAY + message;
        } else {
            formatted = ChatColor.GRAY + "[L] " + sender.getDisplayName() + ChatColor.GRAY + ": " + ChatColor.GRAY + message;
        }
        if (localRadius <= 0) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(formatted);
            }
        } else {
            for (Player p : sender.getWorld().getPlayers()) {
                if (p.getLocation().distance(sender.getLocation()) <= localRadius) {
                    p.sendMessage(formatted);
                }
            }
        }
    }

    void sendGlobalMessage(Player sender, String message) {
        String factionTag = null;
        String icon = "";
        try {
            Optional<String> tagOpt = FactionMemberDAO.getFactionTag(sender.getUniqueId());
            if (tagOpt.isPresent()) {
                factionTag = tagOpt.get();
                FactionRank rank = FactionMemberDAO.getRank(sender.getUniqueId()).orElse(null);
                if (rank != null) icon = rank.getIcon();
            }
        } catch (Exception ignored) {}
        String formatted;
        if (factionTag != null) {
            formatted = ChatColor.GRAY + "[G] " + ChatColor.GRAY + "[" + icon + " " + factionTag + "] " + sender.getDisplayName() + ChatColor.GRAY + ": " + ChatColor.RESET + message;
        } else {
            formatted = ChatColor.GRAY + "[G] " + sender.getDisplayName() + ChatColor.GRAY + ": " + ChatColor.RESET + message;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(formatted);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);
            data.writeUTF("GLOBAL");
            // Não enviamos mais o nome do servidor de origem, apenas o jogador
            // e a mensagem, garantindo que o prefixo [G] seja o mesmo em todos
            // os servidores.
            data.writeUTF(sender.getDisplayName());
            data.writeUTF(message);

            List<String> targets = globalTargets;
            if (!serverName.isEmpty()) {
                targets = globalTargets.stream()
                        .filter(t -> !t.equalsIgnoreCase(serverName))
                        .toList();
            }

            data.writeUTF(String.join(",", targets));
            sender.sendPluginMessage(this, "core:chat", out.toByteArray());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void sendTell(Player sender, String target, String message) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);
            data.writeUTF("TELL");
            data.writeUTF(sender.getName());
            data.writeUTF(sender.getDisplayName());
            data.writeUTF(target);
            data.writeUTF(message);
            sender.sendPluginMessage(this, "core:chat", out.toByteArray());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void sendReply(Player sender, String message) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);
            data.writeUTF("REPLY");
            data.writeUTF(sender.getName());
            data.writeUTF(sender.getDisplayName());
            data.writeUTF(message);
            sender.sendPluginMessage(this, "core:chat", out.toByteArray());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
