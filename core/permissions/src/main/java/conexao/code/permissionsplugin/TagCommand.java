package conexao.code.permissionsplugin;

import conexao.code.permissions.Tag;
import conexao.code.permissions.TagDAO;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TagCommand implements CommandExecutor {
    private final PermissionsPlugin plugin;

    public TagCommand(PermissionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("permissions.manage")) {
            sender.sendMessage(ChatColor.RED + "Sem permissão.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
                createTag(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "addperm":
                modifyPerms(sender, args, true);
                break;
            case "removeperm":
                modifyPerms(sender, args, false);
                break;
            case "assign":
                assignTag(sender, args);
                break;
            case "remove":
                removeTag(sender, args);
                break;
            default:
                sendUsage(sender);
                break;
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Uso: /tag <create|addperm|removeperm|assign|remove> ...");
    }

    private void createTag(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "Uso: /tag create <nome> <cor> <prefixo> <sufixo> <perm1,perm2,...>");
            return;
        }
        String name = args[0];
        String color = args[1];
        String prefix = args[2];
        String suffix = args[3];
        Set<String> perms = new HashSet<>(Arrays.asList(args[4].split(",")));
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    TagDAO.createTag(new Tag(0, name, color, prefix, suffix, perms));
                    sender.sendMessage(ChatColor.GREEN + "Tag criada!");
                } catch (SQLException e) {
                    sender.sendMessage(ChatColor.RED + "Erro ao criar tag: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void modifyPerms(CommandSender sender, String[] args, boolean add) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /tag " + (add ? "addperm" : "removeperm") + " <tag> <perm1,perm2,...>");
            return;
        }
        String tagName = args[1];
        Set<String> perms = new HashSet<>(Arrays.asList(args[2].split(",")));
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Tag tag = TagDAO.getTagByName(tagName);
                    if (tag == null) {
                        sender.sendMessage(ChatColor.RED + "Tag não encontrada.");
                        return;
                    }
                    if (add) {
                        TagDAO.addPermissions(tag.getId(), perms);
                    } else {
                        TagDAO.removePermissions(tag.getId(), perms);
                    }
                    sender.sendMessage(ChatColor.GREEN + "Permissões atualizadas.");
                } catch (SQLException e) {
                    sender.sendMessage(ChatColor.RED + "Erro: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void assignTag(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /tag assign <player> <tag>");
            return;
        }
        String playerName = args[1];
        String tagName = args[2];
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Tag tag = TagDAO.getTagByName(tagName);
                    if (tag == null) {
                        sender.sendMessage(ChatColor.RED + "Tag não encontrada.");
                        return;
                    }
                    Player target = Bukkit.getPlayer(playerName);
                    UUID uuid;
                    if (target != null) {
                        uuid = target.getUniqueId();
                    } else {
                        uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
                    }
                    TagDAO.assignTagToUser(uuid, tag.getId());
                    sender.sendMessage(ChatColor.GREEN + "Tag atribuída.");
                    if (target != null && target.isOnline()) {
                        plugin.loadTag(target);
                    }
                } catch (SQLException e) {
                    sender.sendMessage(ChatColor.RED + "Erro: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void removeTag(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /tag remove <player>");
            return;
        }
        String playerName = args[1];
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Player target = Bukkit.getPlayer(playerName);
                    UUID uuid;
                    if (target != null) {
                        uuid = target.getUniqueId();
                    } else {
                        uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
                    }
                    TagDAO.removeTagFromUser(uuid);
                    sender.sendMessage(ChatColor.GREEN + "Tag removida do jogador.");
                    if (target != null && target.isOnline()) {
                        plugin.loadTag(target);
                    }
                } catch (SQLException e) {
                    sender.sendMessage(ChatColor.RED + "Erro: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}
