package com.mychess.my_chess_backend.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisHealthIndicator implements HealthIndicator {
  private final RedisConnectionFactory connectionFactory;

  @Override
  public Health health() {
    try {
      RedisConnection connection = connectionFactory.getConnection();
      String pong = connection.ping();

      connection.close();

      return Health.up()
          .withDetail("redis", "Available")
          .withDetail("ping", pong)
          .withDetail("status", "Connected")
          .build();

    } catch (Exception e) {
      log.error("Redis health check failed: {}", e.getMessage());

      return Health.down()
          .withDetail("redis", "Unavailable")
          .withDetail("error", e.getMessage())
          .withDetail("status", "Disconnected")
          .build();
    }
  }
}
