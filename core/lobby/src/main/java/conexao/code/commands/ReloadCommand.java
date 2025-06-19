// src/main/java/conexao/code/commands/ReloadCommand.java
package conexao.code.commands;

import conexao.code.manager.ScoreboardManager;
import conexao.code.manager.TabListManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ReloadCommand implements CommandExecutor {
    private final Plugin plugin;
    private final ScoreboardManager sb;
    private final TabListManager tl;

    public ReloadCommand(Plugin plugin, ScoreboardManager sb, TabListManager tl) {
        this.plugin = plugin;
        this.sb = sb;
        this.tl = tl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("lobby.reload")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para isso.");
            return true;
        }
        plugin.reloadConfig();
        sb.reload();
        tl.reload();
        sender.sendMessage(ChatColor.GREEN + "Lobby recarregado com sucesso!");
        return true;
    }
}
