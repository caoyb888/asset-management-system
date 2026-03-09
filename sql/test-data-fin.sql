-- =============================================================================
-- 财务管理模块 — 开发环境永久测试数据
-- =============================================================================
-- 文件：sql/test-data-fin.sql
-- 用途：为 asset-finance 开发联调和手工测试提供固定的基础数据，保留在开发环境，
--        不随 @Transactional 测试用例回滚（财务单元测试使用 H2 内存库，与此无关）
-- 约定：所有测试数据 ID 固定在 93001 ~ 93099 区间，与业务自增 ID 隔离
-- 前提：需先执行（顺序不可调换）：
--        1. sql/test-data-dev.sql   （基础数据 90xxx：project/merchant/brand/shop）
--        2. sql/test-data-inv.sql   （招商数据 91xxx：lease_contract/fee_item）
--        3. sql/test-data-opr.sql   （营运数据 92xxx：ledger/receivable_plan）
-- 执行：脚本幂等，可重复执行（ON DUPLICATE KEY UPDATE / INSERT IGNORE）
-- 更新：2026-03-09
-- =============================================================================
--
-- 数据依赖关系：
--
--   test-data-dev.sql (90xxx)
--   ├── biz_project   = 90001
--   ├── biz_shop      = 90001（在租）
--   ├── biz_merchant  = 90001（品牌商）/ 90002（租户商）
--   └── biz_brand     = 90001
--       │
--       └── test-data-inv.sql (91xxx)
--           ├── inv_lease_contract = 91003（生效合同，2025-01-01 ~ 2026-12-31）
--           └── cfg_fee_item       = 91001（固定租金）/ 91002（物业管理费）
--               │
--               └── test-data-opr.sql (92xxx)
--                   └── opr_contract_ledger = 92003（已推送应收）
--                       │
--                       └── test-data-fin.sql (93xxx) ← 本文件
--
-- 覆盖场景（13 张 fin_* 表）：
--   fin_receivable          93001~93008  多状态应收：已收清/部分收款/待收/逾期/减免/调整
--   fin_receipt             93001~93004  多状态收款单：已核销/部分核销/待核销
--   fin_receipt_detail      93001~93007  收款拆分明细
--   fin_write_off           93001~93003  核销单：已通过/待审核/已驳回
--   fin_write_off_detail    93001~93004  核销明细行
--   fin_voucher             93001~93003  凭证：已上传/已审核/草稿
--   fin_voucher_entry       93001~93006  凭证分录（借贷各3对）
--   fin_deposit_account     93001        保证金账户（余额 4000）
--   fin_deposit_transaction 93001~93003  保证金流水：收入/退款/冲抵(待审)
--   fin_prepay_account      93001        预收款账户（余额 500）
--   fin_prepay_transaction  93001~93002  预收款流水：超额转入/抵冲
--   fin_receivable_deduction 93001       减免单（已通过）
--   fin_receivable_adjustment 93001      调整单（已通过）
--
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 1. 应收管理（fin_receivable） 93001 ~ 93008
-- =============================================================================
-- 93001：2026-01 固定租金，已收清（供核销、凭证关联测试）
-- 93002：2026-02 固定租金，部分收款（8000/15000）
-- 93003：2026-03 固定租金，待收（供当前月应收、核销测试）
-- 93004：2026-04 固定租金，已逾期未收（供欠费统计、看板测试）
-- 93005：2026-01 物业管理费，已收清
-- 93006：2026-02 物业管理费，待收
-- 93007：2025-12 固定租金，含减免（deduction=2000）已收清
-- 93008：2026-03 固定租金，含调增（adjust=1000），待收
-- -----------------------------------------------------------------------------

INSERT INTO fin_receivable
    (id, receivable_code, contract_id, ledger_id, project_id, merchant_id, shop_id,
     fee_item_id, fee_name, billing_start, billing_end, accrual_month, due_date,
     original_amount, adjust_amount, deduction_amount, actual_amount,
     received_amount, outstanding_amount,
     status, is_printed, is_invoiced, version,
     is_deleted, created_by, updated_by)
VALUES
-- 93001：2026-01 租金，已收清
(93001, 'RCV-2026-0101', 91003, 92003, 90001, 90002, 90001,
 91001, '固定租金', '2026-01-01', '2026-01-31', '2026-01', '2026-01-10',
 15000.00, 0.00, 0.00, 15000.00,
 15000.00, 0.00,
 2, 1, 0, 1,
 0, 1, 1),

-- 93002：2026-02 租金，部分收款
(93002, 'RCV-2026-0201', 91003, 92003, 90001, 90002, 90001,
 91001, '固定租金', '2026-02-01', '2026-02-28', '2026-02', '2026-02-10',
 15000.00, 0.00, 0.00, 15000.00,
 8000.00, 7000.00,
 1, 0, 0, 1,
 0, 1, 1),

-- 93003：2026-03 租金，待收（当月）
(93003, 'RCV-2026-0301', 91003, 92003, 90001, 90002, 90001,
 91001, '固定租金', '2026-03-01', '2026-03-31', '2026-03', '2026-03-10',
 15000.00, 0.00, 0.00, 15000.00,
 0.00, 15000.00,
 0, 0, 0, 1,
 0, 1, 1),

-- 93004：2026-04 租金，逾期未收（due_date 早于今天）
(93004, 'RCV-2026-0401', 91003, 92003, 90001, 90002, 90001,
 91001, '固定租金', '2025-12-01', '2025-12-31', '2025-12', '2025-12-10',
 15000.00, 0.00, 0.00, 15000.00,
 0.00, 15000.00,
 0, 0, 0, 1,
 0, 1, 1),

-- 93005：2026-01 物业费，已收清
(93005, 'RCV-2026-0102', 91003, 92003, 90001, 90002, 90001,
 91002, '物业管理费', '2026-01-01', '2026-01-31', '2026-01', '2026-01-10',
 3000.00, 0.00, 0.00, 3000.00,
 3000.00, 0.00,
 2, 1, 0, 1,
 0, 1, 1),

-- 93006：2026-02 物业费，待收
(93006, 'RCV-2026-0202', 91003, 92003, 90001, 90002, 90001,
 91002, '物业管理费', '2026-02-01', '2026-02-28', '2026-02', '2026-02-10',
 3000.00, 0.00, 0.00, 3000.00,
 0.00, 3000.00,
 0, 0, 0, 1,
 0, 1, 1),

-- 93007：2025-12 租金，含减免2000，已收清（actual=13000）
(93007, 'RCV-2025-1201', 91003, 92003, 90001, 90002, 90001,
 91001, '固定租金', '2025-12-01', '2025-12-31', '2025-12', '2025-12-10',
 15000.00, 0.00, 2000.00, 13000.00,
 13000.00, 0.00,
 2, 1, 0, 2,
 0, 1, 1),

-- 93008：2026-03 租金，含调增1000，待收（actual=16000）
(93008, 'RCV-2026-0302', 91003, 92003, 90001, 90002, 90001,
 91001, '固定租金', '2026-03-01', '2026-03-31', '2026-03', '2026-03-10',
 15000.00, 1000.00, 0.00, 16000.00,
 0.00, 16000.00,
 0, 0, 0, 2,
 0, 1, 1)

ON DUPLICATE KEY UPDATE
    received_amount   = VALUES(received_amount),
    outstanding_amount= VALUES(outstanding_amount),
    status            = VALUES(status),
    created_by        = VALUES(created_by),
    updated_by        = VALUES(updated_by);


-- =============================================================================
-- 2. 减免单（fin_receivable_deduction） 93001
-- =============================================================================
-- 93001：对应 93007 应收，减免 2000，已通过审批

INSERT INTO fin_receivable_deduction
    (id, deduction_code, receivable_id, contract_id, deduction_amount, reason,
     status, approval_id, version,
     is_deleted, created_by, updated_by)
VALUES
(93001, 'DED-20251210-001', 93007, 91003, 2000.00, '商家装修期优惠减免',
 1, 'OA-DED-93001', 1,
 0, 1, 1)

ON DUPLICATE KEY UPDATE
    status     = VALUES(status),
    updated_by = VALUES(updated_by);


-- =============================================================================
-- 3. 调整单（fin_receivable_adjustment） 93001
-- =============================================================================
-- 93001：对应 93008 应收，调增 1000，已通过审批

INSERT INTO fin_receivable_adjustment
    (id, adjustment_code, receivable_id, contract_id, adjust_type, adjust_amount, reason,
     status, approval_id, version,
     is_deleted, created_by, updated_by)
VALUES
(93001, 'ADJ-20260301-001', 93008, 91003, 1, 1000.00, '面积测量差异补差',
 1, 'OA-ADJ-93001', 1,
 0, 1, 1)

ON DUPLICATE KEY UPDATE
    status     = VALUES(status),
    updated_by = VALUES(updated_by);


-- =============================================================================
-- 4. 收款单（fin_receipt） 93001 ~ 93004
-- =============================================================================
-- 93001：2026-01-10，18000（租金15000+物业3000），已全部核销（关联93001、93005应收）
-- 93002：2026-02-10，8000，部分核销（关联93002应收，核销8000/15000）
-- 93003：2026-03-05，20000，待核销（供核销提交测试）
-- 93004：未名款，待认领（供未名款绑定测试）
-- -----------------------------------------------------------------------------

INSERT INTO fin_receipt
    (id, receipt_code, contract_id, project_id, merchant_id, brand_id, shop_code,
     total_amount, payment_method, bank_serial_no, payer_name, bank_name, bank_account,
     is_unnamed, accounting_entity, receipt_date, receiver,
     status, write_off_amount, prepay_amount, version,
     is_deleted, created_by, updated_by)
VALUES
-- 93001：已全部核销（18000）
(93001, 'RC-20260110-001', 91003, 90001, 90002, 90001, 'A101',
 18000.00, 1, 'BANK-20260110-88001', '测试商家有限公司', '工商银行杭州支行', '6222000000000001',
 0, '杭州产城资产管理有限公司', '2026-01-10', 'admin',
 2, 18000.00, 0.00, 1,
 0, 1, 1),

-- 93002：部分核销（8000）
(93002, 'RC-20260210-001', 91003, 90001, 90002, 90001, 'A101',
 8000.00, 1, 'BANK-20260210-88002', '测试商家有限公司', '工商银行杭州支行', '6222000000000001',
 0, '杭州产城资产管理有限公司', '2026-02-10', 'admin',
 1, 8000.00, 0.00, 1,
 0, 1, 1),

-- 93003：待核销（20000）
(93003, 'RC-20260305-001', 91003, 90001, 90002, 90001, 'A101',
 20000.00, 1, 'BANK-20260305-88003', '测试商家有限公司', '工商银行杭州支行', '6222000000000001',
 0, '杭州产城资产管理有限公司', '2026-03-05', 'admin',
 0, 0.00, 0.00, 1,
 0, 1, 1),

-- 93004：未名款，待认领
(93004, 'RC-20260301-002', 0, 90001, 0, 0, NULL,
 5000.00, 2, NULL, '未知付款方', NULL, NULL,
 1, '杭州产城资产管理有限公司', '2026-03-01', 'admin',
 0, 0.00, 0.00, 1,
 0, 1, 1)

ON DUPLICATE KEY UPDATE
    status          = VALUES(status),
    write_off_amount= VALUES(write_off_amount),
    updated_by      = VALUES(updated_by);


-- =============================================================================
-- 5. 收款拆分明细（fin_receipt_detail） 93001 ~ 93007
-- =============================================================================

INSERT INTO fin_receipt_detail
    (id, receipt_id, fee_item_id, fee_name, amount, remark,
     is_deleted, created_by, updated_by)
VALUES
-- 93001 收款单的拆分：租金15000+物业3000
(93001, 93001, 91001, '固定租金',   15000.00, '2026年1月租金',   0, 1, 1),
(93002, 93001, 91002, '物业管理费',  3000.00, '2026年1月物业费',  0, 1, 1),
-- 93002 收款单的拆分：租金8000（部分）
(93003, 93002, 91001, '固定租金',    8000.00, '2026年2月租金预收', 0, 1, 1),
-- 93003 收款单的拆分：租金15000+物业3000+多余2000
(93004, 93003, 91001, '固定租金',   15000.00, '2026年3月租金',   0, 1, 1),
(93005, 93003, 91002, '物业管理费',  3000.00, '2026年3月物业费',  0, 1, 1),
(93006, 93003, 91001, '预收押金',    2000.00, '超额预存',        0, 1, 1),
-- 93004 未名款（单笔，无费项）
(93007, 93004, NULL,  '待认领款项',  5000.00, '来源不明，待认领',  0, 1, 1)

ON DUPLICATE KEY UPDATE
    amount     = VALUES(amount),
    updated_by = VALUES(updated_by);


-- =============================================================================
-- 6. 核销单（fin_write_off） 93001 ~ 93003
-- =============================================================================
-- 93001：已审核通过，核销 93001 收款单 → 93001(15000)+93005(3000)应收
-- 93002：待审核，核销 93002 收款单 → 93002 应收 8000（部分）
-- 93003：已驳回，核销 93003 收款单 → 93003 应收（测试驳回场景）
-- -----------------------------------------------------------------------------

INSERT INTO fin_write_off
    (id, write_off_code, receipt_id, contract_id, merchant_id, project_id,
     write_off_type, total_amount, status, upload_status, upload_time,
     approval_id, version,
     is_deleted, created_by, updated_by)
VALUES
-- 93001：已通过
(93001, 'WO-20260110-001', 93001, 91003, 90002, 90001,
 1, 18000.00, 1, 0, NULL,
 'OA-WO-93001', 1,
 0, 1, 1),

-- 93002：待审核
(93002, 'WO-20260210-001', 93002, 91003, 90002, 90001,
 1, 8000.00, 0, 0, NULL,
 'OA-WO-93002', 1,
 0, 1, 1),

-- 93003：已驳回
(93003, 'WO-20260305-001', 93003, 91003, 90002, 90001,
 1, 18000.00, 2, 0, NULL,
 'OA-WO-93003', 1,
 0, 1, 1)

ON DUPLICATE KEY UPDATE
    status     = VALUES(status),
    updated_by = VALUES(updated_by);


-- =============================================================================
-- 7. 核销明细（fin_write_off_detail） 93001 ~ 93004
-- =============================================================================

INSERT INTO fin_write_off_detail
    (id, write_off_id, receivable_id, fee_item_id, accrual_month,
     write_off_amount, overpay_amount,
     is_deleted, created_by, updated_by)
VALUES
-- 核销单 93001 → 应收 93001 租金15000 + 应收 93005 物业3000
(93001, 93001, 93001, 91001, '2026-01', 15000.00, 0.00, 0, 1, 1),
(93002, 93001, 93005, 91002, '2026-01',  3000.00, 0.00, 0, 1, 1),
-- 核销单 93002 → 应收 93002 租金8000（部分）
(93003, 93002, 93002, 91001, '2026-02',  8000.00, 0.00, 0, 1, 1),
-- 核销单 93003（驳回）→ 应收 93003（驳回后未生效）
(93004, 93003, 93003, 91001, '2026-03', 15000.00, 0.00, 0, 1, 1)

ON DUPLICATE KEY UPDATE
    write_off_amount = VALUES(write_off_amount),
    updated_by       = VALUES(updated_by);


-- =============================================================================
-- 8. 凭证（fin_voucher） 93001 ~ 93003
-- =============================================================================
-- 93001：已上传（status=2），借贷各18000（关联核销单93001）
-- 93002：已审核待上传（status=1）
-- 93003：草稿（status=0），供删除/审核测试
-- -----------------------------------------------------------------------------

INSERT INTO fin_voucher
    (id, voucher_code, project_id, account_set, pay_type, voucher_date,
     total_debit, total_credit, status, upload_time, remark, version,
     is_deleted, created_by, updated_by)
VALUES
-- 93001：已上传
(93001, 'VC-20260110-001', 90001, '产城资产账套', 1, '2026-01-10',
 18000.00, 18000.00, 2, '2026-01-15 10:00:00', '2026年1月租金及物业费收款凭证', 1,
 0, 1, 1),

-- 93002：已审核
(93002, 'VC-20260210-001', 90001, '产城资产账套', 1, '2026-02-10',
 8000.00, 8000.00, 1, NULL, '2026年2月租金部分收款凭证', 1,
 0, 1, 1),

-- 93003：草稿
(93003, 'VC-20260305-001', 90001, '产城资产账套', 1, '2026-03-05',
 20000.00, 20000.00, 0, NULL, '2026年3月待审核凭证', 1,
 0, 1, 1)

ON DUPLICATE KEY UPDATE
    status      = VALUES(status),
    upload_time = VALUES(upload_time),
    updated_by  = VALUES(updated_by);


-- =============================================================================
-- 9. 凭证分录（fin_voucher_entry） 93001 ~ 93006
-- =============================================================================
-- 每张凭证两笔分录：DR 银行存款 / CR 应收账款（借贷平衡）

INSERT INTO fin_voucher_entry
    (id, voucher_id, source_type, source_id, account_code, account_name,
     debit_amount, credit_amount, summary,
     is_deleted, created_by, updated_by)
VALUES
-- 凭证 93001 分录（已上传）
(93001, 93001, 1, 93001, '1002', '银行存款',  18000.00,     0.00, '收到2026年1月租金及物业费', 0, 1, 1),
(93002, 93001, 1, 93001, '1122', '应收账款',      0.00, 18000.00, '核销2026年1月应收账款',   0, 1, 1),

-- 凭证 93002 分录（已审核）
(93003, 93002, 1, 93002, '1002', '银行存款',   8000.00,     0.00, '收到2026年2月租金（部分）', 0, 1, 1),
(93004, 93002, 1, 93002, '1122', '应收账款',      0.00,  8000.00, '核销2026年2月部分应收',   0, 1, 1),

-- 凭证 93003 分录（草稿）
(93005, 93003, 1, 93003, '1002', '银行存款',  20000.00,     0.00, '收到2026年3月款项',      0, 1, 1),
(93006, 93003, 1, 93003, '1122', '应收账款',      0.00, 20000.00, '核销2026年3月应收账款',   0, 1, 1)

ON DUPLICATE KEY UPDATE
    debit_amount  = VALUES(debit_amount),
    credit_amount = VALUES(credit_amount),
    updated_by    = VALUES(updated_by);


-- =============================================================================
-- 10. 保证金账户（fin_deposit_account） 93001
-- =============================================================================
-- 合同91003：total_in=6000, total_refund=1000, total_offset=1000, balance=4000

INSERT INTO fin_deposit_account
    (id, contract_id, merchant_id, project_id, fee_item_id,
     balance, total_in, total_offset, total_refund, total_forfeit,
     version, is_deleted, created_by, updated_by)
VALUES
(93001, 91003, 90002, 90001, 91002,
 4000.00, 6000.00, 1000.00, 1000.00, 0.00,
 2, 0, 1, 1)

ON DUPLICATE KEY UPDATE
    balance      = VALUES(balance),
    total_in     = VALUES(total_in),
    total_offset = VALUES(total_offset),
    total_refund = VALUES(total_refund),
    version      = VALUES(version),
    updated_by   = VALUES(updated_by);


-- =============================================================================
-- 11. 保证金流水（fin_deposit_transaction） 93001 ~ 93003
-- =============================================================================
-- 93001：收入 6000（已审核通过）
-- 93002：退款 1000（已审核通过）
-- 93003：冲抵应收 1000（已审核通过）
-- -----------------------------------------------------------------------------

INSERT INTO fin_deposit_transaction
    (id, account_id,
     trans_type, amount, balance_after, trans_date, source_code,
     reason, status, approval_id,
     is_deleted, created_by, updated_by)
VALUES
-- 93001：缴纳6000（已通过），balance_after=6000
(93001, 93001,
 1, 6000.00, 6000.00, '2025-01-10', 'RC-20250110-001',
 '合同签订缴纳保证金', 1, 'OA-DEP-93001',
 0, 1, 1),

-- 93002：退款1000（已通过），balance_after=5000
(93002, 93001,
 3, 1000.00, 5000.00, '2025-06-01', NULL,
 '部分退还保证金', 1, 'OA-DEP-93002',
 0, 1, 1),

-- 93003：冲抵应收93004，1000（已通过），balance_after=4000
(93003, 93001,
 2, 1000.00, 4000.00, '2026-01-20', NULL,
 '保证金冲抵逾期应收', 1, 'OA-DEP-93003',
 0, 1, 1)

ON DUPLICATE KEY UPDATE
    status     = VALUES(status),
    updated_by = VALUES(updated_by);


-- =============================================================================
-- 12. 预收款账户（fin_prepay_account） 93001
-- =============================================================================
-- 合同91003：超额核销转入500，余额500

INSERT INTO fin_prepay_account
    (id, contract_id, merchant_id, project_id, fee_item_id,
     balance, version, is_deleted, created_by, updated_by)
VALUES
(93001, 91003, 90002, 90001, NULL,
 500.00, 1, 0, 1, 1)

ON DUPLICATE KEY UPDATE
    balance    = VALUES(balance),
    version    = VALUES(version),
    updated_by = VALUES(updated_by);


-- =============================================================================
-- 13. 预收款流水（fin_prepay_transaction） 93001 ~ 93002
-- =============================================================================
-- 93001：超额核销自动转入 500（来源：核销单93001，超付500）
-- 93002：抵冲应收 93003，200（抵冲后余额300）

INSERT INTO fin_prepay_transaction
    (id, account_id,
     trans_type, amount, balance_after, trans_date, source_code,
     remark, is_deleted, created_by, updated_by)
VALUES
-- 93001：超额转入500，balance_after=500
(93001, 93001,
 1, 500.00, 500.00, '2026-01-10', 'WO-20260110-001',
 '核销超额自动转入预收款',
 0, 1, 1),

-- 93002：抵冲200，balance_after=300（演示流水，不影响应收已收金额）
(93002, 93001,
 2, 200.00, 300.00, '2026-03-05', NULL,
 '预收款抵冲2026-03应收（演示）',
 0, 1, 1)

ON DUPLICATE KEY UPDATE
    amount     = VALUES(amount),
    updated_by = VALUES(updated_by);


SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 验证查询（执行后可用以下 SQL 确认数据就绪）
-- =============================================================================
-- SELECT COUNT(*) AS fin_receivable_count     FROM fin_receivable          WHERE id BETWEEN 93001 AND 93099;
-- SELECT COUNT(*) AS fin_receipt_count        FROM fin_receipt             WHERE id BETWEEN 93001 AND 93099;
-- SELECT COUNT(*) AS fin_write_off_count      FROM fin_write_off           WHERE id BETWEEN 93001 AND 93099;
-- SELECT COUNT(*) AS fin_voucher_count        FROM fin_voucher             WHERE id BETWEEN 93001 AND 93099;
-- SELECT COUNT(*) AS fin_deposit_count        FROM fin_deposit_account     WHERE id BETWEEN 93001 AND 93099;
-- SELECT COUNT(*) AS fin_prepay_count         FROM fin_prepay_account      WHERE id BETWEEN 93001 AND 93099;
-- SELECT COUNT(*) AS fin_deduction_count      FROM fin_receivable_deduction WHERE id BETWEEN 93001 AND 93099;
-- SELECT COUNT(*) AS fin_adjustment_count     FROM fin_receivable_adjustment WHERE id BETWEEN 93001 AND 93099;
-- -- 预期：8 / 4 / 3 / 3 / 1 / 1 / 1 / 1
-- =============================================================================
