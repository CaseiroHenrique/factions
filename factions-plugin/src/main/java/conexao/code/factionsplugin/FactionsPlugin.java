package conexao.code.factionsplugin;

import conexao.code.common.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionsPlugin extends JavaPlugin {
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
        } catch (Exception ex) {
            getLogger().severe("Erro ao criar tabela de facções: " + ex.getMessage());
        }
        getCommand("f").setExecutor(new FactionCommand(this));
    }
}
