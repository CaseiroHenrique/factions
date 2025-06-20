package conexao.code;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReplyCommand implements CommandExecutor {
    private final ChatPlugin plugin;

    public ReplyCommand(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Comando apenas para jogadores");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Use /" + label + " <mensagem>");
            return true;
        }
        String msg = String.join(" ", args);
        plugin.sendReply((Player) sender, msg);
        return true;
    }
}
