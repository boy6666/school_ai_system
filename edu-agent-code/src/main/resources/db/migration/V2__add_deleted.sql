-- 为 code_exercises 增加逻辑删除列，支持 BaseEntity 的全局逻辑删除（mybatis-plus logic-delete-field=deleted）。
-- 仅在原表无该列时添加，兼容已存在的开发库（Flyway 仅执行一次）。
ALTER TABLE code_exercises
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删';
