-- ============================================================
-- 资产管理系统 - 工作流模块初始化脚本
-- 数据库: asset_db
-- 说明:
--   · 新建 wf_process_definition  流程定义配置表
--   · 新建 wf_process_instance    流程实例表
--   · 新建 wf_approval_record     审批操作记录表
--   · 插入 8 条流程定义种子数据
-- ============================================================

USE asset_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 流程定义配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS wf_process_definition (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    process_key         VARCHAR(64)     NOT NULL         COMMENT '流程 key，与 BPMN process id 对应',
    process_name        VARCHAR(128)    NOT NULL         COMMENT '流程名称',
    business_type       VARCHAR(64)     NOT NULL         COMMENT '对应业务类型枚举',
    bpmn_xml            LONGTEXT                         COMMENT 'BPMN 2.0 XML 定义',
    approver_strategy   VARCHAR(32)     NOT NULL DEFAULT 'ROLE' COMMENT '审批人策略：ROLE/DEPT_LEADER/SPECIFIC_USER/INITIATOR_LEADER',
    approver_config     JSON                             COMMENT '策略参数（角色 ID、用户 ID 列表等）',
    is_enabled          TINYINT         NOT NULL DEFAULT 1 COMMENT '是否启用: 0禁用 1启用',
    version             INT             NOT NULL DEFAULT 1 COMMENT '版本号',
    created_by          BIGINT UNSIGNED                  COMMENT '创建人ID',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by          BIGINT UNSIGNED                  COMMENT '更新人ID',
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted          TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1删除',
    UNIQUE KEY uk_key (process_key),
    INDEX idx_biz_type (business_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程定义配置表';

-- ============================================================
-- 2. 流程实例表
-- ============================================================
CREATE TABLE IF NOT EXISTS wf_process_instance (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    process_key           VARCHAR(64)     NOT NULL         COMMENT '流程定义 key',
    flowable_instance_id  VARCHAR(128)    NOT NULL         COMMENT 'Flowable 引擎流程实例 ID',
    business_type         VARCHAR(64)     NOT NULL         COMMENT '业务类型枚举',
    business_id           BIGINT          NOT NULL         COMMENT '业务单据 ID',
    title                 VARCHAR(256)    NOT NULL         COMMENT '审批标题',
    initiator_id          BIGINT          NOT NULL         COMMENT '发起人用户 ID',
    initiator_name        VARCHAR(64)                      COMMENT '发起人姓名（冗余）',
    project_id            BIGINT                           COMMENT '所属项目 ID（数据权限过滤）',
    current_assignee_id   BIGINT                           COMMENT '当前待审批人 ID',
    current_node_name     VARCHAR(128)                     COMMENT '当前审批节点名称',
    status                TINYINT         NOT NULL DEFAULT 0 COMMENT '状态: 0待审批 1审批中 2已通过 3已驳回 4已撤回 5已作废',
    result_comment        VARCHAR(512)                     COMMENT '最终审批意见',
    priority              TINYINT         DEFAULT 0        COMMENT '优先级: 0普通 1紧急 2加急',
    variables_json        JSON                             COMMENT '扩展变量（金额、合同号等）',
    callback_url          VARCHAR(256)                     COMMENT '回调地址（备用，默认走 Feign）',
    started_at            DATETIME        NOT NULL         COMMENT '流程发起时间',
    finished_at           DATETIME                         COMMENT '流程完成时间',
    duration_ms           BIGINT                           COMMENT '总耗时（毫秒）',
    created_by            BIGINT UNSIGNED                  COMMENT '创建人ID',
    created_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by            BIGINT UNSIGNED                  COMMENT '更新人ID',
    updated_at            DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted            TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1删除',
    UNIQUE KEY uk_biz (business_type, business_id),
    INDEX idx_initiator (initiator_id),
    INDEX idx_assignee (current_assignee_id),
    INDEX idx_status (status),
    INDEX idx_project (project_id),
    INDEX idx_flowable (flowable_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例表';

-- ============================================================
-- 3. 审批操作记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS wf_approval_record (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    instance_id       BIGINT          NOT NULL         COMMENT '关联 wf_process_instance.id',
    flowable_task_id  VARCHAR(128)                     COMMENT 'Flowable 任务 ID',
    node_name         VARCHAR(128)    NOT NULL         COMMENT '审批节点名称',
    node_order        INT             NOT NULL DEFAULT 1 COMMENT '节点序号（用于排序展示）',
    approver_id       BIGINT          NOT NULL         COMMENT '审批人用户 ID',
    approver_name     VARCHAR(64)                      COMMENT '审批人姓名',
    action            TINYINT         NOT NULL         COMMENT '动作: 1通过 2驳回 3转办 4加签 5撤回',
    comment           VARCHAR(512)                     COMMENT '审批意见',
    attachment_urls   VARCHAR(1024)                    COMMENT '附件 URL（JSON 数组）',
    duration_ms       BIGINT                           COMMENT '该节点处理耗时（毫秒）',
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_instance (instance_id),
    INDEX idx_approver (approver_id),
    INDEX idx_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批操作记录表';

-- ============================================================
-- 4. 种子数据：8 种流程定义
-- ============================================================
INSERT INTO wf_process_definition (process_key, process_name, business_type, approver_strategy, approver_config, is_enabled, version, created_by, created_at)
VALUES
    ('INV_INTENTION',       '意向协议审批',   'INV_INTENTION',       'DEPT_LEADER', '{"levels": ["deptLeader", "vp(>=10万)", "gm(>=50万)"]}', 1, 1, NULL, NOW()),
    ('INV_OPENING',         '开业审批',       'INV_OPENING',         'DEPT_LEADER', '{"levels": ["deptLeader", "vp(>=10万)", "gm(>=50万)"]}', 1, 1, NULL, NOW()),
    ('INV_RENT_DECOMP',     '租金分解审批',   'INV_RENT_DECOMP',     'DEPT_LEADER', '{"levels": ["deptLeader", "vp(>=10万)", "gm(>=50万)"]}', 1, 1, NULL, NOW()),
    ('OPR_CONTRACT_CHANGE', '合同变更审批',   'OPR_CONTRACT_CHANGE', 'DEPT_LEADER', '{"levels": ["deptLeader", "vp(>=10万)", "gm(>=50万)"]}', 1, 1, NULL, NOW()),
    ('OPR_TERMINATION',     '合同解约审批',   'OPR_TERMINATION',     'DEPT_LEADER', '{"levels": ["deptLeader", "vp(>=10万)", "gm(>=50万)"]}', 1, 1, NULL, NOW()),
    ('FIN_WRITE_OFF',       '核销审批',       'FIN_WRITE_OFF',       'DEPT_LEADER', '{"levels": ["deptLeader", "vp(>=10万)", "gm(>=50万)"]}', 1, 1, NULL, NOW()),
    ('FIN_DEDUCTION',       '减免审批',       'FIN_DEDUCTION',       'DEPT_LEADER', '{"levels": ["deptLeader", "vp(>=10万)", "gm(>=50万)"]}', 1, 1, NULL, NOW()),
    ('FIN_ADJUSTMENT',      '调整审批',       'FIN_ADJUSTMENT',      'DEPT_LEADER', '{"levels": ["deptLeader", "vp(>=10万)", "gm(>=50万)"]}', 1, 1, NULL, NOW());

SET FOREIGN_KEY_CHECKS = 1;
