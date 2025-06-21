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
        String full = format(tag, player, factionTag, icon, showFaction);
        player.setDisplayName(full);
        player.setPlayerListName(full);
        player.setCustomName(full);
        player.setCustomNameVisible(true);
    }
}
