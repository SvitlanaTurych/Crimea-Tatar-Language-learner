package qirim.app.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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

    @FXML private Label questionNumberLabel;
    @FXML private Label questionLabel;
    @FXML private Label scoreLabel;
    @FXML private Label resultMessageLabel;

    @FXML private GridPane optionsGrid;
    @FXML private HBox navigationHBox;

    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button backToHomeButton;

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
    private int userId = -1;
    private int themeIndex = 0;

    private List<Question> quizQuestions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private List<Integer> userAnswers;
    private List<Button> optionButtons = new ArrayList<>();
    private int selectedOptionIndex = -1;

    @FXML
    public void initialize() {
        // Ініціалізація виконується в setLessonData
    }

    public void setUserId(int userId) {
        this.userId = userId;
        logger.info("UserId встановлено: " + userId);
    }

    public void setThemeIndex(int themeIndex) {
        this.themeIndex = themeIndex;
        logger.info("ThemeIndex встановлено: " + themeIndex);
    }

    public void setLessonData(int lessonId) {
        this.lessonId = lessonId;
        this.quizQuestions = loadQuestionsFromDB();
        this.userAnswers = new ArrayList<>(Collections.nCopies(quizQuestions.size(), -1));

        if (!quizQuestions.isEmpty()) {
            displayQuestion(currentQuestionIndex);
        } else {
            questionLabel.setText("На жаль, запитань для цього уроку не знайдено.");
        }

        updateNavigationButtons();
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

    private void displayQuestion(int index) {
        if (index >= quizQuestions.size() || quizQuestions.isEmpty()) {
            return;
        }

        Question question = quizQuestions.get(index);

        // Оновити номер запитання
        questionNumberLabel.setText("Запитання " + (index + 1) + " з " + quizQuestions.size());

        // Оновити текст запитання
        questionLabel.setText(question.text);

        // Оновити бали
        scoreLabel.setText("Бали: " + calculateScore() + "/" + quizQuestions.size());

        // Очистити попередні кнопки
        optionsGrid.getChildren().clear();
        optionButtons.clear();
        selectedOptionIndex = userAnswers.get(currentQuestionIndex);

        // Створити кнопки для варіантів відповідей
        List<String> options = question.options;
        for (int i = 0; i < options.size(); i++) {
            final int optionIndex = i;
            Button optionButton = createOptionButton(options.get(i), optionIndex);
            optionButtons.add(optionButton);

            // Розташувати в сітці 2x2
            int row = i / 2;
            int col = i % 2;
            optionsGrid.add(optionButton, col, row);
        }
    }

    private Button createOptionButton(String text, int optionIndex) {
        Button button = new Button(text);
        button.setPrefWidth(400);
        button.setPrefHeight(60);

        // Перевірити, чи цей варіант вже був вибраний
        if (userAnswers.get(currentQuestionIndex) == optionIndex) {
            button.setStyle(
                    "-fx-background-color: #D4AF78; " +
                            "-fx-border-color: #D4AF78; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 8; " +
                            "-fx-text-fill: #043184; " +
                            "-fx-font-size: 18px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-cursor: hand;"
            );
        } else {
            button.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-border-color: white; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 8; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 18px; " +
                            "-fx-cursor: hand;"
            );
        }

        // Обробник вибору варіанта
        button.setOnAction(e -> selectOption(optionIndex));

        return button;
    }

    private void selectOption(int optionIndex) {
        selectedOptionIndex = optionIndex;
        userAnswers.set(currentQuestionIndex, optionIndex);

        // Скинути стилі всіх кнопок
        for (int i = 0; i < optionButtons.size(); i++) {
            Button btn = optionButtons.get(i);
            btn.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-border-color: white; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 8; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 18px; " +
                            "-fx-cursor: hand;"
            );
        }

        // Виділити вибрану кнопку
        Button selectedButton = optionButtons.get(optionIndex);
        selectedButton.setStyle(
                "-fx-background-color: #D4AF78; " +
                        "-fx-border-color: #D4AF78; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-text-fill: #043184; " +
                        "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand;"
        );

        // Оновити бали
        scoreLabel.setText("Бали: " + calculateScore() + "/" + quizQuestions.size());
    }

    @FXML
    private void handleNextQuestion(ActionEvent event) {
        if (selectedOptionIndex == -1) {
            // Можна показати повідомлення, що треба вибрати відповідь
            return;
        }

        // Перевірити відповідь і показати результат
        Question currentQuestion = quizQuestions.get(currentQuestionIndex);
        if (selectedOptionIndex == currentQuestion.correctAnswerIndex) {
            showCorrectAnswer();
        } else {
            showIncorrectAnswer();
        }

        // Затримка перед переходом до наступного запитання
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 1 секунда
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            javafx.application.Platform.runLater(() -> {
                if (currentQuestionIndex < quizQuestions.size() - 1) {
                    currentQuestionIndex++;
                    displayQuestion(currentQuestionIndex);
                    updateNavigationButtons();
                } else {
                    showFinalResult();
                }
            });
        }).start();
    }

    private void showCorrectAnswer() {
        Button button = optionButtons.get(selectedOptionIndex);
        button.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-border-color: #4CAF50; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold;"
        );
    }

    private void showIncorrectAnswer() {
        // Показати неправильну відповідь червоним
        Button incorrectButton = optionButtons.get(selectedOptionIndex);
        incorrectButton.setStyle(
                "-fx-background-color: #F44336; " +
                        "-fx-border-color: #F44336; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold;"
        );

        // Показати правильну відповідь зеленим
        Question currentQuestion = quizQuestions.get(currentQuestionIndex);
        Button correctButton = optionButtons.get(currentQuestion.correctAnswerIndex);
        correctButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                        "-fx-border-color: #4CAF50; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold;"
        );
    }

    @FXML
    private void handlePreviousQuestion(ActionEvent event) {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            displayQuestion(currentQuestionIndex);
            updateNavigationButtons();
        }
    }

    private void updateNavigationButtons() {
        prevButton.setDisable(currentQuestionIndex == 0);

        if (currentQuestionIndex == quizQuestions.size() - 1) {
            nextButton.setText("Завершити");
        } else {
            nextButton.setText("Наступне");
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

        // Сховати запитання та варіанти
        questionNumberLabel.setVisible(false);
        questionLabel.setVisible(false);
        optionsGrid.setVisible(false);
        navigationHBox.setVisible(false);

        // Показати результат
        resultMessageLabel.setText(
                String.format("Тест завершено!\nВаш результат: %d з %d балів (%.0f%%)",
                        finalScore, quizQuestions.size(), percentage)
        );
        resultMessageLabel.setVisible(true);
        backToHomeButton.setVisible(true);

        // Зберегти результат
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
    public void goBackToHome(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/qirim/app/home.fxml"));
            Parent root = loader.load();

            HomeController homeController = loader.getController();

            if (userId > 0) {
                homeController.setCurrentUserId(userId);
                homeController.setCurrentThemeIndex(themeIndex);
                logger.info("Повернення на головну: userId=" + userId + ", themeIndex=" + themeIndex);
            } else {
                logger.warning("userId не встановлено в QuizController!");
            }

            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
            stage.setFullScreen(true);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Помилка завантаження home.fxml", e);
        }
    }
}