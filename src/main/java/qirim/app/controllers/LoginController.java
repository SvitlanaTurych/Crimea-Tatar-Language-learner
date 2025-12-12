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
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger logger = Logger.getLogger(LoginController.class.getName());

    @FXML private VBox registerForm;
    @FXML private VBox loginForm;
    @FXML private Label messageLabel;
    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private TextField registerUsernameField;
    @FXML private PasswordField registerPasswordField;
    @FXML private TextField registerEmailField;

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    @FXML
    public void handleRegister() {
        String username = registerUsernameField.getText().trim().toLowerCase();
        String password = registerPasswordField.getText().trim();
        String email = registerEmailField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            messageLabel.setText("⚠️ Заповніть усі обов'язкові поля!");
            return;
        }

        String hashedPassword = hashPassword(password);

        try (Connection conn = DatabaseServices.getConnection()) {
            String checkUserQuery = "SELECT username FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkUserQuery);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                messageLabel.setText("⚠️ Користувач з таким ім'ям вже існує!");
                return;
            }

            String insertUserQuery = "INSERT INTO users (username, email, password) VALUES (?, ?, ?) RETURNING id";
            PreparedStatement insertStmt = conn.prepareStatement(insertUserQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, email);
            insertStmt.setString(3, hashedPassword);
            ResultSet rsInsert = insertStmt.executeQuery();

            if (rsInsert.next()) {
                int newUserId = rsInsert.getInt("id");

                // Ініціалізуємо статистику для нового користувача
                String initStatsQuery = "INSERT INTO user_stats (user_id, total_score, lessons_completed, current_streak, longest_streak) VALUES (?, 0, 0, 0, 0)";
                PreparedStatement statsStmt = conn.prepareStatement(initStatsQuery);
                statsStmt.setInt(1, newUserId);
                statsStmt.executeUpdate();

                logger.info("Новий користувач зареєстрований: " + username + " (ID: " + newUserId + ")");
            }

            messageLabel.setText("✅ Реєстрація успішна! Спробуйте увійти.");
            openLogin();

        } catch (SQLException e) {
            messageLabel.setText("❌ Помилка при реєстрації!");
            logger.log(Level.SEVERE, "Помилка при реєстрації користувача", e);
        }
    }

    @FXML
    public void handleLogin(ActionEvent actionEvent) throws IOException {
        String processedUsername = loginUsernameField.getText().trim().toLowerCase();
        String processedPassword = loginPasswordField.getText().trim();

        if (processedUsername.isEmpty() || processedPassword.isEmpty()) {
            messageLabel.setText("⚠️ Введіть логін та пароль!");
            return;
        }

        int userId = authenticateUserAndGetId(processedUsername, processedPassword);

        if (userId > 0) {
            messageLabel.setText("✅ Вхід успішний!");
            logger.info("Користувач увійшов: " + processedUsername + " (ID: " + userId + ")");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/qirim/app/home.fxml"));
            Parent root = loader.load();

            HomeController homeController = loader.getController();
            homeController.setCurrentUserId(userId);

            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
            stage.setMaximized(true);

        } else {
            messageLabel.setText("❌ Невірний логін або пароль!");
            logger.warning("Невдала спроба входу: " + processedUsername);
        }
    }

    private int authenticateUserAndGetId(String username, String password) {
        try (Connection conn = DatabaseServices.getConnection()) {
            String query = "SELECT id, password FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String storedHashedPassword = rs.getString("password");

                if (BCrypt.checkpw(password, storedHashedPassword)) {
                    return userId;
                }
            }
            return -1;
        } catch (SQLException e) {
            messageLabel.setText("❌ Помилка підключення до бази!");
            logger.log(Level.SEVERE, "Помилка при аутентифікації", e);
            return -1;
        }
    }

    @FXML
    private void openRegister() {
        loginUsernameField.clear();
        loginPasswordField.clear();
        messageLabel.setText("");
        registerForm.setVisible(true);
        loginForm.setVisible(false);
        registerUsernameField.requestFocus();
    }

    @FXML
    public void openLogin() {
        registerUsernameField.clear();
        registerPasswordField.clear();
        registerEmailField.clear();
        messageLabel.setText("");
        registerForm.setVisible(false);
        loginForm.setVisible(true);
        loginUsernameField.requestFocus();
    }
}