// src/main/java/conexao/code/pvp/PvPListener.java
package conexao.code.pvp;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Set;

public class PvPListener implements Listener {
    // armas corpo-a-corpo com cooldown removido via setCooldown
    private static final Set<Material> WEAPONS = Set.of(
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD,
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE
    );

    // A cada hit, limpa cooldown da arma (PvP estilo 1.8)
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        Material inHand = p.getInventory().getItemInMainHand().getType();
        if (WEAPONS.contains(inHand)) {
            p.setCooldown(inHand, 0);
        }
    }
}
