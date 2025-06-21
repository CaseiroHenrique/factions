package conexao.code.nametag;

import conexao.code.common.factions.FactionMemberDAO;
import conexao.code.common.factions.FactionRank;
import conexao.code.permissions.Tag;
import conexao.code.permissions.TagDAO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Optional;

public class NametagPlugin extends JavaPlugin implements Listener {
    private Scoreboard scoreboard;
    private Objective healthObjective;

    @Override
    public void onEnable() {
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        healthObjective = scoreboard.getObjective("vida");
        if (healthObjective == null) {
            healthObjective = scoreboard.registerNewObjective("vida", "health",
                    Component.text("\u2764", NamedTextColor.RED));
            healthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        for (Player p : Bukkit.getOnlinePlayers()) {
            applyNametag(p);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().setScoreboard(scoreboard);
        applyNametag(event.getPlayer());
    }

    public void applyNametag(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            Tag tag = null;
            String faction = null;
            String icon = "";
            try {
                tag = TagDAO.getTagByUser(player.getUniqueId());
                Optional<String> facOpt = FactionMemberDAO.getFactionTag(player.getUniqueId());
                if (facOpt.isPresent()) {
                    faction = facOpt.get();
                    icon = FactionMemberDAO.getRank(player.getUniqueId())
                            .map(FactionRank::getIcon)
                            .orElse("");
                }
            } catch (Exception ex) {
                getLogger().warning("Erro ao carregar tag: " + ex.getMessage());
            }
            Tag finalTag = tag;
            String finalFaction = faction;
            String finalIcon = icon;
            Bukkit.getScheduler().runTask(this, () -> setTeam(player, finalTag, finalFaction, finalIcon));
        });
    }

    private void setTeam(Player player, Tag tag, String factionTag, String icon) {
        Team team = scoreboard.getTeam(player.getName());
        if (team == null) {
            team = scoreboard.registerNewTeam(player.getName());
        }
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        StringBuilder prefix = new StringBuilder();
        if (factionTag != null) {
            prefix.append(ChatColor.GRAY).append("[").append(icon).append(factionTag).append("] ");
        }
        if (tag != null && tag.getPrefix() != null && !tag.getPrefix().isEmpty()) {
            prefix.append(ChatColor.translateAlternateColorCodes('&', tag.getColor() + tag.getPrefix())).append(' ');
        }
        String suffix = "";
        if (tag != null && tag.getSuffix() != null && !tag.getSuffix().isEmpty()) {
            suffix = ChatColor.translateAlternateColorCodes('&', tag.getColor() + tag.getSuffix());
        }

        Component prefixComp = LegacyComponentSerializer.legacySection().deserialize(prefix.toString());
        Component suffixComp = LegacyComponentSerializer.legacySection().deserialize(suffix);
        team.prefix(prefixComp);
        team.suffix(suffixComp);
    }
}
