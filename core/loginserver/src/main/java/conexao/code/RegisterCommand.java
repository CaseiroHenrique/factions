// src/main/java/conexao/code/RegisterCommand.java
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

public class RegisterCommand implements CommandExecutor {
    private final Loginserver plugin;

    public RegisterCommand(Loginserver plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        UUID id = p.getUniqueId();

        // 1) Verifica uso correto do comando
        if (args.length != 2) {
            p.sendMessage(ChatColor.RED + "Uso: /register <senha> <confirmar-senha>");
            return true;
        }
        if (!args[0].equals(args[1])) {
            p.sendMessage(ChatColor.RED + "As senhas não coincidem.");
            return true;
        }

        // 2) Executa consulta e inserção assincronamente
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection conn = DatabaseManager.getConnection()) {
                    // 2.1) Limita número de contas por IP
                    String ip = p.getAddress().getAddress().getHostAddress();
                    int maxPerIp = plugin.getConfig().getInt("registration.max-accounts-per-ip", 3);
                    try (PreparedStatement ipChk = conn.prepareStatement(
                            "SELECT COUNT(*) FROM users WHERE ip_address = ?")) {
                        ipChk.setString(1, ip);
                        try (ResultSet rsIp = ipChk.executeQuery()) {
                            if (rsIp.next() && rsIp.getInt(1) >= maxPerIp) {
                                Bukkit.getScheduler().runTask(plugin, () ->
                                        p.sendMessage(ChatColor.RED +
                                                "Você já atingiu o limite de " +
                                                maxPerIp + " contas para este IP.")
                                );
                                return;
                            }
                        }
                    }

                    // 2.2) Verifica se o nome de usuário já existe
                    try (PreparedStatement chk = conn.prepareStatement(
                            "SELECT 1 FROM users WHERE username = ?")) {
                        chk.setString(1, p.getName());
                        try (ResultSet rs = chk.executeQuery()) {
                            if (rs.next()) {
                                Bukkit.getScheduler().runTask(plugin, () ->
                                        p.sendMessage(ChatColor.RED +
                                                "Já possui conta! Use /login <senha>")
                                );
                                return;
                            }
                        }
                    }

                    // 2.3) Cria hash da senha e insere novo usuário (com IP)
                    String hash = BCrypt.hashpw(args[0], BCrypt.gensalt(12));
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO users(username,password_hash,ip_address) VALUES(?,?,?)")) {
                        ins.setString(1, p.getName());
                        ins.setString(2, hash);
                        ins.setString(3, ip);
                        ins.executeUpdate();
                    }

                    // 2.4) No thread principal: autentica e encaminha pro proxy
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // Marca como autenticado
                        AuthListener.markAuthenticated(id);

                        // Prepara dados de autenticação para o BungeeCord
                        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
                        buf.writeUTF("auth");
                        buf.writeUTF(p.getName());
                        buf.writeBoolean(true);
                        byte[] authData = buf.toByteArray();

                        // Forward via plugin channel
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("Forward");
                        out.writeUTF("ALL");
                        out.writeUTF(Loginserver.AUTH_CHANNEL);
                        out.writeShort(authData.length);
                        out.write(authData);
                        p.sendPluginMessage(plugin, Loginserver.BUNGEE_CHANNEL, out.toByteArray());

                        // Feedback ao jogador
                        p.sendMessage(ChatColor.GREEN + "Conta criada e logado com sucesso!");
                    });

                } catch (SQLException ex) {
                    plugin.getLogger().warning("Erro ao registrar conta: " + ex.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);

        return true;
    }
}
