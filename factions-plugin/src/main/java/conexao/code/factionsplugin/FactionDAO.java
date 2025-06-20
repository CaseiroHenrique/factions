package conexao.code.factionsplugin;

import conexao.code.common.DatabaseManager;

import java.sql.*;

public class FactionDAO {
    public static void ensureTable() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS factions (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "tag VARCHAR(16) NOT NULL UNIQUE," +
                            "name VARCHAR(32) NOT NULL UNIQUE," +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );
        }
    }

    public static boolean existsByTag(String tag) throws SQLException {
        String sql = "SELECT 1 FROM factions WHERE tag = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tag);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static boolean existsByName(String name) throws SQLException {
        String sql = "SELECT 1 FROM factions WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static void createFaction(String tag, String name) throws SQLException {
        String sql = "INSERT INTO factions(tag, name) VALUES(?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tag);
            ps.setString(2, name);
            ps.executeUpdate();
        }
    }
}
