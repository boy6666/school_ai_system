-- teacher 服务：审计列统一（对齐 framework 的 BaseEntity 约定）
-- ------------------------------------------------------------------
-- 背景：V1__init.sql 里 classes/questions/assignments 用了 created_at，
--       但 framework 的 BaseEntity 字段是 createTime/updateTime
--       （MyBatis-Plus map-underscore-to-camel-case → 列 create_time/update_time），
--       由 AutoFillMetaObjectHandler 在 insert/update 时自动填充。
--       必须统一为 create_time/update_time，否则实体继承 BaseEntity 后
--       insert/update 会因列不存在而报错。
-- 方案：追加 V2 而非改 V1 —— 无论 V1 是否已在本机/生产执行过（Flyway 校验和不受影响），
--       新旧库都会收敛到同一结构。grades 已用 create_time，只补 update_time；
--       class_students 是纯关联表（复合主键，不继承 BaseEntity），不改审计列。
-- ------------------------------------------------------------------

-- 1) classes: created_at -> create_time, 补 update_time
ALTER TABLE classes
    CHANGE COLUMN created_at create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE classes
    ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time;

-- 2) questions: created_at -> create_time, 补 update_time
ALTER TABLE questions
    CHANGE COLUMN created_at create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE questions
    ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time;

-- 3) assignments: created_at -> create_time, 补 update_time
ALTER TABLE assignments
    CHANGE COLUMN created_at create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE assignments
    ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time;

-- 4) assignment_items: 补 create_time + update_time（V1 无任何审计列）
ALTER TABLE assignment_items
    ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE assignment_items
    ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time;

-- 5) grades: 已含 create_time, 补 update_time
ALTER TABLE grades
    ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time;
