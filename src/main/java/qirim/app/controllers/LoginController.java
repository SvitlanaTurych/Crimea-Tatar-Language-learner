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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
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
    @FXML private Label passwordRequirementsLabel;
    @FXML private Label emailRequirementsLabel;

    // Константи для валідації
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // КОЛЬОРИ ДИЗАЙНУ (ВІДПОВІДАЮТЬ FXML)
    private static final String STYLE_COLOR_SUCCESS = "#28a745";
    private static final String STYLE_COLOR_ERROR_RAMKA = "#dc3545";
    private static final String STYLE_COLOR_ACCENT = "#D4AF78";
    private static final String STYLE_COLOR_INFO = "#FFFFFF";
    private static final String STYLE_BACKGROUND_ERROR = "rgba(4, 49, 132, 0.9)";

    // Базовий стиль для мітки повідомлень
    private static final String BASE_MESSAGE_STYLE =
            "-fx-font-size: 14px; " +
                    "-fx-padding: 10px; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-border-radius: 8px; " +
                    "-fx-max-width: 450px; " +
                    "-fx-alignment: center; ";

    // Зберігаємо оригінальні стилі
    private String originalPasswordStyle = "";
    private String originalEmailStyle = "";

    @FXML
    public void initialize() {
        if (registerPasswordField != null) {
            originalPasswordStyle = registerPasswordField.getStyle();
        }
        if (registerEmailField != null) {
            originalEmailStyle = registerEmailField.getStyle();
        }

        if (passwordRequirementsLabel != null) {
            passwordRequirementsLabel.setText(
                    "Вимоги до паролю:\n" +
                            "• Мінімум 8 символів\n" +
                            "• Велика літера (A-Z)\n" +
                            "• Маленька літера (a-z)\n" +
                            "• Цифра (0-9)"
            );
            passwordRequirementsLabel.setStyle("-fx-text-fill: " + STYLE_COLOR_INFO + "; -fx-font-size: 11px;");
        }

        if (emailRequirementsLabel != null) {
            emailRequirementsLabel.setText("Приклад: user@example.com");
            emailRequirementsLabel.setStyle("-fx-text-fill: " + STYLE_COLOR_INFO + "; -fx-font-size: 11px;");
        }

        if (registerPasswordField != null) {
            registerPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.isEmpty()) {
                    validatePasswordRealtime(newValue);
                } else {
                    registerPasswordField.setStyle(originalPasswordStyle);
                }
            });
        }

        if (registerEmailField != null) {
            registerEmailField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.isEmpty()) {
                    validateEmailRealtime(newValue);
                } else {
                    registerEmailField.setStyle(originalEmailStyle);
                }
            });
        }
    }

    private List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Пароль не може бути порожнім");
            return errors;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.add("Мінімум " + MIN_PASSWORD_LENGTH + " символів");
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("Хоча б одна велика літера (A-Z)");
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("Хоча б одна мала літера (a-z)");
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            errors.add("Хоча б одна цифра (0-9)");
        }

        return errors;
    }

    private List<String> validateEmail(String email) {
        List<String> errors = new ArrayList<>();

        if (email == null || email.trim().isEmpty()) {
            errors.add("Email не може бути порожнім");
            return errors;
        }

        email = email.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("Невірний формат email адреси");
            return errors;
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            errors.add("Email повинен містити рівно один символ @");
            return errors;
        }

        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() < 3) {
            errors.add("Частина email до @ повинна містити мінімум 3 символи");
        }

        if (!domain.contains(".")) {
            errors.add("Домен повинен містити крапку (наприклад: gmail.com)");
        }

        if (email.contains("..")) {
            errors.add("Email не може містити дві крапки підряд");
        }

        return errors;
    }

    private void validatePasswordRealtime(String password) {
        List<String> errors = validatePassword(password);

        if (errors.isEmpty()) {
            // Зелена рамка для успіху
            registerPasswordField.setStyle(originalPasswordStyle +
                    "-fx-border-color: " + STYLE_COLOR_SUCCESS + "; -fx-border-width: 3;");
        } else {
            // Червона рамка для помилки
            registerPasswordField.setStyle(originalPasswordStyle +
                    "-fx-border-color: " + STYLE_COLOR_ERROR_RAMKA + "; -fx-border-width: 3;");
        }
    }

    private void validateEmailRealtime(String email) {
        List<String> errors = validateEmail(email);

        if (errors.isEmpty()) {
            // Зелена рамка для успіху
            registerEmailField.setStyle(originalEmailStyle +
                    "-fx-border-color: " + STYLE_COLOR_SUCCESS + "; -fx-border-width: 3;");
        } else {
            // Червона рамка для помилки
            registerEmailField.setStyle(originalEmailStyle +
                    "-fx-border-color: " + STYLE_COLOR_ERROR_RAMKA + "; -fx-border-width: 3;");
        }
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    @FXML
    public void handleRegister() {
        String username = registerUsernameField.getText().trim().toLowerCase();
        String password = registerPasswordField.getText();
        String email = registerEmailField.getText().trim().toLowerCase();
        String errorMessage = "";

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            errorMessage = "Заповніть усі обов'язкові поля!";
        } else {
            List<String> emailErrors = validateEmail(email);
            List<String> passwordErrors = validatePassword(password);

            if (!emailErrors.isEmpty()) {
                errorMessage += "Невірний email:\n- " + String.join("\n- ", emailErrors);
            }

            if (!passwordErrors.isEmpty()) {
                if (!errorMessage.isEmpty()) errorMessage += "\n\n";
                errorMessage += "Невірний пароль. Вимоги:\n- " + String.join("\n- ", passwordErrors);
            }

            if (username.length() < 3 && errorMessage.isEmpty()) {
                errorMessage = "Ім'я користувача повинно містити мінімум 3 символи!";
            }
        }

        if (!errorMessage.isEmpty()) {
            messageLabel.setText(errorMessage);
            messageLabel.setStyle(BASE_MESSAGE_STYLE +
                    "-fx-text-fill: " + STYLE_COLOR_ACCENT + "; " +
                    "-fx-background-color: " + STYLE_BACKGROUND_ERROR + ";");
            return;
        }

        String hashedPassword = hashPassword(password);

        try (Connection conn = DatabaseServices.getConnection()) {
            String checkUserQuery = "SELECT username FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkUserQuery);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                messageLabel.setText("Користувач з таким ім'ям вже існує!");
                messageLabel.setStyle(BASE_MESSAGE_STYLE +
                        "-fx-text-fill: " + STYLE_COLOR_ACCENT + "; " +
                        "-fx-background-color: " + STYLE_BACKGROUND_ERROR + ";");
                return;
            }

            String checkEmailQuery = "SELECT email FROM users WHERE email = ?";
            PreparedStatement checkEmailStmt = conn.prepareStatement(checkEmailQuery);
            checkEmailStmt.setString(1, email);
            ResultSet rsEmail = checkEmailStmt.executeQuery();

            if (rsEmail.next()) {
                messageLabel.setText("Користувач з таким email вже існує!");
                messageLabel.setStyle(BASE_MESSAGE_STYLE +
                        "-fx-text-fill: " + STYLE_COLOR_ACCENT + "; " +
                        "-fx-background-color: " + STYLE_BACKGROUND_ERROR + ";");
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

                String initStatsQuery = "INSERT INTO user_stats (user_id, total_score, lessons_completed, current_streak, longest_streak) VALUES (?, 0, 0, 0, 0)";
                PreparedStatement statsStmt = conn.prepareStatement(initStatsQuery);
                statsStmt.setInt(1, newUserId);
                statsStmt.executeUpdate();

                logger.info("Новий користувач зареєстрований: " + username + " (ID: " + newUserId + ")");
            }

            messageLabel.setText("Реєстрація успішна! Спробуйте увійти.");
            messageLabel.setStyle(BASE_MESSAGE_STYLE +
                    "-fx-text-fill: " + STYLE_COLOR_SUCCESS + "; " +
                    "-fx-background-color: " + STYLE_BACKGROUND_ERROR + ";"); // Темний фон для успіху

            registerUsernameField.clear();
            registerPasswordField.clear();
            registerEmailField.clear();
            registerPasswordField.setStyle(originalPasswordStyle);
            registerEmailField.setStyle(originalEmailStyle);

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            pause.setOnFinished(e -> openLogin());
            pause.play();

        } catch (SQLException e) {
            messageLabel.setText("Помилка при реєстрації! Перевірте базу даних.");
            messageLabel.setStyle(BASE_MESSAGE_STYLE +
                    "-fx-text-fill: " + STYLE_COLOR_ERROR_RAMKA + "; " +
                    "-fx-background-color: " + STYLE_BACKGROUND_ERROR + ";");
            logger.log(Level.SEVERE, "Помилка при реєстрації користувача", e);
        }
    }

    @FXML
    public void handleLogin(ActionEvent actionEvent) throws IOException {
        String processedUsername = loginUsernameField.getText().trim().toLowerCase();
        String processedPassword = loginPasswordField.getText();

        if (processedUsername.isEmpty() || processedPassword.isEmpty()) {
            messageLabel.setText("Введіть логін та пароль!");
            messageLabel.setStyle(BASE_MESSAGE_STYLE +
                    "-fx-text-fill: " + STYLE_COLOR_ACCENT + "; " +
                    "-fx-background-color: " + STYLE_BACKGROUND_ERROR + ";");
            return;
        }

        int userId = authenticateUserAndGetId(processedUsername, processedPassword);

        if (userId > 0) {
            messageLabel.setText("✅ Вхід успішний!");
            messageLabel.setStyle(BASE_MESSAGE_STYLE +
                    "-fx-text-fill: " + STYLE_COLOR_SUCCESS + "; " +
                    "-fx-background-color: " + STYLE_BACKGROUND_ERROR + ";");
            logger.info("Користувач увійшов: " + processedUsername + " (ID: " + userId + ")");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/qirim/app/home.fxml"));
            Parent root = loader.load();

            HomeController homeController = loader.getController();
            homeController.setCurrentUserId(userId);

            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(scene);

            stage.setFullScreen(true);
            stage.show();

        } else {
            messageLabel.setText("Невірний логін або пароль!");
            messageLabel.setStyle(BASE_MESSAGE_STYLE +
                    "-fx-text-fill: " + STYLE_COLOR_ACCENT + "; " +
                    "-fx-background-color: " + STYLE_BACKGROUND_ERROR + ";");
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
            messageLabel.setText("Помилка підключення до бази!");
            messageLabel.setStyle(BASE_MESSAGE_STYLE +
                    "-fx-text-fill: " + STYLE_COLOR_ERROR_RAMKA + "; " +
                    "-fx-background-color: " + STYLE_BACKGROUND_ERROR + ";");
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
        registerPasswordField.setStyle(originalPasswordStyle);
        registerEmailField.setStyle(originalEmailStyle);
        messageLabel.setText("");
        registerForm.setVisible(false);
        loginForm.setVisible(true);
        loginUsernameField.requestFocus();
    }
}