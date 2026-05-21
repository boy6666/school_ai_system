package com.eduagent.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    @Column(name = "course_code", length = 50)
    private String courseCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(length = 50)
    private String duration;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CourseDifficulty difficulty = CourseDifficulty.基础;

    @Column(columnDefinition = "JSON")
    private String tags;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CourseStatus status = CourseStatus.draft;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    public enum CourseDifficulty {
        入门, 基础, 进阶, 高级
    }

    public enum CourseStatus {
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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public CourseDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(CourseDifficulty difficulty) { this.difficulty = difficulty; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public CourseStatus getStatus() { return status; }
    public void setStatus(CourseStatus status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}
