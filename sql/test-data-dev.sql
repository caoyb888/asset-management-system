-- =============================================================================
-- 基础数据管理模块 — 开发环境永久测试数据
-- =============================================================================
-- 文件：sql/test-data-dev.sql
-- 用途：为 asset-base 单元/集成测试提供固定的基础数据，保留在开发环境，
--        不随 @Transactional 测试用例回滚
-- 约定：所有测试数据 ID 固定在 90001 ~ 90099 区间，与业务自增 ID 隔离
-- 执行：脚本幂等，可重复执行（INSERT IGNORE / ON DUPLICATE KEY UPDATE）
-- 更新：2026-03-03
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 系统公司（sys_company）
-- -----------------------------------------------------------------------------
INSERT INTO sys_company (id, company_code, company_name, status, created_at, updated_at)
VALUES (90001, 'TEST-CO-001', '产城测试科技有限公司', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 2. 系统用户（sys_user，项目负责人）
-- -----------------------------------------------------------------------------
INSERT INTO sys_user (id, username, real_name, status, created_at, updated_at)
VALUES (90001, 'test_manager', '测试负责人-张三', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), updated_at = NOW();

INSERT INTO sys_user (id, username, real_name, status, created_at, updated_at)
VALUES (90002, 'test_operator', '测试操作员-李四', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 3. 项目（biz_project）
--    PRJ-90001 天河广场：运营中（status=1），用于主流程测试
--    PRJ-90002 南沙项目：筹备中（status=0），用于状态过滤测试
-- -----------------------------------------------------------------------------
INSERT INTO biz_project
    (id, project_code, project_name, company_id, province, city, address,
     property_type, business_type, building_area, operating_area,
     operation_status, opening_date, manager_id, image_urls, is_deleted,
     created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 'PRJ-90001', '测试项目-天河广场', 90001,
     '广东省', '广州市', '广州市天河区天河路100号',
     1,  -- 国有
     1,  -- 自持
     50000.00, 35000.00,
     1,  -- 开业
     '2022-01-01', 90001,
     '[{"url":"https://example.com/img/prj90001_1.jpg","name":"外观图","sort":1}]',
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE
    project_name = VALUES(project_name),
    operation_status = VALUES(operation_status),
    updated_at = NOW();

INSERT INTO biz_project
    (id, project_code, project_name, company_id, province, city, address,
     property_type, business_type, building_area, operating_area,
     operation_status, opening_date, manager_id, image_urls, is_deleted,
     created_by, created_at, updated_by, updated_at)
VALUES
    (90002, 'PRJ-90002', '测试项目-南沙科创园', 90001,
     '广东省', '广州市', '广州市南沙区环市大道88号',
     2,  -- 集体
     2,  -- 租赁
     30000.00, 22000.00,
     0,  -- 筹备
     NULL, 90002,
     NULL,
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE
    project_name = VALUES(project_name),
    operation_status = VALUES(operation_status),
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 4. 项目合同甲方（biz_project_contract）
-- -----------------------------------------------------------------------------
INSERT INTO biz_project_contract
    (id, project_id, party_a_name, party_a_abbr, party_a_address,
     party_a_phone, business_license, legal_representative, email,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 90001, '产城测试科技有限公司', '产城测试',
     '广州市天河区天河路100号产城大厦',
     '020-88888888', '91440101MA9XXXXXX1X', '王总', 'wangzong@test.com',
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE party_a_name = VALUES(party_a_name), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 5. 财务联系人（biz_project_finance_contact）
-- -----------------------------------------------------------------------------
INSERT INTO biz_project_finance_contact
    (id, project_id, contact_name, phone, email, credit_code,
     seal_type, seal_desc, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 90001, '财务总监-陈敏', '13800138001', 'chenmin@test.com',
     '91440101MA9XXXXXX1X', '合同章', '用于签署租赁合同',
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE contact_name = VALUES(contact_name), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 6. 银行账号（biz_project_bank）
--    90001 默认账号，90002 备用账号（用于测试多账号场景）
-- -----------------------------------------------------------------------------
INSERT INTO biz_project_bank
    (id, project_id, bank_name, bank_account, account_name,
     is_default, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 90001, '中国工商银行广州天河支行', '6228481234567890123',
     '产城测试科技有限公司', 1, 0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE bank_name = VALUES(bank_name), updated_at = NOW();

INSERT INTO biz_project_bank
    (id, project_id, bank_name, bank_account, account_name,
     is_default, is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90002, 90001, '中国建设银行广州天河支行', '6227001234567890456',
     '产城测试科技有限公司', 0, 0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE bank_name = VALUES(bank_name), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 7. 楼栋（biz_building）
--    90001 A座：有楼层，用于测试"有关联时禁止删除"
--    90002 B座：无楼层无商铺，用于测试"成功删除"
-- -----------------------------------------------------------------------------
INSERT INTO biz_building
    (id, project_id, building_code, building_name, status,
     building_area, operating_area, above_floors, below_floors, image_url,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 90001, 'BLDG-90001', 'A座（测试楼栋-有楼层）', 1,
     20000.00, 15000.00, 5, 1, NULL,
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE building_name = VALUES(building_name), updated_at = NOW();

INSERT INTO biz_building
    (id, project_id, building_code, building_name, status,
     building_area, operating_area, above_floors, below_floors, image_url,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90002, 90001, 'BLDG-90002', 'B座（测试楼栋-可删除）', 1,
     10000.00, 7000.00, 3, 0, NULL,
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE building_name = VALUES(building_name), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 8. 楼层（biz_floor）
--    90001 A座1F：有商铺，测试"有商铺时禁止删除楼层"
--    90002 A座2F：有商铺 A201，测试跨楼层合并校验
-- -----------------------------------------------------------------------------
INSERT INTO biz_floor
    (id, project_id, building_id, floor_code, floor_name, status,
     building_area, operating_area, remark, image_url,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 90001, 90001, 'FLR-90001', '1F（测试楼层）', 1,
     8000.00, 6000.00, '用于商铺拆合集成测试', NULL,
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE floor_name = VALUES(floor_name), updated_at = NOW();

INSERT INTO biz_floor
    (id, project_id, building_id, floor_code, floor_name, status,
     building_area, operating_area, remark, image_url,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90002, 90001, 90001, 'FLR-90002', '2F（测试楼层）', 1,
     8000.00, 6000.00, '用于跨楼层合并校验测试', NULL,
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE floor_name = VALUES(floor_name), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 9. 商铺（biz_shop）
--    90001 A101：空置300㎡，用于拆分测试（sourceShopId）
--    90002 A102：在租100㎡，用于合并测试（来源之一）
--    90003 A103：空置100㎡，用于合并测试（来源之一）
--    90004 A201：空置200㎡，位于2F，用于跨楼层合并校验（拒绝与1F合并）
-- -----------------------------------------------------------------------------
INSERT INTO biz_shop
    (id, project_id, building_id, floor_id, shop_code, shop_type,
     rent_area, measured_area, building_area, operating_area,
     shop_status, count_leasing_rate, count_rental_rate, count_opening_rate,
     signed_format, planned_format, owner_name, owner_contact, owner_phone,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 90001, 90001, 90001, 'A101', 1,
     300.00, 305.00, 310.00, 300.00,
     0,  -- 空置
     1, 1, 1,
     NULL, '餐饮', '产城科技', '王经理', '13900000001',
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE shop_code = VALUES(shop_code), rent_area = VALUES(rent_area), updated_at = NOW();

INSERT INTO biz_shop
    (id, project_id, building_id, floor_id, shop_code, shop_type,
     rent_area, measured_area, building_area, operating_area,
     shop_status, count_leasing_rate, count_rental_rate, count_opening_rate,
     signed_format, planned_format, owner_name, owner_contact, owner_phone,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90002, 90001, 90001, 90001, 'A102', 2,
     100.00, 102.00, 105.00, 100.00,
     1,  -- 在租
     1, 1, 1,
     '零售', '零售', '产城科技', '李经理', '13900000002',
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE shop_code = VALUES(shop_code), rent_area = VALUES(rent_area), updated_at = NOW();

INSERT INTO biz_shop
    (id, project_id, building_id, floor_id, shop_code, shop_type,
     rent_area, measured_area, building_area, operating_area,
     shop_status, count_leasing_rate, count_rental_rate, count_opening_rate,
     signed_format, planned_format, owner_name, owner_contact, owner_phone,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90003, 90001, 90001, 90001, 'A103', 2,
     100.00, 103.00, 106.00, 100.00,
     0,  -- 空置
     1, 1, 1,
     NULL, '零售', '产城科技', '赵经理', '13900000003',
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE shop_code = VALUES(shop_code), rent_area = VALUES(rent_area), updated_at = NOW();

INSERT INTO biz_shop
    (id, project_id, building_id, floor_id, shop_code, shop_type,
     rent_area, measured_area, building_area, operating_area,
     shop_status, count_leasing_rate, count_rental_rate, count_opening_rate,
     signed_format, planned_format, owner_name, owner_contact, owner_phone,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90004, 90001, 90001, 90002, 'A201', 1,
     200.00, 202.00, 205.00, 200.00,
     0,  -- 空置，位于2F（不同楼层）
     1, 1, 1,
     NULL, '娱乐', '产城科技', '孙经理', '13900000004',
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE shop_code = VALUES(shop_code), rent_area = VALUES(rent_area), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 10. 品牌（biz_brand）
--     90001 星巴克（测试品牌）：高端/连锁/餐饮，含1个主联系人
-- -----------------------------------------------------------------------------
INSERT INTO biz_brand
    (id, brand_code, brand_name_cn, brand_name_en, format_type,
     brand_level, cooperation_type, business_nature, chain_type,
     project_stage, group_name, hq_address, main_cities, website, phone,
     brand_type, avg_rent, min_customer_price, brand_intro,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 'BRD-90001', '星巴克（测试品牌）', 'Starbucks Test', '餐饮-咖啡',
     1,  -- 高端
     1,  -- 直营
     1,  -- 餐饮
     1,  -- 连锁
     '成熟期', '星巴克集团', '西雅图市', '北上广深', 'https://www.starbucks.com.cn', '400-123-4567',
     1,  -- MALL
     350.00, 35.00, '全球知名咖啡连锁品牌，提供高品质咖啡及轻食。（测试数据）',
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE brand_name_cn = VALUES(brand_name_cn), updated_at = NOW();

INSERT INTO biz_brand_contact
    (id, brand_id, contact_name, phone, email, position, is_primary,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 90001, '品牌招商负责人-陈薇', '13700000001', 'chenwei@starbucks-test.com',
     '华南区招商总监', 1,
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE contact_name = VALUES(contact_name), updated_at = NOW();

INSERT INTO biz_brand_contact
    (id, brand_id, contact_name, phone, email, position, is_primary,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90002, 90001, '品牌运营联系人-刘洋', '13700000002', 'liuyang@starbucks-test.com',
     '华南区运营经理', 0,
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE contact_name = VALUES(contact_name), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 11. 商家（biz_merchant）
--     90001 美食广场餐饮有限公司：审核通过（audit_status=1），企业性质
--           身份证号原文：110101199001011234（已用SM4加密，密文仅供参考）
--           注意：实际密文需使用项目配置的 SM4 密钥重新生成
--           开发调试可直接写原文，Service 读取时会自动尝试解密并脱敏
--     90002 待审核个体商家：待审核（audit_status=0），个体户
-- -----------------------------------------------------------------------------
INSERT INTO biz_merchant
    (id, project_id, merchant_code, merchant_name, merchant_attr, merchant_nature,
     format_type, natural_person, id_card, address, phone,
     merchant_level, audit_status, is_deleted,
     created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 90001, 'MCH-90001', '美食广场餐饮有限公司（测试）',
     2,  -- 企业
     1,  -- 民营
     '餐饮', '张法人', '110101199001011234',  -- 明文（开发环境可用明文，测试解密兜底逻辑）
     '广州市天河区天河路100号A座101室', '020-88880001',
     2,  -- 良好
     1,  -- 通过
     0,
     90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE merchant_name = VALUES(merchant_name), audit_status = VALUES(audit_status), updated_at = NOW();

INSERT INTO biz_merchant
    (id, project_id, merchant_code, merchant_name, merchant_attr, merchant_nature,
     format_type, natural_person, id_card, address, phone,
     merchant_level, audit_status, is_deleted,
     created_by, created_at, updated_by, updated_at)
VALUES
    (90002, 90001, 'MCH-90002', '待审核个体商家（测试）',
     1,  -- 个体户
     1,  -- 民营
     '零售', '李个体', '440106199505051234',  -- 明文（待审核，用于审核接口测试）
     '广州市天河区天河路100号A座102室', '13800000002',
     3,  -- 一般
     0,  -- 待审核
     0,
     90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE merchant_name = VALUES(merchant_name), audit_status = VALUES(audit_status), updated_at = NOW();

-- 商家联系人
INSERT INTO biz_merchant_contact
    (id, merchant_id, contact_name, phone, email, position, is_primary,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 90001, '商家主要联系人-周敏', '13600000001', 'zhoumin@meishi-test.com',
     '总经理', 1,
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE contact_name = VALUES(contact_name), updated_at = NOW();

-- 商家开票信息
INSERT INTO biz_merchant_invoice
    (id, merchant_id, invoice_title, tax_number, bank_name, bank_account,
     address, phone, is_default, is_deleted,
     created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 90001, '美食广场餐饮有限公司', '91440101MA9TEST001',
     '招商银行广州天河支行', '755900000000001',
     '广州市天河区天河路100号', '020-88880001',
     1, 0,
     90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE invoice_title = VALUES(invoice_title), updated_at = NOW();

-- 商家诚信记录
INSERT INTO biz_merchant_credit
    (id, merchant_id, record_type, content, record_date, operator_id,
     is_deleted, created_by, created_at, updated_by, updated_at)
VALUES
    (90001, 90001, 1, '2025年度优秀商家，按时缴纳租金，配合度高。', '2025-12-31', 90001,
     0, 90001, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE content = VALUES(content), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 12. 通知公告（biz_notice）
--     90001 已发布公告：用于发布/下架/已读接口测试
--     90002 草稿公告：用于编辑发布时间自动填充测试
-- -----------------------------------------------------------------------------
INSERT INTO biz_notice
    (id, title, content, notice_type, status, scheduled_time, publish_time,
     created_by, is_deleted, created_at, updated_by, updated_at)
VALUES
    (90001, '【测试公告】2026年春节放假通知', '<p>全体商家注意：2026年春节假期为1月28日至2月4日，请合理安排营业计划。</p>',
     2,  -- 公告
     1,  -- 已发布
     NULL, '2026-01-15 09:00:00',
     90001, 0, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), updated_at = NOW();

INSERT INTO biz_notice
    (id, title, content, notice_type, status, scheduled_time, publish_time,
     created_by, is_deleted, created_at, updated_by, updated_at)
VALUES
    (90002, '【测试草稿】园区改造施工通知（草稿）', '<p>（草稿）天河广场A座1F将于3月进行改造施工，请相关商家提前做好准备。</p>',
     1,  -- 通知
     0,  -- 草稿
     NULL, NULL,
     90001, 0, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), updated_at = NOW();

-- 定时发送公告（状态=草稿，scheduled_time未来，用于 XXL-Job 定时发布测试）
INSERT INTO biz_notice
    (id, title, content, notice_type, status, scheduled_time, publish_time,
     created_by, is_deleted, created_at, updated_by, updated_at)
VALUES
    (90003, '【测试定时】4月消防演练通知', '<p>4月15日上午10:00将进行全园消防演练，请各商家配合疏散。</p>',
     1,  -- 通知
     0,  -- 草稿（等待定时发布）
     '2026-04-10 08:00:00', NULL,  -- scheduled_time 已过则 XXL-Job 会发布
     90001, 0, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 13. 新闻资讯（biz_news）
-- -----------------------------------------------------------------------------
INSERT INTO biz_news
    (id, title, content, category, status, publish_time,
     created_by, is_deleted, created_at, updated_by, updated_at)
VALUES
    (90001, '【测试资讯】天河广场荣获2025年度最佳商业地产奖',
     '<p>（测试数据）近日，天河广场荣获由中国商业联合会颁发的2025年度最佳商业地产奖。</p>',
     3,  -- 招商
     1,  -- 上架
     '2026-01-10 10:00:00',
     90001, 0, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), updated_at = NOW();

INSERT INTO biz_news
    (id, title, content, category, status, publish_time,
     created_by, is_deleted, created_at, updated_by, updated_at)
VALUES
    (90002, '【测试草稿】园区新增停车位规划公告（草稿）',
     '<p>（草稿测试数据）园区计划新增地下停车位200个，预计2026年Q3竣工。</p>',
     1,  -- 新闻
     0,  -- 草稿
     NULL,
     90001, 0, NOW(), 90001, NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status), updated_at = NOW();

-- =============================================================================
-- 数据验证查询（执行后可快速确认数据写入正确）
-- =============================================================================
-- SELECT '=== 测试数据汇总 ===' AS info;
-- SELECT 'sys_company' AS tbl, COUNT(*) AS cnt FROM sys_company WHERE id >= 90001
-- UNION ALL
-- SELECT 'biz_project',         COUNT(*) FROM biz_project         WHERE id >= 90001 AND is_deleted=0
-- UNION ALL
-- SELECT 'biz_building',        COUNT(*) FROM biz_building        WHERE id >= 90001 AND is_deleted=0
-- UNION ALL
-- SELECT 'biz_floor',           COUNT(*) FROM biz_floor           WHERE id >= 90001 AND is_deleted=0
-- UNION ALL
-- SELECT 'biz_shop',            COUNT(*) FROM biz_shop            WHERE id >= 90001 AND is_deleted=0
-- UNION ALL
-- SELECT 'biz_brand',           COUNT(*) FROM biz_brand           WHERE id >= 90001 AND is_deleted=0
-- UNION ALL
-- SELECT 'biz_merchant',        COUNT(*) FROM biz_merchant        WHERE id >= 90001 AND is_deleted=0
-- UNION ALL
-- SELECT 'biz_notice',          COUNT(*) FROM biz_notice          WHERE id >= 90001 AND is_deleted=0
-- UNION ALL
-- SELECT 'biz_news',            COUNT(*) FROM biz_news            WHERE id >= 90001 AND is_deleted=0;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- END OF FILE
-- =============================================================================
