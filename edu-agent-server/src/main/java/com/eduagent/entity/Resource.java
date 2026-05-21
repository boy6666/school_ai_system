package com.eduagent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ResourceType type;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ResourceDifficulty difficulty = ResourceDifficulty.基础;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_size", length = 50)
    private String fileSize;

    @Column(length = 255)
    private String cover;

    @Column(length = 100)
    private String author;

    private Double rating = 0.0;

    private Integer views = 0;

    private Integer favorites = 0;

    @Column(length = 50)
    private String duration;

    @Column(name = "course_id", length = 50)
    private String courseId;

    @Column(name = "course_name", length = 100)
    private String courseName;

    @Column(name = "chapter_name", length = 100)
    private String chapterName;

    @Column(name = "chapter_count")
    private Integer chapterCount = 0;

    @Column(columnDefinition = "JSON")
    private String tags;

    @Column(columnDefinition = "JSON")
    private String goals;

    @Column(name = "suitable_for", columnDefinition = "JSON")
    private String suitableFor;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ResourceStatus status = ResourceStatus.draft;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    public enum ResourceType {
        文档, PPT, 视频, 动画, 题库, 代码案例, 实验项目, 拓展阅读, 思维导图
    }

    public enum ResourceDifficulty {
        入门, 基础, 进阶, 高级
    }

    public enum ResourceStatus {
        draft, published, archived
    }

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
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public ResourceType getType() { return type; }
    public void setType(ResourceType type) { this.type = type; }
    public ResourceDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(ResourceDifficulty difficulty) { this.difficulty = difficulty; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileSize() { return fileSize; }
    public void setFileSize(String fileSize) { this.fileSize = fileSize; }
    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }
    public Integer getFavorites() { return favorites; }
    public void setFavorites(Integer favorites) { this.favorites = favorites; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getChapterName() { return chapterName; }
    public void setChapterName(String chapterName) { this.chapterName = chapterName; }
    public Integer getChapterCount() { return chapterCount; }
    public void setChapterCount(Integer chapterCount) { this.chapterCount = chapterCount; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }
    public String getSuitableFor() { return suitableFor; }
    public void setSuitableFor(String suitableFor) { this.suitableFor = suitableFor; }
    public ResourceStatus getStatus() { return status; }
    public void setStatus(ResourceStatus status) { this.status = status; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}
