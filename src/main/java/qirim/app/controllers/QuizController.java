package qirim.app.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import qirim.app.services.DatabaseServices;
import qirim.app.services.UserProgressService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuizController {

    private static final Logger logger = Logger.getLogger(QuizController.class.getName());

    @FXML private Label questionLabel;
    @FXML private Label scoreLabel;
    @FXML private VBox optionsContainer;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button backToHomeButton;
    @FXML private Label resultMessageLabel;

    private static class Question {
        int questionId;
        String text;
        List<String> options;
        List<Integer> optionIds;
        int correctAnswerIndex;

        public Question(int questionId, String text, List<String> options, List<Integer> optionIds, int correctAnswerIndex) {
            this.questionId = questionId;
            this.text = text;
            this.options = options;
            this.optionIds = optionIds;
            this.correctAnswerIndex = correctAnswerIndex;
        }
    }

    private int lessonId;
    private int userId = -1; // ID поточного користувача
    private List<Question> quizQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private List<Integer> userAnswers;

    public void setLessonData(int lessonId) {
        this.lessonId = lessonId;
        this.quizQuestions = loadQuestionsFromDB();
        this.userAnswers = new ArrayList<>(Collections.nCopies(quizQuestions.size(), -1));

        if (!quizQuestions.isEmpty()) {
            displayQuestion();
        } else {
            questionLabel.setText("На жаль, запитань для цього уроку не знайдено.");
        }

        updateNavigationButtons();
    }

    private int themeIndex = 0; // Додайте це поле

    public void setUserId(int userId) {
        this.userId = userId;
        logger.info("UserId встановлено: " + userId);
    }

    // Додайте новий метод
    public void setThemeIndex(int themeIndex) {
        this.themeIndex = themeIndex;
        logger.info("ThemeIndex встановлено: " + themeIndex);
    }

    @FXML
    public void initialize() {
        // Порожній initialize
    }

    private List<Question> loadQuestionsFromDB() {
        List<Question> questions = new ArrayList<>();
        String questionsQuery = "SELECT question_id, question_text FROM questions WHERE lesson_id = ? ORDER BY question_number";

        try (Connection conn = DatabaseServices.getConnection();
             PreparedStatement stmt = conn.prepareStatement(questionsQuery)) {

            stmt.setInt(1, this.lessonId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int questionId = rs.getInt("question_id");
                String questionText = rs.getString("question_text");

                List<String> options = new ArrayList<>();
                List<Integer> optionIds = new ArrayList<>();
                int correctAnswerIndex = -1;

                // Завантажуємо варіанти відповідей з таблиці questions_options
                String optionsQuery = "SELECT option_id, option_text, is_correct FROM questions_options WHERE question_id = ? ORDER BY option_number";
                try (PreparedStatement optionStmt = conn.prepareStatement(optionsQuery)) {
                    optionStmt.setInt(1, questionId);
                    ResultSet optionsRs = optionStmt.executeQuery();

                    int index = 0;
                    while (optionsRs.next()) {
                        int optionId = optionsRs.getInt("option_id");
                        String optionText = optionsRs.getString("option_text");
                        boolean isCorrect = optionsRs.getBoolean("is_correct");

                        options.add(optionText);
                        optionIds.add(optionId);

                        if (isCorrect) {
                            correctAnswerIndex = index;
                        }
                        index++;
                    }
                }

                if (!options.isEmpty()) {
                    questions.add(new Question(questionId, questionText, options, optionIds, correctAnswerIndex));
                }
            }

            logger.info("Успішно завантажено " + questions.size() + " запитань для уроку ID: " + this.lessonId);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка при завантаженні запитань з БД", e);
            questionLabel.setText("Помилка: Не вдалося завантажити запитання.");
        }

        return questions;
    }

    private void displayQuestion() {
        if (quizQuestions.isEmpty()) return;

        Question current = quizQuestions.get(currentQuestionIndex);
        questionLabel.setText("Запитання " + (currentQuestionIndex + 1) + " з " + quizQuestions.size() + ": " + current.text);
        optionsContainer.getChildren().clear();
        optionsContainer.setDisable(false);

        for (int i = 0; i < current.options.size(); i++) {
            Button optionButton = new Button(current.options.get(i));
            optionButton.setPrefWidth(300);
            optionButton.getStyleClass().add("quiz-option-button");

            final int answerIndex = i;
            optionButton.setOnAction(e -> handleAnswer(answerIndex));

            if (userAnswers.get(currentQuestionIndex) == i) {
                optionButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");
            }

            optionsContainer.getChildren().add(optionButton);
        }

        scoreLabel.setText("Бали: " + calculateScore() + " / " + quizQuestions.size());
    }

    private void handleAnswer(int selectedIndex) {
        userAnswers.set(currentQuestionIndex, selectedIndex);
        displayQuestion();

        if (currentQuestionIndex < quizQuestions.size() - 1) {
            handleNextQuestion(null);
        } else {
            showFinalResult();
        }
    }

    private int calculateScore() {
        int tempScore = 0;
        for (int i = 0; i < quizQuestions.size(); i++) {
            if (userAnswers.get(i) != -1 && userAnswers.get(i) == quizQuestions.get(i).correctAnswerIndex) {
                tempScore++;
            }
        }
        return tempScore;
    }

    private void showFinalResult() {
        int finalScore = calculateScore();
        double percentage = (double) finalScore / quizQuestions.size() * 100;

        resultMessageLabel.setText(String.format("Тест завершено! Ваш результат: %d/%d (%.0f%%).", finalScore, quizQuestions.size(), percentage));
        resultMessageLabel.setVisible(true);

        optionsContainer.setDisable(true);
        prevButton.setDisable(true);
        nextButton.setDisable(true);
        backToHomeButton.setVisible(true);

        // Зберігаємо результат у базу даних
        if (userId > 0) {
            try {
                boolean saved = UserProgressService.saveTestResult(
                        userId, lessonId, finalScore, quizQuestions.size()
                );

                if (saved) {
                    logger.info("Результат збережено: користувач " + userId + ", урок " + lessonId +
                            ", бали " + finalScore + "/" + quizQuestions.size());
                } else {
                    logger.warning("Не вдалося зберегти результат тесту");
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Помилка при збереженні результату", e);
            }
        } else {
            logger.warning("UserId не встановлено, результат не збережено");
        }

        logger.info("Тест завершено: " + finalScore + "/" + quizQuestions.size());
    }

    @FXML
    private void handleNextQuestion(ActionEvent event) {
        if (currentQuestionIndex < quizQuestions.size() - 1) {
            currentQuestionIndex++;
            displayQuestion();
            updateNavigationButtons();
        }
    }

    @FXML
    private void handlePreviousQuestion(ActionEvent event) {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            displayQuestion();
            updateNavigationButtons();
        }
    }

    private void updateNavigationButtons() {
        resultMessageLabel.setVisible(false);
        backToHomeButton.setVisible(false);
        optionsContainer.setDisable(false);

        prevButton.setDisable(currentQuestionIndex == 0);
        nextButton.setDisable(currentQuestionIndex == quizQuestions.size() - 1);
    }

    @FXML
    public void goBackToHome(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/qirim/app/home.fxml"));
            Parent root = loader.load();

            HomeController homeController = loader.getController();

            if (userId > 0) {
                homeController.setCurrentUserId(userId);
                homeController.setCurrentThemeIndex(themeIndex); // Встановлюємо тему
                logger.info("Повернення на головну: userId=" + userId + ", themeIndex=" + themeIndex);
            } else {
                logger.warning("userId не встановлено в QuizController!");
            }

            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Помилка завантаження home.fxml", e);
        }
    }
}