package com.eduagent.resource.dto;

public class FeedbackReq {
    private Boolean liked;
    private String difficultyFeedback;

    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }
    public String getDifficultyFeedback() { return difficultyFeedback; }
    public void setDifficultyFeedback(String difficultyFeedback) { this.difficultyFeedback = difficultyFeedback; }
}
