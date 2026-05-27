package com.eduagent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true)
    private Long studentId;

    @Column(length = 100)
    private String major;

    @Column(length = 50)
    private String grade;

    @Column(length = 100)
    private String course;

    @Column(length = 100)
    private String topic;

    @Column(name = "learning_goal", columnDefinition = "TEXT")
    private String learningGoal;

    @Column(name = "knowledge_base", columnDefinition = "TEXT")
    private String knowledgeBase;

    @Column(name = "current_mastery", columnDefinition = "TEXT")
    private String currentMastery;

    @Column(name = "cognitive_style", columnDefinition = "TEXT")
    private String cognitiveStyle;

    @Column(length = 50)
    private String pace;

    @Column(columnDefinition = "JSON")
    private String weaknesses;

    @Column(name = "mistake_patterns", columnDefinition = "JSON")
    private String mistakePatterns;

    @Column(name = "learning_behavior", columnDefinition = "TEXT")
    private String learningBehavior;

    @Column(name = "resource_preference", columnDefinition = "JSON")
    private String resourcePreference;

    @Column(name = "overall_type", length = 20)
    private String overallType;

    @Column(name = "profile_suggestions", columnDefinition = "JSON")
    private String profileSuggestions;

    @Column(name = "last_score")
    private Integer lastScore;

    @Column(name = "last_suggestion", columnDefinition = "TEXT")
    private String lastSuggestion;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getLearningGoal() { return learningGoal; }
    public void setLearningGoal(String learningGoal) { this.learningGoal = learningGoal; }
    public String getKnowledgeBase() { return knowledgeBase; }
    public void setKnowledgeBase(String knowledgeBase) { this.knowledgeBase = knowledgeBase; }
    public String getCurrentMastery() { return currentMastery; }
    public void setCurrentMastery(String currentMastery) { this.currentMastery = currentMastery; }
    public String getCognitiveStyle() { return cognitiveStyle; }
    public void setCognitiveStyle(String cognitiveStyle) { this.cognitiveStyle = cognitiveStyle; }
    public String getPace() { return pace; }
    public void setPace(String pace) { this.pace = pace; }
    public String getWeaknesses() { return weaknesses; }
    public void setWeaknesses(String weaknesses) { this.weaknesses = weaknesses; }
    public String getMistakePatterns() { return mistakePatterns; }
    public void setMistakePatterns(String mistakePatterns) { this.mistakePatterns = mistakePatterns; }
    public String getLearningBehavior() { return learningBehavior; }
    public void setLearningBehavior(String learningBehavior) { this.learningBehavior = learningBehavior; }
    public String getResourcePreference() { return resourcePreference; }
    public void setResourcePreference(String resourcePreference) { this.resourcePreference = resourcePreference; }
    public String getOverallType() { return overallType; }
    public void setOverallType(String overallType) { this.overallType = overallType; }
    public String getProfileSuggestions() { return profileSuggestions; }
    public void setProfileSuggestions(String profileSuggestions) { this.profileSuggestions = profileSuggestions; }
    public Integer getLastScore() { return lastScore; }
    public void setLastScore(Integer lastScore) { this.lastScore = lastScore; }
    public String getLastSuggestion() { return lastSuggestion; }
    public void setLastSuggestion(String lastSuggestion) { this.lastSuggestion = lastSuggestion; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}
