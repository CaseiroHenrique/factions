package conexao.code.common.factions;

import conexao.code.common.DatabaseManager;

import java.sql.*;
import java.util.Optional;

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

    /**
     * Verifica se existe uma facção com a tag informada.
     * O valor da tag é normalizado para letras maiúsculas para
     * evitar problemas de diferença de caixa.
     */
    public static boolean existsByTag(String tag) throws SQLException {
        String sql = "SELECT 1 FROM factions WHERE tag = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tag.toUpperCase());
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

    public static int createFaction(String tag, String name) throws SQLException {
        String sql = "INSERT INTO factions(tag, name) VALUES(?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Sempre armazena a tag em maiúsculas
            ps.setString(1, tag.toUpperCase());
            ps.setString(2, name);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Falha ao criar faccao");
    }

    public static Optional<Integer> getIdByTag(String tag) throws SQLException {
        String sql = "SELECT id FROM factions WHERE tag = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tag.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("id"));
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<String> getTagById(int id) throws SQLException {
        String sql = "SELECT tag FROM factions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("tag"));
                }
            }
        }
        return Optional.empty();
    }

    public static void deleteFaction(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM factions WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
