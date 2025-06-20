package conexao.code.menu;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.destroystokyo.paper.profile.PlayerProfile;
import java.util.UUID;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerSelectorMenu implements Listener {
    private final Plugin plugin;
    private final Inventory menu;
    private final Map<Integer, String> serverBySlot = new HashMap<>();

    public ServerSelectorMenu(Plugin plugin) {
        this.plugin = plugin;
        this.menu = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&aServidores"));
        loadConfig();
    }

    private void loadConfig() {
        serverBySlot.clear();
        menu.clear();
        FileConfiguration cfg = plugin.getConfig();
        List<Map<?, ?>> servers = cfg.getMapList("servers");
        for (Map<?, ?> map : servers) {
            Object slotObj = map.get("slot");
            Object itemObj = map.get("item");
            Object nameObj = map.get("name");
            Object loreObj = map.get("lore");
            Object serverObj = map.get("server");
            Object headOwner = map.get("head-owner");
            Object headTexture = map.get("head-texture");

            if (slotObj == null || serverObj == null) continue;
            int slot = (int) slotObj;
            if (slot < 0 || slot >= menu.getSize()) continue;

            Material mat = null;
            if (itemObj != null) {
                mat = Material.matchMaterial(String.valueOf(itemObj));
                if (mat == null) continue;
            }

            String name = nameObj != null ? String.valueOf(nameObj) : (mat != null ? mat.name() : "");

            @SuppressWarnings("unchecked")
            List<String> lore = loreObj instanceof List ? (List<String>) loreObj : List.of();

            ItemStack item;
            ItemMeta meta;
            if (headOwner != null || headTexture != null) {
                item = new ItemStack(Material.PLAYER_HEAD);
                meta = item.getItemMeta();
                if (meta instanceof SkullMeta skullMeta) {
                    if (headOwner != null) {
                        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(String.valueOf(headOwner)));
                    }
                    if (headTexture != null) {
                        PlayerProfile profile = (PlayerProfile) Bukkit.createPlayerProfile(UUID.randomUUID());
                        profile.setProperty(new ProfileProperty("textures", String.valueOf(headTexture)));
                        skullMeta.setPlayerProfile(profile);
                    }
                    meta = skullMeta;
                }
            } else if (mat != null) {
                item = new ItemStack(mat);
                meta = item.getItemMeta();
            } else {
                continue;
            }

            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                List<String> cLore = new ArrayList<>();
                for (String line : lore) {
                    cLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(cLore);
                item.setItemMeta(meta);
            }

            menu.setItem(slot, item);
            serverBySlot.put(slot, String.valueOf(serverObj));
        }
    }

    private void connect(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void open(Player player) {
        player.openInventory(menu);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!e.hasItem()) return;
        ItemStack stack = e.getItem();
        if (stack.getType() != Material.COMPASS) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        if (ChatColor.translateAlternateColorCodes('&', "&aServidores").equals(meta.getDisplayName())) {
            e.setCancelled(true);
            open(e.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTopInventory().equals(menu)) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        String server = serverBySlot.get(slot);
        if (server != null && e.getWhoClicked() instanceof Player p) {
            p.closeInventory();
            connect(p, server);
        }
    }
}
