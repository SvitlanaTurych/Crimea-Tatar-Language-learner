package qirim.app.model;

public class LeaderboardEntry {
    private int rank;
    private String username;
    private int totalScore;
    private int lessonsCompleted;
    private int currentStreak;

    public LeaderboardEntry(int rank, String username, int totalScore,
                            int lessonsCompleted, int currentStreak) {
        this.rank = rank;
        this.username = username;
        this.totalScore = totalScore;
        this.lessonsCompleted = lessonsCompleted;
        this.currentStreak = currentStreak;
    }

    public int getRank() {
        return rank;
    }

    public String getUsername() {
        return username;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getLessonsCompleted() {
        return lessonsCompleted;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public void setLessonsCompleted(int lessonsCompleted) {
        this.lessonsCompleted = lessonsCompleted;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }
}