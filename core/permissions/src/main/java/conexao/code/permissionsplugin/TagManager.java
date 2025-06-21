package conexao.code.permissionsplugin;

import conexao.code.permissions.Tag;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TagManager {
    public String format(Tag tag, Player player, String factionTag, String icon, boolean showFaction) {
        String coloredPrefix = ChatColor.translateAlternateColorCodes('&', tag.getColor() + tag.getPrefix());
        String coloredName = ChatColor.translateAlternateColorCodes('&', tag.getColor() + player.getName());
        String base;
        if (tag.getPrefix() == null || tag.getPrefix().trim().isEmpty()) {
            base = coloredPrefix + coloredName;
        } else {
            base = coloredPrefix + " " + coloredName;
        }
        if (showFaction && factionTag != null) {
            return base + " " + ChatColor.GRAY + "[" + icon + factionTag + "]";
        }
        return base;
    }

    public void apply(Player player, Tag tag, String factionTag, String icon, boolean showFaction) {
        // Nome base sem a tag da facção, utilizado no chat
        String base = format(tag, player, null, "", false);
        // Nome completo (com facção) mostrado na lista de jogadores e acima do personagem
        String full = format(tag, player, factionTag, icon, showFaction);

        // Evita a repetição da tag da facção no chat usando apenas o nome base
        player.setDisplayName(base);
        player.setPlayerListName(full);
        player.setCustomName(full);
        player.setCustomNameVisible(true);
    }
}
