package conexao.code.factionsplugin;

import conexao.code.common.DatabaseManager;
import conexao.code.common.factions.FactionDAO;
import conexao.code.common.factions.FactionMemberDAO;
import conexao.code.factionsplugin.listener.FriendlyFireListener;
import conexao.code.factionsplugin.listener.FactionPlayerListener;
import conexao.code.factionsplugin.FactionChatCommand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionsPlugin extends JavaPlugin {

    private final Map<UUID, Invite> invites = new ConcurrentHashMap<>();

    public Map<UUID, Invite> getInvites() {
        return invites;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String host = getConfig().getString("mysql.host");
        int port = getConfig().getInt("mysql.port");
        String database = getConfig().getString("mysql.database");
        String user = getConfig().getString("mysql.user");
        String pass = getConfig().getString("mysql.password");
        DatabaseManager.init(host, port, database, user, pass);
        try {
            FactionDAO.ensureTable();
            FactionMemberDAO.ensureTable();
        } catch (Exception ex) {
            getLogger().severe("Erro ao criar tabela de facções: " + ex.getMessage());
        }
        getCommand("f").setExecutor(new FactionCommand(this));
        getCommand("c").setExecutor(new FactionChatCommand(this));
        getServer().getPluginManager().registerEvents(new FriendlyFireListener(), this);
        getServer().getPluginManager().registerEvents(new FactionPlayerListener(this), this);
    }

    public static class Invite {
        public final int factionId;
        public final UUID inviter;
        public final long expireAt;

        public Invite(int factionId, UUID inviter, long expireAt) {
            this.factionId = factionId;
            this.inviter = inviter;
            this.expireAt = expireAt;
        }
    }
}
