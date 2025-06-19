// src/main/java/conexao/code/AuthListener.java
package conexao.code;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;
import conexao.code.common.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AuthListener implements Listener {
    private static final Set<UUID> authenticated = new HashSet<>();
    private static final Map<UUID, BukkitRunnable> countdownTasks = new HashMap<>();
    private final Loginserver plugin;

    public AuthListener(Loginserver plugin) {
        this.plugin = plugin;
    }

    /**
     * Marca o jogador como autenticado, cancela a contagem e aplica a scoreboard.
     */
    public static void markAuthenticated(UUID id) {
        authenticated.add(id);

        // Aplica scoreboard assim que autenticado
        Player p = Bukkit.getPlayer(id);
        if (p != null && p.isOnline()) {
            Loginserver.sbManager.apply(p);
        }

        // Cancela contagem regressiva
        BukkitRunnable task = countdownTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        // Aplica a scoreboard imediatamente ao entrar
        Loginserver.sbManager.apply(p);

        // Limpa estado anterior
        authenticated.remove(id);
        BukkitRunnable prev = countdownTasks.remove(id);
        if (prev != null) {
            prev.cancel();
        }

        // Verifica existência de conta
        boolean hasAccount = false;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            st.setString(1, p.getName());
            try (ResultSet rs = st.executeQuery()) {
                hasAccount = rs.next();
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Erro ao verificar conta: " + ex.getMessage());
        }

        // Envia prompt de login ou register
        String key = hasAccount ? "messages.prompt-login" : "messages.prompt-register";
        String msg = plugin.getConfig().getString(key);
        if (msg != null) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }

        // Inicia contagem regressiva para /login
        int timeout = plugin.getConfig().getInt("login-timeout-seconds", 60);
        BukkitRunnable countdown = new BukkitRunnable() {
            int remaining = timeout;
            @Override
            public void run() {
                if (!p.isOnline() || authenticated.contains(id)) {
                    this.cancel();
                    countdownTasks.remove(id);
                    return;
                }
                String bar = ChatColor.RED + "Você possui " + remaining + " segundos para se logar.";
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(bar));
                if (remaining-- <= 0) {
                    String kickMsg = plugin.getConfig().getString("messages.kick-timeout", "&cTempo esgotado.");
                    p.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMsg));
                    this.cancel();
                }
            }
        };
        countdown.runTaskTimer(plugin, 0L, 20L);
        countdownTasks.put(id, countdown);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
        UUID id = e.getPlayer().getUniqueId();
        authenticated.remove(id);
        BukkitRunnable task = countdownTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!authenticated.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Você não está autenticado! Use /login ou /register");
        }
        // se quiser encaminhar ao proxy, pode descomentar e ajustar aqui
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (!authenticated.contains(e.getPlayer().getUniqueId())) {
            String msg = e.getMessage().toLowerCase(Locale.ROOT);
            if (!msg.startsWith("/register") && !msg.startsWith("/login")) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "Autentique-se: /register ou /login");
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!authenticated.contains(e.getPlayer().getUniqueId())
                && !e.getFrom().getBlock().equals(e.getTo().getBlock())) {
            e.setTo(e.getFrom());
        }
    }
}
