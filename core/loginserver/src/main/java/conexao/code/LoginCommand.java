// LoginCommand.java
package conexao.code;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import conexao.code.common.DatabaseManager;
import conexao.code.permissions.Tag;
import conexao.code.permissions.TagDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

public class LoginCommand implements CommandExecutor {
    private final Loginserver plugin;

    public LoginCommand(Loginserver plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        UUID id = p.getUniqueId();

        if (args.length != 1) {
            p.sendMessage(ChatColor.RED + "Uso: /login <senha>");
            return true;
        }

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
                            ok = BCrypt.checkpw(args[0], rs.getString("password_hash"));
                        }
                    }

                } catch (SQLException ex) {
                    plugin.getLogger().warning(ex.getMessage());
                }

                final boolean success = ok;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!success) {
                        p.sendMessage(ChatColor.RED + "Senha incorreta.");
                        return;
                    }
                    // autoriza e encaminha pro proxy
                    AuthListener.markAuthenticated(id);
                    Tag tag = null;
                    try {
                        tag = TagDAO.getTagByUser(id);
                    } catch (SQLException ex) {
                        plugin.getLogger().warning("Erro ao obter tag: " + ex.getMessage());
                    }
                    ByteArrayDataOutput buf = ByteStreams.newDataOutput();
                    buf.writeUTF("auth");
                    buf.writeUTF(p.getName());
                    buf.writeBoolean(true);
                    if (tag != null) {
                        buf.writeBoolean(true);
                        buf.writeUTF(tag.getName());
                        buf.writeUTF(empty(tag.getColor()));
                        buf.writeUTF(empty(tag.getPrefix()));
                        buf.writeUTF(empty(tag.getSuffix()));
                        buf.writeUTF(String.join(",", tag.getPermissions()));
                    } else {
                        buf.writeBoolean(false);
                    }
                    byte[] authData = buf.toByteArray();

                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Forward");
                    out.writeUTF("ALL");
                    out.writeUTF(Loginserver.AUTH_CHANNEL);
                    out.writeShort(authData.length);
                    out.write(authData);
                    p.sendPluginMessage(plugin, Loginserver.BUNGEE_CHANNEL, out.toByteArray());

                    p.sendMessage(ChatColor.GREEN + "Logado com sucesso!");
                });
            }
        }.runTaskAsynchronously(plugin);

        return true;
    }

    private String empty(String s) {
        return s == null ? "" : s;
    }
}
