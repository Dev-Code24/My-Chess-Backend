package com.mychess.my_chess_backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
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
}
