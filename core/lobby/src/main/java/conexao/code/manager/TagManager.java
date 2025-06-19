package conexao.code.manager;

import conexao.code.permissions.Tag;
import conexao.code.permissions.TagDAO;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.*;

public class TagManager {
    private final Plugin plugin;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public TagManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player) {
        UUID uuid = player.getUniqueId();
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Tag tag = TagDAO.getTagByUser(uuid);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (tag != null) {
                            setDisplay(player, tag);
                            applyPermissions(player, tag);
                        } else {
                            clear(uuid);
                        }
                    });
                } catch (SQLException ex) {
                    plugin.getLogger().warning("Erro ao carregar tag: " + ex.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void clear(UUID uuid) {
        PermissionAttachment att = attachments.remove(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            if (att != null) {
                p.removeAttachment(att);
            }
            p.setDisplayName(p.getName());
            p.setPlayerListName(p.getName());
        }
    }

    private void setDisplay(Player p, Tag tag) {
        String prefix = colorize(tag.getPrefix());
        String suffix = colorize(tag.getSuffix());
        String color = colorize(tag.getColor());
        String name = prefix + color + p.getName() + suffix;
        p.setDisplayName(name);
        p.setPlayerListName(name);
    }

    private void applyPermissions(Player p, Tag tag) {
        PermissionAttachment old = attachments.remove(p.getUniqueId());
        if (old != null) {
            p.removeAttachment(old);
        }
        Set<String> perms = tag.getPermissions();
        if (perms != null && !perms.isEmpty()) {
            PermissionAttachment att = p.addAttachment(plugin);
            for (String perm : perms) {
                att.setPermission(perm, true);
            }
            attachments.put(p.getUniqueId(), att);
        }
    }

    private String colorize(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
}
