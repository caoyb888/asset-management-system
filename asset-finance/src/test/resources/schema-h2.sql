-- H2 内存库 - 财务模块测试用表（MySQL 兼容模式）
-- 对应 sql/finance_v2_init.sql，去掉 MySQL 特有语法

DROP TABLE IF EXISTS fin_prepay_transaction;
DROP TABLE IF EXISTS fin_prepay_account;
DROP TABLE IF EXISTS fin_deposit_transaction;
DROP TABLE IF EXISTS fin_deposit_account;
DROP TABLE IF EXISTS fin_voucher_entry;
DROP TABLE IF EXISTS fin_voucher;
DROP TABLE IF EXISTS fin_receivable_adjustment;
DROP TABLE IF EXISTS fin_receivable_deduction;
DROP TABLE IF EXISTS fin_write_off_detail;
DROP TABLE IF EXISTS fin_write_off;
DROP TABLE IF EXISTS fin_receivable;
DROP TABLE IF EXISTS fin_receipt_detail;
DROP TABLE IF EXISTS fin_receipt;

CREATE TABLE fin_receipt (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    receipt_code      VARCHAR(50)     NOT NULL,
    contract_id       BIGINT          NOT NULL DEFAULT 0,
    project_id        BIGINT          DEFAULT NULL,
    merchant_id       BIGINT          DEFAULT NULL,
    brand_id          BIGINT          DEFAULT NULL,
    shop_code         VARCHAR(50)     DEFAULT NULL,
    total_amount      DECIMAL(14,2)   NOT NULL DEFAULT 0.00,
    payment_method    TINYINT         DEFAULT 1,
    bank_serial_no    VARCHAR(100)    DEFAULT NULL,
    payer_name        VARCHAR(200)    DEFAULT NULL,
    bank_name         VARCHAR(200)    DEFAULT NULL,
    bank_account      VARCHAR(50)     DEFAULT NULL,
    is_unnamed        TINYINT         DEFAULT 0,
    accounting_entity VARCHAR(200)    DEFAULT NULL,
    receipt_date      DATE            NOT NULL DEFAULT CURRENT_DATE,
    receiver          VARCHAR(50)     DEFAULT NULL,
    status            TINYINT         DEFAULT 0,
    write_off_amount  DECIMAL(14,2)   DEFAULT 0.00,
    prepay_amount     DECIMAL(14,2)   DEFAULT 0.00,
    version           INT             DEFAULT 1,
    created_by        BIGINT          DEFAULT NULL,
    created_at        DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by        BIGINT          DEFAULT NULL,
    updated_at        DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted        TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE fin_receipt_detail (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    receipt_id  BIGINT          NOT NULL,
    fee_item_id BIGINT          DEFAULT NULL,
    fee_name    VARCHAR(100)    DEFAULT NULL,
    amount      DECIMAL(14,2)   NOT NULL DEFAULT 0.00,
    remark      VARCHAR(500)    DEFAULT NULL,
    created_by  BIGINT          DEFAULT NULL,
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by  BIGINT          DEFAULT NULL,
    updated_at  DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted  TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE fin_receivable (
    id                 BIGINT          NOT NULL AUTO_INCREMENT,
    receivable_code    VARCHAR(50)     NOT NULL,
    contract_id        BIGINT          NOT NULL DEFAULT 0,
    ledger_id          BIGINT          DEFAULT NULL,
    project_id         BIGINT          DEFAULT NULL,
    merchant_id        BIGINT          DEFAULT NULL,
    shop_id            BIGINT          DEFAULT NULL,
    fee_item_id        BIGINT          DEFAULT NULL,
    fee_name           VARCHAR(100)    DEFAULT NULL,
    billing_start      DATE            DEFAULT NULL,
    billing_end        DATE            DEFAULT NULL,
    accrual_month      VARCHAR(7)      DEFAULT NULL,
    due_date           DATE            DEFAULT NULL,
    original_amount    DECIMAL(14,2)   NOT NULL DEFAULT 0.00,
    adjust_amount      DECIMAL(14,2)   DEFAULT 0.00,
    deduction_amount   DECIMAL(14,2)   DEFAULT 0.00,
    actual_amount      DECIMAL(14,2)   DEFAULT 0.00,
    received_amount    DECIMAL(14,2)   DEFAULT 0.00,
    outstanding_amount DECIMAL(14,2)   DEFAULT 0.00,
    status             TINYINT         DEFAULT 0,
    is_printed         TINYINT         DEFAULT 0,
    is_invoiced        TINYINT         DEFAULT 0,
    version            INT             DEFAULT 1,
    created_by         BIGINT          DEFAULT NULL,
    created_at         DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by         BIGINT          DEFAULT NULL,
    updated_at         DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted         TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE fin_receivable_deduction (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    deduction_code   VARCHAR(50)     NOT NULL,
    receivable_id    BIGINT          NOT NULL,
    contract_id      BIGINT          DEFAULT NULL,
    deduction_amount DECIMAL(14,2)   NOT NULL DEFAULT 0.00,
    reason           VARCHAR(1000)   DEFAULT NULL,
    status           TINYINT         DEFAULT 0,
    approval_id      VARCHAR(100)    DEFAULT NULL,
    version          INT             DEFAULT 1,
    created_by       BIGINT          DEFAULT NULL,
    created_at       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by       BIGINT          DEFAULT NULL,
    updated_at       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE fin_receivable_adjustment (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    adjustment_code  VARCHAR(50)     NOT NULL,
    receivable_id    BIGINT          NOT NULL,
    contract_id      BIGINT          DEFAULT NULL,
    adjust_type      TINYINT         DEFAULT 1,
    adjust_amount    DECIMAL(14,2)   NOT NULL DEFAULT 0.00,
    reason           VARCHAR(1000)   DEFAULT NULL,
    status           TINYINT         DEFAULT 0,
    approval_id      VARCHAR(100)    DEFAULT NULL,
    version          INT             DEFAULT 1,
    created_by       BIGINT          DEFAULT NULL,
    created_at       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by       BIGINT          DEFAULT NULL,
    updated_at       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE fin_write_off (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    write_off_code VARCHAR(50)     NOT NULL,
    receipt_id     BIGINT          DEFAULT NULL,
    contract_id    BIGINT          DEFAULT NULL,
    merchant_id    BIGINT          DEFAULT NULL,
    project_id     BIGINT          DEFAULT NULL,
    write_off_type TINYINT         DEFAULT 1,
    total_amount   DECIMAL(14,2)   NOT NULL DEFAULT 0.00,
    status         TINYINT         DEFAULT 0,
    upload_status  TINYINT         DEFAULT 0,
    upload_time    DATETIME        DEFAULT NULL,
    approval_id    VARCHAR(100)    DEFAULT NULL,
    version        INT             DEFAULT 1,
    created_by     BIGINT          DEFAULT NULL,
    created_at     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by     BIGINT          DEFAULT NULL,
    updated_at     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE fin_write_off_detail (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    write_off_id     BIGINT          NOT NULL,
    receivable_id    BIGINT          NOT NULL,
    fee_item_id      BIGINT          DEFAULT NULL,
    accrual_month    VARCHAR(7)      DEFAULT NULL,
    write_off_amount DECIMAL(14,2)   NOT NULL DEFAULT 0.00,
    overpay_amount   DECIMAL(14,2)   DEFAULT 0.00,
    created_by       BIGINT          DEFAULT NULL,
    created_at       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by       BIGINT          DEFAULT NULL,
    updated_at       DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE fin_voucher (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    voucher_code VARCHAR(50)     NOT NULL,
    project_id   BIGINT          DEFAULT NULL,
    account_set  VARCHAR(50)     DEFAULT NULL,
    pay_type     TINYINT         DEFAULT 1,
    voucher_date DATE            DEFAULT NULL,
    total_debit  DECIMAL(14,2)   DEFAULT 0.00,
    total_credit DECIMAL(14,2)   DEFAULT 0.00,
    status       TINYINT         DEFAULT 0,
    upload_time  DATETIME        DEFAULT NULL,
    remark       VARCHAR(500)    DEFAULT NULL,
    version      INT             DEFAULT 1,
    created_by   BIGINT          DEFAULT NULL,
    created_at   DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by   BIGINT          DEFAULT NULL,
    updated_at   DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted   TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE fin_voucher_entry (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    voucher_id     BIGINT          NOT NULL,
    source_type    TINYINT         DEFAULT NULL,
    source_id      BIGINT          DEFAULT NULL,
    account_code   VARCHAR(50)     DEFAULT NULL,
    account_name   VARCHAR(200)    DEFAULT NULL,
    debit_amount   DECIMAL(14,2)   DEFAULT 0.00,
    credit_amount  DECIMAL(14,2)   DEFAULT 0.00,
    summary        VARCHAR(500)    DEFAULT NULL,
    created_by     BIGINT          DEFAULT NULL,
    created_at     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by     BIGINT          DEFAULT NULL,
    updated_at     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted     TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE fin_deposit_account (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    contract_id   BIGINT          NOT NULL,
    merchant_id   BIGINT          DEFAULT NULL,
    project_id    BIGINT          DEFAULT NULL,
    fee_item_id   BIGINT          DEFAULT NULL,
    balance       DECIMAL(14,2)   DEFAULT 0.00,
    total_in      DECIMAL(14,2)   DEFAULT 0.00,
    total_offset  DECIMAL(14,2)   DEFAULT 0.00,
    total_refund  DECIMAL(14,2)   DEFAULT 0.00,
    total_forfeit DECIMAL(14,2)   DEFAULT 0.00,
    version       INT             DEFAULT 1,
    created_by    BIGINT          DEFAULT NULL,
    created_at    DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by    BIGINT          DEFAULT NULL,
    updated_at    DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE fin_deposit_transaction (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    account_id    BIGINT          NOT NULL,
    trans_type    TINYINT         NOT NULL,
    amount        DECIMAL(14,2)   NOT NULL DEFAULT 0.00,
    balance_after DECIMAL(14,2)   DEFAULT 0.00,
    trans_date    DATE            DEFAULT NULL,
    source_code   VARCHAR(50)     DEFAULT NULL,
    reason        VARCHAR(500)    DEFAULT NULL,
    status        TINYINT         DEFAULT 0,
    approval_id   VARCHAR(100)    DEFAULT NULL,
    created_by    BIGINT          DEFAULT NULL,
    created_at    DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by    BIGINT          DEFAULT NULL,
    updated_at    DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE fin_prepay_account (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    contract_id BIGINT          NOT NULL,
    merchant_id BIGINT          DEFAULT NULL,
    project_id  BIGINT          DEFAULT NULL,
    fee_item_id BIGINT          DEFAULT NULL,
    balance     DECIMAL(14,2)   DEFAULT 0.00,
    version     INT             DEFAULT 1,
    created_by  BIGINT          DEFAULT NULL,
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by  BIGINT          DEFAULT NULL,
    updated_at  DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted  TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE fin_prepay_transaction (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    account_id    BIGINT          NOT NULL,
    trans_type    TINYINT         NOT NULL,
    amount        DECIMAL(14,2)   NOT NULL DEFAULT 0.00,
    balance_after DECIMAL(14,2)   DEFAULT 0.00,
    trans_date    DATE            DEFAULT NULL,
    source_code   VARCHAR(50)     DEFAULT NULL,
    remark        VARCHAR(500)    DEFAULT NULL,
    created_by    BIGINT          DEFAULT NULL,
    created_at    DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_by    BIGINT          DEFAULT NULL,
    updated_at    DATETIME        DEFAULT CURRENT_TIMESTAMP,
    is_deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
