package qirim.app.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import qirim.app.services.DatabaseServices;

import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger logger = Logger.getLogger(LoginController.class.getName());

    @FXML
    private VBox registerForm;

    @FXML
    private VBox loginForm;

    @FXML
    private Label messageLabel;

    // --- Поля для форми ВХОДУ (унікальні fx:id) ---
    @FXML
    private TextField loginUsernameField;
    @FXML
    private PasswordField loginPasswordField;

    // --- Поля для форми РЕЄСТРАЦІЇ (унікальні fx:id) ---
    @FXML
    private TextField registerUsernameField;
    @FXML
    private PasswordField registerPasswordField;
    @FXML
    private TextField registerEmailField;

    // ====================================================================
    // МЕТОД ШИФРУВАННЯ ПАРОЛЯ (BCrypt)
    // Генерує хеш з урахуванням солі (salt)
    // ====================================================================
    private String hashPassword(String password) {
        String salt = BCrypt.gensalt();
        return BCrypt.hashpw(password, salt);
    }


    @FXML
    public void handleRegister() {
        // Видаляємо пробіли (.trim()) та приводимо ім'я до нижнього регістру (.toLowerCase()) для надійності
        String rawUsername = registerUsernameField.getText();
        String username = rawUsername.trim().toLowerCase();
        String password = registerPasswordField.getText().trim();
        String email = registerEmailField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            messageLabel.setText("⚠️ Заповніть усі обов'язкові поля!");
            return;
        }

        // ХЕШУВАННЯ ПАРОЛЯ ПЕРЕД ЗБЕРІГАННЯМ (BCrypt)
        String hashedPassword = hashPassword(password);

        try (Connection conn = DatabaseServices.getConnection()) {
            // Перевірка наявності користувача (з використанням нижнього регістру)
            String checkUserQuery = "SELECT username FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkUserQuery);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                messageLabel.setText("⚠️ Користувач з таким ім'ям вже існує!");
                return;
            }

            // Вставка хешованого пароля
            String insertUserQuery = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertUserQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, email);
            insertStmt.setString(3, hashedPassword); // ЗБЕРІГАЄМО BCrypt ХЕШ
            insertStmt.executeUpdate();

            messageLabel.setText("✅ Реєстрація успішна! Спробуйте увійти.");
            logger.info("Новий користувач зареєстрований: " + username);

            // Автоматичний перехід на форму входу
            openLogin();

        } catch (SQLException e) {
            messageLabel.setText("❌ Помилка при реєстрації!");
            logger.log(Level.SEVERE, "Помилка при реєстрації користувача", e);
        }
    }

    @FXML
    public void handleLogin(ActionEvent actionEvent) throws IOException {
        String rawUsername = loginUsernameField.getText();
        String rawPassword = loginPasswordField.getText();

        String processedUsername = rawUsername.trim().toLowerCase();
        String processedPassword = rawPassword.trim();

        if (processedUsername.isEmpty() || processedPassword.isEmpty()) {
            messageLabel.setText("⚠️ Введіть логін та пароль!");
            return;
        }

        if (authenticateUser(processedUsername, processedPassword)) {

            messageLabel.setText("✅ Вхід успішний!");
            logger.info("Користувач увійшов: " + rawUsername.trim());

            // Перехід на домашню сторінку
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/qirim/app/home.fxml")));
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.show();
        } else {
            messageLabel.setText("❌ Невірний логін або пароль!");
            logger.warning("Невдала спроба входу для користувача: " + rawUsername.trim());
        }
    }

    private boolean authenticateUser(String username, String password) {
        try (Connection conn = DatabaseServices.getConnection()) {
            // 1. SELECT: Знаходимо користувача ТІЛЬКИ за ім'ям, щоб отримати його хеш
            String query = "SELECT password FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHashedPassword = rs.getString("password");

                // 2. CHECK: Використовуємо BCrypt.checkpw для безпечної перевірки.
                // Порівнює введений 'password' з хешем, використовуючи його сіль.
                return BCrypt.checkpw(password, storedHashedPassword);
            }
            return false; // Користувач не знайдений
        } catch (SQLException e) {
            messageLabel.setText("❌ Помилка підключення до бази!");
            logger.log(Level.SEVERE, "Помилка при аутентифікації користувача", e);
            return false;
        }
    }

    // 🔹 Перемикання форм
    @FXML
    private void openRegister() {
        // Очищення полів та повідомлень
        loginUsernameField.clear();
        loginPasswordField.clear();
        messageLabel.setText("");

        registerForm.setVisible(true);
        loginForm.setVisible(false);

        // Запит фокуса
        registerUsernameField.requestFocus();
    }

    @FXML
    public void openLogin() {
        // Очищення полів та повідомлень
        registerUsernameField.clear();
        registerPasswordField.clear();
        registerEmailField.clear();
        messageLabel.setText("");

        registerForm.setVisible(false);
        loginForm.setVisible(true);

        // Запит фокуса
        loginUsernameField.requestFocus();
    }
}