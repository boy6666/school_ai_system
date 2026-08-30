-- 成绩表补充 code 服务受理号：教师重判分（POST /api/edu-agent-code/submissions/{id}/regrade）的入参来源。
-- 双落点写入：受理回执（GradeServiceImpl.upsert）+ assignment.graded 事件回填（AssignmentGradedConsumer），互为兜底。
-- 老数据为 NULL 不回填：重判入口仅对"有受理号"的成绩可见。
ALTER TABLE `grades`
  ADD COLUMN `submission_id` BIGINT DEFAULT NULL COMMENT 'code 服务受理号（教师重判分入口）' AFTER `language`;
