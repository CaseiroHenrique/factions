// GameSettingsListener.java
package conexao.code;

import org.bukkit.entity.Player;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameSettingsListener implements Listener {
    private final Loginserver plugin;

    public GameSettingsListener(Loginserver plugin) {
        this.plugin = plugin;
    }

    // cancelar spawn de qualquer Monster
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getEntity() instanceof Monster) {
            e.setCancelled(true);
        }
    }

    // encher fome e impedir decaimento
    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player) {
            e.setCancelled(true);
            ((Player) e.getEntity()).setFoodLevel(20);
        }
    }

    // impedir qualquer dano ao jogador
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            e.setCancelled(true);
        }
    }

    // remover mensagens de join/quit
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
    }

    // desativar todo o chat
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        e.setCancelled(true);
    }
}
