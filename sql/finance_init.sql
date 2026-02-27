-- ============================================================
-- 财务管理模块数据库初始化脚本
-- 11张表：应收/收款/核销/凭证/保证金/预收款/减免
-- ============================================================

-- 1. 应收明细表（接收营运模块推送的应收计划，财务侧正式AR）
CREATE TABLE IF NOT EXISTS `fin_receivable` (
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `receivable_code` varchar(50)     NOT NULL COMMENT '应收编号（AR + yyyyMMdd + 6位流水）',
    `contract_id`     bigint          NOT NULL COMMENT '合同ID',
    `ledger_id`       bigint          DEFAULT NULL COMMENT '台账ID',
    `plan_id`         bigint          DEFAULT NULL COMMENT '来源应收计划ID（opr_receivable_plan.id）',
    `project_id`      bigint          NOT NULL COMMENT '项目ID',
    `merchant_id`     bigint          NOT NULL COMMENT '商家ID',
    `brand_id`        bigint          DEFAULT NULL COMMENT '品牌ID',
    `shop_id`         bigint          DEFAULT NULL COMMENT '商铺ID',
    `fee_item_id`     bigint          DEFAULT NULL COMMENT '收款项目ID',
    `fee_name`        varchar(100)    DEFAULT NULL COMMENT '费项名称（快照）',
    `billing_start`   date            DEFAULT NULL COMMENT '账期开始',
    `billing_end`     date            DEFAULT NULL COMMENT '账期结束',
    `due_date`        date            DEFAULT NULL COMMENT '应收日期/付款截止日',
    `accrual_month`   varchar(7)      DEFAULT NULL COMMENT '归属月份（yyyy-MM）',
    `original_amount` decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '原始金额（含税）',
    `actual_amount`   decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '实际应收金额（减免后）',
    `received_amount` decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '已收金额',
    `reduction_amount` decimal(14,2)  NOT NULL DEFAULT 0.00 COMMENT '减免金额',
    `status`          tinyint         NOT NULL DEFAULT 0 COMMENT '状态（0待收/1部分收款/2已收/3已作废/4已减免）',
    `overdue_days`    int             DEFAULT 0 COMMENT '逾期天数（>0表示逾期）',
    `created_by`      bigint          DEFAULT NULL COMMENT '创建人',
    `created_at`      datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      bigint          DEFAULT NULL COMMENT '更新人',
    `updated_at`      datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_receivable_code` (`receivable_code`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_due_date` (`due_date`),
    KEY `idx_accrual_month` (`accrual_month`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应收明细表';

-- 2. 收款单主表
CREATE TABLE IF NOT EXISTS `fin_receipt` (
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `receipt_code`    varchar(50)     NOT NULL COMMENT '收款单号（SK + yyyyMMdd + 4位流水）',
    `contract_id`     bigint          NOT NULL COMMENT '合同ID',
    `project_id`      bigint          NOT NULL COMMENT '项目ID',
    `merchant_id`     bigint          NOT NULL COMMENT '商家ID',
    `total_amount`    decimal(14,2)   NOT NULL COMMENT '收款总金额',
    `unwritten_amount` decimal(14,2)  NOT NULL DEFAULT 0.00 COMMENT '未核销金额',
    `payment_method`  tinyint         NOT NULL COMMENT '收款方式（1银行转账/2现金/3支付宝/4微信/5其他）',
    `receipt_date`    date            NOT NULL COMMENT '收款日期',
    `bank_serial_no`  varchar(100)    DEFAULT NULL COMMENT '银行流水号',
    `payer_name`      varchar(100)    DEFAULT NULL COMMENT '付款方名称',
    `bank_name`       varchar(100)    DEFAULT NULL COMMENT '开户行名称',
    `remark`          varchar(500)    DEFAULT NULL COMMENT '备注',
    `status`          tinyint         NOT NULL DEFAULT 0 COMMENT '状态（0待核销/1部分核销/2已核销/3已作废）',
    `created_by`      bigint          DEFAULT NULL COMMENT '创建人',
    `created_at`      datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      bigint          DEFAULT NULL COMMENT '更新人',
    `updated_at`      datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_receipt_code` (`receipt_code`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_receipt_date` (`receipt_date`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收款单主表';

-- 3. 核销单主表（一张收款单可产生多张核销单）
CREATE TABLE IF NOT EXISTS `fin_write_off` (
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `write_off_code`  varchar(50)     NOT NULL COMMENT '核销单号（HX + yyyyMMdd + 4位流水）',
    `receipt_id`      bigint          NOT NULL COMMENT '收款单ID',
    `contract_id`     bigint          NOT NULL COMMENT '合同ID',
    `project_id`      bigint          NOT NULL COMMENT '项目ID',
    `merchant_id`     bigint          NOT NULL COMMENT '商家ID',
    `write_off_amount` decimal(14,2)  NOT NULL COMMENT '本次核销金额',
    `write_off_date`  date            NOT NULL COMMENT '核销日期',
    `write_off_type`  tinyint         NOT NULL DEFAULT 1 COMMENT '核销类型（1正常/2负数核销/3余额处理）',
    `remark`          varchar(500)    DEFAULT NULL COMMENT '备注',
    `status`          tinyint         NOT NULL DEFAULT 1 COMMENT '状态（1有效/2已撤销）',
    `created_by`      bigint          DEFAULT NULL COMMENT '创建人',
    `created_at`      datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      bigint          DEFAULT NULL COMMENT '更新人',
    `updated_at`      datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_write_off_code` (`write_off_code`),
    KEY `idx_receipt_id` (`receipt_id`),
    KEY `idx_contract_id` (`contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核销单主表';

-- 4. 核销明细（核销单对应的应收明细）
CREATE TABLE IF NOT EXISTS `fin_write_off_detail` (
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `write_off_id`    bigint          NOT NULL COMMENT '核销单ID',
    `receivable_id`   bigint          NOT NULL COMMENT '应收明细ID',
    `amount`          decimal(14,2)   NOT NULL COMMENT '核销金额',
    `created_by`      bigint          DEFAULT NULL COMMENT '创建人',
    `created_at`      datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      bigint          DEFAULT NULL COMMENT '更新人',
    `updated_at`      datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_write_off_id` (`write_off_id`),
    KEY `idx_receivable_id` (`receivable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核销明细表';

-- 5. 财务凭证主表
CREATE TABLE IF NOT EXISTS `fin_voucher` (
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `voucher_no`      varchar(50)     NOT NULL COMMENT '凭证号',
    `voucher_type`    tinyint         NOT NULL COMMENT '凭证类型（1收款凭证/2核销凭证/3减免凭证）',
    `source_id`       bigint          DEFAULT NULL COMMENT '来源单据ID（收款单/核销单等）',
    `source_type`     tinyint         DEFAULT NULL COMMENT '来源类型（1收款单/2核销单/3减免）',
    `voucher_date`    date            NOT NULL COMMENT '凭证日期',
    `total_debit`     decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '借方合计',
    `total_credit`    decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '贷方合计',
    `summary`         varchar(200)    DEFAULT NULL COMMENT '摘要',
    `status`          tinyint         NOT NULL DEFAULT 0 COMMENT '状态（0草稿/1已审核/2已过账）',
    `audit_by`        bigint          DEFAULT NULL COMMENT '审核人',
    `audit_time`      datetime        DEFAULT NULL COMMENT '审核时间',
    `created_by`      bigint          DEFAULT NULL COMMENT '创建人',
    `created_at`      datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      bigint          DEFAULT NULL COMMENT '更新人',
    `updated_at`      datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_voucher_no` (`voucher_no`),
    KEY `idx_source` (`source_type`, `source_id`),
    KEY `idx_voucher_date` (`voucher_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务凭证主表';

-- 6. 凭证分录
CREATE TABLE IF NOT EXISTS `fin_voucher_entry` (
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `voucher_id`      bigint          NOT NULL COMMENT '凭证ID',
    `line_no`         int             NOT NULL COMMENT '行号',
    `account_code`    varchar(20)     NOT NULL COMMENT '科目编码',
    `account_name`    varchar(100)    NOT NULL COMMENT '科目名称',
    `summary`         varchar(200)    DEFAULT NULL COMMENT '摘要',
    `debit_amount`    decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '借方金额',
    `credit_amount`   decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '贷方金额',
    `created_by`      bigint          DEFAULT NULL COMMENT '创建人',
    `created_at`      datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      bigint          DEFAULT NULL COMMENT '更新人',
    `updated_at`      datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='凭证分录表';

-- 7. 保证金台账（每个有效合同一条）
CREATE TABLE IF NOT EXISTS `fin_deposit` (
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `contract_id`     bigint          NOT NULL COMMENT '合同ID',
    `project_id`      bigint          NOT NULL COMMENT '项目ID',
    `merchant_id`     bigint          NOT NULL COMMENT '商家ID',
    `required_amount` decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '应缴保证金',
    `paid_amount`     decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '已缴金额',
    `balance`         decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '当前余额',
    `status`          tinyint         NOT NULL DEFAULT 0 COMMENT '状态（0未缴/1部分缴/2已缴足/3已退还/4已罚没）',
    `created_by`      bigint          DEFAULT NULL COMMENT '创建人',
    `created_at`      datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      bigint          DEFAULT NULL COMMENT '更新人',
    `updated_at`      datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contract_id` (`contract_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='保证金台账';

-- 8. 保证金流水记录
CREATE TABLE IF NOT EXISTS `fin_deposit_record` (
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `deposit_id`      bigint          NOT NULL COMMENT '保证金台账ID',
    `contract_id`     bigint          NOT NULL COMMENT '合同ID',
    `operation_type`  tinyint         NOT NULL COMMENT '操作类型（1缴纳/2冲抵应收/3退款/4罚没）',
    `amount`          decimal(14,2)   NOT NULL COMMENT '操作金额（正数入/负数出）',
    `balance_after`   decimal(14,2)   NOT NULL COMMENT '操作后余额',
    `operation_date`  date            NOT NULL COMMENT '操作日期',
    `remark`          varchar(500)    DEFAULT NULL COMMENT '备注',
    `created_by`      bigint          DEFAULT NULL COMMENT '创建人',
    `created_at`      datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      bigint          DEFAULT NULL COMMENT '更新人',
    `updated_at`      datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_deposit_id` (`deposit_id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_operation_date` (`operation_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='保证金流水记录';

-- 9. 预收款台账（每个合同一条）
CREATE TABLE IF NOT EXISTS `fin_prepayment` (
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `contract_id`     bigint          NOT NULL COMMENT '合同ID',
    `project_id`      bigint          NOT NULL COMMENT '项目ID',
    `merchant_id`     bigint          NOT NULL COMMENT '商家ID',
    `balance`         decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '当前余额',
    `total_in`        decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '累计入账',
    `total_out`       decimal(14,2)   NOT NULL DEFAULT 0.00 COMMENT '累计出账（抵冲/退款）',
    `status`          tinyint         NOT NULL DEFAULT 0 COMMENT '状态（0正常/1已清零）',
    `created_by`      bigint          DEFAULT NULL COMMENT '创建人',
    `created_at`      datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      bigint          DEFAULT NULL COMMENT '更新人',
    `updated_at`      datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contract_id` (`contract_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预收款台账';

-- 10. 预收款流水记录
CREATE TABLE IF NOT EXISTS `fin_prepayment_record` (
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `prepayment_id`   bigint          NOT NULL COMMENT '预收款台账ID',
    `contract_id`     bigint          NOT NULL COMMENT '合同ID',
    `operation_type`  tinyint         NOT NULL COMMENT '操作类型（1暂存入账/2抵冲应收/3退款）',
    `amount`          decimal(14,2)   NOT NULL COMMENT '操作金额（正数入/负数出）',
    `balance_after`   decimal(14,2)   NOT NULL COMMENT '操作后余额',
    `operation_date`  date            NOT NULL COMMENT '操作日期',
    `ref_receivable_id` bigint        DEFAULT NULL COMMENT '关联应收ID（抵冲时）',
    `remark`          varchar(500)    DEFAULT NULL COMMENT '备注',
    `created_by`      bigint          DEFAULT NULL COMMENT '创建人',
    `created_at`      datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      bigint          DEFAULT NULL COMMENT '更新人',
    `updated_at`      datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_prepayment_id` (`prepayment_id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_operation_date` (`operation_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预收款流水记录';

-- 11. 减免调整记录
CREATE TABLE IF NOT EXISTS `fin_reduction` (
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `reduction_code`  varchar(50)     NOT NULL COMMENT '减免单号（JM + yyyyMMdd + 4位流水）',
    `receivable_id`   bigint          NOT NULL COMMENT '应收明细ID',
    `contract_id`     bigint          NOT NULL COMMENT '合同ID',
    `project_id`      bigint          NOT NULL COMMENT '项目ID',
    `merchant_id`     bigint          NOT NULL COMMENT '商家ID',
    `reduction_type`  tinyint         NOT NULL COMMENT '减免类型（1租金减免/2费项减免/3优惠政策/4其他）',
    `reduction_amount` decimal(14,2)  NOT NULL COMMENT '减免金额',
    `reason`          varchar(500)    NOT NULL COMMENT '减免原因',
    `approval_id`     varchar(100)    DEFAULT NULL COMMENT 'OA审批流程ID',
    `status`          tinyint         NOT NULL DEFAULT 0 COMMENT '状态（0草稿/1审批中/2已生效/3已驳回）',
    `effective_date`  date            DEFAULT NULL COMMENT '生效日期',
    `created_by`      bigint          DEFAULT NULL COMMENT '创建人',
    `created_at`      datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      bigint          DEFAULT NULL COMMENT '更新人',
    `updated_at`      datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      tinyint         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reduction_code` (`reduction_code`),
    KEY `idx_receivable_id` (`receivable_id`),
    KEY `idx_contract_id` (`contract_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='减免调整记录';
