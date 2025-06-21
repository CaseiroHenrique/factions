package conexao.code.legacycombat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Simple plugin that reverts combat mechanics back to 1.8 style.
 */
public class LegacyCombatPlugin extends JavaPlugin implements Listener {
    // players currently blocking with a sword
    private final Set<UUID> blocking = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        // set attack speed for online players
        for (Player player : getServer().getOnlinePlayers()) {
            applyAttackSpeed(player);
        }
    }

    @Override
    public void onDisable() {
        blocking.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyAttackSpeed(event.getPlayer());
    }

    private void applyAttackSpeed(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr != null) {
            attr.setBaseValue(20.0D); // effectively removes cooldown
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (item == null) return;
        Material type = item.getType();
        if (type == Material.SHIELD) {
            // disable shield usage
            event.setCancelled(true);
            return;
        }
        if (type.name().contains("_SWORD") && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            // start blocking for short period
            blocking.add(event.getPlayer().getUniqueId());
            getServer().getScheduler().runTaskLater(this, () -> blocking.remove(event.getPlayer().getUniqueId()), 10L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        blocking.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        Material type = weapon.getType();
        // cancel sweep damage
        if (event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setCancelled(true);
            return;
        }
        // old axe damage values
        if (type.name().endsWith("_AXE")) {
            double damage = getLegacyAxeDamage(type);
            if (damage > 0) {
                event.setDamage(damage);
            }
        }
        // sword blocking
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (blocking.contains(victim.getUniqueId())) {
                event.setDamage(event.getDamage() * 0.5);
            }
        }
    }

    private double getLegacyAxeDamage(Material type) {
        switch (type) {
            case WOODEN_AXE:
            case GOLDEN_AXE:
                return 6.0; // 3 hearts
            case STONE_AXE:
                return 8.0; // 4 hearts
            case IRON_AXE:
                return 10.0; // 5 hearts
            case DIAMOND_AXE:
                return 12.0; // 6 hearts
            case NETHERITE_AXE:
                return 14.0; // 7 hearts
            default:
                return -1.0;
        }
    }
}
