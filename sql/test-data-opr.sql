-- =============================================================================
-- 营运管理模块 — 开发环境永久测试数据
-- =============================================================================
-- 文件：sql/test-data-opr.sql
-- 用途：为 asset-operation 单元/集成测试提供固定的基础数据，保留在开发环境，
--        不随 @Transactional 测试用例回滚
-- 约定：所有测试数据 ID 固定在 92001 ~ 92099 区间，与业务自增 ID 隔离
-- 前提：需先执行 sql/test-data-dev.sql（基础数据 90xxx）
--        再执行 sql/test-data-inv.sql（招商数据 91xxx）
-- 执行：脚本幂等，可重复执行（ON DUPLICATE KEY UPDATE）
-- 更新：2026-03-06
-- =============================================================================
--
-- 数据依赖关系：
--
--   test-data-dev.sql (90xxx)
--   ├── biz_project=90001, biz_shop=90001(已租), biz_merchant=90001/90002, biz_brand=90001
--   │
--   └── test-data-inv.sql (91xxx)
--       ├── inv_lease_contract=91003(生效), cfg_fee_item=91001
--       ├── inv_lease_contract_shop=91002(contract91003→shop90001)
--       ├── inv_lease_contract_fee=91002(固定租金)/91003(提成)
--       ├── inv_lease_contract_fee_stage=91001(提成率10%/保底8000)
--       ├── inv_lease_contract_billing=91001~91012(12个月×15000)
--       │
--       └── test-data-opr.sql (92xxx) ← 本文件
--
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 1. 合同台账（opr_contract_ledger）
-- =============================================================================
-- 92001：进行中，待双签+待生成应收 → 双签确认、生成应收测试
-- 92002：进行中，已双签+已生成应收+待审核 → 审核、推送测试
-- 92003：进行中，已推送+已审核 → 变更/解约关联用
-- 92004：已解约 → 状态查询测试
-- -----------------------------------------------------------------------------

INSERT INTO opr_contract_ledger
    (id, ledger_code, contract_id, project_id, merchant_id, brand_id,
     contract_type, contract_start, contract_end,
     double_sign_status, double_sign_date,
     receivable_status, audit_status, status,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92001, 'TZ260301000001', 91003, 90001, 90002, 90001,
     1, '2025-01-01', '2026-12-31',
     0, NULL,
     0, 0, 0,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    ledger_code = VALUES(ledger_code), status = VALUES(status),
    double_sign_status = VALUES(double_sign_status),
    receivable_status = VALUES(receivable_status),
    audit_status = VALUES(audit_status), updated_at = NOW();

INSERT INTO opr_contract_ledger
    (id, ledger_code, contract_id, project_id, merchant_id, brand_id,
     contract_type, contract_start, contract_end,
     double_sign_status, double_sign_date,
     receivable_status, audit_status, status,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92002, 'TZ260301000002', 91003, 90001, 90002, 90001,
     1, '2025-01-01', '2026-12-31',
     1, '2026-01-15 10:00:00',
     1, 0, 0,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    ledger_code = VALUES(ledger_code), status = VALUES(status),
    double_sign_status = VALUES(double_sign_status),
    receivable_status = VALUES(receivable_status),
    audit_status = VALUES(audit_status), updated_at = NOW();

INSERT INTO opr_contract_ledger
    (id, ledger_code, contract_id, project_id, merchant_id, brand_id,
     contract_type, contract_start, contract_end,
     double_sign_status, double_sign_date,
     receivable_status, audit_status, status,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92003, 'TZ260301000003', 91003, 90001, 90002, 90001,
     1, '2025-01-01', '2026-12-31',
     1, '2026-01-10 09:00:00',
     2, 1, 0,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    ledger_code = VALUES(ledger_code), status = VALUES(status),
    double_sign_status = VALUES(double_sign_status),
    receivable_status = VALUES(receivable_status),
    audit_status = VALUES(audit_status), updated_at = NOW();

INSERT INTO opr_contract_ledger
    (id, ledger_code, contract_id, project_id, merchant_id, brand_id,
     contract_type, contract_start, contract_end,
     double_sign_status, double_sign_date,
     receivable_status, audit_status, status,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92004, 'TZ260301000004', 91003, 90001, 90002, 90001,
     1, '2025-01-01', '2026-12-31',
     1, '2025-06-01 09:00:00',
     2, 1, 2,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    ledger_code = VALUES(ledger_code), status = VALUES(status),
    double_sign_status = VALUES(double_sign_status),
    receivable_status = VALUES(receivable_status),
    audit_status = VALUES(audit_status), updated_at = NOW();

-- =============================================================================
-- 2. 应收计划（opr_receivable_plan）
-- =============================================================================
-- 92001：台账92002 下，待收+未推送 → 推送测试
-- 92002：台账92002 下，待收+已推送 → 变更红冲测试
-- 92003：台账92003 下，已收+已推送 → 完成状态
-- 92004：台账92003 下，待收，部分收款(3000/10000) → 解约清算测试
-- 92005：台账92003 下，待收，未收款 → 解约清算测试
-- -----------------------------------------------------------------------------

INSERT INTO opr_receivable_plan
    (id, ledger_id, contract_id, shop_id, fee_item_id, fee_name,
     billing_start, billing_end, due_date,
     amount, received_amount, status,
     push_status, push_time, push_idempotent_key,
     source_type, version,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92001, 92002, 91003, 90001, 91001, '测试租金',
     '2026-01-01', '2026-01-31', '2026-01-01',
     15000.00, 0.00, 0,
     0, NULL, NULL,
     1, 1,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount), status = VALUES(status),
    push_status = VALUES(push_status), updated_at = NOW();

INSERT INTO opr_receivable_plan
    (id, ledger_id, contract_id, shop_id, fee_item_id, fee_name,
     billing_start, billing_end, due_date,
     amount, received_amount, status,
     push_status, push_time, push_idempotent_key,
     source_type, version,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92002, 92002, 91003, 90001, 91001, '测试租金',
     '2026-02-01', '2026-02-28', '2026-02-01',
     15000.00, 0.00, 0,
     1, '2026-01-20 14:00:00', 'receivable_92002_1',
     1, 1,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount), status = VALUES(status),
    push_status = VALUES(push_status), updated_at = NOW();

INSERT INTO opr_receivable_plan
    (id, ledger_id, contract_id, shop_id, fee_item_id, fee_name,
     billing_start, billing_end, due_date,
     amount, received_amount, status,
     push_status, push_time, push_idempotent_key,
     source_type, version,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92003, 92003, 91003, 90001, 91001, '测试租金',
     '2026-01-01', '2026-01-31', '2026-01-01',
     15000.00, 15000.00, 2,
     1, '2026-01-05 10:00:00', 'receivable_92003_1',
     1, 1,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount), received_amount = VALUES(received_amount),
    status = VALUES(status), push_status = VALUES(push_status), updated_at = NOW();

INSERT INTO opr_receivable_plan
    (id, ledger_id, contract_id, shop_id, fee_item_id, fee_name,
     billing_start, billing_end, due_date,
     amount, received_amount, status,
     push_status, push_time, push_idempotent_key,
     source_type, version,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92004, 92003, 91003, 90001, 91001, '测试租金',
     '2026-02-01', '2026-02-28', '2026-02-01',
     10000.00, 3000.00, 0,
     1, '2026-01-25 10:00:00', 'receivable_92004_1',
     1, 1,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount), received_amount = VALUES(received_amount),
    status = VALUES(status), push_status = VALUES(push_status), updated_at = NOW();

INSERT INTO opr_receivable_plan
    (id, ledger_id, contract_id, shop_id, fee_item_id, fee_name,
     billing_start, billing_end, due_date,
     amount, received_amount, status,
     push_status, push_time, push_idempotent_key,
     source_type, version,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92005, 92003, 91003, 90001, 91001, '测试租金',
     '2026-03-01', '2026-03-31', '2026-03-01',
     10000.00, 0.00, 0,
     1, '2026-01-25 10:00:00', 'receivable_92005_1',
     1, 1,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount), received_amount = VALUES(received_amount),
    status = VALUES(status), push_status = VALUES(push_status), updated_at = NOW();

-- =============================================================================
-- 3. 合同变更（opr_contract_change）
-- =============================================================================
-- 92001：草稿(0)，租金变更 → 可编辑、可提交审批
-- 92002：审批中(1)，租期变更 → 审批回调测试
-- 92003：已通过(2)，租金变更 → 变更历史查询
-- -----------------------------------------------------------------------------

INSERT INTO opr_contract_change
    (id, change_code, contract_id, ledger_id, project_id,
     status, effective_date, reason, approval_id, impact_summary,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92001, 'BG260301000001', 91003, 92003, 90001,
     0, '2026-04-01', '测试租金调整-草稿', NULL, NULL,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    change_code = VALUES(change_code), status = VALUES(status), updated_at = NOW();

INSERT INTO opr_contract_change
    (id, change_code, contract_id, ledger_id, project_id,
     status, effective_date, reason, approval_id, impact_summary,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92002, 'BG260301000002', 91003, 92003, 90001,
     1, '2026-07-01', '测试租期延长-审批中', 'APPROVAL-92002-1709000000', NULL,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    change_code = VALUES(change_code), status = VALUES(status), updated_at = NOW();

INSERT INTO opr_contract_change
    (id, change_code, contract_id, ledger_id, project_id,
     status, effective_date, reason, approval_id, impact_summary,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92003, 'BG260301000003', 91003, 92003, 90001,
     2, '2026-02-01', '测试租金调整-已通过', 'APPROVAL-92003-1708000000',
     '{"affectedCount":2,"amountDiff":6000}',
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    change_code = VALUES(change_code), status = VALUES(status), updated_at = NOW();

-- =============================================================================
-- 4. 变更类型关联（opr_contract_change_type）
-- =============================================================================
-- 92001：变更92001 → RENT
-- 92002：变更92002 → TERM
-- 92003：变更92003 → RENT
-- -----------------------------------------------------------------------------

INSERT INTO opr_contract_change_type
    (id, change_id, change_type_code,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92001, 92001, 'RENT', 0, '90001', NOW(), '90001', NOW()),
    (92002, 92002, 'TERM', 0, '90001', NOW(), '90001', NOW()),
    (92003, 92003, 'RENT', 0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    change_type_code = VALUES(change_type_code), updated_at = NOW();

-- =============================================================================
-- 5. 变更明细（opr_contract_change_detail）
-- =============================================================================
-- 92001：变更92001 的租金字段变更明细 (15000→18000)
-- 92002：变更92002 的租期字段变更明细
-- -----------------------------------------------------------------------------

INSERT INTO opr_contract_change_detail
    (id, change_id, field_name, field_label, old_value, new_value, data_type,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92001, 92001, 'rentAmount', '月租金额', '15000', '18000', 'decimal',
     0, '90001', NOW(), '90001', NOW()),
    (92002, 92002, 'contractEnd', '合同结束日', '2026-12-31', '2027-06-30', 'date',
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    old_value = VALUES(old_value), new_value = VALUES(new_value), updated_at = NOW();

-- =============================================================================
-- 6. 营收填报（opr_revenue_report）
-- =============================================================================
-- 92001：合同91003，2026-01-15，50000元，待确认 → 可编辑
-- 92002：合同91003，2026-01-16，60000元，已确认 → 不可编辑
-- -----------------------------------------------------------------------------

INSERT INTO opr_revenue_report
    (id, project_id, contract_id, shop_id, merchant_id,
     report_date, report_month, revenue_amount, status,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92001, 90001, 91003, 90001, 90002,
     '2026-01-15', '2026-01', 50000.00, 0,
     0, '90001', NOW(), '90001', NOW()),
    (92002, 90001, 91003, 90001, 90002,
     '2026-01-16', '2026-01', 60000.00, 1,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    revenue_amount = VALUES(revenue_amount), status = VALUES(status), updated_at = NOW();

-- =============================================================================
-- 7. 浮动租金（opr_floating_rent）
-- =============================================================================
-- 92001：合同91003，2025-12月，浮动租金5000 → 历史查询
-- -----------------------------------------------------------------------------

INSERT INTO opr_floating_rent
    (id, contract_id, shop_id, calc_month,
     monthly_revenue, fixed_rent, commission_rate, commission_amount, floating_rent,
     calc_formula, receivable_id,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92001, 91003, 90001, '2025-12',
     200000.00, 15000.00, 10.00, 20000.00, 5000.00,
     '固定提成: 200000×10%=20000, 减固定租金15000, 浮动=5000', NULL,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    floating_rent = VALUES(floating_rent), updated_at = NOW();

-- =============================================================================
-- 8. 客流填报（opr_passenger_flow）
-- =============================================================================
-- 92001：项目90001，2026-01-15，1500人，手动录入 → 可编辑
-- 92002：项目90001，2026-01-16，1800人，导入来源 → 不可编辑
-- -----------------------------------------------------------------------------

INSERT INTO opr_passenger_flow
    (id, project_id, building_id, floor_id,
     report_date, flow_count, source_type,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92001, 90001, 90001, 90001,
     '2026-01-15', 1500, 1,
     0, '90001', NOW(), '90001', NOW()),
    (92002, 90001, 90001, 90001,
     '2026-01-16', 1800, 2,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    flow_count = VALUES(flow_count), source_type = VALUES(source_type), updated_at = NOW();

-- =============================================================================
-- 9. 合同解约（opr_contract_termination）
-- =============================================================================
-- 92001：到期解约(1)，草稿(0) → 可编辑、可计算清算、可提交审批
-- 92002：提前解约(2)，审批中(1)，settlementAmount=25000 → 审批回调测试
-- 92003：重签解约(3)，已生效(2) → 已完成查询
-- -----------------------------------------------------------------------------

INSERT INTO opr_contract_termination
    (id, termination_code, contract_id, ledger_id, project_id,
     merchant_id, brand_id, shop_id,
     termination_type, termination_date, reason, new_contract_id,
     penalty_amount, refund_deposit, unsettled_amount, settlement_amount,
     status, approval_id,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92001, 'JY260301000001', 91003, 92003, 90001,
     90002, 90001, 90001,
     1, '2026-12-31', '合同到期自然解约-测试', NULL,
     0.00, 0.00, 0.00, 0.00,
     0, NULL,
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    termination_code = VALUES(termination_code), status = VALUES(status),
    settlement_amount = VALUES(settlement_amount), updated_at = NOW();

INSERT INTO opr_contract_termination
    (id, termination_code, contract_id, ledger_id, project_id,
     merchant_id, brand_id, shop_id,
     termination_type, termination_date, reason, new_contract_id,
     penalty_amount, refund_deposit, unsettled_amount, settlement_amount,
     status, approval_id,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92002, 'JY260301000002', 91003, 92003, 90001,
     90002, 90001, 90001,
     2, '2026-06-30', '提前解约-测试', NULL,
     8000.00, 0.00, 17000.00, 25000.00,
     1, 'TERM-APPROVAL-92002-1709000000',
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    termination_code = VALUES(termination_code), status = VALUES(status),
    settlement_amount = VALUES(settlement_amount), updated_at = NOW();

INSERT INTO opr_contract_termination
    (id, termination_code, contract_id, ledger_id, project_id,
     merchant_id, brand_id, shop_id,
     termination_type, termination_date, reason, new_contract_id,
     penalty_amount, refund_deposit, unsettled_amount, settlement_amount,
     status, approval_id,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92003, 'JY260301000003', 91003, 92003, 90001,
     90002, 90001, 90001,
     3, '2026-12-31', '合同重签-测试', NULL,
     0.00, 0.00, 0.00, 0.00,
     2, 'TERM-APPROVAL-92003-1708000000',
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    termination_code = VALUES(termination_code), status = VALUES(status),
    settlement_amount = VALUES(settlement_amount), updated_at = NOW();

-- =============================================================================
-- 10. 解约清算明细（opr_termination_settlement）
-- =============================================================================
-- 92001：解约92001 未收租费明细，20000元
-- 92002：解约92002 违约金明细，5000元
-- 92003：解约92002 未收租费明细，17000元
-- -----------------------------------------------------------------------------

INSERT INTO opr_termination_settlement
    (id, termination_id, item_type, item_name, amount, remark,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92001, 92001, 1, '未收租费', 20000.00, '台账92003下待收应收合计',
     0, '90001', NOW(), '90001', NOW()),
    (92002, 92002, 2, '提前解约违约金', 8000.00, '日租金×剩余天数×违约率',
     0, '90001', NOW(), '90001', NOW()),
    (92003, 92002, 1, '未收租费', 17000.00, '台账92003下待收应收合计',
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    amount = VALUES(amount), item_name = VALUES(item_name), updated_at = NOW();

-- =============================================================================
-- 11. 预警记录（opr_alert_record）
-- =============================================================================
-- 92001：合同到期预警，台账92003，站内信，待发(0) → 可取消
-- 92002：应收到期预警，应收92001，站内信，已发(1) → 已完成
-- -----------------------------------------------------------------------------

INSERT INTO opr_alert_record
    (id, alert_type, target_id, alert_date, channel,
     sent_status, sent_time, remark,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (92001, 1, 92003, '2026-12-01', 1,
     0, NULL, '合同到期预警-测试（待发送）',
     0, '90001', NOW(), '90001', NOW()),
    (92002, 2, 92001, '2026-01-01', 1,
     1, '2026-01-01 08:00:00', '应收到期预警-测试（已发送）',
     0, '90001', NOW(), '90001', NOW())
ON DUPLICATE KEY UPDATE
    sent_status = VALUES(sent_status), updated_at = NOW();

-- =============================================================================
-- 数据加载完成
-- 共插入/更新：
--   opr_contract_ledger        × 4  (92001~92004)
--   opr_receivable_plan        × 5  (92001~92005)
--   opr_contract_change        × 3  (92001~92003)
--   opr_contract_change_type   × 3  (92001~92003)
--   opr_contract_change_detail × 2  (92001~92002)
--   opr_revenue_report         × 2  (92001~92002)
--   opr_floating_rent          × 1  (92001)
--   opr_passenger_flow         × 2  (92001~92002)
--   opr_contract_termination   × 3  (92001~92003)
--   opr_termination_settlement × 3  (92001~92003)
--   opr_alert_record           × 2  (92001~92002)
--   合计：30 条记录
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 1;
