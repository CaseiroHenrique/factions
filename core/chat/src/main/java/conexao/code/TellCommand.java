package conexao.code;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class TellCommand implements CommandExecutor {
    private final ChatPlugin plugin;

    public TellCommand(ChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Comando apenas para jogadores");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Use /" + label + " <jogador> <mensagem>");
            return true;
        }
        String target = args[0];
        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.sendTell((Player) sender, target, msg);
        return true;
    }
}
