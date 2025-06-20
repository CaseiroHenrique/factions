package conexao.code.common.factions;

import conexao.code.common.DatabaseManager;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class FactionMemberDAO {
    public static void ensureTable() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS faction_members (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "faction_id INT NOT NULL, " +
                            "player_uuid VARCHAR(36) NOT NULL UNIQUE, " +
                            "`rank` VARCHAR(16) NOT NULL, " +
                            "FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );
        }
    }

    public static void addMember(int factionId, UUID uuid, FactionRank rank) throws SQLException {
        String sql = "INSERT INTO faction_members(faction_id, player_uuid, rank) VALUES(?,?,?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, factionId);
            ps.setString(2, uuid.toString());
            ps.setString(3, rank.name());
            ps.executeUpdate();
        }
    }

    public static void removeMember(UUID uuid) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM faction_members WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public static Optional<Integer> getFactionId(UUID uuid) throws SQLException {
        String sql = "SELECT faction_id FROM faction_members WHERE player_uuid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("faction_id"));
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<FactionRank> getRank(UUID uuid) throws SQLException {
        String sql = "SELECT rank FROM faction_members WHERE player_uuid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(FactionRank.valueOf(rs.getString("rank")));
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<UUID> getLeader(int factionId) throws SQLException {
        String sql = "SELECT player_uuid FROM faction_members WHERE faction_id = ? AND rank = 'REI'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, factionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(UUID.fromString(rs.getString("player_uuid")));
                }
            }
        }
        return Optional.empty();
    }

    public static void updateRank(UUID uuid, FactionRank rank) throws SQLException {
        String sql = "UPDATE faction_members SET rank = ? WHERE player_uuid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rank.name());
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    public static void removeMembersByFaction(int factionId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM faction_members WHERE faction_id = ?")) {
            ps.setInt(1, factionId);
            ps.executeUpdate();
        }
    }

    public static Optional<String> getFactionTag(UUID uuid) throws SQLException {
        Optional<Integer> id = getFactionId(uuid);
        if (id.isPresent()) {
            return FactionDAO.getTagById(id.get());
        }
        return Optional.empty();
    }
}
