-- ============================================================
-- 用户自定义扩展字段 - 数据库初始化脚本
-- 执行前提: init.sql + system_init.sql + system_patch_v2.sql 已执行
-- 说明:
--   PART 1: 创建字段元数据定义表 sys_ext_field_def
--   PART 2: 各业务表追加 ext_fields JSON 列（幂等）
--   PART 3: 验证结果
-- 幂等: 可重复执行，不影响已有数据
-- ============================================================

USE asset_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 辅助存储过程：按需 ADD COLUMN（幂等）
-- ============================================================
DROP PROCEDURE IF EXISTS sp_add_col;
DELIMITER //
CREATE PROCEDURE sp_add_col(
    IN p_tbl  VARCHAR(64),
    IN p_col  VARCHAR(64),
    IN p_def  TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_tbl
          AND COLUMN_NAME  = p_col
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_tbl, '` ADD COLUMN `', p_col, '` ', p_def);
        PREPARE s FROM @ddl;
        EXECUTE s;
        DEALLOCATE PREPARE s;
    END IF;
END //
DELIMITER ;

-- ============================================================
-- PART 1: 创建字段元数据定义表
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_ext_field_def (
    id           BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY                               COMMENT '主键',
    module_code  VARCHAR(32)      NOT NULL                                                 COMMENT '模块编码（project/shop/brand/merchant/intention/contract/ledger/change/receivable/receipt）',
    field_key    VARCHAR(64)      NOT NULL                                                 COMMENT '字段标识（英文+下划线，用作 JSON key，创建后不可修改）',
    field_label  VARCHAR(64)      NOT NULL                                                 COMMENT '字段显示名称（中文）',
    field_type   VARCHAR(16)      NOT NULL                                                 COMMENT '字段类型：text/textarea/number/date/select/radio/checkbox',
    options_json JSON             NULL                                                     COMMENT '选项列表，仅 select/radio/checkbox 使用，格式：[{"label":"xxx","value":"xxx"}]',
    required     TINYINT(1)       NOT NULL DEFAULT 0                                       COMMENT '是否必填：0-否 1-是',
    sort_order   INT              NOT NULL DEFAULT 0                                       COMMENT '排序序号（升序）',
    show_in_list TINYINT(1)       NOT NULL DEFAULT 0                                       COMMENT '是否在列表页展示：0-否 1-是',
    show_in_form TINYINT(1)       NOT NULL DEFAULT 1                                       COMMENT '是否在表单页展示：0-否 1-是',
    default_val  VARCHAR(255)     NULL                                                     COMMENT '默认值（字符串形式存储）',
    placeholder  VARCHAR(128)     NULL                                                     COMMENT '输入提示文本',
    max_length   INT              NULL                                                     COMMENT '最大长度，text/textarea 适用',
    min_val      DECIMAL(18,4)    NULL                                                     COMMENT '最小值，number 适用',
    max_val      DECIMAL(18,4)    NULL                                                     COMMENT '最大值，number 适用',
    is_deleted   TINYINT(1)       NOT NULL DEFAULT 0                                       COMMENT '逻辑删除：0-正常 1-已删除',
    created_by   BIGINT UNSIGNED  NULL                                                     COMMENT '创建人ID',
    created_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP                       COMMENT '创建时间',
    updated_by   BIGINT UNSIGNED  NULL                                                     COMMENT '更新人ID',
    updated_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_module_field (module_code, field_key),
    INDEX idx_module_code  (module_code),
    INDEX idx_sort_order   (module_code, sort_order),
    INDEX idx_is_deleted   (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义扩展字段元数据定义表';

-- ============================================================
-- PART 2: 各业务表追加 ext_fields JSON 列
-- ============================================================

-- ── 基础数据模块 ──────────────────────────────────────────────
CALL sp_add_col('biz_project',  'ext_fields', "JSON NULL COMMENT '用户自定义扩展字段（JSON）'");
CALL sp_add_col('biz_shop',     'ext_fields', "JSON NULL COMMENT '用户自定义扩展字段（JSON）'");
CALL sp_add_col('biz_brand',    'ext_fields', "JSON NULL COMMENT '用户自定义扩展字段（JSON）'");
CALL sp_add_col('biz_merchant', 'ext_fields', "JSON NULL COMMENT '用户自定义扩展字段（JSON）'");

-- ── 招商管理模块 ─────────────────────────────────────────────
CALL sp_add_col('inv_intention',      'ext_fields', "JSON NULL COMMENT '用户自定义扩展字段（JSON）'");
CALL sp_add_col('inv_lease_contract', 'ext_fields', "JSON NULL COMMENT '用户自定义扩展字段（JSON）'");

-- ── 营运管理模块 ─────────────────────────────────────────────
CALL sp_add_col('opr_contract_ledger', 'ext_fields', "JSON NULL COMMENT '用户自定义扩展字段（JSON）'");
CALL sp_add_col('opr_contract_change', 'ext_fields', "JSON NULL COMMENT '用户自定义扩展字段（JSON）'");

-- ── 财务管理模块 ─────────────────────────────────────────────
CALL sp_add_col('fin_receivable', 'ext_fields', "JSON NULL COMMENT '用户自定义扩展字段（JSON）'");
CALL sp_add_col('fin_receipt',    'ext_fields', "JSON NULL COMMENT '用户自定义扩展字段（JSON）'");

-- ============================================================
-- 清理辅助存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS sp_add_col;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- PART 3: 验证结果
-- ============================================================

-- 验证 sys_ext_field_def 表结构
SELECT '=== sys_ext_field_def 表结构 ===' AS info;
SHOW COLUMNS FROM sys_ext_field_def;

-- 验证各业务表 ext_fields 列
SELECT '=== 各业务表 ext_fields 列确认 ===' AS info;
SELECT
    TABLE_NAME    AS `表名`,
    COLUMN_NAME   AS `列名`,
    DATA_TYPE     AS `类型`,
    COLUMN_COMMENT AS `注释`
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'asset_db'
  AND COLUMN_NAME  = 'ext_fields'
ORDER BY TABLE_NAME;
