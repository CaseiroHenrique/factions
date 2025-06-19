package conexao.code.commands;

import conexao.code.common.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ChangePasswordCommand implements CommandExecutor {
    private final Plugin plugin;

    public ChangePasswordCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        UUID id = p.getUniqueId();

        if (args.length != 2) {
            p.sendMessage(ChatColor.RED + "Uso: /trocarsenha <senha-antiga> <senha-nova>");
            return true;
        }

        String oldPass = args[0];
        String newPass = args[1];

        new BukkitRunnable() {
            @Override
            public void run() {
                boolean ok = false;
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement st = conn.prepareStatement(
                             "SELECT password_hash FROM users WHERE username = ?")) {
                    st.setString(1, p.getName());
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            ok = BCrypt.checkpw(oldPass, rs.getString("password_hash"));
                        }
                    }
                } catch (SQLException ex) {
                    plugin.getLogger().warning(ex.getMessage());
                }

                if (!ok) {
                    Bukkit.getScheduler().runTask(plugin,
                            () -> p.sendMessage(ChatColor.RED + "Senha antiga incorreta."));
                    return;
                }

                String newHash = BCrypt.hashpw(newPass, BCrypt.gensalt(12));
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement st = conn.prepareStatement(
                             "UPDATE users SET password_hash = ? WHERE username = ?")) {
                    st.setString(1, newHash);
                    st.setString(2, p.getName());
                    st.executeUpdate();
                } catch (SQLException ex) {
                    plugin.getLogger().warning(ex.getMessage());
                    Bukkit.getScheduler().runTask(plugin,
                            () -> p.sendMessage(ChatColor.RED + "Erro ao atualizar senha."));
                    return;
                }

                Bukkit.getScheduler().runTask(plugin,
                        () -> p.sendMessage(ChatColor.GREEN + "Senha alterada com sucesso!"));
            }
        }.runTaskAsynchronously(plugin);

        return true;
    }
}
