package conexao.code.factionsplugin.listener;

import conexao.code.common.factions.FactionMemberDAO;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

public class FriendlyFireListener implements Listener {
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        try {
            Optional<Integer> damFac = FactionMemberDAO.getFactionId(damager.getUniqueId());
            Optional<Integer> vicFac = FactionMemberDAO.getFactionId(victim.getUniqueId());
            if (damFac.isPresent() && vicFac.isPresent() && damFac.get().equals(vicFac.get())) {
                event.setCancelled(true);
            }
        } catch (Exception ignored) {
        }
    }
}
