package conexao.code.factionsplugin.listener;

import conexao.code.common.factions.FactionMemberDAO;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import conexao.code.factionsplugin.FactionsPlugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FactionPlayerListener implements Listener {
    private final FactionsPlugin plugin;

    public FactionPlayerListener(FactionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Optional<Integer> facOpt = FactionMemberDAO.getFactionId(player.getUniqueId());
                    if (facOpt.isEmpty()) return;
                    int facId = facOpt.get();
                    List<UUID> members = FactionMemberDAO.getMembers(facId);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String msg = ChatColor.GREEN + player.getName() + " Entrou no jogo.";
                        for (UUID uuid : members) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) p.sendMessage(msg);
                        }
                    });
                } catch (Exception ignored) {}
            }
        }.runTaskAsynchronously(plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Optional<Integer> facOpt = FactionMemberDAO.getFactionId(player.getUniqueId());
                    if (facOpt.isEmpty()) return;
                    int facId = facOpt.get();
                    List<UUID> members = FactionMemberDAO.getMembers(facId);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        String msg = ChatColor.GREEN + player.getName() + " Desconectou-se.";
                        for (UUID uuid : members) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) p.sendMessage(msg);
                        }
                    });
                } catch (Exception ignored) {}
            }
        }.runTaskAsynchronously(plugin);
    }
}
