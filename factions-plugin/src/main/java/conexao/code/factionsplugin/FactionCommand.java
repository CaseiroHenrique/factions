package conexao.code.factionsplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class FactionCommand implements CommandExecutor {
    private final FactionsPlugin plugin;

    public FactionCommand(FactionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("criar")) {
            sender.sendMessage(ChatColor.YELLOW + " Uso: /f criar <tag> <nome> ");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + " Comando apenas para jogadores. ");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + " Uso: /f criar <tag> <nome> ");
            return true;
        }

        String tag = args[1];
        String name = args[2];

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (FactionDAO.existsByTag(tag)) {
                        sender.sendMessage(ChatColor.RED + " Tag já utilizada. ");
                        return;
                    }
                    if (FactionDAO.existsByName(name)) {
                        sender.sendMessage(ChatColor.RED + " Nome de facção já existe. ");
                        return;
                    }
                    FactionDAO.createFaction(tag, name);
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + " Erro ao criar facção. ");
                    plugin.getLogger().warning(e.getMessage());
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(" ");
                    sender.sendMessage(ChatColor.GREEN + "Facção criada com sucesso!");
                    sender.sendMessage(ChatColor.AQUA + " Tag: " + ChatColor.WHITE + tag + ChatColor.AQUA + " Nome: " + ChatColor.WHITE + name);
                    sender.sendMessage(" ");
                });
            }
        }.runTaskAsynchronously(plugin);
        return true;
    }
}
