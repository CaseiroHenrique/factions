package conexao.code.commands;

import conexao.code.manager.TagManager;
import conexao.code.permissions.Tag;
import conexao.code.permissions.TagDAO;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TagCommand implements CommandExecutor {
    private final Plugin plugin;
    private final TagManager tagManager;

    public TagCommand(Plugin plugin, TagManager tagManager) {
        this.plugin = plugin;
        this.tagManager = tagManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lobby.tag")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para isso.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Uso: /tag <create|set|remove|addperm|delperm>");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
                handleCreate(sender, args);
                break;
            case "set":
                handleSet(sender, args);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            case "addperm":
                handleAddPerm(sender, args);
                break;
            case "delperm":
                handleDelPerm(sender, args);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Subcomando desconhecido.");
                break;
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "Uso: /tag create <nome> <cor> <prefixo> <sufixo> [perms...]");
            return;
        }
        String name = args[1];
        String color = args[2];
        String prefix = args[3];
        String suffix = args[4];
        Set<String> perms = new HashSet<>();
        if (args.length > 5) {
            perms.addAll(Arrays.asList(Arrays.copyOfRange(args, 5, args.length)));
        }
        Tag tag = new Tag(0, name, color, prefix, suffix, perms);
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    TagDAO.createTag(tag);
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(ChatColor.GREEN + "Tag criada com sucesso."));
                } catch (SQLException ex) {
                    plugin.getLogger().warning("Erro ao criar tag: " + ex.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(ChatColor.RED + "Falha ao criar tag."));
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /tag set <jogador> <tag>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado ou offline.");
            return;
        }
        String tagName = args[2];
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Tag tag = TagDAO.getTagByName(tagName);
                    if (tag == null) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                sender.sendMessage(ChatColor.RED + "Tag inexistente."));
                        return;
                    }
                    TagDAO.assignTagToUser(target.getUniqueId(), tag.getId());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        tagManager.apply(target);
                        sender.sendMessage(ChatColor.GREEN + "Tag atribuída!");
                    });
                } catch (SQLException ex) {
                    plugin.getLogger().warning("Erro ao atribuir tag: " + ex.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(ChatColor.RED + "Falha ao atribuir tag."));
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /tag remove <jogador>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado ou offline.");
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    TagDAO.removeTagFromUser(target.getUniqueId());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        tagManager.clear(target.getUniqueId());
                        sender.sendMessage(ChatColor.GREEN + "Tag removida!");
                    });
                } catch (SQLException ex) {
                    plugin.getLogger().warning("Erro ao remover tag: " + ex.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(ChatColor.RED + "Falha ao remover tag."));
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleAddPerm(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /tag addperm <tag> <permissao>");
            return;
        }
        String tagName = args[1];
        String perm = args[2];
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Tag tag = TagDAO.getTagByName(tagName);
                    if (tag == null) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                sender.sendMessage(ChatColor.RED + "Tag inexistente."));
                        return;
                    }
                    TagDAO.addPermissions(tag.getId(), new HashSet<>(Arrays.asList(perm)));
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(ChatColor.GREEN + "Permissão adicionada."));
                } catch (SQLException ex) {
                    plugin.getLogger().warning("Erro ao adicionar permissão: " + ex.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(ChatColor.RED + "Falha ao adicionar permissão."));
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleDelPerm(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /tag delperm <tag> <permissao>");
            return;
        }
        String tagName = args[1];
        String perm = args[2];
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Tag tag = TagDAO.getTagByName(tagName);
                    if (tag == null) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                sender.sendMessage(ChatColor.RED + "Tag inexistente."));
                        return;
                    }
                    TagDAO.removePermissions(tag.getId(), new HashSet<>(Arrays.asList(perm)));
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(ChatColor.GREEN + "Permissão removida."));
                } catch (SQLException ex) {
                    plugin.getLogger().warning("Erro ao remover permissão: " + ex.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(ChatColor.RED + "Falha ao remover permissão."));
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}
