package qirim.app.model;

/**
 * Модель для прогресу користувача
 */
public class UserProgress {
    private final int totalLessons;
    private final int completedLessons;
    private final double progressPercentage;

    public UserProgress(int totalLessons, int completedLessons, double progressPercentage) {
        this.totalLessons = totalLessons;
        this.completedLessons = completedLessons;
        this.progressPercentage = progressPercentage;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    /**
     * Повертає прогрес у вигляді десяткового числа (0.0 - 1.0) для ProgressBar
     */
    public double getProgressDecimal() {
        return progressPercentage / 100.0;
    }

    @Override
    public String toString() {
        return String.format("%d/%d (%.1f%%)", completedLessons, totalLessons, progressPercentage);
    }
}