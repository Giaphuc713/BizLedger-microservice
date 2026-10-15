package com.overcode250204.identityservice.service.impl;

import com.overcode250204.identityservice.service.IHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * HealthServiceImpl — Service implementation mapping real verification tests
 * for core platform modules: PostgreSQL DB, Kafka cluster, Elasticsearch nodes,
 * and Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthService implements IHealthService {

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final KafkaAdmin kafkaAdmin;

    @Override
    public Map<String, Object> checkLiveness() {
        return Map.of("alive", true);
    }

    @Override
    public Map<String, Object> checkReadiness() {
        boolean isDocker = environment.acceptsProfiles(Profiles.of("docker"));

        // 1. PostgreSQL check
        long pgLatency = checkPostgres();
        String pgStatus = pgLatency >= 0 ? "up" : "down";

        // 2. Redis check
        String redisHost = isDocker ? "bizledger-redis" : "localhost";
        long redisLatency = checkRedis(redisHost, 6379);
        String redisStatus = redisLatency >= 0 ? "up" : "down";

        // 3. Elasticsearch check
        String esHost = isDocker ? "bizledger-elasticsearch" : "localhost";
        long esLatency = checkElasticsearch(esHost, 9200);
        String esStatus = esLatency >= 0 ? "up" : "down";

        // 4. Kafka check
        String kafkaHost = isDocker ? "bizledger-kafka" : "localhost";
        int kafkaPort = isDocker ? 19092 : 9092;
        if (kafkaAdmin != null) {
            Object bootstrap = kafkaAdmin.getConfigurationProperties().get("bootstrap.servers");
            if (bootstrap != null) {
                String val = bootstrap.toString();
                String[] parts = val.split(",");
                if (parts.length > 0) {
                    String[] hostPort = parts[0].trim().split(":");
                    kafkaHost = hostPort[0];
                    if (hostPort.length > 1) {
                        try {
                            kafkaPort = Integer.parseInt(hostPort[1]);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
        long kafkaLatency = checkTcpConnection(kafkaHost, kafkaPort);
        String kafkaStatus = kafkaLatency >= 0 ? "up" : "down";

        List<Map<String, Object>> services = List.of(
                Map.of("name", "PostgreSQL", "status", pgStatus, "latencyMs", pgLatency >= 0 ? pgLatency : 0),
                Map.of("name", "Kafka", "status", kafkaStatus, "latencyMs", kafkaLatency >= 0 ? kafkaLatency : 0),
                Map.of("name", "Elasticsearch", "status", esStatus, "latencyMs", esLatency >= 0 ? esLatency : 0),
                Map.of("name", "Redis", "status", redisStatus, "latencyMs", redisLatency >= 0 ? redisLatency : 0));

        boolean ready = "up".equals(pgStatus) && "up".equals(kafkaStatus) && "up".equals(esStatus)
                && "up".equals(redisStatus);

        log.debug("[HealthService] Postgres:{} ({}ms), Redis:{} ({}ms), ES:{} ({}ms), Kafka:{} ({}ms) -> Ready:{}",
                pgStatus, pgLatency, redisStatus, redisLatency, esStatus, esLatency, kafkaStatus, kafkaLatency, ready);

        return Map.of(
                "ready", ready,
                "services", services);
    }

    private long checkPostgres() {
        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            log.warn("[HealthService] PostgreSQL connection check failed: {}", e.getMessage());
            return -1;
        }
    }

    private long checkTcpConnection(String host, int port) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return System.currentTimeMillis() - start;
        } catch (Exception e) {
            log.warn("[HealthService] TCP check failed for {}:{}: {}", host, port, e.getMessage());
            return -1;
        }
    }

    private long checkElasticsearch(String host, int port) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(2000);
            socket.connect(new InetSocketAddress(host, port), 2000);
            OutputStream out = socket.getOutputStream();
            out.write("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes());
            out.flush();
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[64];
            int read = in.read(buffer);
            if (read > 0) {
                String resp = new String(buffer, 0, read);
                if (resp.contains("HTTP/1.1")) {
                    return System.currentTimeMillis() - start;
                }
            }
            return -1;
        } catch (Exception e) {
            log.warn("[HealthService] Elasticsearch HTTP ping failed on {}:{}: {}", host, port, e.getMessage());
            return -1;
        }
    }

    private long checkRedis(String host, int port) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(2000);
            socket.connect(new InetSocketAddress(host, port), 2000);
            OutputStream out = socket.getOutputStream();
            out.write("PING\r\n".getBytes());
            out.flush();
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[64];
            int read = in.read(buffer);
            if (read > 0) {
                String resp = new String(buffer, 0, read);
                if (resp.contains("PONG") || resp.contains("NOAUTH")) {
                    return System.currentTimeMillis() - start;
                }
            }
            return -1;
        } catch (Exception e) {
            log.warn("[HealthService] Redis PING check failed on {}:{}: {}", host, port, e.getMessage());
            return -1;
        }
    }
}
