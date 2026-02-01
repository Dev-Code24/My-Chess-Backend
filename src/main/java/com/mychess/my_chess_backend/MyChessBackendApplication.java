package com.mychess.my_chess_backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.EnableRetry;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
@EnableRetry
public class MyChessBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyChessBackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner testConnection(DataSource dataSource) {
		return args -> {
			try (Connection connection = dataSource.getConnection()) {
				System.out.println("✅ Database connection successful!");
				System.out.println("DB Url: " + connection.getMetaData().getURL());
			} catch (Exception e) {
				System.err.println("❌ DB connection failed: " + e.getMessage());
				e.printStackTrace();
			}
		};
	}

	@Bean
	public CommandLineRunner testRedisConnection(RedisTemplate<String, Object> redisTemplate) {
		return args -> {
			try {
				redisTemplate.opsForValue().set("startup_health_check", "connected");
				String result = (String) redisTemplate.opsForValue().get("startup_health_check");

				if ("connected".equals(result)) {
					System.out.println("✅ Redis connection successful! Startup check passed.");
					redisTemplate.delete("startup_health_check");
				} else {
					System.err.println("❌ Redis connection check failed: Unexpected result.");
				}
			} catch (Exception e) {
				System.err.println("❌ Redis connection failed: " + e.getMessage());
			}
		};
	}
}
