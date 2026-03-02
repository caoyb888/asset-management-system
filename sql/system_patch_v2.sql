-- ============================================================
-- 系统管理模块 - 数据库补丁 V2
-- 执行前提: init.sql + system_init.sql 已执行
-- 说明:
--   PART 1: ALTER 现有表，补全 BaseEntity 审计字段 / 修正列名
--   PART 2: 新建缺失表（IF NOT EXISTS 保证幂等）
--   PART 3: 补全种子数据
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

-- 辅助存储过程：按需 RENAME COLUMN（幂等）
DROP PROCEDURE IF EXISTS sp_rename_col;
DELIMITER //
CREATE PROCEDURE sp_rename_col(
    IN p_tbl     VARCHAR(64),
    IN p_old_col VARCHAR(64),
    IN p_new_col VARCHAR(64),
    IN p_def     TEXT
)
BEGIN
    -- 只有旧列存在且新列不存在时才执行 RENAME
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_tbl
          AND COLUMN_NAME  = p_old_col
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_tbl
          AND COLUMN_NAME  = p_new_col
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_tbl, '` RENAME COLUMN `', p_old_col, '` TO `', p_new_col, '`');
        PREPARE s FROM @ddl;
        EXECUTE s;
        DEALLOCATE PREPARE s;
    END IF;
END //
DELIMITER ;

-- ============================================================
-- PART 1: 修正现有表列名 / 补全 BaseEntity 字段
-- ============================================================

-- ── 1.1 sys_role：补全 BaseEntity 五件套 ──
CALL sp_add_col('sys_role', 'is_deleted', "TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1删除'");
CALL sp_add_col('sys_role', 'created_by', "BIGINT UNSIGNED COMMENT '创建人ID'");
CALL sp_add_col('sys_role', 'created_at', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL sp_add_col('sys_role', 'updated_by', "BIGINT UNSIGNED COMMENT '更新人ID'");
CALL sp_add_col('sys_role', 'updated_at', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");

UPDATE sys_role SET is_deleted = 0 WHERE is_deleted IS NULL;

-- ── 1.2 sys_menu：列名修正 create_time→created_at / update_time→updated_at，补全 remark ──
CALL sp_rename_col('sys_menu', 'create_time', 'created_at', NULL);
CALL sp_rename_col('sys_menu', 'update_time', 'updated_at', NULL);
CALL sp_add_col('sys_menu', 'remark', "VARCHAR(500) DEFAULT '' COMMENT '备注'");

-- ── 1.3 sys_dict_type：补全 BaseEntity 五件套 ──
CALL sp_add_col('sys_dict_type', 'is_deleted', "TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1删除'");
CALL sp_add_col('sys_dict_type', 'created_by', "BIGINT UNSIGNED COMMENT '创建人ID'");
CALL sp_add_col('sys_dict_type', 'created_at', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL sp_add_col('sys_dict_type', 'updated_by', "BIGINT UNSIGNED COMMENT '更新人ID'");
CALL sp_add_col('sys_dict_type', 'updated_at', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");

UPDATE sys_dict_type SET is_deleted = 0 WHERE is_deleted IS NULL;

-- ── 1.4 sys_dict_data：补全 css_class + BaseEntity 五件套 ──
CALL sp_add_col('sys_dict_data', 'css_class',  "VARCHAR(100) DEFAULT '' COMMENT '样式属性（el-tag type）'");
CALL sp_add_col('sys_dict_data', 'is_deleted', "TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1删除'");
CALL sp_add_col('sys_dict_data', 'created_by', "BIGINT UNSIGNED COMMENT '创建人ID'");
CALL sp_add_col('sys_dict_data', 'created_at', "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
CALL sp_add_col('sys_dict_data', 'updated_by', "BIGINT UNSIGNED COMMENT '更新人ID'");
CALL sp_add_col('sys_dict_data', 'updated_at', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");

UPDATE sys_dict_data SET is_deleted = 0 WHERE is_deleted IS NULL;

-- ── 1.5 sys_user：补全 real_name（防止旧版 Flyway 遗漏） ──
CALL sp_add_col('sys_user', 'real_name', "VARCHAR(50) DEFAULT '' COMMENT '真实姓名'");

-- ============================================================
-- PART 2: 新建缺失表
-- ============================================================

-- ── 2.1 登录日志表 sys_login_log ──
CREATE TABLE IF NOT EXISTS sys_login_log (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username    VARCHAR(64)  NOT NULL                     COMMENT '用户名',
    ip_addr     VARCHAR(64)  DEFAULT ''                   COMMENT 'IP地址',
    browser     VARCHAR(100) DEFAULT ''                   COMMENT '浏览器',
    os          VARCHAR(100) DEFAULT ''                   COMMENT '操作系统',
    status      TINYINT      NOT NULL DEFAULT 0           COMMENT '状态: 0成功 1失败',
    msg         VARCHAR(255) DEFAULT ''                   COMMENT '消息/失败原因',
    login_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    INDEX idx_username   (username),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- ── 2.2 角色自定义数据权限表 sys_role_data ──
CREATE TABLE IF NOT EXISTS sys_role_data (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    role_id     BIGINT UNSIGNED NOT NULL                  COMMENT '角色ID',
    dept_id     BIGINT UNSIGNED NOT NULL                  COMMENT '部门ID（自定义数据权限）',
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色自定义数据权限（部门）';

-- ── 2.3 分类管理表 sys_category ──
CREATE TABLE IF NOT EXISTS sys_category (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    category_type   VARCHAR(50)  NOT NULL                      COMMENT '分类维度，如 asset_type/format/area',
    parent_id       BIGINT UNSIGNED NOT NULL DEFAULT 0         COMMENT '父节点ID，0=根节点',
    ancestors       VARCHAR(500) NOT NULL DEFAULT '0'          COMMENT '祖级路径，逗号分隔',
    category_code   VARCHAR(50)  NOT NULL                      COMMENT '分类编码',
    category_name   VARCHAR(100) NOT NULL                      COMMENT '分类名称',
    level           INT          NOT NULL DEFAULT 1            COMMENT '层级深度，从1开始',
    sort_order      INT          NOT NULL DEFAULT 0            COMMENT '同级排序',
    status          TINYINT      NOT NULL DEFAULT 1            COMMENT '状态：0停用 1启用',
    remark          VARCHAR(200)                               COMMENT '备注',
    is_deleted      TINYINT      NOT NULL DEFAULT 0            COMMENT '逻辑删除: 0正常 1删除',
    created_by      BIGINT UNSIGNED                            COMMENT '创建人ID',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by      BIGINT UNSIGNED                            COMMENT '更新人ID',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category_type (category_type),
    INDEX idx_parent_id     (parent_id),
    INDEX idx_is_deleted    (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统分类管理表';

INSERT IGNORE INTO sys_category (id, category_type, parent_id, ancestors, category_code, category_name, level, sort_order) VALUES
(1, 'asset_type', 0, '0',   'RE',     '不动产',       1, 1),
(2, 'asset_type', 1, '0,1', 'RE-COM', '商业地产',     2, 1),
(3, 'asset_type', 1, '0,1', 'RE-OFF', '办公地产',     2, 2),
(4, 'asset_type', 1, '0,1', 'RE-IND', '工业地产',     2, 3),
(5, 'asset_type', 0, '0',   'FA',     '固定资产',     1, 2),
(6, 'asset_type', 5, '0,5', 'FA-EQ',  '设备设施',     2, 1),
(7, 'asset_type', 5, '0,5', 'FA-VH',  '交通运输设备', 2, 2);

-- ── 2.4 编码规则表 sys_code_rule ──
CREATE TABLE IF NOT EXISTS sys_code_rule (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    rule_key        VARCHAR(50)  NOT NULL                      COMMENT '规则唯一标识键',
    rule_name       VARCHAR(100) NOT NULL                      COMMENT '规则名称',
    prefix          VARCHAR(20)  NOT NULL DEFAULT ''           COMMENT '编码前缀，如 PRJ',
    date_format     VARCHAR(20)  NOT NULL DEFAULT 'yyyyMM'     COMMENT '日期格式',
    sep             CHAR(1)      NOT NULL DEFAULT '-'          COMMENT '分隔符',
    seq_length      INT          NOT NULL DEFAULT 4            COMMENT '序列号位数（前补零）',
    reset_type      TINYINT      NOT NULL DEFAULT 1            COMMENT '重置周期：0不重置 1按年 2按月 3按日',
    current_seq     BIGINT       NOT NULL DEFAULT 0            COMMENT '当前序列号',
    current_period  VARCHAR(20)  NOT NULL DEFAULT ''           COMMENT '当前周期，用于判断是否需要重置',
    status          TINYINT      NOT NULL DEFAULT 1            COMMENT '状态：0停用 1启用',
    remark          VARCHAR(200)                               COMMENT '备注',
    is_deleted      TINYINT      NOT NULL DEFAULT 0            COMMENT '逻辑删除: 0正常 1删除',
    created_by      BIGINT UNSIGNED                            COMMENT '创建人ID',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by      BIGINT UNSIGNED                            COMMENT '更新人ID',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_rule_key (rule_key),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务编码规则表';

INSERT IGNORE INTO sys_code_rule (rule_key, rule_name, prefix, date_format, sep, seq_length, reset_type, remark) VALUES
('project',   '项目编码',     'PRJ', 'yyyyMM', '-', 4, 2, '项目基础信息编码'),
('building',  '楼栋编码',     'BLD', 'yyyyMM', '-', 4, 2, '楼栋编码'),
('shop',      '商铺编码',     'SHP', 'yyyyMM', '-', 4, 2, '商铺编码'),
('contract',  '合同编码',     'CTR', 'yyyyMM', '-', 4, 2, '招商合同编码'),
('intention', '意向协议编码', 'INT', 'yyyyMM', '-', 4, 2, '意向协议编码'),
('receipt',   '收款单编码',   'RCP', 'yyyyMM', '-', 4, 2, '财务收款单编码'),
('writeoff',  '核销单编码',   'WOF', 'yyyyMM', '-', 4, 2, '核销单编码'),
('voucher',   '凭证编码',     'VCH', 'yyyyMM', '-', 4, 2, '财务凭证编码');

-- ── 2.5 租费算法配置表 sys_rent_algorithm（TASK-SYS-10）──
CREATE TABLE IF NOT EXISTS sys_rent_algorithm (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    algo_name       VARCHAR(200) NOT NULL                      COMMENT '算法名称',
    algo_code       VARCHAR(50)  NOT NULL                      COMMENT '算法编码',
    algo_type       TINYINT      NOT NULL                      COMMENT '类型: 1固定租金 2浮动租金 3取高差额 4固定物管 5浮动物管 6取高物管差额 7装修期管理费 8垃圾清运费',
    calc_mode       TINYINT                                    COMMENT '计算模式: 1月单价 2日单价',
    month_rule      TINYINT                                    COMMENT '整月规则: 1整月 2按实际天数',
    formula         VARCHAR(1000)                              COMMENT '计算公式表达式',
    params_schema   TEXT                                       COMMENT '参数Schema定义(JSON)',
    default_params  TEXT                                       COMMENT '默认参数值(JSON)',
    status          TINYINT      NOT NULL DEFAULT 1            COMMENT '状态：0停用 1启用',
    remark          VARCHAR(500)                               COMMENT '说明',
    is_deleted      TINYINT      NOT NULL DEFAULT 0            COMMENT '逻辑删除: 0正常 1删除',
    created_by      BIGINT UNSIGNED                            COMMENT '创建人ID',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by      BIGINT UNSIGNED                            COMMENT '更新人ID',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_algo_code (algo_code),
    INDEX idx_algo_type  (algo_type),
    INDEX idx_status     (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租费算法配置表';

-- ── 2.6 租费算法阶梯配置表 sys_rent_algo_step（TASK-SYS-10）──
CREATE TABLE IF NOT EXISTS sys_rent_algo_step (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    algo_id         BIGINT UNSIGNED NOT NULL                   COMMENT '算法ID',
    step_start      DECIMAL(14,2)                              COMMENT '阶梯起始值（如营业额起点）',
    step_end        DECIMAL(14,2)                              COMMENT '阶梯终止值',
    rate            DECIMAL(5,2)                               COMMENT '比例费率(%)',
    fixed_amount    DECIMAL(14,2)                              COMMENT '固定金额',
    sort_order      INT                                        COMMENT '排序',
    is_deleted      TINYINT      NOT NULL DEFAULT 0            COMMENT '逻辑删除: 0正常 1删除',
    created_by      BIGINT UNSIGNED                            COMMENT '创建人ID',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by      BIGINT UNSIGNED                            COMMENT '更新人ID',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_algo_id    (algo_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租费算法阶梯配置表';

-- ── 2.7 系统参数表 sys_config（TASK-SYS-11）──
CREATE TABLE IF NOT EXISTS sys_config (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    config_name     VARCHAR(200) NOT NULL                      COMMENT '参数名称',
    config_key      VARCHAR(100) NOT NULL                      COMMENT '参数键',
    config_value    VARCHAR(1000)                              COMMENT '参数值',
    config_type     TINYINT                                    COMMENT '类型: 1系统内置 2自定义',
    remark          VARCHAR(500)                               COMMENT '描述',
    is_deleted      TINYINT      NOT NULL DEFAULT 0            COMMENT '逻辑删除: 0正常 1删除',
    created_by      BIGINT UNSIGNED                            COMMENT '创建人ID',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by      BIGINT UNSIGNED                            COMMENT '更新人ID',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数表';

INSERT IGNORE INTO sys_config (config_name, config_key, config_value, config_type, remark) VALUES
('密码最小长度',    'sys.password.minLength',  '8',   1, '用户密码最小字符长度'),
('密码有效天数',    'sys.password.expireDays', '90',  1, '密码过期天数，0不过期'),
('登录失败锁定次数','sys.login.maxFailCount',  '5',   1, '连续失败超过此次数锁定账号'),
('账号锁定分钟数',  'sys.login.lockMinutes',   '30',  1, '账号锁定持续时间（分钟）'),
('日志保留天数',    'sys.log.retentionDays',   '180', 1, '操作日志/登录日志保留天数');

-- ============================================================
-- PART 3: 补全字典数据种子
-- ============================================================
INSERT IGNORE INTO sys_dict_data (dict_type, dict_label, dict_value, css_class, sort_order) VALUES
('sys_user_status', '正常', '1', 'success', 1),
('sys_user_status', '停用', '0', 'danger',  2),
('sys_dept_status', '正常', '1', 'success', 1),
('sys_dept_status', '停用', '0', 'danger',  2),
('sys_post_status', '正常', '1', 'success', 1),
('sys_post_status', '停用', '0', 'danger',  2),
('sys_role_status', '正常', '1', 'success', 1),
('sys_role_status', '停用', '0', 'danger',  2),
('sys_menu_type', '目录', 'M', '',        1),
('sys_menu_type', '菜单', 'C', 'primary', 2),
('sys_menu_type', '按钮', 'F', 'warning', 3),
('sys_data_scope', '全部数据权限',   '1', '', 1),
('sys_data_scope', '自定义数据权限', '2', '', 2),
('sys_data_scope', '本部门数据权限', '3', '', 3),
('sys_data_scope', '本部门及以下',   '4', '', 4),
('sys_data_scope', '仅本人数据权限', '5', '', 5),
('biz_format_type', '餐饮', '1', '', 1),
('biz_format_type', '零售', '2', '', 2),
('biz_format_type', '娱乐', '3', '', 3),
('biz_format_type', '服务', '4', '', 4),
('biz_format_type', '办公', '5', '', 5),
('biz_format_type', '其他', '9', '', 9),
('biz_property_type', '国有', '1', '', 1),
('biz_property_type', '集体', '2', '', 2),
('biz_property_type', '私有', '3', '', 3),
('biz_property_type', '其他', '4', '', 4),
('biz_business_type', '自持', '1', 'primary', 1),
('biz_business_type', '租赁', '2', 'success', 2),
('biz_business_type', '合作', '3', 'warning', 3),
('biz_operation_status', '筹备中', '0', 'info',    1),
('biz_operation_status', '已开业', '1', 'success', 2),
('biz_operation_status', '已停业', '2', 'danger',  3),
('inv_contract_type', '新签', '1', 'primary', 1),
('inv_contract_type', '续签', '2', 'success', 2),
('inv_contract_type', '转让', '3', 'warning', 3),
('inv_payment_cycle', '月付',   '1',  '', 1),
('inv_payment_cycle', '季付',   '3',  '', 2),
('inv_payment_cycle', '半年付', '6',  '', 3),
('inv_payment_cycle', '年付',   '12', '', 4),
('inv_rent_scheme_type', '固定租金', '1', 'primary', 1),
('inv_rent_scheme_type', '浮动租金', '2', 'success', 2),
('inv_rent_scheme_type', '取高差额', '3', 'warning', 3);

-- ============================================================
-- 清理辅助存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS sp_add_col;
DROP PROCEDURE IF EXISTS sp_rename_col;

SET FOREIGN_KEY_CHECKS = 1;

-- ── 验证结果 ──
SELECT TABLE_NAME, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'asset_db' AND TABLE_NAME LIKE 'sys_%'
ORDER BY TABLE_NAME;
