// src/main/java/conexao/code/common/DatabaseManager.java
package conexao.code.common;

import java.sql.*;

public class DatabaseManager {
    private static String url;
    private static String user;
    private static String pass;

    /**
     * Inicializa a conexão e garante a criação das tabelas necessárias.
     *
     * @param host       endereço do servidor MySQL
     * @param port       porta do MySQL
     * @param database   nome do banco de dados
     * @param username   usuário do banco
     * @param password   senha do banco
     */
    public static void init(String host, int port, String database, String username, String password) {
        // Monta a URL de conexão
        url  = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true",
                host, port, database
        );
        user = username;
        pass = password;

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {

            // 1) Cria tabela users incluindo ip_address
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users (" +
                            "username      VARCHAR(16)    PRIMARY KEY," +
                            "password_hash VARCHAR(60)    NOT NULL," +
                            "ip_address    VARCHAR(45)    NOT NULL DEFAULT ''," +
                            "created_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );

            // 2) Caso a tabela já existisse sem a coluna ip_address, adiciona-la
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "users", "ip_address")) {
                if (!rs.next()) {
                    stmt.executeUpdate(
                            "ALTER TABLE users ADD COLUMN ip_address VARCHAR(45) NOT NULL DEFAULT '';"
                    );
                }
            }

            // 3) Cria tabela de logs de kick
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS kick_log (" +
                            "id          BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "username    VARCHAR(16)            NOT NULL," +
                            "reason      VARCHAR(255)           NOT NULL," +
                            "kicked_at   TIMESTAMP               DEFAULT CURRENT_TIMESTAMP" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fornece uma conexão ativa com o banco.
     *
     * @return Connection
     * @throws SQLException em caso de falha na conexão
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Insere um registro de usuário na tabela 'users', com fallback para criar
     * a coluna 'ip_address' caso ela não exista, e reexecuta o INSERT.
     *
     * @param username     nome de usuário
     * @param passwordHash hash da senha
     * @param ip           endereço IP do jogador
     * @throws SQLException em caso de erro diferente de coluna ausente
     */
    public static void insertUser(String username, String passwordHash, String ip) throws SQLException {
        String sql = "INSERT INTO users(username, password_hash, ip_address) VALUES(?,?,?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, ip);
            ps.executeUpdate();

        } catch (SQLException e) {
            // SQLState 42S22 => coluna não existe
            if ("42S22".equals(e.getSQLState()) && e.getMessage().contains("ip_address")) {
                // Cria coluna ip_address e tenta inserir novamente
                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                            "ALTER TABLE users ADD COLUMN ip_address VARCHAR(45) NOT NULL DEFAULT '';"
                    );
                }
                // Reexecuta o INSERT
                try (Connection conn = getConnection();
                     PreparedStatement ps2 = conn.prepareStatement(sql)) {
                    ps2.setString(1, username);
                    ps2.setString(2, passwordHash);
                    ps2.setString(3, ip);
                    ps2.executeUpdate();
                }
            } else {
                throw e;
            }
        }
    }
}
