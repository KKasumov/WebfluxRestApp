package com.kasumov.WebfluxRestApp.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;

@Slf4j
@Component
public class DatabaseInitializer {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.name}")
    private String dbName;

    @Value("${spring.r2dbc.username}")
    private String dbUsername;

    @Value("${spring.r2dbc.password}")
    private String dbPassword;


    @PostConstruct
    public void initialize() throws SQLException {
        log.info("Connecting to the MySQL server...");

        boolean dbExists;
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery("SHOW DATABASES;");
            dbExists = false;
            while (resultSet.next()) {
                dbExists = dbName.equals(resultSet.getString(1));
                if (dbExists) {
                    break;
                }
            }

            if (!dbExists) {
                log.error("No database found.");
                log.info("Starting to create a new database...");
                statement.executeUpdate("CREATE DATABASE " + dbName);
                log.info("Database created successfully.");
            }
        }

        if (!dbExists) {
            log.error("Database creation failed.");
            System.exit(1);
        } else {
            log.info("Connected to MySQL server successfully.");
        }
    }
}