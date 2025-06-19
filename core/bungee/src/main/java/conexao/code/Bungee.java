package conexao.code;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Bungee extends Plugin implements Listener {
    private static final String AUTH_CHANNEL = "core:auth";
    private final Set<String> authenticated = ConcurrentHashMap.newKeySet();
    private ServerInfo lobby;

    @Override
    public void onEnable() {
        // registra canal BungeeCord e listener
        getProxy().registerChannel("BungeeCord");
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
        if (success) {
            authenticated.add(name);
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
}
