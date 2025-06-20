package conexao.code.permissionsplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PermissionsPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("tag").setExecutor(new TagCommand(this));
    }

    @Override
    public void onDisable() {
        attachments.values().forEach(PermissionAttachment::remove);
        attachments.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        loadTag(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        PermissionAttachment att = attachments.remove(e.getPlayer().getUniqueId());
        if (att != null) e.getPlayer().removeAttachment(att);
    }

    void loadTag(Player p) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                conexao.code.permissions.Tag tag;
                try {
                    tag = conexao.code.permissions.TagDAO.getTagByUser(p.getUniqueId());
                } catch (Exception ex) {
                    getLogger().warning("Erro ao carregar tag: " + ex.getMessage());
                    return;
                }
                if (tag == null) return;
                org.bukkit.Bukkit.getScheduler().runTask(PermissionsPlugin.this, () -> {
                    PermissionAttachment att = attachments.remove(p.getUniqueId());
                    if (att != null) p.removeAttachment(att);
                    att = p.addAttachment(PermissionsPlugin.this);
                    attachments.put(p.getUniqueId(), att);
                    for (String perm : tag.getPermissions()) {
                        att.setPermission(perm, true);
                    }
                });
            }
        }.runTaskAsynchronously(this);
    }
}
