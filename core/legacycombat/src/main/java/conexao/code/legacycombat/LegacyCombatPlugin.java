package conexao.code.legacycombat;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Simple plugin that reverts combat mechanics back to 1.8 style.
 */
public class LegacyCombatPlugin extends JavaPlugin implements Listener {
    // players currently blocking with a sword
    private final Set<UUID> blocking = new HashSet<>();
    // configured damage values for weapons
    private final Map<Material, Double> weaponDamage = new EnumMap<>(Material.class);

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        loadDamageValues();
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
        // apply configured damage for swords and axes
        Double damage = weaponDamage.get(type);
        if (damage != null) {
            event.setDamage(damage);
        }
        // sword blocking
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (blocking.contains(victim.getUniqueId())) {
                event.setDamage(event.getDamage() * 0.5);
            }
        }
    }

    private void loadDamageValues() {
        weaponDamage.clear();
        ConfigurationSection axes = getConfig().getConfigurationSection("axe-damage");
        if (axes != null) {
            for (String key : axes.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat != null) {
                    weaponDamage.put(mat, axes.getDouble(key));
                }
            }
        }
        ConfigurationSection swords = getConfig().getConfigurationSection("sword-damage");
        if (swords != null) {
            for (String key : swords.getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat != null) {
                    weaponDamage.put(mat, swords.getDouble(key));
                }
            }
        }
    }
}
