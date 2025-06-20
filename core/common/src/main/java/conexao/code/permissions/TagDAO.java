package conexao.code.permissions;

import conexao.code.common.DatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * DAO para manipular tags e relacionamento com jogadores.
 */
public class TagDAO {

    public static void createTag(Tag tag) throws SQLException {
        String sql = "INSERT INTO tags(name, color, prefix, suffix, permissions) VALUES(?,?,?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tag.getName());
            ps.setString(2, tag.getColor());
            ps.setString(3, tag.getPrefix());
            ps.setString(4, tag.getSuffix());
            ps.setString(5, String.join(",", tag.getPermissions()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    tag.setId(rs.getInt(1));
                }
            }
        }
    }

    public static void assignTagToUser(UUID uuid, int tagId) throws SQLException {
        String sql = "REPLACE INTO user_tags(user_uuid, tag_id) VALUES(?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, tagId);
            ps.executeUpdate();
        }
    }

    public static void removeTagFromUser(UUID uuid) throws SQLException {
        String sql = "DELETE FROM user_tags WHERE user_uuid=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public static void addPermissions(int tagId, Set<String> toAdd) throws SQLException {
        Tag tag = getTag(tagId);
        if (tag == null) return;
        Set<String> perms = new HashSet<>(tag.getPermissions());
        perms.addAll(toAdd);
        updateTagPermissions(tagId, perms);
    }

    public static void removePermissions(int tagId, Set<String> toRemove) throws SQLException {
        Tag tag = getTag(tagId);
        if (tag == null) return;
        Set<String> perms = new HashSet<>(tag.getPermissions());
        perms.removeAll(toRemove);
        updateTagPermissions(tagId, perms);
    }

    public static void updateTagPermissions(int tagId, Set<String> permissions) throws SQLException {
        String sql = "UPDATE tags SET permissions=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, String.join(",", permissions));
            ps.setInt(2, tagId);
            ps.executeUpdate();
        }
    }

    /**
     * Atualiza todas as informações da tag.
     */
    public static void updateTag(Tag tag) throws SQLException {
        String sql = "UPDATE tags SET color=?, prefix=?, suffix=?, permissions=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tag.getColor());
            ps.setString(2, tag.getPrefix());
            ps.setString(3, tag.getSuffix());
            ps.setString(4, String.join(",", tag.getPermissions()));
            ps.setInt(5, tag.getId());
            ps.executeUpdate();
        }
    }

    public static Tag getTag(int id) throws SQLException {
        String sql = "SELECT * FROM tags WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return fromResultSet(rs);
                }
            }
        }
        return null;
    }

    public static Tag getTagByName(String name) throws SQLException {
        String sql = "SELECT * FROM tags WHERE name=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return fromResultSet(rs);
                }
            }
        }
        return null;
    }

    public static Tag getTagByUser(UUID uuid) throws SQLException {
        String sql = "SELECT t.* FROM tags t JOIN user_tags ut ON t.id = ut.tag_id WHERE ut.user_uuid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return fromResultSet(rs);
                }
            }
        }
        return null;
    }

    private static Tag fromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String color = rs.getString("color");
        String prefix = rs.getString("prefix");
        String suffix = rs.getString("suffix");
        String permStr = rs.getString("permissions");
        Set<String> perms = new HashSet<>();
        if (permStr != null && !permStr.isEmpty()) {
            perms.addAll(Arrays.asList(permStr.split(",")));
        }
        return new Tag(id, name, color, prefix, suffix, perms);
    }
}
