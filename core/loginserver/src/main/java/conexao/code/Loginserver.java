// src/main/java/conexao/code/Loginserver.java
package conexao.code;

import conexao.code.common.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.plugin.java.JavaPlugin;

public class Loginserver extends JavaPlugin {
    static final String BUNGEE_CHANNEL = "BungeeCord";
    static final String AUTH_CHANNEL  = "core:auth";

    // exposto para uso em outros lugares
    public static ScoreboardManager sbManager;

    @Override
    public void onEnable() {
        // 1. carrega seu config.yml (só faz nada se já existir)
        saveDefaultConfig();

        // 2. inicializa DatabaseManager
        String host     = getConfig().getString("mysql.host");
        int    port     = getConfig().getInt   ("mysql.port");
        String database = getConfig().getString("mysql.database");
        String user     = getConfig().getString("mysql.user");
        String pass     = getConfig().getString("mysql.password");
        DatabaseManager.init(host, port, database, user, pass);

        // 3. instancia o ScoreboardManager **depois** do saveDefaultConfig()
        sbManager = new ScoreboardManager(this);

        // 4. canal Bungee
        getServer().getMessenger().registerOutgoingPluginChannel(this, BUNGEE_CHANNEL);

        // 5. game rules e despawn de mobs
        Bukkit.getWorlds().forEach(w -> {
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            w.setTime(1000L);
            w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        });
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Bukkit.getWorlds().forEach(w -> {
                w.setTime(1000L);
                w.getEntities().stream()
                        .filter(e -> e instanceof org.bukkit.entity.Monster)
                        .forEach(e -> e.remove());
            });
        }, 0L, 20L);

        // 6. listeners e comandos
        getServer().getPluginManager().registerEvents(new GameSettingsListener(this), this);
        getServer().getPluginManager().registerEvents(new AuthListener(this),    this);
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("login").   setExecutor(new LoginCommand(this));
        getCommand("trocarsenha").setExecutor(new ChangePasswordCommand(this));
    }

    @Override
    public void onDisable() {}
}
