package conexao.code.common;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.UUID;

public class RedisManager {

    private static JedisPool jedisPool;
    private static int sessionTtlSeconds;

    /**
     * Inicializa a conexão com o Redis e define o TTL padrão para sessão.
     *
     * @param host             endereço do servidor Redis
     * @param port             porta do Redis (ex: 6379)
     * @param password         senha do Redis (ou null/"" se não usar)
     * @param timeoutMillis    timeout de conexão em milissegundos
     * @param sessionTtlSeconds tempo de vida em cache de cada sessão, em segundos
     */
    public static void init(String host, int port, String password, int timeoutMillis, int sessionTtlSeconds) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(64);
        poolConfig.setMaxIdle(16);
        poolConfig.setMinIdle(4);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        if (password != null && !password.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, host, port, timeoutMillis, password);
        } else {
            jedisPool = new JedisPool(poolConfig, host, port, timeoutMillis);
        }

        RedisManager.sessionTtlSeconds = sessionTtlSeconds;
    }

    /**
     * Retorna um recurso Jedis da pool.
     */
    private static Jedis getResource() {
        if (jedisPool == null) {
            throw new IllegalStateException("RedisManager não inicializado. Chame RedisManager.init(...) primeiro.");
        }
        return jedisPool.getResource();
    }

    /**
     * Armazena em cache a autenticação do usuário por UUID, com TTL configurado.
     *
     * @param uuid UUID do jogador
     */
    public static void cacheAuthenticated(UUID uuid) {
        try (Jedis jedis = getResource()) {
            String key = getKey(uuid);
            // valor "1" é suficiente; o TTL controla expiração automática
            jedis.setex(key, sessionTtlSeconds, "1");
        }
    }

    /**
     * Verifica se o UUID está autenticado no cache Redis.
     *
     * @param uuid UUID do jogador
     * @return true se existir chave válida (não expirada)
     */
    public static boolean isAuthenticated(UUID uuid) {
        try (Jedis jedis = getResource()) {
            return jedis.exists(getKey(uuid));
        }
    }

    /**
     * Remove do cache a autenticação do usuário (por logout, expiração manual etc).
     *
     * @param uuid UUID do jogador
     */
    public static void removeAuthentication(UUID uuid) {
        try (Jedis jedis = getResource()) {
            jedis.del(getKey(uuid));
        }
    }

    /**
     * Fecha a pool de conexões. Chamar em onDisable() do plugin.
     */
    public static void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    /**
     * Gera a chave única no Redis para armazenar a sessão de um UUID.
     */
    private static String getKey(UUID uuid) {
        return "auth:session:" + uuid.toString();
    }
}
