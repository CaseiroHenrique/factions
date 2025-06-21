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
import org.bukkit.plugin.Plugin;
import conexao.code.manager.ScoreboardManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.lang.reflect.Field;
import java.util.UUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerSelectorMenu implements Listener {
    private final Plugin plugin;
    private final ScoreboardManager scoreboardManager;
    private final Inventory menu;
    private final Map<Integer, String> serverBySlot = new HashMap<>();

    private static class Entry {
        int slot;
        ItemStack baseItem;
        String name;
        List<String> lore;
        String server;
    }

    private final List<Entry> entries = new ArrayList<>();

    public ServerSelectorMenu(Plugin plugin, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
        this.menu = Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&aServidores"));
        loadConfig();
    }

    private void loadConfig() {
        serverBySlot.clear();
        entries.clear();
        FileConfiguration cfg = plugin.getConfig();
        List<Map<?, ?>> servers = cfg.getMapList("servers");
        for (Map<?, ?> map : servers) {
            Object slotObj   = map.get("slot");
            Object itemObj   = map.get("item");
            Object headObj   = map.get("head");
            Object nameObj   = map.get("name");
            Object loreObj   = map.get("lore");
            Object serverObj = map.get("server");

            if (slotObj == null || serverObj == null) continue;
            int slot = (int) slotObj;
            if (slot < 0 || slot >= menu.getSize()) continue;

            ItemStack item = null;
            Material mat = null;

            if (headObj != null) {
                String skin = String.valueOf(headObj);
                item = createHead(skin);
                mat = item.getType();
            } else if (itemObj != null) {
                mat = Material.matchMaterial(String.valueOf(itemObj));
                if (mat != null) {
                    item = new ItemStack(mat);
                }
            }

            if (item == null || mat == null) continue;

            String name = nameObj != null ? String.valueOf(nameObj) : mat.name();
            @SuppressWarnings("unchecked")
            List<String> lore = loreObj instanceof List ? (List<String>) loreObj : List.of();

            Entry entry = new Entry();
            entry.slot = slot;
            entry.baseItem = item;
            entry.name = name;
            entry.lore = lore;
            entry.server = String.valueOf(serverObj);
            entries.add(entry);
        }
    }

    private ItemStack createHead(String skin) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            if (skin.startsWith("eyJ")) {
                GameProfile profile = new GameProfile(UUID.randomUUID(), null);
                profile.getProperties().put("textures", new Property("textures", skin));
                try {
                    Field profileField = meta.getClass().getDeclaredField("profile");
                    profileField.setAccessible(true);
                    profileField.set(meta, profile);
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                }
            } else {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(skin));
            }
            head.setItemMeta(meta);
        }

        return head;
    }

    private void connect(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void open(Player player) {
        menu.clear();
        serverBySlot.clear();
        for (Entry entry : entries) {
            ItemStack item = entry.baseItem.clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', entry.name));
                List<String> loreLines = new ArrayList<>();
                for (String line : entry.lore) {
                    String replaced = scoreboardManager.applyPlaceholders(line);
                    loreLines.add(ChatColor.translateAlternateColorCodes('&', replaced));
                }
                meta.setLore(loreLines);
                item.setItemMeta(meta);
            }
            menu.setItem(entry.slot, item);
            serverBySlot.put(entry.slot, entry.server);
            scoreboardManager.requestPlayerCount(entry.server);
        }
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
