package qirim.app.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Клас для керування підключенням до бази даних PostgreSQL.
 *
 * УВАГА: Усунено статичне кешування підключення,
 * щоб уникнути помилки "Connection has been closed".
 * Тепер кожен виклик getConnection() повертає нове з'єднання,
 * яке автоматично закривається в контролері завдяки try-with-resources.
 */
public class DatabaseServices {

    // ВІДДАЛЕНІ ДАНІ (ПЕРЕКОНАЙТЕСЬ, ЩО ВОНИ ВВЕДЕНІ ПРАВИЛЬНО)
    private static final String URL = "jdbc:postgresql://ep-patient-meadow-agkm9anb-pooler.c-2.eu-central-1.aws.neon.tech/neondb?sslmode=require";
    private static final String USER = "neondb_owner";
    private static final String PASSWORD = "npg_ht3VH9JvbfxR";

    // ПРИБРАНО: private static Connection connection; - Усуваємо статичне кешування.

    private static final Logger LOGGER = Logger.getLogger(DatabaseServices.class.getName());

    /**
     * Створює та повертає нове з'єднання з базою даних.
     *
     * @return Об'єкт Connection.
     * @throws SQLException Якщо виникає помилка підключення.
     */
    public static Connection getConnection() throws SQLException {
        // ЗАВЖДИ ПОВЕРТАЄМО НОВЕ З'ЄДНАННЯ
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        LOGGER.info("✅ Нове підключення до бази даних встановлено.");
        return conn;
    }

    /**
     * ПРИБРАНО closeConnection(), оскільки
     * контролери використовують try-with-resources для автоматичного закриття.
     */

    // ПРИБРАНО: public static void main(String[] args) { ... }
}