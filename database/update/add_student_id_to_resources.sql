-- 给 resources 表添加 student_id 列（用于关联学生生成的资源）
ALTER TABLE resources ADD COLUMN student_id BIGINT DEFAULT NULL COMMENT '学生ID' AFTER teacher_id;
CREATE INDEX idx_resources_student_id ON resources(student_id);
