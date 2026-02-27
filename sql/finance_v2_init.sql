-- ==========================================
-- 财务管理模块数据库 V2 重建脚本
-- 数据库：asset_db
-- 执行说明：此脚本幂等，可重复执行；会删除旧表后重建
-- 架构规范：DECIMAL(14,2)金额精度 | 五件套审计字段 | 复合唯一索引
-- ==========================================

USE asset_db;

-- ------------------------------------------
-- 第一步：删除旧命名表（finance_init.sql 遗留）
-- ------------------------------------------
DROP TABLE IF EXISTS `fin_reduction`;
DROP TABLE IF EXISTS `fin_deposit_record`;
DROP TABLE IF EXISTS `fin_deposit`;
DROP TABLE IF EXISTS `fin_prepayment_record`;
DROP TABLE IF EXISTS `fin_prepayment`;

-- ------------------------------------------
-- 第二步：删除新命名表（幂等，按依赖倒序）
-- ------------------------------------------
DROP TABLE IF EXISTS `fin_prepay_transaction`;
DROP TABLE IF EXISTS `fin_prepay_account`;
DROP TABLE IF EXISTS `fin_deposit_transaction`;
DROP TABLE IF EXISTS `fin_deposit_account`;
DROP TABLE IF EXISTS `fin_voucher_entry`;
DROP TABLE IF EXISTS `fin_voucher`;
DROP TABLE IF EXISTS `fin_receivable_adjustment`;
DROP TABLE IF EXISTS `fin_receivable_deduction`;
DROP TABLE IF EXISTS `fin_write_off_detail`;
DROP TABLE IF EXISTS `fin_write_off`;
DROP TABLE IF EXISTS `fin_receivable`;
DROP TABLE IF EXISTS `fin_receipt_detail`;
DROP TABLE IF EXISTS `fin_receipt`;

-- ------------------------------------------
-- 第三步：创建新表（按依赖正序）
-- ------------------------------------------

-- 收款单表
CREATE TABLE `fin_receipt` (
    `id`                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT                             COMMENT '主键ID',
    `receipt_code`      VARCHAR(50)      NOT NULL                                            COMMENT '收款单号（系统自动生成）',
    `contract_id`       BIGINT UNSIGNED  NOT NULL                                            COMMENT '合同ID',
    `project_id`        BIGINT UNSIGNED  DEFAULT NULL                                        COMMENT '项目ID（自动带出）',
    `merchant_id`       BIGINT UNSIGNED  DEFAULT NULL                                        COMMENT '商家ID（自动带出）',
    `brand_id`          BIGINT UNSIGNED  DEFAULT NULL                                        COMMENT '品牌ID',
    `shop_code`         VARCHAR(50)      DEFAULT NULL                                        COMMENT '店铺编号',
    `total_amount`      DECIMAL(14,2)    NOT NULL DEFAULT '0.00'                             COMMENT '实收总金额',
    `payment_method`    TINYINT          DEFAULT '1'                                         COMMENT '收款方式：1银行转账/2现金/3支票/4POS',
    `bank_serial_no`    VARCHAR(100)     DEFAULT NULL                                        COMMENT '银行流水号',
    `payer_name`        VARCHAR(200)     DEFAULT NULL                                        COMMENT '收款单位（付款方名称）',
    `bank_name`         VARCHAR(200)     DEFAULT NULL                                        COMMENT '收款银行',
    `bank_account`      VARCHAR(50)      DEFAULT NULL                                        COMMENT '收款账号',
    `is_unnamed`        TINYINT          DEFAULT '0'                                         COMMENT '是否未名款项：0否/1是',
    `accounting_entity` VARCHAR(200)     DEFAULT NULL                                        COMMENT '核算主体',
    `receipt_date`      DATE             NOT NULL                                            COMMENT '收款日期',
    `receiver`          VARCHAR(50)      DEFAULT NULL                                        COMMENT '收款人',
    `status`            TINYINT          DEFAULT '0'                                         COMMENT '状态：0待核销/1部分核销/2已全部核销/3已作废',
    `write_off_amount`  DECIMAL(14,2)    DEFAULT '0.00'                                      COMMENT '已核销金额',
    `prepay_amount`     DECIMAL(14,2)    DEFAULT '0.00'                                      COMMENT '转预存款金额',
    `version`           INT UNSIGNED     DEFAULT '1'                                         COMMENT '乐观锁版本号',
    `created_by`        VARCHAR(50)      NOT NULL DEFAULT 'system'                           COMMENT '创建人',
    `created_at`        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP                  COMMENT '创建时间',
    `updated_by`        VARCHAR(50)      NOT NULL DEFAULT 'system'                           COMMENT '更新人',
    `updated_at`        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT          NOT NULL DEFAULT '0'                                COMMENT '逻辑删除：0正常/1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_receipt_code_version_deleted` (`receipt_code`, `version`, `is_deleted`) COMMENT '复合唯一索引：支持逻辑删除后重建',
    KEY `idx_contract_id`  (`contract_id`),
    KEY `idx_project_id`   (`project_id`),
    KEY `idx_merchant_id`  (`merchant_id`),
    KEY `idx_receipt_date` (`receipt_date`),
    KEY `idx_status`       (`status`),
    KEY `idx_is_unnamed`   (`is_unnamed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款单表';

-- 收款拆分明细表
CREATE TABLE `fin_receipt_detail` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                                    COMMENT '主键ID',
    `receipt_id`  BIGINT UNSIGNED NOT NULL                                                   COMMENT '收款单ID',
    `fee_item_id` BIGINT UNSIGNED DEFAULT NULL                                               COMMENT '费项ID',
    `fee_name`    VARCHAR(100)    DEFAULT NULL                                               COMMENT '费项名称（冗余存储）',
    `amount`      DECIMAL(14,2)   NOT NULL DEFAULT '0.00'                                   COMMENT '拆分金额',
    `remark`      VARCHAR(500)    DEFAULT NULL                                               COMMENT '备注',
    `created_by`  VARCHAR(50)     NOT NULL DEFAULT 'system'                                 COMMENT '创建人',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                        COMMENT '创建时间',
    `updated_by`  VARCHAR(50)     NOT NULL DEFAULT 'system'                                 COMMENT '更新人',
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT         NOT NULL DEFAULT '0'                                      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_receipt_id`  (`receipt_id`),
    KEY `idx_fee_item_id` (`fee_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款拆分明细表';

-- 应收台账表（财务视角）
CREATE TABLE `fin_receivable` (
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                             COMMENT '主键ID',
    `receivable_code`    VARCHAR(50)     NOT NULL                                            COMMENT '应收编码',
    `contract_id`        BIGINT UNSIGNED NOT NULL                                            COMMENT '合同ID',
    `ledger_id`          BIGINT UNSIGNED DEFAULT NULL                                        COMMENT '合同台账ID（业务台账关联）',
    `project_id`         BIGINT UNSIGNED DEFAULT NULL                                        COMMENT '项目ID',
    `merchant_id`        BIGINT UNSIGNED DEFAULT NULL                                        COMMENT '商家ID',
    `shop_id`            BIGINT UNSIGNED DEFAULT NULL                                        COMMENT '商铺ID',
    `fee_item_id`        BIGINT UNSIGNED DEFAULT NULL                                        COMMENT '费项ID',
    `fee_name`           VARCHAR(100)    DEFAULT NULL                                        COMMENT '费项名称（冗余存储）',
    `billing_start`      DATE            DEFAULT NULL                                        COMMENT '账期开始',
    `billing_end`        DATE            DEFAULT NULL                                        COMMENT '账期结束',
    `accrual_month`      VARCHAR(7)      DEFAULT NULL                                        COMMENT '权责月（YYYY-MM）',
    `due_date`           DATE            DEFAULT NULL                                        COMMENT '应收日期',
    `original_amount`    DECIMAL(14,2)   NOT NULL DEFAULT '0.00'                            COMMENT '原始应收金额（不可修改）',
    `adjust_amount`      DECIMAL(14,2)   DEFAULT '0.00'                                     COMMENT '累计调整金额',
    `deduction_amount`   DECIMAL(14,2)   DEFAULT '0.00'                                     COMMENT '累计减免金额',
    `actual_amount`      DECIMAL(14,2)   DEFAULT '0.00'                                     COMMENT '实际应收=原始+调整-减免',
    `received_amount`    DECIMAL(14,2)   DEFAULT '0.00'                                     COMMENT '已收金额',
    `outstanding_amount` DECIMAL(14,2)   DEFAULT '0.00'                                     COMMENT '欠费金额=实际应收-已收',
    `status`             TINYINT         DEFAULT '0'                                         COMMENT '状态：0待收/1部分收款/2已收清/3已减免/4已作废',
    `is_printed`         TINYINT         DEFAULT '0'                                         COMMENT '是否已打印：0否/1是',
    `is_invoiced`        TINYINT         DEFAULT '0'                                         COMMENT '是否已开票：0否/1是',
    `version`            INT UNSIGNED    DEFAULT '1'                                         COMMENT '乐观锁版本号',
    `created_by`         VARCHAR(50)     NOT NULL DEFAULT 'system'                           COMMENT '创建人',
    `created_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                  COMMENT '创建时间',
    `updated_by`         VARCHAR(50)     NOT NULL DEFAULT 'system'                           COMMENT '更新人',
    `updated_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`         TINYINT         NOT NULL DEFAULT '0'                                COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_receivable_code_version_deleted` (`receivable_code`, `version`, `is_deleted`),
    KEY `idx_contract_id`        (`contract_id`),
    KEY `idx_project_id`         (`project_id`),
    KEY `idx_merchant_id`        (`merchant_id`),
    KEY `idx_fee_item_id`        (`fee_item_id`),
    KEY `idx_accrual_month`      (`accrual_month`),
    KEY `idx_due_date`           (`due_date`),
    KEY `idx_status`             (`status`),
    KEY `idx_outstanding_amount` (`outstanding_amount`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收台账表';

-- 应收减免单表
CREATE TABLE `fin_receivable_deduction` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                               COMMENT '主键ID',
    `deduction_code`   VARCHAR(50)     NOT NULL                                              COMMENT '减免单号',
    `receivable_id`    BIGINT UNSIGNED NOT NULL                                              COMMENT '应收记录ID',
    `contract_id`      BIGINT UNSIGNED DEFAULT NULL                                          COMMENT '合同ID',
    `deduction_amount` DECIMAL(14,2)   NOT NULL DEFAULT '0.00'                              COMMENT '减免金额',
    `reason`           VARCHAR(1000)   DEFAULT NULL                                          COMMENT '减免原因',
    `status`           TINYINT         DEFAULT '0'                                           COMMENT '状态：0待审批/1通过/2驳回',
    `approval_id`      VARCHAR(100)    DEFAULT NULL                                          COMMENT '审批流程ID',
    `version`          INT UNSIGNED    DEFAULT '1'                                           COMMENT '乐观锁版本号',
    `created_by`       VARCHAR(50)     NOT NULL DEFAULT 'system'                             COMMENT '创建人',
    `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_by`       VARCHAR(50)     NOT NULL DEFAULT 'system'                             COMMENT '更新人',
    `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       TINYINT         NOT NULL DEFAULT '0'                                  COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_deduction_code_version_deleted` (`deduction_code`, `version`, `is_deleted`),
    KEY `idx_receivable_id` (`receivable_id`),
    KEY `idx_contract_id`   (`contract_id`),
    KEY `idx_status`        (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收减免单表';

-- 应收调整单表
CREATE TABLE `fin_receivable_adjustment` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                               COMMENT '主键ID',
    `adjustment_code`  VARCHAR(50)     NOT NULL                                              COMMENT '调整单号',
    `receivable_id`    BIGINT UNSIGNED NOT NULL                                              COMMENT '应收记录ID',
    `contract_id`      BIGINT UNSIGNED DEFAULT NULL                                          COMMENT '合同ID',
    `adjust_type`      TINYINT         DEFAULT '1'                                           COMMENT '调整类型：1增加/2减少',
    `adjust_amount`    DECIMAL(14,2)   NOT NULL DEFAULT '0.00'                              COMMENT '调整金额',
    `reason`           VARCHAR(1000)   DEFAULT NULL                                          COMMENT '调整原因',
    `status`           TINYINT         DEFAULT '0'                                           COMMENT '状态：0待审批/1通过/2驳回',
    `approval_id`      VARCHAR(100)    DEFAULT NULL                                          COMMENT '审批流程ID',
    `version`          INT UNSIGNED    DEFAULT '1'                                           COMMENT '乐观锁版本号',
    `created_by`       VARCHAR(50)     NOT NULL DEFAULT 'system'                             COMMENT '创建人',
    `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_by`       VARCHAR(50)     NOT NULL DEFAULT 'system'                             COMMENT '更新人',
    `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       TINYINT         NOT NULL DEFAULT '0'                                  COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_adjustment_code_version_deleted` (`adjustment_code`, `version`, `is_deleted`),
    KEY `idx_receivable_id` (`receivable_id`),
    KEY `idx_contract_id`   (`contract_id`),
    KEY `idx_status`        (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收调整单表';

-- 核销单表
CREATE TABLE `fin_write_off` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                                 COMMENT '主键ID',
    `write_off_code` VARCHAR(50)     NOT NULL                                                COMMENT '核销单号',
    `receipt_id`     BIGINT UNSIGNED DEFAULT NULL                                            COMMENT '关联收款单ID（现金核销时）',
    `contract_id`    BIGINT UNSIGNED DEFAULT NULL                                            COMMENT '合同ID',
    `merchant_id`    BIGINT UNSIGNED DEFAULT NULL                                            COMMENT '商家ID',
    `project_id`     BIGINT UNSIGNED DEFAULT NULL                                            COMMENT '项目ID',
    `write_off_type` TINYINT         DEFAULT '1'                                             COMMENT '核销类型：1收款核销/2保证金核销/3预收款核销/4负数核销',
    `total_amount`   DECIMAL(14,2)   NOT NULL DEFAULT '0.00'                                COMMENT '核销总金额',
    `status`         TINYINT         DEFAULT '0'                                             COMMENT '状态：0待审核/1审核通过/2驳回',
    `upload_status`  TINYINT         DEFAULT '0'                                             COMMENT '上传状态：0未上传/1已上传',
    `upload_time`    DATETIME        DEFAULT NULL                                            COMMENT '上传时间',
    `approval_id`    VARCHAR(100)    DEFAULT NULL                                            COMMENT '审批流程ID（OA系统）',
    `version`        INT UNSIGNED    DEFAULT '1'                                             COMMENT '乐观锁版本号',
    `created_by`     VARCHAR(50)     NOT NULL DEFAULT 'system'                               COMMENT '创建人',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                      COMMENT '创建时间',
    `updated_by`     VARCHAR(50)     NOT NULL DEFAULT 'system'                               COMMENT '更新人',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT         NOT NULL DEFAULT '0'                                    COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_write_off_code_version_deleted` (`write_off_code`, `version`, `is_deleted`),
    KEY `idx_receipt_id`     (`receipt_id`),
    KEY `idx_contract_id`    (`contract_id`),
    KEY `idx_merchant_id`    (`merchant_id`),
    KEY `idx_status`         (`status`),
    KEY `idx_upload_status`  (`upload_status`),
    KEY `idx_write_off_type` (`write_off_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核销单表';

-- 核销明细表
CREATE TABLE `fin_write_off_detail` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                               COMMENT '主键ID',
    `write_off_id`     BIGINT UNSIGNED NOT NULL                                              COMMENT '核销单ID',
    `receivable_id`    BIGINT UNSIGNED NOT NULL                                              COMMENT '应收记录ID',
    `fee_item_id`      BIGINT UNSIGNED DEFAULT NULL                                          COMMENT '费项ID',
    `accrual_month`    VARCHAR(7)      DEFAULT NULL                                          COMMENT '权责月（YYYY-MM）',
    `write_off_amount` DECIMAL(14,2)   NOT NULL DEFAULT '0.00'                              COMMENT '核销金额',
    `overpay_amount`   DECIMAL(14,2)   DEFAULT '0.00'                                       COMMENT '超出转预存款金额',
    `created_by`       VARCHAR(50)     NOT NULL DEFAULT 'system'                             COMMENT '创建人',
    `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
    `updated_by`       VARCHAR(50)     NOT NULL DEFAULT 'system'                             COMMENT '更新人',
    `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       TINYINT         NOT NULL DEFAULT '0'                                  COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_write_off_id`  (`write_off_id`),
    KEY `idx_receivable_id` (`receivable_id`),
    KEY `idx_accrual_month` (`accrual_month`),
    KEY `idx_fee_item_id`   (`fee_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核销明细表';

-- 财务凭证表
CREATE TABLE `fin_voucher` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                                   COMMENT '主键ID',
    `voucher_code` VARCHAR(50)     NOT NULL                                                  COMMENT '凭证编号',
    `project_id`   BIGINT UNSIGNED DEFAULT NULL                                              COMMENT '项目ID',
    `account_set`  VARCHAR(50)     DEFAULT NULL                                              COMMENT '账套',
    `pay_type`     TINYINT         DEFAULT '1'                                               COMMENT '收付类型：1收款/2付款',
    `voucher_date` DATE            DEFAULT NULL                                              COMMENT '凭证日期',
    `total_debit`  DECIMAL(14,2)   DEFAULT '0.00'                                           COMMENT '借方合计',
    `total_credit` DECIMAL(14,2)   DEFAULT '0.00'                                           COMMENT '贷方合计',
    `status`       TINYINT         DEFAULT '0'                                               COMMENT '状态：0待审核/1已审核/2已上传',
    `upload_time`  DATETIME        DEFAULT NULL                                              COMMENT '上传时间',
    `remark`       VARCHAR(500)    DEFAULT NULL                                              COMMENT '摘要',
    `version`      INT UNSIGNED    DEFAULT '1'                                               COMMENT '乐观锁版本号',
    `created_by`   VARCHAR(50)     NOT NULL DEFAULT 'system'                                 COMMENT '创建人',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                        COMMENT '创建时间',
    `updated_by`   VARCHAR(50)     NOT NULL DEFAULT 'system'                                 COMMENT '更新人',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT         NOT NULL DEFAULT '0'                                      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_voucher_code_version_deleted` (`voucher_code`, `version`, `is_deleted`),
    KEY `idx_project_id`   (`project_id`),
    KEY `idx_voucher_date` (`voucher_date`),
    KEY `idx_status`       (`status`),
    KEY `idx_account_set`  (`account_set`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务凭证表';

-- 凭证分录表
CREATE TABLE `fin_voucher_entry` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                                 COMMENT '主键ID',
    `voucher_id`     BIGINT UNSIGNED NOT NULL                                                COMMENT '凭证ID',
    `source_type`    TINYINT         DEFAULT NULL                                            COMMENT '来源类型：1收款单/2核销单/3应收单',
    `source_id`      BIGINT UNSIGNED DEFAULT NULL                                            COMMENT '来源单据ID',
    `account_code`   VARCHAR(50)     DEFAULT NULL                                            COMMENT '会计科目编码',
    `account_name`   VARCHAR(200)    DEFAULT NULL                                            COMMENT '会计科目名称',
    `debit_amount`   DECIMAL(14,2)   DEFAULT '0.00'                                         COMMENT '借方金额',
    `credit_amount`  DECIMAL(14,2)   DEFAULT '0.00'                                         COMMENT '贷方金额',
    `summary`        VARCHAR(500)    DEFAULT NULL                                            COMMENT '摘要',
    `created_by`     VARCHAR(50)     NOT NULL DEFAULT 'system'                               COMMENT '创建人',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                      COMMENT '创建时间',
    `updated_by`     VARCHAR(50)     NOT NULL DEFAULT 'system'                               COMMENT '更新人',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT         NOT NULL DEFAULT '0'                                    COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_voucher_id`       (`voucher_id`),
    KEY `idx_source_type_id`   (`source_type`, `source_id`),
    KEY `idx_account_code`     (`account_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='凭证分录表';

-- 保证金账户表
CREATE TABLE `fin_deposit_account` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                                  COMMENT '主键ID',
    `contract_id`   BIGINT UNSIGNED NOT NULL                                                 COMMENT '合同ID',
    `merchant_id`   BIGINT UNSIGNED DEFAULT NULL                                             COMMENT '商家ID',
    `project_id`    BIGINT UNSIGNED DEFAULT NULL                                             COMMENT '项目ID',
    `fee_item_id`   BIGINT UNSIGNED DEFAULT NULL                                             COMMENT '保证金费项ID',
    `balance`       DECIMAL(14,2)   DEFAULT '0.00'                                          COMMENT '当前余额',
    `total_in`      DECIMAL(14,2)   DEFAULT '0.00'                                          COMMENT '累计收入',
    `total_offset`  DECIMAL(14,2)   DEFAULT '0.00'                                          COMMENT '累计冲抵',
    `total_refund`  DECIMAL(14,2)   DEFAULT '0.00'                                          COMMENT '累计退款',
    `total_forfeit` DECIMAL(14,2)   DEFAULT '0.00'                                          COMMENT '累计罚没',
    `version`       INT UNSIGNED    DEFAULT '1'                                              COMMENT '乐观锁版本号（并发控制）',
    `created_by`    VARCHAR(50)     NOT NULL DEFAULT 'system'                                COMMENT '创建人',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                       COMMENT '创建时间',
    `updated_by`    VARCHAR(50)     NOT NULL DEFAULT 'system'                                COMMENT '更新人',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT         NOT NULL DEFAULT '0'                                     COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contract_fee_version_deleted` (`contract_id`, `fee_item_id`, `version`, `is_deleted`) COMMENT '合同+费项维度唯一',
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_project_id`  (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保证金账户表';

-- 保证金流水表
CREATE TABLE `fin_deposit_transaction` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                                   COMMENT '主键ID',
    `account_id`   BIGINT UNSIGNED NOT NULL                                                  COMMENT '保证金账户ID',
    `trans_type`   TINYINT         NOT NULL                                                  COMMENT '交易类型：1收入/2冲抵/3退款/4罚没',
    `amount`       DECIMAL(14,2)   NOT NULL DEFAULT '0.00'                                  COMMENT '交易金额',
    `balance_after` DECIMAL(14,2)  DEFAULT '0.00'                                           COMMENT '交易后余额',
    `trans_date`   DATE            DEFAULT NULL                                              COMMENT '交易日期',
    `source_code`  VARCHAR(50)     DEFAULT NULL                                              COMMENT '关联单据号（如收款单号/核销单号）',
    `reason`       VARCHAR(500)    DEFAULT NULL                                              COMMENT '原因说明',
    `status`       TINYINT         DEFAULT '0'                                               COMMENT '状态：0待审核/1已审核',
    `approval_id`  VARCHAR(100)    DEFAULT NULL                                              COMMENT '审批流程ID',
    `created_by`   VARCHAR(50)     NOT NULL DEFAULT 'system'                                 COMMENT '创建人',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                        COMMENT '创建时间',
    `updated_by`   VARCHAR(50)     NOT NULL DEFAULT 'system'                                 COMMENT '更新人',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT         NOT NULL DEFAULT '0'                                      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_account_id`  (`account_id`),
    KEY `idx_trans_type`  (`trans_type`),
    KEY `idx_trans_date`  (`trans_date`),
    KEY `idx_source_code` (`source_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保证金流水表';

-- 预收款账户表
CREATE TABLE `fin_prepay_account` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                                    COMMENT '主键ID',
    `contract_id` BIGINT UNSIGNED NOT NULL                                                   COMMENT '合同ID',
    `merchant_id` BIGINT UNSIGNED DEFAULT NULL                                               COMMENT '商家ID',
    `project_id`  BIGINT UNSIGNED DEFAULT NULL                                               COMMENT '项目ID',
    `fee_item_id` BIGINT UNSIGNED DEFAULT NULL                                               COMMENT '费项ID（可按费项分别记余额，为空时通用）',
    `balance`     DECIMAL(14,2)   DEFAULT '0.00'                                            COMMENT '当前余额',
    `version`     INT UNSIGNED    DEFAULT '1'                                                COMMENT '乐观锁版本号',
    `created_by`  VARCHAR(50)     NOT NULL DEFAULT 'system'                                  COMMENT '创建人',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                         COMMENT '创建时间',
    `updated_by`  VARCHAR(50)     NOT NULL DEFAULT 'system'                                  COMMENT '更新人',
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT         NOT NULL DEFAULT '0'                                       COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contract_fee_version_deleted` (`contract_id`, `fee_item_id`, `version`, `is_deleted`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_project_id`  (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预收款账户表';

-- 预收款流水表
CREATE TABLE `fin_prepay_transaction` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT                                   COMMENT '主键ID',
    `account_id`   BIGINT UNSIGNED NOT NULL                                                  COMMENT '预收款账户ID',
    `trans_type`   TINYINT         NOT NULL                                                  COMMENT '类型：1转入（超额转预存）/2抵冲应收/3退款',
    `amount`       DECIMAL(14,2)   NOT NULL DEFAULT '0.00'                                  COMMENT '金额',
    `balance_after` DECIMAL(14,2)  DEFAULT '0.00'                                           COMMENT '交易后余额',
    `trans_date`   DATE            DEFAULT NULL                                              COMMENT '交易日期',
    `source_code`  VARCHAR(50)     DEFAULT NULL                                              COMMENT '关联单据号',
    `remark`       VARCHAR(500)    DEFAULT NULL                                              COMMENT '备注',
    `created_by`   VARCHAR(50)     NOT NULL DEFAULT 'system'                                 COMMENT '创建人',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP                        COMMENT '创建时间',
    `updated_by`   VARCHAR(50)     NOT NULL DEFAULT 'system'                                 COMMENT '更新人',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT         NOT NULL DEFAULT '0'                                      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_account_id`  (`account_id`),
    KEY `idx_trans_type`  (`trans_type`),
    KEY `idx_trans_date`  (`trans_date`),
    KEY `idx_source_code` (`source_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预收款流水表';

-- ==========================================
-- 验证：显示已建表
-- ==========================================
SELECT table_name, table_comment
FROM information_schema.tables
WHERE table_schema = 'asset_db'
  AND table_name LIKE 'fin_%'
ORDER BY table_name;
