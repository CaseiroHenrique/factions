package conexao.code.commands;

import conexao.code.hologram.HologramManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SetHologramCommand implements CommandExecutor {
    private final Plugin plugin;
    private final HologramManager hologramManager;

    public SetHologramCommand(Plugin plugin, HologramManager manager) {
        this.plugin = plugin;
        this.hologramManager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Comando apenas para jogadores.");
            return true;
        }
        if (!sender.hasPermission("lobby.sethologram")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para isso.");
            return true;
        }
        Location loc = player.getLocation();
        plugin.getConfig().set("hologram.world", loc.getWorld().getName());
        plugin.getConfig().set("hologram.x", loc.getX());
        plugin.getConfig().set("hologram.y", loc.getY());
        plugin.getConfig().set("hologram.z", loc.getZ());
        plugin.saveConfig();
        if (hologramManager != null) {
            hologramManager.setLocation(loc);
        }
        sender.sendMessage(ChatColor.GREEN + "Holograma definido!");
        return true;
    }
}
