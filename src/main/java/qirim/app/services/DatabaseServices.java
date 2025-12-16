package qirim.app.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;


public class DatabaseServices {

    private static final String URL = "jdbc:postgresql://ep-patient-meadow-agkm9anb-pooler.c-2.eu-central-1.aws.neon.tech/neondb?sslmode=require";
    private static final String USER = "neondb_owner";
    private static final String PASSWORD = "npg_ht3VH9JvbfxR";

    private static final Logger LOGGER = Logger.getLogger(DatabaseServices.class.getName());

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        LOGGER.info("✅ Нове підключення до бази даних встановлено.");
        return conn;
    }
}