package qirim.app.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import qirim.app.model.LeaderboardEntry;
import qirim.app.model.UserProgress;
import qirim.app.services.DatabaseServices;
import qirim.app.services.UserProgressService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeController {

    private static final Logger logger = Logger.getLogger(HomeController.class.getName());

    @FXML private VBox centerArea;
    @FXML private VBox lessonsList;
    @FXML private Label topicLabel;
    @FXML private Button prevTopicButton;
    @FXML private Button nextTopicButton;

    // Елементи правої панелі для прогресу та лідерів
    @FXML private Label userNameLabel;
    @FXML private Label streakLabel;
    @FXML private ProgressBar progressBar;
    @FXML private TableView<LeaderboardEntry> leaderboard;
    @FXML private TableColumn<LeaderboardEntry, String> nameColumn;
    @FXML private TableColumn<LeaderboardEntry, Integer> scoreColumn;

    private ObservableList<Node> originalCenterChildren;

    // ID поточного користувача (отримується при вході)
    private int currentUserId = 1; // TODO: Отримувати з сесії після логіну

    private static class Lesson {
        int lessonId;
        String title;
        int lessonNumber;

        public Lesson(int lessonId, String title, int lessonNumber) {
            this.lessonId = lessonId;
            this.title = title;
            this.lessonNumber = lessonNumber;
        }
    }

    private static class Theme {
        int themeId;
        String name;
        int themeNumber;
        List<Lesson> lessons;

        public Theme(int themeId, String name, int themeNumber, List<Lesson> lessons) {
            this.themeId = themeId;
            this.name = name;
            this.themeNumber = themeNumber;
            this.lessons = lessons;
        }
    }

    private List<Theme> themesList = new ArrayList<>();
    private int currentThemeIndex = 0;

    @FXML
    public void initialize() {
        originalCenterChildren = FXCollections.observableArrayList();
        originalCenterChildren.addAll(centerArea.getChildren());

        // Налаштування таблиці лідерів (якщо елементи є в FXML)
        if (leaderboard != null && nameColumn != null && scoreColumn != null) {
            setupLeaderboard();
        }

        themesList = loadThemesFromDB();

        if (!themesList.isEmpty()) {
            updateCenterContent(currentThemeIndex);
        } else {
            topicLabel.setText("Помилка: Теми курсу не знайдено в базі даних.");
            prevTopicButton.setDisable(true);
            nextTopicButton.setDisable(true);
        }

        // Завантаження даних користувача (якщо елементи є в FXML)
        if (userNameLabel != null || streakLabel != null || progressBar != null) {
            loadUserData();
        }
    }

    private void setupLeaderboard() {
        // PropertyValueFactory шукає методи getUsername() та getTotalScore()
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("totalScore"));

        // Встановлюємо стиль для таблиці
        leaderboard.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadLeaderboardData();
    }

    private void loadLeaderboardData() {
        try {
            List<LeaderboardEntry> leaders = UserProgressService.getLeaderboard(10);
            ObservableList<LeaderboardEntry> leaderboardData = FXCollections.observableArrayList(leaders);
            leaderboard.setItems(leaderboardData);
            logger.info("Таблиця лідерів оновлена");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Не вдалося завантажити таблицю лідерів", e);
        }
    }

    private void loadUserData() {
        try {
            // Завантаження імені користувача
            if (userNameLabel != null) {
                String username = getUsernameById(currentUserId);
                userNameLabel.setText("Merhaba, " + username + "!");
            }

            // Завантаження streak
            if (streakLabel != null) {
                UserProgressService.UserStats stats = UserProgressService.getUserStats(currentUserId);
                streakLabel.setText("🔥 " + stats.currentStreak + " днів");
            }

            // Завантаження прогресу
            if (progressBar != null) {
                UserProgress progress = UserProgressService.getUserProgress(currentUserId);
                progressBar.setProgress(progress.getProgressDecimal());
            }

            logger.info("Дані користувача завантажено");

        } catch (Exception e) {
            logger.log(Level.WARNING, "Помилка при завантаженні даних користувача", e);
        }
    }

    private String getUsernameById(int userId) {
        String sql = "SELECT username FROM users WHERE id = ?";

        try (Connection conn = DatabaseServices.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("username");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка при отриманні імені користувача", e);
        }

        return "Користувач";
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        loadUserData();
    }

    private List<Theme> loadThemesFromDB() {
        List<Theme> loadedThemes = new ArrayList<>();
        String themesQuery = "SELECT theme_id, theme_name, theme_number FROM themes ORDER BY theme_number";

        try (Connection conn = DatabaseServices.getConnection();
             PreparedStatement stmt = conn.prepareStatement(themesQuery);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int themeId = rs.getInt("theme_id");
                String themeName = rs.getString("theme_name");
                int themeNumber = rs.getInt("theme_number");

                List<Lesson> lessons = loadLessonsForTheme(conn, themeId);
                loadedThemes.add(new Theme(themeId, themeName, themeNumber, lessons));
            }

            logger.info("Успішно завантажено " + loadedThemes.size() + " тем з бази даних.");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка при завантаженні тем з БД.", e);
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Помилка БД");
            errorAlert.setHeaderText("Не вдалося завантажити структуру курсу.");
            errorAlert.setContentText("Перевірте підключення до бази даних та наявність таблиці 'themes'.");
            errorAlert.showAndWait();
        }

        return loadedThemes;
    }

    private List<Lesson> loadLessonsForTheme(Connection conn, int themeId) {
        List<Lesson> lessons = new ArrayList<>();
        String lessonsQuery = "SELECT lesson_id, lesson_name, lesson_number FROM lessons WHERE theme_id = ? ORDER BY lesson_number";

        try (PreparedStatement stmt = conn.prepareStatement(lessonsQuery)) {
            stmt.setInt(1, themeId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int lessonId = rs.getInt("lesson_id");
                String lessonName = rs.getString("lesson_name");
                int lessonNumber = rs.getInt("lesson_number");

                lessons.add(new Lesson(lessonId, lessonName, lessonNumber));
            }

            logger.info("Завантажено " + lessons.size() + " уроків для теми ID: " + themeId);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка при завантаженні уроків для теми ID: " + themeId, e);
        }

        return lessons;
    }

    @FXML
    public void handleNextTopic(ActionEvent event) {
        if (currentThemeIndex < themesList.size() - 1) {
            currentThemeIndex++;
            updateCenterContent(currentThemeIndex);
        }
    }

    @FXML
    public void handlePreviousTopic(ActionEvent event) {
        if (currentThemeIndex > 0) {
            currentThemeIndex--;
            updateCenterContent(currentThemeIndex);
        }
    }

    private void updateCenterContent(int index) {
        Theme currentTheme = themesList.get(index);

        if (centerArea.getChildren().size() != originalCenterChildren.size()) {
            centerArea.getChildren().setAll(originalCenterChildren);
        }

        topicLabel.setText(currentTheme.name);
        prevTopicButton.setDisable(index == 0);
        nextTopicButton.setDisable(index == themesList.size() - 1);

        lessonsList.getChildren().clear();

        if (currentTheme.lessons.isEmpty()) {
            Label noLessonsLabel = new Label("Уроків для цієї теми ще не додано");
            noLessonsLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            lessonsList.getChildren().add(noLessonsLabel);
        } else {
            for (Lesson lesson : currentTheme.lessons) {
                Button lessonButton = new Button(lesson.title);
                lessonButton.setPrefWidth(250);
                lessonButton.getStyleClass().add("lesson-button");

                // Перевіряємо, чи урок завершено (якщо UserProgressService доступний)
                try {
                    boolean completed = UserProgressService.isLessonCompleted(currentUserId, lesson.lessonId);
                    if (completed) {
                        lessonButton.setText(lesson.title + " ✓");
                        lessonButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Не вдалося перевірити статус завершення уроку", e);
                }

                lessonButton.setOnAction(this::openLesson);
                lessonButton.setUserData(lesson);

                lessonsList.getChildren().add(lessonButton);
            }
        }
    }

    public void goBackToTopicList(ActionEvent event) {
        updateCenterContent(currentThemeIndex);

        // Оновлюємо дані після повернення
        if (userNameLabel != null || streakLabel != null || progressBar != null) {
            loadUserData();
        }
        if (leaderboard != null) {
            loadLeaderboardData();
        }
    }

    @FXML
    public void openLesson(ActionEvent event) {
        Button sourceButton = (Button) event.getSource();
        Lesson selectedLesson = (Lesson) sourceButton.getUserData();

        int lessonId = selectedLesson.lessonId;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/qirim/app/quiz.fxml"));
            Parent root = loader.load();

            QuizController quizController = loader.getController();
            quizController.setLessonData(lessonId);
            quizController.setUserId(currentUserId); // Передаємо ID користувача

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();

            logger.info("Користувач перейшов до тесту: " + selectedLesson.title + " (Lesson ID: " + lessonId + ")");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Помилка завантаження quiz.fxml", e);
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Помилка завантаження");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Не вдалося завантажити інтерфейс тесту. Перевірте, чи існує файл quiz.fxml.");
            errorAlert.showAndWait();
        }
    }

    public void handleLogout(ActionEvent actionEvent) throws IOException {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/qirim/app/login.fxml")));
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();

            stage.show();
            stage.setFullScreen(true);
            stage.setScene(new Scene(root));

            logger.info("Користувач вийшов із системи");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Помилка завантаження сторінки входу", e);
        }
    }

    @FXML
    private void openLogin() {}
}