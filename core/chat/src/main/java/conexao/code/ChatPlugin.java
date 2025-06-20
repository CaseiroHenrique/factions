package conexao.code;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class ChatPlugin extends JavaPlugin implements Listener {
    private String serverName;
    private double localRadius;
    private List<String> globalTargets;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        serverName = cfg.getString("server-name", "server");
        localRadius = cfg.getDouble("local-radius", 60.0);
        globalTargets = cfg.getStringList("global-servers");

        getServer().getMessenger().registerOutgoingPluginChannel(this, "core:chat");
        getCommand("g").setExecutor(new GlobalChatCommand(this));
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        e.setCancelled(true);
        Player sender = e.getPlayer();
        String message = e.getMessage();
        String formatted = ChatColor.GRAY + "[L] " + ChatColor.WHITE + sender.getName() + ChatColor.GRAY + ": " + ChatColor.RESET + message;
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
        String formatted = ChatColor.GRAY + "[G] " + ChatColor.WHITE + sender.getName() + ChatColor.GRAY + ": " + ChatColor.RESET + message;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(formatted);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);
            data.writeUTF("GLOBAL");
            data.writeUTF(serverName);
            data.writeUTF(sender.getName());
            data.writeUTF(message);
            data.writeUTF(String.join(",", globalTargets));
            sender.sendPluginMessage(this, "core:chat", out.toByteArray());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
