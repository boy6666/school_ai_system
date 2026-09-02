-- code 服务核心表：提交与判分报告（dev-wuyoucheng §2.2）
-- 采用 create_time / update_time / deleted 三列，与 common 的 BaseEntity 审计/逻辑删除约定一致。
-- STATUS：0=待处理 1=运行中 2=已完成 3=超时 4=编译失败(不进沙箱) 5=判分失败

CREATE TABLE IF NOT EXISTS `code_submissions` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `student_id`          BIGINT       NOT NULL,
    `assignment_id`       BIGINT       NULL COMMENT '作业 id（用于 assignment.graded 事件回填关联，逻辑引用 teacher_db）',
    `assignment_item_id`   BIGINT       NULL COMMENT '作业题目项 id',
    `language`            VARCHAR(16)  NOT NULL DEFAULT 'java',
    `class_name`          VARCHAR(128) NULL COMMENT '入口类名，用于编译/运行定位',
    `expected_output`      LONGTEXT     NULL COMMENT '期望输出，参与判分权重',
    `source_code`         LONGTEXT     NOT NULL,
    `status`              TINYINT       NOT NULL DEFAULT 0,
    `stdout`             LONGTEXT      NULL,
    `stderr`             LONGTEXT      NULL,
    `run_time_ms`         INT           NULL,
    `create_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
    INDEX idx_student (student_id),
    INDEX idx_assignment (assignment_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码提交表';

CREATE TABLE IF NOT EXISTS `code_check_reports` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `submission_id`   BIGINT   NOT NULL,
    `compile_ok`      TINYINT  NOT NULL DEFAULT 0,
    `compile_msg`      LONGTEXT NULL,
    `checkstyle`      JSON     NULL COMMENT 'Checkstyle 结构化报告',
    `pmd`            JSON     NULL COMMENT 'PMD 结构化报告',
    `ai_suggestion`   LONGTEXT NULL COMMENT 'AI 纠错讲解',
    `overall_score`   INT      NOT NULL DEFAULT 0,
    `score_detail`    JSON     NULL COMMENT '判分权重明细',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
    INDEX idx_submission (submission_id),
    CONSTRAINT `fk_report_submission` FOREIGN KEY (`submission_id`) REFERENCES `code_submissions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码检查/判分报告';
