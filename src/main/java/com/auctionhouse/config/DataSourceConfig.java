package com.auctionhouse.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * DataSource configuration for production (Railway) environment.
 * Parses Railway's DATABASE_URL (postgres://) and converts to JDBC format.
 */
@Configuration
@Profile("prod")
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            throw new IllegalStateException("DATABASE_URL environment variable is not set. " +
                "Make sure PostgreSQL is added to your Railway project.");
        }

        try {
            URI dbUri = new URI(databaseUrl);
            
            String username = "postgres";
            String password = "";
            if (dbUri.getUserInfo() != null) {
                String userInfo = dbUri.getUserInfo();
                int firstColon = userInfo.indexOf(':');
                username = firstColon >= 0 ? userInfo.substring(0, firstColon) : userInfo;
                password = firstColon >= 0 ? userInfo.substring(firstColon + 1) : "";
            }
            String host = dbUri.getHost();
            int port = dbUri.getPort() != -1 ? dbUri.getPort() : 5432;
            String database = dbUri.getPath().substring(1); // Remove leading '/'
            
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?ssl=true&sslmode=require", 
                host, port, database);
            
            System.out.println("✅ Connecting to PostgreSQL: " + host + ":" + port + "/" + database);
            
            return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .type(HikariDataSource.class)
                .build();
                
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to parse DATABASE_URL: " + databaseUrl, e);
        }
    }
}
