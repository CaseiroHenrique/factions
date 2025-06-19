package conexao.code.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LobbySettingsListener implements Listener {
    private static final ItemStack COMPASS;
    private static final ItemStack SWORD;
    static {
        COMPASS = new ItemStack(Material.COMPASS);
        ItemMeta cMeta = COMPASS.getItemMeta();
        if (cMeta != null) {
            cMeta.setUnbreakable(true);
            COMPASS.setItemMeta(cMeta);
        }
        SWORD = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta sMeta = SWORD.getItemMeta();
        if (sMeta != null) {
            sMeta.setUnbreakable(true);
            SWORD.setItemMeta(sMeta);
        }
    }

    private void giveItems(Player p) {
        p.getInventory().clear();
        p.getInventory().setItem(0, COMPASS.clone());
        p.getInventory().setItem(1, SWORD.clone());
    }

    private void applyPlayerSettings(Player p) {
        p.setGameMode(GameMode.ADVENTURE);
        p.setFoodLevel(20);
        p.setMaxHealth(2.0);
        p.setHealth(2.0);
        p.setHealthScale(2.0);
        p.setHealthScaled(true);
        giveItems(p);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
        applyPlayerSettings(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        applyPlayerSettings(e.getPlayer());
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player player) {
            e.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Material type = e.getItemDrop().getItemStack().getType();
        if (type == Material.COMPASS || type.toString().endsWith("_SWORD")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        e.message(null);
    }
}
