-- ============================================================
-- 工作流模块 V2 补丁 — 流程节点可视化配置
-- 数据库: asset_db
-- 说明: 新增 wf_node_config 表，支持轻量可视化审批链路配置
-- 执行顺序: 在 workflow_init.sql 之后执行
-- ============================================================

USE asset_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 流程节点配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS wf_node_config (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    definition_id     BIGINT UNSIGNED NOT NULL             COMMENT '关联 wf_process_definition.id',
    node_id           VARCHAR(64)     NOT NULL             COMMENT '节点标识（BPMN element id），同一流程内唯一',
    node_type         VARCHAR(32)     NOT NULL             COMMENT '节点类型：START/APPROVER/CONDITION/END',
    node_name         VARCHAR(128)    NOT NULL             COMMENT '节点显示名称',
    node_order        INT             NOT NULL DEFAULT 0   COMMENT '节点排序（从0开始，END固定99）',

    -- 审批节点（node_type = APPROVER）专用字段
    approver_strategy VARCHAR(32)                          COMMENT '审批人策略：DEPT_LEADER/ROLE/SPECIFIC_USER/INITIATOR_LEADER',
    role_code         VARCHAR(64)                          COMMENT '角色编码（strategy=ROLE 时使用，如 ROLE_VP）',
    user_id           BIGINT UNSIGNED                      COMMENT '指定用户ID（strategy=SPECIFIC_USER 时使用）',
    timeout_hours     INT                                  COMMENT '审批超时时长（小时），NULL=不限',

    -- 条件节点（node_type = CONDITION）专用字段
    condition_type    VARCHAR(32)                          COMMENT '条件类型：AMOUNT/CUSTOM',
    condition_op      VARCHAR(16)                          COMMENT '运算符：GT/GTE/LT/LTE/EQ',
    condition_value   DECIMAL(18, 2)                       COMMENT '条件阈值（金额条件时使用）',
    condition_expr    VARCHAR(512)                         COMMENT '自定义 EL 表达式（CUSTOM 条件时使用）',

    -- 通用字段
    remark            VARCHAR(256)                         COMMENT '节点备注',
    created_by        BIGINT UNSIGNED                      COMMENT '创建人ID',
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by        BIGINT UNSIGNED                      COMMENT '更新人ID',
    updated_at        DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted        TINYINT         NOT NULL DEFAULT 0   COMMENT '逻辑删除：0正常 1删除',

    INDEX idx_definition (definition_id),
    INDEX idx_order (definition_id, node_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程节点配置表';

-- ============================================================
-- 2. 初始化 8 种流程定义的默认节点配置
--    三级金额网关：部门主管 → [≥10万] → 副总裁(ROLE_VP) → [≥50万] → 总经理(ROLE_GM)
-- ============================================================

-- INV_INTENTION — 意向协议审批
INSERT IGNORE INTO wf_node_config
    (definition_id, node_id, node_type, node_name, node_order, approver_strategy, role_code,
     condition_type, condition_op, condition_value, created_at)
SELECT d.id, v.node_id, v.node_type, v.node_name, v.node_order,
       v.approver_strategy, v.role_code, v.condition_type, v.condition_op, v.condition_value, NOW()
FROM wf_process_definition d
JOIN (
    SELECT 'start'           AS node_id, 'START'     AS node_type, '发起申请'      AS node_name,  0 AS node_order, NULL           AS approver_strategy, NULL        AS role_code, NULL     AS condition_type, NULL  AS condition_op, NULL        AS condition_value
    UNION ALL
    SELECT 'node_dept',                  'APPROVER',               '部门主管审批',                1,               'DEPT_LEADER',                        NULL,                    NULL,                       NULL,             NULL
    UNION ALL
    SELECT 'gw_amount1',                 'CONDITION',              '金额条件（10万）',             2,               NULL,                                 NULL,                    'AMOUNT',                   'GTE',            100000.00
    UNION ALL
    SELECT 'node_vp',                    'APPROVER',               '分管领导审批',                 3,               'ROLE',                               'ROLE_VP',               NULL,                       NULL,             NULL
    UNION ALL
    SELECT 'gw_amount2',                 'CONDITION',              '金额条件（50万）',             4,               NULL,                                 NULL,                    'AMOUNT',                   'GTE',            500000.00
    UNION ALL
    SELECT 'node_gm',                    'APPROVER',               '总经理审批',                   5,               'ROLE',                               'ROLE_GM',               NULL,                       NULL,             NULL
    UNION ALL
    SELECT 'end',                        'END',                    '审批完成',                    99,               NULL,                                 NULL,                    NULL,                       NULL,             NULL
) v ON TRUE
WHERE d.process_key = 'INV_INTENTION' AND d.is_deleted = 0;

-- INV_OPENING — 开业审批
INSERT IGNORE INTO wf_node_config
    (definition_id, node_id, node_type, node_name, node_order, approver_strategy, role_code,
     condition_type, condition_op, condition_value, created_at)
SELECT d.id, v.node_id, v.node_type, v.node_name, v.node_order,
       v.approver_strategy, v.role_code, v.condition_type, v.condition_op, v.condition_value, NOW()
FROM wf_process_definition d
JOIN (
    SELECT 'start' AS node_id, 'START' AS node_type, '发起申请' AS node_name, 0 AS node_order, NULL AS approver_strategy, NULL AS role_code, NULL AS condition_type, NULL AS condition_op, NULL AS condition_value
    UNION ALL SELECT 'node_dept', 'APPROVER', '部门主管审批', 1, 'DEPT_LEADER', NULL, NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount1', 'CONDITION', '金额条件（10万）', 2, NULL, NULL, 'AMOUNT', 'GTE', 100000.00
    UNION ALL SELECT 'node_vp', 'APPROVER', '分管领导审批', 3, 'ROLE', 'ROLE_VP', NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount2', 'CONDITION', '金额条件（50万）', 4, NULL, NULL, 'AMOUNT', 'GTE', 500000.00
    UNION ALL SELECT 'node_gm', 'APPROVER', '总经理审批', 5, 'ROLE', 'ROLE_GM', NULL, NULL, NULL
    UNION ALL SELECT 'end', 'END', '审批完成', 99, NULL, NULL, NULL, NULL, NULL
) v ON TRUE
WHERE d.process_key = 'INV_OPENING' AND d.is_deleted = 0;

-- INV_RENT_DECOMP — 租金分解审批
INSERT IGNORE INTO wf_node_config
    (definition_id, node_id, node_type, node_name, node_order, approver_strategy, role_code,
     condition_type, condition_op, condition_value, created_at)
SELECT d.id, v.node_id, v.node_type, v.node_name, v.node_order,
       v.approver_strategy, v.role_code, v.condition_type, v.condition_op, v.condition_value, NOW()
FROM wf_process_definition d
JOIN (
    SELECT 'start' AS node_id, 'START' AS node_type, '发起申请' AS node_name, 0 AS node_order, NULL AS approver_strategy, NULL AS role_code, NULL AS condition_type, NULL AS condition_op, NULL AS condition_value
    UNION ALL SELECT 'node_dept', 'APPROVER', '部门主管审批', 1, 'DEPT_LEADER', NULL, NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount1', 'CONDITION', '金额条件（10万）', 2, NULL, NULL, 'AMOUNT', 'GTE', 100000.00
    UNION ALL SELECT 'node_vp', 'APPROVER', '分管领导审批', 3, 'ROLE', 'ROLE_VP', NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount2', 'CONDITION', '金额条件（50万）', 4, NULL, NULL, 'AMOUNT', 'GTE', 500000.00
    UNION ALL SELECT 'node_gm', 'APPROVER', '总经理审批', 5, 'ROLE', 'ROLE_GM', NULL, NULL, NULL
    UNION ALL SELECT 'end', 'END', '审批完成', 99, NULL, NULL, NULL, NULL, NULL
) v ON TRUE
WHERE d.process_key = 'INV_RENT_DECOMP' AND d.is_deleted = 0;

-- OPR_CONTRACT_CHANGE — 合同变更审批
INSERT IGNORE INTO wf_node_config
    (definition_id, node_id, node_type, node_name, node_order, approver_strategy, role_code,
     condition_type, condition_op, condition_value, created_at)
SELECT d.id, v.node_id, v.node_type, v.node_name, v.node_order,
       v.approver_strategy, v.role_code, v.condition_type, v.condition_op, v.condition_value, NOW()
FROM wf_process_definition d
JOIN (
    SELECT 'start' AS node_id, 'START' AS node_type, '发起申请' AS node_name, 0 AS node_order, NULL AS approver_strategy, NULL AS role_code, NULL AS condition_type, NULL AS condition_op, NULL AS condition_value
    UNION ALL SELECT 'node_dept', 'APPROVER', '部门主管审批', 1, 'DEPT_LEADER', NULL, NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount1', 'CONDITION', '金额条件（10万）', 2, NULL, NULL, 'AMOUNT', 'GTE', 100000.00
    UNION ALL SELECT 'node_vp', 'APPROVER', '分管领导审批', 3, 'ROLE', 'ROLE_VP', NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount2', 'CONDITION', '金额条件（50万）', 4, NULL, NULL, 'AMOUNT', 'GTE', 500000.00
    UNION ALL SELECT 'node_gm', 'APPROVER', '总经理审批', 5, 'ROLE', 'ROLE_GM', NULL, NULL, NULL
    UNION ALL SELECT 'end', 'END', '审批完成', 99, NULL, NULL, NULL, NULL, NULL
) v ON TRUE
WHERE d.process_key = 'OPR_CONTRACT_CHANGE' AND d.is_deleted = 0;

-- OPR_TERMINATION — 合同解约审批
INSERT IGNORE INTO wf_node_config
    (definition_id, node_id, node_type, node_name, node_order, approver_strategy, role_code,
     condition_type, condition_op, condition_value, created_at)
SELECT d.id, v.node_id, v.node_type, v.node_name, v.node_order,
       v.approver_strategy, v.role_code, v.condition_type, v.condition_op, v.condition_value, NOW()
FROM wf_process_definition d
JOIN (
    SELECT 'start' AS node_id, 'START' AS node_type, '发起申请' AS node_name, 0 AS node_order, NULL AS approver_strategy, NULL AS role_code, NULL AS condition_type, NULL AS condition_op, NULL AS condition_value
    UNION ALL SELECT 'node_dept', 'APPROVER', '部门主管审批', 1, 'DEPT_LEADER', NULL, NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount1', 'CONDITION', '金额条件（10万）', 2, NULL, NULL, 'AMOUNT', 'GTE', 100000.00
    UNION ALL SELECT 'node_vp', 'APPROVER', '分管领导审批', 3, 'ROLE', 'ROLE_VP', NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount2', 'CONDITION', '金额条件（50万）', 4, NULL, NULL, 'AMOUNT', 'GTE', 500000.00
    UNION ALL SELECT 'node_gm', 'APPROVER', '总经理审批', 5, 'ROLE', 'ROLE_GM', NULL, NULL, NULL
    UNION ALL SELECT 'end', 'END', '审批完成', 99, NULL, NULL, NULL, NULL, NULL
) v ON TRUE
WHERE d.process_key = 'OPR_TERMINATION' AND d.is_deleted = 0;

-- FIN_WRITE_OFF — 核销审批
INSERT IGNORE INTO wf_node_config
    (definition_id, node_id, node_type, node_name, node_order, approver_strategy, role_code,
     condition_type, condition_op, condition_value, created_at)
SELECT d.id, v.node_id, v.node_type, v.node_name, v.node_order,
       v.approver_strategy, v.role_code, v.condition_type, v.condition_op, v.condition_value, NOW()
FROM wf_process_definition d
JOIN (
    SELECT 'start' AS node_id, 'START' AS node_type, '发起申请' AS node_name, 0 AS node_order, NULL AS approver_strategy, NULL AS role_code, NULL AS condition_type, NULL AS condition_op, NULL AS condition_value
    UNION ALL SELECT 'node_dept', 'APPROVER', '部门主管审批', 1, 'DEPT_LEADER', NULL, NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount1', 'CONDITION', '金额条件（10万）', 2, NULL, NULL, 'AMOUNT', 'GTE', 100000.00
    UNION ALL SELECT 'node_vp', 'APPROVER', '分管领导审批', 3, 'ROLE', 'ROLE_VP', NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount2', 'CONDITION', '金额条件（50万）', 4, NULL, NULL, 'AMOUNT', 'GTE', 500000.00
    UNION ALL SELECT 'node_gm', 'APPROVER', '总经理审批', 5, 'ROLE', 'ROLE_GM', NULL, NULL, NULL
    UNION ALL SELECT 'end', 'END', '审批完成', 99, NULL, NULL, NULL, NULL, NULL
) v ON TRUE
WHERE d.process_key = 'FIN_WRITE_OFF' AND d.is_deleted = 0;

-- FIN_DEDUCTION — 减免审批
INSERT IGNORE INTO wf_node_config
    (definition_id, node_id, node_type, node_name, node_order, approver_strategy, role_code,
     condition_type, condition_op, condition_value, created_at)
SELECT d.id, v.node_id, v.node_type, v.node_name, v.node_order,
       v.approver_strategy, v.role_code, v.condition_type, v.condition_op, v.condition_value, NOW()
FROM wf_process_definition d
JOIN (
    SELECT 'start' AS node_id, 'START' AS node_type, '发起申请' AS node_name, 0 AS node_order, NULL AS approver_strategy, NULL AS role_code, NULL AS condition_type, NULL AS condition_op, NULL AS condition_value
    UNION ALL SELECT 'node_dept', 'APPROVER', '部门主管审批', 1, 'DEPT_LEADER', NULL, NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount1', 'CONDITION', '金额条件（10万）', 2, NULL, NULL, 'AMOUNT', 'GTE', 100000.00
    UNION ALL SELECT 'node_vp', 'APPROVER', '分管领导审批', 3, 'ROLE', 'ROLE_VP', NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount2', 'CONDITION', '金额条件（50万）', 4, NULL, NULL, 'AMOUNT', 'GTE', 500000.00
    UNION ALL SELECT 'node_gm', 'APPROVER', '总经理审批', 5, 'ROLE', 'ROLE_GM', NULL, NULL, NULL
    UNION ALL SELECT 'end', 'END', '审批完成', 99, NULL, NULL, NULL, NULL, NULL
) v ON TRUE
WHERE d.process_key = 'FIN_DEDUCTION' AND d.is_deleted = 0;

-- FIN_ADJUSTMENT — 调整审批
INSERT IGNORE INTO wf_node_config
    (definition_id, node_id, node_type, node_name, node_order, approver_strategy, role_code,
     condition_type, condition_op, condition_value, created_at)
SELECT d.id, v.node_id, v.node_type, v.node_name, v.node_order,
       v.approver_strategy, v.role_code, v.condition_type, v.condition_op, v.condition_value, NOW()
FROM wf_process_definition d
JOIN (
    SELECT 'start' AS node_id, 'START' AS node_type, '发起申请' AS node_name, 0 AS node_order, NULL AS approver_strategy, NULL AS role_code, NULL AS condition_type, NULL AS condition_op, NULL AS condition_value
    UNION ALL SELECT 'node_dept', 'APPROVER', '部门主管审批', 1, 'DEPT_LEADER', NULL, NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount1', 'CONDITION', '金额条件（10万）', 2, NULL, NULL, 'AMOUNT', 'GTE', 100000.00
    UNION ALL SELECT 'node_vp', 'APPROVER', '分管领导审批', 3, 'ROLE', 'ROLE_VP', NULL, NULL, NULL
    UNION ALL SELECT 'gw_amount2', 'CONDITION', '金额条件（50万）', 4, NULL, NULL, 'AMOUNT', 'GTE', 500000.00
    UNION ALL SELECT 'node_gm', 'APPROVER', '总经理审批', 5, 'ROLE', 'ROLE_GM', NULL, NULL, NULL
    UNION ALL SELECT 'end', 'END', '审批完成', 99, NULL, NULL, NULL, NULL, NULL
) v ON TRUE
WHERE d.process_key = 'FIN_ADJUSTMENT' AND d.is_deleted = 0;

SET FOREIGN_KEY_CHECKS = 1;
