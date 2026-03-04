-- =============================================================================
-- 招商管理模块 — 开发环境永久测试数据
-- =============================================================================
-- 文件：sql/test-data-inv.sql
-- 用途：为 asset-investment 单元/集成测试提供固定的基础数据，保留在开发环境，
--        不随 @Transactional 测试用例回滚
-- 约定：所有测试数据 ID 固定在 91001 ~ 91099 区间，与业务自增 ID 隔离
-- 前提：需先执行 sql/test-data-dev.sql（依赖 biz_project/biz_shop/biz_merchant 等基础数据）
-- 执行：脚本幂等，可重复执行（ON DUPLICATE KEY UPDATE）
-- 更新：2026-03-04
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 计租方案（cfg_rent_scheme）
--    91001：固定租金方案（启用），供费项关联、列表查询测试
--    91002：提成方案（停用），供启用/停用切换测试
-- -----------------------------------------------------------------------------
INSERT INTO cfg_rent_scheme
    (id, scheme_code, scheme_name, charge_type, payment_cycle, billing_mode,
     strategy_bean_name, status, description, is_deleted, created_at, updated_at)
VALUES
    (91001, 'SCH-TEST-001', '测试固定租金方案', 1, 1, 1,
     'fixedRentStrategy', 1, '固定租金方案-测试专用', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    scheme_name = VALUES(scheme_name), status = VALUES(status), updated_at = NOW();

INSERT INTO cfg_rent_scheme
    (id, scheme_code, scheme_name, charge_type, payment_cycle, billing_mode,
     strategy_bean_name, status, description, is_deleted, created_at, updated_at)
VALUES
    (91002, 'SCH-TEST-002', '测试提成方案（停用）', 2, 3, 1,
     'fixedCommissionStrategy', 0, '停用方案-用于启用停用切换测试', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    scheme_name = VALUES(scheme_name), status = VALUES(status), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 2. 收款项目（cfg_fee_item）
--    91001：租金类（itemType=1，isRequired 强制=1，启用）
--    91002：保证金类（itemType=2，非必填，启用）
--    91003：物管费（itemType=3，停用）——用于"默认列表不返回停用项"断言
-- -----------------------------------------------------------------------------
INSERT INTO cfg_fee_item
    (id, item_code, item_name, item_type, is_required, sort_order, status,
     is_deleted, created_at, updated_at)
VALUES
    (91001, 'FEE-RENT-TEST', '测试租金', 1, 1, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    item_name = VALUES(item_name), is_required = VALUES(is_required),
    status = VALUES(status), updated_at = NOW();

INSERT INTO cfg_fee_item
    (id, item_code, item_name, item_type, is_required, sort_order, status,
     is_deleted, created_at, updated_at)
VALUES
    (91002, 'FEE-DEPOSIT-TEST', '测试保证金', 2, 0, 2, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    item_name = VALUES(item_name), status = VALUES(status), updated_at = NOW();

INSERT INTO cfg_fee_item
    (id, item_code, item_name, item_type, is_required, sort_order, status,
     is_deleted, created_at, updated_at)
VALUES
    (91003, 'FEE-MGMT-TEST', '测试物管费（停用）', 3, 0, 3, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    item_name = VALUES(item_name), status = VALUES(status), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 3. 意向协议主表（inv_intention）
--    91001：草稿（status=0）—— 可删除、可提交审批
--    91002：审批中（status=1）—— 不可删除、审批回调测试
--    91003：已通过（status=2）—— 含商铺和费项，用于意向转合同测试
--    91004：已转合同（status=4）—— 不可删除测试
-- -----------------------------------------------------------------------------
INSERT INTO inv_intention
    (id, intention_code, intention_name, project_id, merchant_id, brand_id,
     signing_entity, rent_scheme_id,
     contract_start, contract_end, payment_cycle, billing_mode,
     status, total_amount, version, is_current, is_deleted, created_at, updated_at)
VALUES
    (91001, 'INV202603TEST01', '测试意向-草稿', 90001, 90001, 90001,
     '测试签约主体A', 91001,
     '2026-07-01', '2028-06-30', 1, 1,
     0, NULL, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    intention_name = VALUES(intention_name), status = VALUES(status), updated_at = NOW();

INSERT INTO inv_intention
    (id, intention_code, intention_name, project_id, merchant_id, brand_id,
     signing_entity, rent_scheme_id,
     contract_start, contract_end, payment_cycle, billing_mode,
     status, total_amount, approval_id, version, is_current, is_deleted, created_at, updated_at)
VALUES
    (91002, 'INV202603TEST02', '测试意向-审批中', 90001, 90001, 90001,
     '测试签约主体B', 91001,
     '2026-08-01', '2028-07-31', 3, 1,
     1, NULL, 'MOCK-INV-91002', 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    intention_name = VALUES(intention_name), status = VALUES(status), updated_at = NOW();

INSERT INTO inv_intention
    (id, intention_code, intention_name, project_id, merchant_id, brand_id,
     signing_entity, rent_scheme_id,
     contract_start, contract_end, payment_cycle, billing_mode,
     status, total_amount, approval_id, version, is_current, is_deleted, created_at, updated_at)
VALUES
    (91003, 'INV202603TEST03', '测试意向-已通过（可转合同）', 90001, 90001, 90001,
     '测试签约主体C', 91001,
     '2026-01-01', '2027-12-31', 1, 1,
     2, 480000.00, 'MOCK-INV-91003', 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    intention_name = VALUES(intention_name), status = VALUES(status),
    total_amount = VALUES(total_amount), updated_at = NOW();

INSERT INTO inv_intention
    (id, intention_code, intention_name, project_id, merchant_id, brand_id,
     signing_entity, rent_scheme_id,
     contract_start, contract_end, payment_cycle, billing_mode,
     status, total_amount, version, is_current, is_deleted, created_at, updated_at)
VALUES
    (91004, 'INV202603TEST04', '测试意向-已转合同', 90001, 90002, 90001,
     '测试签约主体D', 91001,
     '2025-01-01', '2026-12-31', 1, 1,
     4, 240000.00, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    intention_name = VALUES(intention_name), status = VALUES(status), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 4. 意向协议商铺关联（inv_intention_shop）
--    91001：意向91003 绑定商铺90001（shopId依赖 test-data-dev.sql 中的 biz_shop）
-- -----------------------------------------------------------------------------
INSERT INTO inv_intention_shop
    (id, intention_id, shop_id, building_id, floor_id, format_type, area,
     is_deleted, created_at, updated_at)
VALUES
    (91001, 91003, 90001, 90001, 90001, '餐饮', 200.00, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    area = VALUES(area), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 5. 意向协议费项（inv_intention_fee）
--    91001：意向91003 的固定租金费项（unitPrice=100, area=200, 合同周期24个月=480000）
-- -----------------------------------------------------------------------------
INSERT INTO inv_intention_fee
    (id, intention_id, fee_item_id, fee_name, charge_type,
     unit_price, area, amount,
     start_date, end_date, period_index,
     formula_params, is_deleted, created_at, updated_at)
VALUES
    (91001, 91003, 91001, '租金', 1,
     100.00, 200.00, 480000.00,
     '2026-01-01', '2027-12-31', 1,
     NULL, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    unit_price = VALUES(unit_price), area = VALUES(area),
    amount = VALUES(amount), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 6. 招商合同主表（inv_lease_contract）
--    91001：草稿（status=0）—— 可发起审批、可删除
--    91002：审批中（status=1）—— 审批回调+写版本快照测试
--    91003：生效（status=2）—— 不可删除测试
-- -----------------------------------------------------------------------------
INSERT INTO inv_lease_contract
    (id, contract_code, contract_name, project_id, merchant_id, brand_id,
     signing_entity, rent_scheme_id,
     contract_start, contract_end, payment_cycle, billing_mode,
     status, total_amount, version, is_current, is_deleted, created_at, updated_at)
VALUES
    (91001, 'LC20260304TEST01', '测试合同-草稿', 90001, 90001, 90001,
     '测试合同主体A', 91001,
     '2026-07-01', '2028-06-30', 3, 1,
     0, NULL, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    contract_name = VALUES(contract_name), status = VALUES(status), updated_at = NOW();

INSERT INTO inv_lease_contract
    (id, contract_code, contract_name, project_id, merchant_id, brand_id,
     signing_entity, rent_scheme_id,
     contract_start, contract_end, payment_cycle, billing_mode,
     status, total_amount, approval_id, version, is_current, is_deleted, created_at, updated_at)
VALUES
    (91002, 'LC20260304TEST02', '测试合同-审批中', 90001, 90001, 90001,
     '测试合同主体B', 91001,
     '2026-08-01', '2028-07-31', 3, 1,
     1, NULL, 'MOCK-CTR-91002', 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    contract_name = VALUES(contract_name), status = VALUES(status), updated_at = NOW();

INSERT INTO inv_lease_contract
    (id, contract_code, contract_name, project_id, merchant_id, brand_id,
     signing_entity, rent_scheme_id,
     contract_start, contract_end, payment_cycle, billing_mode,
     status, total_amount, approval_id, version, is_current, is_deleted, created_at, updated_at)
VALUES
    (91003, 'LC20260304TEST03', '测试合同-生效', 90001, 90002, 90001,
     '测试合同主体C', 91001,
     '2025-01-01', '2026-12-31', 6, 1,
     2, 240000.00, 'MOCK-CTR-91003', 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    contract_name = VALUES(contract_name), status = VALUES(status), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 7. 合同商铺关联（inv_lease_contract_shop）
--    91001：合同91001 绑定商铺90002（另一个商铺，不与意向测试冲突）
-- -----------------------------------------------------------------------------
INSERT INTO inv_lease_contract_shop
    (id, contract_id, shop_id, building_id, floor_id, format_type,
     area, rent_unit_price, property_unit_price,
     is_deleted, created_at, updated_at)
VALUES
    (91001, 91001, 90002, 90001, 90001, '零售', 150.00, 80.00, 25.00, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    area = VALUES(area), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 8. 合同费项（inv_lease_contract_fee）
--    91001：合同91001 的固定租金费项（unitPrice=80, area=150, 合同12个月=144000）
-- -----------------------------------------------------------------------------
INSERT INTO inv_lease_contract_fee
    (id, contract_id, fee_item_id, fee_name, charge_type,
     unit_price, area, amount,
     start_date, end_date, period_index,
     formula_params, is_deleted, created_at, updated_at)
VALUES
    (91001, 91001, 91001, '租金', 1,
     80.00, 150.00, 144000.00,
     '2026-07-01', '2027-06-30', 1,
     NULL, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    unit_price = VALUES(unit_price), area = VALUES(area),
    amount = VALUES(amount), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 9. 开业审批主表（inv_opening_approval）
--    91001：草稿（status=0）—— 可提交审批
--    91002：审批中（status=1）—— 审批驳回测试（驳回后保存 snapshot_data）
--    91003：驳回（status=3）—— 基于历史创建新单测试
-- -----------------------------------------------------------------------------
INSERT INTO inv_opening_approval
    (id, approval_code, project_id, shop_id, contract_id, merchant_id,
     planned_opening_date, status, is_deleted, created_at, updated_at)
VALUES
    (91001, 'OA-TEST-91001', 90001, 90001, 91001, 90001,
     '2026-09-01', 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    status = VALUES(status), updated_at = NOW();

INSERT INTO inv_opening_approval
    (id, approval_code, project_id, shop_id, contract_id, merchant_id,
     planned_opening_date, status, approval_id, is_deleted, created_at, updated_at)
VALUES
    (91002, 'OA-TEST-91002', 90001, 90002, 91002, 90001,
     '2026-10-01', 1, 'MOCK-OPENING-91002', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    status = VALUES(status), updated_at = NOW();

INSERT INTO inv_opening_approval
    (id, approval_code, project_id, shop_id, contract_id, merchant_id,
     planned_opening_date, status, is_deleted, created_at, updated_at)
VALUES
    (91003, 'OA-TEST-91003', 90001, 90001, 91003, 90002,
     '2025-12-01', 3, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    status = VALUES(status), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 10. 租决政策主表（inv_rent_policy）
--     91001：草稿（status=0）—— 可发起审批、保存指标
--     91002：已通过（status=2）—— /approved 接口只返回此记录；供租金分解引用
-- -----------------------------------------------------------------------------
INSERT INTO inv_rent_policy
    (id, policy_code, project_id, policy_type,
     year1_rent, year2_rent, year1_property_fee, year2_property_fee,
     min_lease_term, max_lease_term,
     rent_growth_rate, fee_growth_rate, free_rent_period, deposit_months,
     payment_cycle, status, is_deleted, created_at, updated_at)
VALUES
    (91001, 'ZJ-TEST-91001', 90001, 1,
     120.00, 130.00, 35.00, 38.00,
     12, 60,
     5.00, 3.00, 1, 3,
     3, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    status = VALUES(status), updated_at = NOW();

INSERT INTO inv_rent_policy
    (id, policy_code, project_id, policy_type,
     year1_rent, year2_rent, year1_property_fee, year2_property_fee,
     min_lease_term, max_lease_term,
     rent_growth_rate, fee_growth_rate, free_rent_period, deposit_months,
     payment_cycle, status, approval_id, is_deleted, created_at, updated_at)
VALUES
    (91002, 'ZJ-TEST-91002', 90001, 1,
     100.00, 110.00, 30.00, 33.00,
     12, 60,
     5.00, 3.00, 1, 3,
     3, 2, 'MOCK-POL-91002', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    status = VALUES(status), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 11. 租决政策分类指标（inv_rent_policy_indicator）
--     91001：政策91002 主力店（shopCategory=1）指标
-- -----------------------------------------------------------------------------
INSERT INTO inv_rent_policy_indicator
    (id, policy_id, shop_category, rent_price, property_fee_price,
     format_type, rent_growth_rate, fee_growth_rate,
     free_rent_months, deposit_months,
     is_deleted, created_at, updated_at)
VALUES
    (91001, 91002, 1, 100.00, 30.00,
     '餐饮', 5.00, 3.00,
     1, 3,
     0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    rent_price = VALUES(rent_price), property_fee_price = VALUES(property_fee_price),
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 12. 租金分解主表（inv_rent_decomposition）
--     91001：草稿（status=0）—— 保存明细、汇总计算、发起审批测试
--     91002：审批中（status=1）—— 审批回调测试
-- -----------------------------------------------------------------------------
INSERT INTO inv_rent_decomposition
    (id, decomp_code, project_id, policy_id,
     total_annual_rent, total_annual_fee,
     status, policy_snapshot, is_deleted, created_at, updated_at)
VALUES
    (91001, 'RD-TEST-91001', 90001, 91002,
     NULL, NULL,
     0,
     JSON_OBJECT('policyCode', 'ZJ-TEST-91002', 'year1Rent', 100, 'year2Rent', 110),
     0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    status = VALUES(status), updated_at = NOW();

INSERT INTO inv_rent_decomposition
    (id, decomp_code, project_id, policy_id,
     total_annual_rent, total_annual_fee,
     status, approval_id, policy_snapshot, is_deleted, created_at, updated_at)
VALUES
    (91002, 'RD-TEST-91002', 90001, 91002,
     600000.00, 180000.00,
     1, 'MOCK-DECOMP-91002',
     JSON_OBJECT('policyCode', 'ZJ-TEST-91002', 'year1Rent', 100, 'year2Rent', 110),
     0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    status = VALUES(status), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 13. 租金分解明细（inv_rent_decomp_detail）
--     91001：分解91001 主力店明细（100元/㎡·月 × 500㎡ × 12 = 600000）
--            供 DECOMP-I-03 汇总计算断言验证
-- -----------------------------------------------------------------------------
INSERT INTO inv_rent_decomp_detail
    (id, decomp_id, shop_category, format_type,
     rent_unit_price, property_unit_price, area,
     annual_rent, annual_fee, remark,
     is_deleted, created_at, updated_at)
VALUES
    (91001, 91001, 1, '餐饮',
     100.00, 30.00, 500.00,
     600000.00, 180000.00, '测试主力店明细',
     0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    rent_unit_price = VALUES(rent_unit_price),
    property_unit_price = VALUES(property_unit_price),
    area = VALUES(area),
    annual_rent = VALUES(annual_rent),
    annual_fee = VALUES(annual_fee),
    updated_at = NOW();

-- =============================================================================
-- 数据加载完成
-- 共插入/更新：
--   cfg_rent_scheme       × 2  (91001, 91002)
--   cfg_fee_item          × 3  (91001, 91002, 91003)
--   inv_intention         × 4  (91001, 91002, 91003, 91004)
--   inv_intention_shop    × 1  (91001)
--   inv_intention_fee     × 1  (91001)
--   inv_lease_contract    × 3  (91001, 91002, 91003)
--   inv_lease_contract_shop × 1 (91001)
--   inv_lease_contract_fee  × 1 (91001)
--   inv_opening_approval  × 3  (91001, 91002, 91003)
--   inv_rent_policy       × 2  (91001, 91002)
--   inv_rent_policy_indicator × 1 (91001)
--   inv_rent_decomposition  × 2 (91001, 91002)
--   inv_rent_decomp_detail  × 1 (91001)
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 1;
