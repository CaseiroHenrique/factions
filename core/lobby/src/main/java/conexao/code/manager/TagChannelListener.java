package conexao.code.manager;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import conexao.code.permissions.Tag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Listener para receber dados de tag do proxy.
 */
public class TagChannelListener implements PluginMessageListener {
    public static final String TAG_CHANNEL = "core:tag";
    private final Plugin plugin;
    private final TagManager tagManager;

    public TagChannelListener(Plugin plugin, TagManager tagManager) {
        this.plugin = plugin;
        this.tagManager = tagManager;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!TAG_CHANNEL.equals(channel)) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        if (!"tag".equals(in.readUTF())) return;
        String playerName = in.readUTF();
        String name = in.readUTF();
        String color = emptyToNull(in.readUTF());
        String prefix = emptyToNull(in.readUTF());
        String suffix = emptyToNull(in.readUTF());
        String permsStr = in.readUTF();
        Set<String> perms = new HashSet<>();
        if (!permsStr.isEmpty()) {
            perms.addAll(Arrays.asList(permsStr.split(",")));
        }
        Tag tag = new Tag(0, name, color, prefix, suffix, perms);
        Player target = Bukkit.getPlayerExact(playerName);
        if (target != null) {
            tagManager.apply(target, tag);
        }
    }

    private String emptyToNull(String s) {
        return s.isEmpty() ? null : s;
    }
}
