package conexao.code.factionsplugin;

import conexao.code.common.factions.FactionMemberDAO;
import conexao.code.common.factions.FactionRank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FactionChatCommand implements CommandExecutor {
    private final FactionsPlugin plugin;

    public FactionChatCommand(FactionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando apenas para jogadores");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " <mensagem>");
            return true;
        }
        String message = String.join(" ", args);
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Optional<Integer> facOpt = FactionMemberDAO.getFactionId(player.getUniqueId());
                    if (facOpt.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "Você não está em facção.");
                        return;
                    }
                    int facId = facOpt.get();
                    FactionRank rank = FactionMemberDAO.getRank(player.getUniqueId()).orElse(FactionRank.PLEBEU);
                    List<UUID> members = FactionMemberDAO.getMembers(facId);
                    String formatted = ChatColor.WHITE + rank.getIcon() + " " + player.getName() + ": " + ChatColor.AQUA + message;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (UUID uuid : members) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) p.sendMessage(formatted);
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        }.runTaskAsynchronously(plugin);
        return true;
    }
}
