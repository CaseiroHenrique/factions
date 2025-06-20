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
            // Não enviamos mais o nome do servidor de origem, apenas o jogador
            // e a mensagem, garantindo que o prefixo [G] seja o mesmo em todos
            // os servidores.
            data.writeUTF(sender.getName());
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
}
