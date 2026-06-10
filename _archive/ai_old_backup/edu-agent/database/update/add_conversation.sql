USE edu_agent;

CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    session_id VARCHAR(100) DEFAULT NULL,
    question TEXT NOT NULL,
    answer MEDIUMTEXT,
    intent VARCHAR(50) DEFAULT NULL,
    intent_confidence VARCHAR(20) DEFAULT NULL,
    evaluation_report TEXT DEFAULT NULL,
    resource_dir VARCHAR(500) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_session (student_id, session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
