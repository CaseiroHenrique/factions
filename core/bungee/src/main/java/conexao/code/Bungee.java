package conexao.code;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PlayerListItemRemove;
import net.md_5.bungee.protocol.packet.PlayerListItemUpdate;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Bungee extends Plugin implements Listener {
    private static final String AUTH_CHANNEL = "core:auth";
    private static final String CHAT_CHANNEL = "core:chat";
    private final Set<String> authenticated = ConcurrentHashMap.newKeySet();
    private final Map<String, String> lastTell = new ConcurrentHashMap<>();
    private ServerInfo lobby;

    @Override
    public void onEnable() {
        // registra canais e listener
        getProxy().registerChannel("BungeeCord");
        getProxy().registerChannel(CHAT_CHANNEL);
        getProxy().getPluginManager().registerListener(this, this);
        lobby = getProxy().getServerInfo("lobby");
    }

    /**
     * Converte um jogador em entrada para PlayerListItem.
     */
    private PlayerListItem.Item toItem(ProxiedPlayer p) {
        PlayerListItem.Item it = new PlayerListItem.Item();
        it.setUuid(p.getUniqueId());
        it.setUsername(p.getName());
        it.setGamemode(0);
        it.setPing(p.getPing());
        it.setListed(true);
        return it;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (CHAT_CHANNEL.equals(e.getTag())) {
            ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
            String type = in.readUTF();
            if ("GLOBAL".equalsIgnoreCase(type)) {
                // O servidor de origem não é mais enviado pelo plugin de chat.
                String name = in.readUTF();
                String message = in.readUTF();
                String targets = in.readUTF();
                String formatted = "\u00a77[G] " + name + "\u00a77: \u00a7r" + message;
                for (String s : targets.split(",")) {
                    ServerInfo info = getProxy().getServerInfo(s.trim());
                    if (info != null) {
                        for (ProxiedPlayer p : info.getPlayers()) {
                            p.sendMessage(new TextComponent(formatted));
                        }
                    }
                }
            } else if ("TELL".equalsIgnoreCase(type)) {
                String senderName = in.readUTF();
                String senderDisplay = in.readUTF();
                String targetName = in.readUTF();
                String msg = in.readUTF();
                ProxiedPlayer senderP = getProxy().getPlayer(senderName);
                ProxiedPlayer targetP = getProxy().getPlayer(targetName);
                if (targetP != null && senderP != null) {
                    String targetDisplay = targetP.getDisplayName();
                    senderP.sendMessage(new TextComponent("\u00a77Mensagem enviada para " + targetDisplay + ": " + msg));
                    targetP.sendMessage(new TextComponent("\u00a77Mensagem recebida de " + senderDisplay + ": " + msg));
                    lastTell.put(senderName.toLowerCase(), targetName.toLowerCase());
                    lastTell.put(targetName.toLowerCase(), senderName.toLowerCase());
                } else if (senderP != null) {
                    senderP.sendMessage(new TextComponent("\u00a7cJogador n\u00e3o encontrado."));
                }
            } else if ("REPLY".equalsIgnoreCase(type)) {
                String senderName = in.readUTF();
                String senderDisplay = in.readUTF();
                String msg = in.readUTF();
                ProxiedPlayer senderP = getProxy().getPlayer(senderName);
                String targetName = lastTell.get(senderName.toLowerCase());
                ProxiedPlayer targetP = targetName != null ? getProxy().getPlayer(targetName) : null;
                if (targetP != null && senderP != null) {
                    String targetDisplay = targetP.getDisplayName();
                    senderP.sendMessage(new TextComponent("\u00a77Mensagem enviada para " + targetDisplay + ": " + msg));
                    targetP.sendMessage(new TextComponent("\u00a77Mensagem recebida de " + senderDisplay + ": " + msg));
                    lastTell.put(senderName.toLowerCase(), targetName.toLowerCase());
                    lastTell.put(targetName.toLowerCase(), senderName.toLowerCase());
                } else if (senderP != null) {
                    senderP.sendMessage(new TextComponent("\u00a7cNingu\u00e9m para responder."));
                }
            }
            return;
        }
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

    @EventHandler
    public void onServerConnected(ServerConnectedEvent e) {
        final ProxiedPlayer joined = e.getPlayer();

        // Envia informações da lista de jogadores após a conexão
        getProxy().getScheduler().runAsync(this, () -> {
            List<PlayerListItem.Item> existing = new ArrayList<>();
            for (ProxiedPlayer p : getProxy().getPlayers()) {
                if (p != joined) {
                    existing.add(toItem(p));
                }
            }
            if (!existing.isEmpty()) {
                PlayerListItemUpdate pkt = new PlayerListItemUpdate();
                pkt.setActions(EnumSet.of(PlayerListItemUpdate.Action.ADD_PLAYER));
                pkt.setItems(existing.toArray(new PlayerListItem.Item[0]));
                joined.unsafe().sendPacket(pkt);
            }

            PlayerListItemUpdate pktNew = new PlayerListItemUpdate();
            pktNew.setActions(EnumSet.of(PlayerListItemUpdate.Action.ADD_PLAYER));
            pktNew.setItems(new PlayerListItem.Item[]{toItem(joined)});
            for (ProxiedPlayer p : getProxy().getPlayers()) {
                p.unsafe().sendPacket(pktNew);
            }
        });
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent e) {
        ProxiedPlayer left = e.getPlayer();
        PlayerListItemRemove pkt = new PlayerListItemRemove();
        pkt.setUuids(new UUID[]{left.getUniqueId()});
        for (ProxiedPlayer p : getProxy().getPlayers()) {
            p.unsafe().sendPacket(pkt);
        }
        authenticated.remove(left.getName());
    }
}
