package conexao.code;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import conexao.code.permissions.Tag;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Bungee extends Plugin implements Listener {
    private static final String AUTH_CHANNEL = "core:auth";
    public static final String TAG_CHANNEL = "core:tag";
    private final Set<String> authenticated = ConcurrentHashMap.newKeySet();
    private final java.util.Map<String, Tag> userTags = new ConcurrentHashMap<>();
    private ServerInfo lobby;

    @Override
    public void onEnable() {
        // registra canal BungeeCord e listener
        getProxy().registerChannel("BungeeCord");
        getProxy().registerChannel(TAG_CHANNEL);
        getProxy().getPluginManager().registerListener(this, this);
        lobby = getProxy().getServerInfo("lobby");
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (!"BungeeCord".equals(e.getTag())) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
        String sub = in.readUTF();
        if (!"Forward".equals(sub)) return;

        String target = in.readUTF();    // "ALL"
        String channel = in.readUTF();   // "core:auth"
        short len = in.readShort();
        byte[] data = new byte[len];
        in.readFully(data);

        if (!AUTH_CHANNEL.equals(channel)) return;

        // desserializa payload
        ByteArrayDataInput din = ByteStreams.newDataInput(data);
        if (!"auth".equals(din.readUTF())) return;

        String name    = din.readUTF();
        boolean success = din.readBoolean();
        Tag tag = null;
        boolean hasTag = din.readBoolean();
        if (hasTag) {
            String tn = din.readUTF();
            String color = emptyToNull(din.readUTF());
            String prefix = emptyToNull(din.readUTF());
            String suffix = emptyToNull(din.readUTF());
            String permStr = din.readUTF();
            java.util.Set<String> perms = new java.util.HashSet<>();
            if (!permStr.isEmpty()) {
                for (String s : permStr.split(",")) perms.add(s);
            }
            tag = new Tag(0, tn, color, prefix, suffix, perms);
        }
        if (success) {
            authenticated.add(name);
            if (tag != null) userTags.put(name, tag);
            ProxiedPlayer p = getProxy().getPlayer(name);
            if (p != null && lobby != null) {
                p.connect(lobby);
            }
        }
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent e) {
        ProxiedPlayer p = e.getPlayer();
        String tgt = e.getTarget().getName();
        if (!authenticated.contains(p.getName())
                && !"loginserver".equalsIgnoreCase(tgt)) {
            p.sendMessage(new TextComponent("§cVocê precisa autenticar antes!"));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent e) {
        ProxiedPlayer p = e.getPlayer();
        Tag tag = userTags.get(p.getName());
        if (tag == null) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("tag");
        out.writeUTF(p.getName());
        out.writeUTF(tag.getName());
        out.writeUTF(empty(tag.getColor()));
        out.writeUTF(empty(tag.getPrefix()));
        out.writeUTF(empty(tag.getSuffix()));
        out.writeUTF(String.join(",", tag.getPermissions()));
        e.getServer().sendData(TAG_CHANNEL, out.toByteArray());
    }

    private String empty(String s) {
        return s == null ? "" : s;
    }

    private String emptyToNull(String s) {
        return s.isEmpty() ? null : s;
    }
}
