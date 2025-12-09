package qirim.app.services;

import qirim.app.model.LeaderboardEntry;
import qirim.app.model.UserProgress;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserProgressService {

    private static final Logger logger = Logger.getLogger(UserProgressService.class.getName());

    /**
     * Зберігає результат проходження тесту та оновлює прогрес користувача
     */
    public static boolean saveTestResult(int userId, int lessonId, int score, int totalQuestions) {
        String sql = "SELECT update_user_progress(?, ?, ?, ?)";

        try (Connection conn = DatabaseServices.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, lessonId);
            stmt.setInt(3, score);
            stmt.setInt(4, totalQuestions);

            stmt.execute();
            logger.info("Прогрес оновлено для користувача " + userId + ", урок " + lessonId);
            return true;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка при збереженні результату тесту", e);
            return false;
        }
    }

    /**
     * Отримує загальний прогрес користувача
     */
    public static UserProgress getUserProgress(int userId) {
        String sql = "SELECT * FROM get_user_progress(?)";

        try (Connection conn = DatabaseServices.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int totalLessons = rs.getInt("total_lessons");
                int completedLessons = rs.getInt("completed_lessons");
                double progressPercentage = rs.getDouble("progress_percentage");

                return new UserProgress(totalLessons, completedLessons, progressPercentage);
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка при отриманні прогресу користувача", e);
        }

        return new UserProgress(0, 0, 0.0);
    }

    /**
     * Отримує статистику користувача (бали, streak тощо)
     */
    public static UserStats getUserStats(int userId) {
        String sql = "SELECT COALESCE(us.total_score, 0) as total_score, " +
                "COALESCE(us.lessons_completed, 0) as lessons_completed, " +
                "COALESCE(us.current_streak, 0) as current_streak, " +
                "COALESCE(us.longest_streak, 0) as longest_streak " +
                "FROM user_stats us WHERE us.user_id = ?";

        try (Connection conn = DatabaseServices.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new UserStats(
                        rs.getInt("total_score"),
                        rs.getInt("lessons_completed"),
                        rs.getInt("current_streak"),
                        rs.getInt("longest_streak")
                );
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка при отриманні статистики користувача", e);
        }

        return new UserStats(0, 0, 0, 0);
    }

    /**
     * Отримує таблицю лідерів (топ користувачів)
     */
    public static List<LeaderboardEntry> getLeaderboard(int limit) {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        String sql = "SELECT username, total_score, lessons_completed, current_streak " +
                "FROM leaderboard LIMIT ?";

        try (Connection conn = DatabaseServices.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            int rank = 1;
            while (rs.next()) {
                leaderboard.add(new LeaderboardEntry(
                        rank++,
                        rs.getString("username"),
                        rs.getInt("total_score"),
                        rs.getInt("lessons_completed"),
                        rs.getInt("current_streak")
                ));
            }

            logger.info("Завантажено " + leaderboard.size() + " записів у таблиці лідерів");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка при завантаженні таблиці лідерів", e);
        }

        return leaderboard;
    }

    /**
     * Перевіряє, чи урок завершено користувачем
     */
    public static boolean isLessonCompleted(int userId, int lessonId) {
        String sql = "SELECT completed FROM user_lesson_progress " +
                "WHERE user_id = ? AND lesson_id = ?";

        try (Connection conn = DatabaseServices.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, lessonId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("completed");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка при перевірці завершення уроку", e);
        }

        return false;
    }

    /**
     * Отримує кращий результат користувача для уроку
     */
    public static int getBestScore(int userId, int lessonId) {
        String sql = "SELECT score FROM user_lesson_progress " +
                "WHERE user_id = ? AND lesson_id = ?";

        try (Connection conn = DatabaseServices.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, lessonId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("score");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Помилка при отриманні кращого результату", e);
        }

        return 0;
    }

    // Внутрішній клас для статистики користувача
    public static class UserStats {
        public final int totalScore;
        public final int lessonsCompleted;
        public final int currentStreak;
        public final int longestStreak;

        public UserStats(int totalScore, int lessonsCompleted, int currentStreak, int longestStreak) {
            this.totalScore = totalScore;
            this.lessonsCompleted = lessonsCompleted;
            this.currentStreak = currentStreak;
            this.longestStreak = longestStreak;
        }
    }
}