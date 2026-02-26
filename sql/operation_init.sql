-- =====================================================================
-- 营运管理模块数据库初始化脚本
-- 涵盖 14 张表 + 复合索引优化（§3.2）
-- 执行前提：asset_db 数据库已存在（由 init.sql 创建）
-- =====================================================================

USE asset_db;

-- 合同台账表
CREATE TABLE IF NOT EXISTS `opr_contract_ledger` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `ledger_code` varchar(50) NOT NULL COMMENT '台账编号',
    `contract_id` bigint NOT NULL COMMENT '关联租赁合同ID',
    `project_id` bigint NOT NULL COMMENT '项目ID',
    `merchant_id` bigint DEFAULT NULL COMMENT '商家ID',
    `brand_id` bigint DEFAULT NULL COMMENT '品牌ID',
    `contract_type` tinyint DEFAULT NULL COMMENT '合同类型（1租赁/2联营/3临时）',
    `contract_start` date DEFAULT NULL COMMENT '合同开始日期',
    `contract_end` date DEFAULT NULL COMMENT '合同到期日期',
    `double_sign_status` tinyint DEFAULT 0 COMMENT '双签状态（0待双签/1已双签）',
    `double_sign_date` datetime DEFAULT NULL COMMENT '双签完成时间',
    `receivable_status` tinyint DEFAULT 0 COMMENT '应收生成状态（0未生成/1已生成/2已推送）',
    `audit_status` tinyint DEFAULT 0 COMMENT '审核状态（0待审核/1通过/2驳回）',
    `status` tinyint DEFAULT 0 COMMENT '台账状态（0进行中/1已完成/2已解约）',
    `push_time` datetime DEFAULT NULL COMMENT '应收推送时间',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ledger_code` (`ledger_code`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_brand_id` (`brand_id`),
    KEY `idx_status` (`status`),
    KEY `idx_contract_end` (`contract_end`),
    KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同台账表';

-- 应收计划表
CREATE TABLE IF NOT EXISTS `opr_receivable_plan` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `ledger_id` bigint NOT NULL COMMENT '合同台账ID',
    `contract_id` bigint NOT NULL COMMENT '合同ID',
    `shop_id` bigint DEFAULT NULL COMMENT '商铺ID',
    `fee_item_id` bigint DEFAULT NULL COMMENT '收款项目ID',
    `fee_name` varchar(100) DEFAULT NULL COMMENT '费项名称',
    `billing_start` date DEFAULT NULL COMMENT '账期开始',
    `billing_end` date DEFAULT NULL COMMENT '账期结束',
    `due_date` date DEFAULT NULL COMMENT '应收日期',
    `amount` decimal(14,2) NOT NULL COMMENT '应收金额',
    `received_amount` decimal(14,2) DEFAULT 0.00 COMMENT '已收金额',
    `status` tinyint DEFAULT 0 COMMENT '状态（0待收/1部分收款/2已收/3已作废）',
    `push_status` tinyint DEFAULT 0 COMMENT '推送状态（0未推送/1已推送）',
    `push_time` datetime DEFAULT NULL COMMENT '推送财务时间',
    `push_idempotent_key` varchar(100) DEFAULT NULL COMMENT '推送幂等键（receivable_{id}_{version}）',
    `source_type` tinyint DEFAULT 1 COMMENT '来源（1合同生成/2变更生成/3浮动租金/4一次性录入）',
    `version` int DEFAULT 1 COMMENT '版本号（变更后重生时递增）',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_ledger_id` (`ledger_id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_due_date` (`due_date`),
    KEY `idx_status` (`status`),
    KEY `idx_push_status` (`push_status`),
    KEY `idx_source_type` (`source_type`),
    KEY `idx_billing_period` (`billing_start`, `billing_end`),
    KEY `idx_version` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应收计划表';

-- 一次性首款记录表
CREATE TABLE IF NOT EXISTS `opr_one_time_payment` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `ledger_id` bigint NOT NULL COMMENT '合同台账ID',
    `contract_id` bigint DEFAULT NULL COMMENT '合同ID',
    `fee_item_id` bigint DEFAULT NULL COMMENT '收款项目',
    `receivable_id` bigint DEFAULT NULL COMMENT '关联生成的应收计划ID（生成应收后回填）',
    `amount` decimal(14,2) NOT NULL COMMENT '金额',
    `billing_start` date DEFAULT NULL COMMENT '账期开始',
    `billing_end` date DEFAULT NULL COMMENT '账期结束',
    `entry_type` tinyint DEFAULT NULL COMMENT '录入类型（1单笔/2多笔/3历史账期）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_ledger_id` (`ledger_id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_receivable_id` (`receivable_id`),
    KEY `idx_entry_type` (`entry_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='一次性首款记录表';

-- 合同变更表
CREATE TABLE IF NOT EXISTS `opr_contract_change` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `change_code` varchar(50) NOT NULL COMMENT '变更单号（BG + yyyyMMdd + 4位流水）',
    `contract_id` bigint NOT NULL COMMENT '原合同ID',
    `ledger_id` bigint DEFAULT NULL COMMENT '关联台账ID',
    `project_id` bigint DEFAULT NULL COMMENT '项目ID',
    `status` tinyint DEFAULT 0 COMMENT '状态（0草稿/1审批中/2通过/3驳回）',
    `effective_date` date DEFAULT NULL COMMENT '变更生效日期',
    `reason` varchar(1000) DEFAULT NULL COMMENT '变更原因',
    `approval_id` varchar(100) DEFAULT NULL COMMENT '审批流程实例ID',
    `impact_summary` json DEFAULT NULL COMMENT '变更影响预览暂存（受影响应收笔数/金额差异等）',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_change_code` (`change_code`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_ledger_id` (`ledger_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_status` (`status`),
    KEY `idx_effective_date` (`effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同变更表';

-- 合同变更类型关联表（支持多选变更类型，替代逗号分隔字段）
-- change_type_code 枚举值：RENT/BRAND/TENANT/FEE/CLAUSE/TERM/AREA/COMPANY
CREATE TABLE IF NOT EXISTS `opr_contract_change_type` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `change_id` bigint NOT NULL COMMENT '变更单ID',
    `change_type_code` varchar(50) NOT NULL COMMENT '变更类型编码（RENT/BRAND/TENANT/FEE/CLAUSE/TERM/AREA/COMPANY）',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_change_id` (`change_id`),
    KEY `idx_change_type_code` (`change_type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同变更类型关联表';

-- 合同变更明细表（字段级变更前后对比）
CREATE TABLE IF NOT EXISTS `opr_contract_change_detail` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `change_id` bigint NOT NULL COMMENT '变更单ID',
    `field_name` varchar(100) NOT NULL COMMENT '变更字段名',
    `field_label` varchar(100) DEFAULT NULL COMMENT '字段中文名',
    `old_value` text DEFAULT NULL COMMENT '变更前值',
    `new_value` text DEFAULT NULL COMMENT '变更后值',
    `data_type` varchar(50) DEFAULT NULL COMMENT '数据类型（string/decimal/date等）',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_change_id` (`change_id`),
    KEY `idx_field_name` (`field_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同变更明细表';

-- 合同变更快照表（变更前后完整快照，支持回溯）
CREATE TABLE IF NOT EXISTS `opr_contract_change_snapshot` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `change_id` bigint NOT NULL COMMENT '变更单ID',
    `snapshot_type` tinyint DEFAULT NULL COMMENT '快照类型（1合同主表/2费项/3应收）',
    `snapshot_data` longtext DEFAULT NULL COMMENT '快照数据（JSON序列化）',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_change_id` (`change_id`),
    KEY `idx_snapshot_type` (`snapshot_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同变更快照表';

-- 营收填报表（按日录入营业额）
CREATE TABLE IF NOT EXISTS `opr_revenue_report` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id` bigint NOT NULL COMMENT '项目ID',
    `contract_id` bigint NOT NULL COMMENT '合同ID',
    `shop_id` bigint DEFAULT NULL COMMENT '商铺ID',
    `merchant_id` bigint DEFAULT NULL COMMENT '商家ID',
    `report_date` date NOT NULL COMMENT '填报日期（具体某天）',
    `report_month` varchar(7) NOT NULL COMMENT '填报月份（YYYY-MM）',
    `revenue_amount` decimal(14,2) NOT NULL COMMENT '营业额',
    `status` tinyint DEFAULT 0 COMMENT '状态（0待确认/1已确认）',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_report_date` (`report_date`),
    KEY `idx_report_month` (`report_month`),
    KEY `idx_status` (`status`),
    -- ⚠️ 唯一键含 is_deleted：应用层需额外校验 is_deleted=0 记录唯一性，防止重复录入
    UNIQUE KEY `uk_contract_date` (`contract_id`, `report_date`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营收填报表';

-- 浮动租金表（月维度浮动租金计算结果）
CREATE TABLE IF NOT EXISTS `opr_floating_rent` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `contract_id` bigint NOT NULL COMMENT '合同ID',
    `shop_id` bigint DEFAULT NULL COMMENT '商铺ID',
    `calc_month` varchar(7) NOT NULL COMMENT '计算月份（YYYY-MM）',
    `monthly_revenue` decimal(14,2) DEFAULT NULL COMMENT '月营业额',
    `fixed_rent` decimal(14,2) DEFAULT NULL COMMENT '固定租金',
    `commission_rate` decimal(5,2) DEFAULT NULL COMMENT '提成比例（%）',
    `commission_amount` decimal(14,2) DEFAULT NULL COMMENT '提成金额',
    `floating_rent` decimal(14,2) DEFAULT NULL COMMENT '浮动租金（取高后差额或提成结果）',
    `calc_formula` varchar(500) DEFAULT NULL COMMENT '计算公式说明',
    `receivable_id` bigint DEFAULT NULL COMMENT '关联生成的应收记录ID',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_calc_month` (`calc_month`),
    KEY `idx_receivable_id` (`receivable_id`),
    -- ⚠️ 同 opr_revenue_report，应用层需额外校验 is_deleted=0 记录唯一性
    UNIQUE KEY `uk_contract_month` (`contract_id`, `calc_month`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浮动租金表';

-- 浮动租金阶梯明细表（多档累进提成，支持计算过程透明审计）
CREATE TABLE IF NOT EXISTS `opr_floating_rent_tier` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `floating_rent_id` bigint NOT NULL COMMENT '浮动租金记录ID',
    `tier_no` int NOT NULL COMMENT '阶梯档位序号（从1开始）',
    `revenue_from` decimal(14,2) DEFAULT NULL COMMENT '本档起始营业额（NULL表示从0起）',
    `revenue_to` decimal(14,2) DEFAULT NULL COMMENT '本档终止营业额（NULL表示无上限）',
    `rate` decimal(5,2) NOT NULL COMMENT '本档提成比例（%）',
    `tier_amount` decimal(14,2) NOT NULL COMMENT '本档计算提成金额',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_floating_rent_id` (`floating_rent_id`),
    KEY `idx_tier_no` (`floating_rent_id`, `tier_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浮动租金阶梯明细表';

-- 客流填报表（支持项目/楼栋/楼层三级维度）
CREATE TABLE IF NOT EXISTS `opr_passenger_flow` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id` bigint NOT NULL COMMENT '项目ID',
    `building_id` bigint DEFAULT NULL COMMENT '楼栋ID（可选）',
    `floor_id` bigint DEFAULT NULL COMMENT '楼层ID（可选）',
    `report_date` date NOT NULL COMMENT '填报日期',
    `flow_count` int NOT NULL COMMENT '客流人数',
    `source_type` tinyint DEFAULT 1 COMMENT '数据来源（1手动/2导入/3设备对接）',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_building_id` (`building_id`),
    KEY `idx_floor_id` (`floor_id`),
    KEY `idx_report_date` (`report_date`),
    KEY `idx_source_type` (`source_type`),
    UNIQUE KEY `uk_project_building_floor_date` (`project_id`, `building_id`, `floor_id`, `report_date`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客流填报表';

-- 合同解约表
CREATE TABLE IF NOT EXISTS `opr_contract_termination` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `termination_code` varchar(50) NOT NULL COMMENT '解约单号（JY + yyyyMMdd + 4位流水）',
    `contract_id` bigint NOT NULL COMMENT '原合同ID',
    `ledger_id` bigint DEFAULT NULL COMMENT '关联台账ID',
    `project_id` bigint DEFAULT NULL COMMENT '项目ID',
    `merchant_id` bigint DEFAULT NULL COMMENT '商家ID',
    `brand_id` bigint DEFAULT NULL COMMENT '品牌ID',
    `shop_id` bigint DEFAULT NULL COMMENT '商铺ID',
    `termination_type` tinyint NOT NULL COMMENT '解约类型（1到期/2提前/3重签）',
    `termination_date` date NOT NULL COMMENT '解约日期',
    `reason` varchar(1000) DEFAULT NULL COMMENT '解约原因',
    `new_contract_id` bigint DEFAULT NULL COMMENT '重签新合同ID',
    `penalty_amount` decimal(14,2) DEFAULT 0.00 COMMENT '违约金（提前解约时）',
    `refund_deposit` decimal(14,2) DEFAULT 0.00 COMMENT '退还保证金',
    `unsettled_amount` decimal(14,2) DEFAULT 0.00 COMMENT '未结算应收',
    `settlement_amount` decimal(14,2) DEFAULT 0.00 COMMENT '清算总额（正数应收/负数应退）',
    `status` tinyint DEFAULT 0 COMMENT '状态（0草稿/1审批中/2已生效/3驳回）',
    `approval_id` varchar(100) DEFAULT NULL COMMENT '审批流程ID',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_termination_code` (`termination_code`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_ledger_id` (`ledger_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_termination_type` (`termination_type`),
    KEY `idx_status` (`status`),
    KEY `idx_termination_date` (`termination_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同解约表';

-- 解约清算明细表
CREATE TABLE IF NOT EXISTS `opr_termination_settlement` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `termination_id` bigint NOT NULL COMMENT '解约单ID',
    `item_type` tinyint DEFAULT NULL COMMENT '明细类型（1未收租金/2违约金/3保证金退还/4其他）',
    `item_name` varchar(100) DEFAULT NULL COMMENT '明细名称',
    `amount` decimal(14,2) DEFAULT NULL COMMENT '金额（正数应收/负数应退）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_termination_id` (`termination_id`),
    KEY `idx_item_type` (`item_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='解约清算明细表';

-- 预警记录表（合同/应收到期多渠道预警，防重复发送）
-- alert_type: 1合同到期预警 2应收到期预警
-- channel: 1站内信 2邮件 3短信
-- sent_status: 0待发送 1已发送 2发送失败 3已取消
CREATE TABLE IF NOT EXISTS `opr_alert_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `alert_type` tinyint NOT NULL COMMENT '预警类型（1合同到期/2应收到期）',
    `target_id` bigint NOT NULL COMMENT '预警目标ID（合同台账ID或应收计划ID）',
    `alert_date` date NOT NULL COMMENT '预警触发日期',
    `channel` tinyint NOT NULL COMMENT '发送渠道（1站内信/2邮件/3短信）',
    `sent_status` tinyint DEFAULT 0 COMMENT '发送状态（0待发送/1已发送/2发送失败/3已取消）',
    `sent_time` datetime DEFAULT NULL COMMENT '实际发送时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注（失败原因、取消原因等）',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint DEFAULT 0 COMMENT '删除标识（0正常/1已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_alert_type_target` (`alert_type`, `target_id`),
    KEY `idx_alert_date` (`alert_date`),
    KEY `idx_sent_status` (`sent_status`),
    -- 去重唯一键：同类型+同目标+同日期+同渠道只发一次
    UNIQUE KEY `uk_alert_dedup` (`alert_type`, `target_id`, `alert_date`, `channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警记录表';

-- =====================================================================
-- 复合索引优化（§3.2 节）
-- =====================================================================

-- 1. 合同台账多条件筛选（含 is_deleted 过滤，线上查询必带逻辑删除条件）
ALTER TABLE opr_contract_ledger ADD INDEX idx_composite_query (
    is_deleted, project_id, audit_status, status, contract_end
);

-- 2. 应收计划账期与状态联合查询
ALTER TABLE opr_receivable_plan ADD INDEX idx_billing_status (
    is_deleted, ledger_id, billing_start, billing_end, status
);

-- 3. 营收填报数据完整性校验
ALTER TABLE opr_revenue_report ADD INDEX idx_date_range (
    is_deleted, contract_id, report_date, report_month
);

-- 4. 浮动租金计算查询优化
ALTER TABLE opr_floating_rent ADD INDEX idx_calc_query (
    is_deleted, contract_id, calc_month, monthly_revenue
);
