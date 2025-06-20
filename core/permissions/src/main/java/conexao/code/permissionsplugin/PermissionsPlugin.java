package conexao.code.permissionsplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import conexao.code.common.DatabaseManager;

import java.io.File;
import java.util.HashSet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import conexao.code.permissions.Tag;
import org.bukkit.ChatColor;

public class PermissionsPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final Map<UUID, Tag> tags = new HashMap<>();
    private String defaultTagName;

    @Override
    public void onEnable() {
        // Inicializa configuracao e banco de dados
        saveDefaultConfig();
        defaultTagName = getConfig().getString("default-tag");
        String host     = getConfig().getString("mysql.host");
        int    port     = getConfig().getInt("mysql.port");
        String database = getConfig().getString("mysql.database");
        String user     = getConfig().getString("mysql.user");
        String pass     = getConfig().getString("mysql.password");
        DatabaseManager.init(host, port, database, user, pass);

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("tag").setExecutor(new TagCommand(this));
        loadTagsFromFile();
    }

    @Override
    public void onDisable() {
        attachments.values().forEach(PermissionAttachment::remove);
        attachments.clear();
        tags.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        loadTag(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        PermissionAttachment att = attachments.remove(e.getPlayer().getUniqueId());
        if (att != null) e.getPlayer().removeAttachment(att);
        tags.remove(e.getPlayer().getUniqueId());
    }

    void loadTag(Player p) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                conexao.code.permissions.Tag tag;
                try {
                    tag = conexao.code.permissions.TagDAO.getTagByUser(p.getUniqueId());
                    if (tag == null && defaultTagName != null) {
                        Tag def = conexao.code.permissions.TagDAO.getTagByName(defaultTagName);
                        if (def != null) {
                            conexao.code.permissions.TagDAO.assignTagToUser(p.getUniqueId(), def.getId());
                            tag = def;
                        }
                    }
                } catch (Exception ex) {
                    getLogger().warning("Erro ao carregar tag: " + ex.getMessage());
                    return;
                }
                if (tag == null) return;
                final Tag finalTag = tag;
                org.bukkit.Bukkit.getScheduler().runTask(PermissionsPlugin.this, () -> {
                    PermissionAttachment att = attachments.remove(p.getUniqueId());
                    if (att != null) p.removeAttachment(att);
                    att = p.addAttachment(PermissionsPlugin.this);
                    attachments.put(p.getUniqueId(), att);
                    for (String perm : finalTag.getPermissions()) {
                        att.setPermission(perm, true);
                    }
                    tags.put(p.getUniqueId(), finalTag);
                    String coloredPrefix = ChatColor.translateAlternateColorCodes('&', finalTag.getColor() + finalTag.getPrefix());
                    String coloredName = ChatColor.translateAlternateColorCodes('&', finalTag.getColor() + p.getName());
                    String full = coloredPrefix + " " + coloredName;
                    p.setDisplayName(full);
                    p.setPlayerListName(full);
                    p.setCustomName(full);
                    p.setCustomNameVisible(true);
                });
            }
        }.runTaskAsynchronously(this);
    }

    private void loadTagsFromFile() {
        File file = new File(getDataFolder(), "tags.yml");
        if (!file.exists()) {
            saveResource("tags.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = cfg.getConfigurationSection("tags");
        if (sec == null) return;
        for (String name : sec.getKeys(false)) {
            ConfigurationSection t = sec.getConfigurationSection(name);
            if (t == null) continue;
            String color = t.getString("color", "");
            String prefix = t.getString("prefix", "");
            String suffix = t.getString("suffix", "");
            HashSet<String> perms = new HashSet<>(t.getStringList("permissions"));
            conexao.code.permissions.Tag tag = new conexao.code.permissions.Tag(0, name, color, prefix, suffix, perms);
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        conexao.code.permissions.Tag existing = conexao.code.permissions.TagDAO.getTagByName(name);
                        if (existing == null) {
                            conexao.code.permissions.TagDAO.createTag(tag);
                        } else {
                            tag.setId(existing.getId());
                            conexao.code.permissions.TagDAO.updateTag(tag);
                        }
                    } catch (Exception ex) {
                        getLogger().warning("Erro ao processar tag '" + name + "': " + ex.getMessage());
                    }
                }
            }.runTaskAsynchronously(this);
        }
    }
}
