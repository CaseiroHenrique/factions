package conexao.code.factionsplugin;

import conexao.code.common.factions.FactionDAO;
import conexao.code.common.factions.FactionMemberDAO;
import conexao.code.common.factions.FactionRank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Map;
import java.util.UUID;
import java.util.Optional;

public class FactionCommand implements CommandExecutor {
    private final FactionsPlugin plugin;

    public FactionCommand(FactionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + " Comando apenas para jogadores.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "criar" -> handleCreate(player, args);
            case "convidar" -> handleInvite(player, args);
            case "aceitar", "entrar" -> handleAccept(player);
            case "sair" -> handleLeave(player);
            case "desfazer" -> handleDisband(player);
            case "transferir" -> handleTransfer(player, args);
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Uso: /f criar <tag> <nome>");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /f criar <tag> <nome>");
            return;
        }
        String tag = args[1];
        String name = args[2];
        if (tag.length() > 3) {
            player.sendMessage(ChatColor.RED + "Tag deve ter no máximo 3 caracteres.");
            return;
        }
        if (name.length() > 12) {
            player.sendMessage(ChatColor.RED + "Nome deve ter no máximo 12 caracteres.");
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (FactionMemberDAO.getFactionId(player.getUniqueId()).isPresent()) {
                        player.sendMessage(ChatColor.RED + "Você já pertence a uma facção.");
                        return;
                    }
                    if (FactionDAO.existsByTag(tag)) {
                        player.sendMessage(ChatColor.RED + "Tag já utilizada.");
                        return;
                    }
                    if (FactionDAO.existsByName(name)) {
                        player.sendMessage(ChatColor.RED + "Nome de facção já existe.");
                        return;
                    }
                    int id = FactionDAO.createFaction(tag, name);
                    FactionMemberDAO.addMember(id, player.getUniqueId(), FactionRank.REI);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Erro ao criar facção.");
                    plugin.getLogger().warning(e.getMessage());
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String msg = ChatColor.translateAlternateColorCodes('&', "&eFacção &f[" + tag + "] " + name + " criada com sucesso!");
                    player.sendMessage(msg);
                });
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /f convidar <nick>");
            return;
        }
        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Jogador não encontrado.");
            return;
        }
        try {
            Optional<Integer> facOpt = FactionMemberDAO.getFactionId(player.getUniqueId());
            if (facOpt.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Você não pertence a uma facção.");
                return;
            }
            int factionId = facOpt.get();
            Optional<FactionRank> rankOpt = FactionMemberDAO.getRank(player.getUniqueId());
            if (rankOpt.isEmpty() || rankOpt.get() != FactionRank.REI) {
                player.sendMessage(ChatColor.RED + "Apenas o Rei pode convidar.");
                return;
            }
            Map<UUID, FactionsPlugin.Invite> invites = plugin.getInvites();
            FactionsPlugin.Invite existing = invites.get(target.getUniqueId());
            long now = System.currentTimeMillis();
            if (existing != null && existing.expireAt > now) {
                player.sendMessage(ChatColor.RED + "Este jogador já possui um convite pendente.");
                return;
            }
            FactionsPlugin.Invite invite = new FactionsPlugin.Invite(factionId, player.getUniqueId(), now + 5 * 60_000L);
            invites.put(target.getUniqueId(), invite);
            TextComponent accept = Component.text("[Sim]", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/f aceitar"));
            TextComponent deny = Component.text("[Não]", NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/nao"));
            target.sendMessage(ChatColor.YELLOW + "Facção " + FactionDAO.getTagById(factionId).orElse("") + " te convidou");
            target.sendMessage(ChatColor.YELLOW + "Deseja aceitar?");
            Component message = Component.text()
                    .append(accept)
                    .append(Component.space())
                    .append(deny)
                    .build();
            target.sendMessage(message);
            player.sendMessage(ChatColor.GREEN + "Convite enviado para " + target.getName());
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Erro ao convidar jogador.");
        }
    }

    private void handleAccept(Player player) {
        Map<UUID, FactionsPlugin.Invite> invites = plugin.getInvites();
        FactionsPlugin.Invite invite = invites.remove(player.getUniqueId());
        if (invite == null || invite.expireAt < System.currentTimeMillis()) {
            player.sendMessage(ChatColor.RED + "Você não possui convite válido.");
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (FactionMemberDAO.getFactionId(player.getUniqueId()).isPresent()) {
                        player.sendMessage(ChatColor.RED + "Você já está em uma facção.");
                        return;
                    }
                    FactionMemberDAO.addMember(invite.factionId, player.getUniqueId(), FactionRank.ESCUDEIRO);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Erro ao entrar na facção.");
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.GREEN + "Você entrou na facção!");
                });
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleLeave(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Optional<FactionRank> rankOpt = FactionMemberDAO.getRank(player.getUniqueId());
                    if (rankOpt.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "Você não está em facção.");
                        return;
                    }
                    if (rankOpt.get() == FactionRank.REI) {
                        player.sendMessage(ChatColor.RED + "Transfira ou desfaca a facção antes.");
                        return;
                    }
                    FactionMemberDAO.removeMember(player.getUniqueId());
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Erro ao sair da facção.");
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.GREEN + "Você saiu da facção."));
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleDisband(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Optional<Integer> facOpt = FactionMemberDAO.getFactionId(player.getUniqueId());
                    if (facOpt.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "Você não possui facção.");
                        return;
                    }
                    Optional<FactionRank> rankOpt = FactionMemberDAO.getRank(player.getUniqueId());
                    if (rankOpt.isEmpty() || rankOpt.get() != FactionRank.REI) {
                        player.sendMessage(ChatColor.RED + "Apenas o Rei pode desfazer.");
                        return;
                    }
                    int id = facOpt.get();
                    FactionMemberDAO.removeMembersByFaction(id);
                    FactionDAO.deleteFaction(id);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Erro ao desfazer facção.");
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.GREEN + "Facção desfeita."));
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Uso: /f transferir <nick>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Jogador não encontrado.");
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Optional<Integer> facOpt = FactionMemberDAO.getFactionId(player.getUniqueId());
                    Optional<Integer> facTarget = FactionMemberDAO.getFactionId(target.getUniqueId());
                    if (facOpt.isEmpty() || facTarget.isEmpty() || !facOpt.get().equals(facTarget.get())) {
                        player.sendMessage(ChatColor.RED + "Ambos precisam estar na mesma facção.");
                        return;
                    }
                    Optional<FactionRank> rankOpt = FactionMemberDAO.getRank(player.getUniqueId());
                    if (rankOpt.isEmpty() || rankOpt.get() != FactionRank.REI) {
                        player.sendMessage(ChatColor.RED + "Apenas o Rei pode transferir.");
                        return;
                    }
                    FactionMemberDAO.updateRank(player.getUniqueId(), FactionRank.CAVALEIRO);
                    FactionMemberDAO.updateRank(target.getUniqueId(), FactionRank.REI);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Erro ao transferir liderança.");
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.GREEN + "Você transferiu a coroa.");
                    target.sendMessage(ChatColor.GREEN + "Você agora é o Rei da facção!");
                });
            }
        }.runTaskAsynchronously(plugin);
    }
}
