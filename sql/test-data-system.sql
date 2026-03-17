-- =============================================================================
-- 系统管理模块 — 开发环境永久测试数据
-- =============================================================================
-- 文件：sql/test-data-system.sql
-- 用途：为 asset-system 单元/集成测试提供固定的基础数据，保留在开发环境，
--        不随 @Transactional 测试用例回滚
-- 约定：所有测试数据 ID 固定在 91001 ~ 91199 区间，与业务自增 ID 及其他模块测试数据隔离
-- 执行：脚本幂等，可重复执行（INSERT IGNORE）
-- 密码：测试用户密码统一为 Test@12345，SM3 哈希后存储
-- 依赖：init.sql + system_init.sql + system_patch_v2.sql 已执行
-- =============================================================================

USE asset_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 1. 部门/机构（sys_dept）— 三级组织树 + 1个空部门
-- =============================================================================
-- sys_dept (id=91001) 集团总部 [根节点, parent_id=0, ancestors="0"]
-- ├── sys_dept (id=91002) 华南区域公司 [parent_id=91001, ancestors="0,91001"]
-- │   ├── sys_dept (id=91003) 天河项目部 [parent_id=91002, ancestors="0,91001,91002"]
-- │   └── sys_dept (id=91004) 番禺项目部 [parent_id=91002, ancestors="0,91001,91002"]
-- └── sys_dept (id=91005) 华东区域公司 [parent_id=91001, ancestors="0,91001"]
--     └── sys_dept (id=91006) 浦东项目部 [parent_id=91005, ancestors="0,91001,91005"]
-- sys_dept (id=91007) 空部门 [无子部门无用户, 用于删除测试]

INSERT IGNORE INTO sys_dept (id, parent_id, ancestors, dept_name, dept_code, sort_order, leader, phone, email, status, is_deleted, created_at, updated_at) VALUES
(91001, 0,     '0',              '集团总部',     'TEST-HQ',    0, '张总',   '13800000001', 'hq@test.com',    1, 0, NOW(), NOW()),
(91002, 91001, '0,91001',        '华南区域公司', 'TEST-HN',    1, '李总',   '13800000002', 'hn@test.com',    1, 0, NOW(), NOW()),
(91003, 91002, '0,91001,91002',  '天河项目部',   'TEST-HN-TH', 1, '王经理', '13800000003', 'th@test.com',    1, 0, NOW(), NOW()),
(91004, 91002, '0,91001,91002',  '番禺项目部',   'TEST-HN-PY', 2, '赵经理', '13800000004', 'py@test.com',    1, 0, NOW(), NOW()),
(91005, 91001, '0,91001',        '华东区域公司', 'TEST-HD',    2, '孙总',   '13800000005', 'hd@test.com',    1, 0, NOW(), NOW()),
(91006, 91005, '0,91001,91005',  '浦东项目部',   'TEST-HD-PD', 1, '周经理', '13800000006', 'pd@test.com',    1, 0, NOW(), NOW()),
(91007, 0,     '0',              '空部门',       'TEST-EMPTY', 99, '',      '',            '',               1, 0, NOW(), NOW());

-- =============================================================================
-- 2. 岗位（sys_post）— 3个启用 + 1个停用
-- =============================================================================
INSERT IGNORE INTO sys_post (id, post_code, post_name, sort_order, status, remark, is_deleted, created_at, updated_at) VALUES
(91001, 'TEST_GM',    '总经理',   1, 1, '测试岗位-总经理',   0, NOW(), NOW()),
(91002, 'TEST_PM',    '项目经理', 2, 1, '测试岗位-项目经理', 0, NOW(), NOW()),
(91003, 'TEST_ZS',    '招商专员', 3, 1, '测试岗位-招商专员', 0, NOW(), NOW()),
(91004, 'TEST_STOP',  '停用岗位', 4, 0, '测试岗位-停用',     0, NOW(), NOW());

-- =============================================================================
-- 3. 角色（sys_role）— 5种数据权限范围 + 1个停用
-- =============================================================================
-- data_scope: 1全部 2自定义 3本部门 4本部门及以下 5仅本人
INSERT IGNORE INTO sys_role (id, role_name, role_code, data_scope, sort_order, status, remark, is_deleted, created_at, updated_at) VALUES
(91001, '测试超管',     'TEST_SUPER_ADMIN',   1, 1, 1, '数据权限=全部',         0, NOW(), NOW()),
(91002, '测试区域经理', 'TEST_AREA_MANAGER',  3, 2, 1, '数据权限=本部门',       0, NOW(), NOW()),
(91003, '测试项目经理', 'TEST_PROJECT_MANAGER',4, 3, 1, '数据权限=本部门及下级', 0, NOW(), NOW()),
(91004, '测试自定义权限','TEST_CUSTOM_SCOPE',  2, 4, 1, '数据权限=自定义',       0, NOW(), NOW()),
(91005, '测试普通员工', 'TEST_EMPLOYEE',       5, 5, 1, '数据权限=仅本人',       0, NOW(), NOW()),
(91006, '测试停用角色', 'TEST_DISABLED_ROLE',  1, 6, 0, '停用角色',              0, NOW(), NOW());

-- =============================================================================
-- 4. 角色自定义数据权限（sys_role_data）— 角色91004可见天河+番禺
-- =============================================================================
INSERT IGNORE INTO sys_role_data (id, role_id, dept_id, is_deleted, created_at, updated_at) VALUES
(91001, 91004, 91003, 0, NOW(), NOW()),
(91002, 91004, 91004, 0, NOW(), NOW());

-- =============================================================================
-- 5. 用户（sys_user）— 8个测试用户
-- =============================================================================
-- 密码统一: Test@12345 → SM3: 923992114bce0e956a7eb2ae11d329841a3426222bd1e27f038798ca057876d4
INSERT IGNORE INTO sys_user (id, username, password, real_name, dept_id, phone, email, status, is_deleted, created_at, updated_at) VALUES
(91001, 'test_admin',      '923992114bce0e956a7eb2ae11d329841a3426222bd1e27f038798ca057876d4', '测试管理员',   91001, '13900000001', 'admin@test.com',    1, 0, NOW(), NOW()),
(91002, 'test_area_mgr',   '923992114bce0e956a7eb2ae11d329841a3426222bd1e27f038798ca057876d4', '华南区域经理', 91002, '13900000002', 'area@test.com',     1, 0, NOW(), NOW()),
(91003, 'test_proj_mgr',   '923992114bce0e956a7eb2ae11d329841a3426222bd1e27f038798ca057876d4', '天河项目经理', 91003, '13900000003', 'proj@test.com',     1, 0, NOW(), NOW()),
(91004, 'test_custom_user', '923992114bce0e956a7eb2ae11d329841a3426222bd1e27f038798ca057876d4', '自定义权限用户',91002, '13900000004', 'custom@test.com',   1, 0, NOW(), NOW()),
(91005, 'test_normal_user', '923992114bce0e956a7eb2ae11d329841a3426222bd1e27f038798ca057876d4', '普通员工',     91003, '13900000005', 'normal@test.com',   1, 0, NOW(), NOW()),
(91006, 'test_disabled',   '923992114bce0e956a7eb2ae11d329841a3426222bd1e27f038798ca057876d4', '停用用户',     91001, '13900000006', 'disabled@test.com', 0, 0, NOW(), NOW()),
(91007, 'test_locked',     '923992114bce0e956a7eb2ae11d329841a3426222bd1e27f038798ca057876d4', '锁定用户',     91001, '13900000007', 'locked@test.com',   1, 0, NOW(), NOW()),
(91008, 'test_multi_role', '923992114bce0e956a7eb2ae11d329841a3426222bd1e27f038798ca057876d4', '多角色用户',   91003, '13900000008', 'multi@test.com',    1, 0, NOW(), NOW());

-- 注意: test_locked 用户的锁定状态(login_fail_count/lock_time)通过 Redis 管理，
--       集成测试中需在 @BeforeEach 中通过 SysTokenService 设置 Redis 锁定数据

-- =============================================================================
-- 6. 用户角色关联（sys_user_role）
-- =============================================================================
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(91001, 91001),   -- test_admin      → 测试超管
(91002, 91002),   -- test_area_mgr   → 测试区域经理
(91003, 91003),   -- test_proj_mgr   → 测试项目经理
(91004, 91004),   -- test_custom_user → 测试自定义权限
(91005, 91005),   -- test_normal_user → 测试普通员工
(91006, 91001),   -- test_disabled   → 测试超管（但用户已停用）
(91007, 91005),   -- test_locked     → 测试普通员工
(91008, 91003),   -- test_multi_role → 测试项目经理（角色1）
(91008, 91005);   -- test_multi_role → 测试普通员工（角色2）

-- =============================================================================
-- 7. 用户岗位关联（sys_user_post）
-- =============================================================================
INSERT IGNORE INTO sys_user_post (user_id, post_id) VALUES
(91001, 91001),   -- test_admin      → 总经理
(91002, 91002),   -- test_area_mgr   → 项目经理
(91003, 91002),   -- test_proj_mgr   → 项目经理
(91004, 91003),   -- test_custom_user → 招商专员
(91005, 91003),   -- test_normal_user → 招商专员
(91008, 91002);   -- test_multi_role → 项目经理

-- =============================================================================
-- 8. 菜单/权限（sys_menu）— 1个目录 + 3个菜单 + 3个按钮 + 1个隐藏 + 1个停用
-- =============================================================================
-- menu_type: M=目录 C=菜单 F=按钮/权限（对应 system_init.sql 中的约定）
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order, visible, status) VALUES
(91001, 0,     '测试系统管理', 'M', '/test-sys',           '',                       '',                  'Setting', 0, 1, 1),
(91002, 91001, '测试用户管理', 'C', '/test-sys/users',     'sys/user/index',         'sys:user:list',     'User',    1, 1, 1),
(91003, 91002, '新增用户',     'F', '',                    '',                        'sys:user:add',      '',        1, 1, 1),
(91004, 91002, '编辑用户',     'F', '',                    '',                        'sys:user:edit',     '',        2, 1, 1),
(91005, 91002, '删除用户',     'F', '',                    '',                        'sys:user:delete',   '',        3, 1, 1),
(91006, 91001, '测试角色管理', 'C', '/test-sys/roles',     'sys/role/index',         'sys:role:list',     'Key',     2, 1, 1),
(91007, 91001, '测试菜单管理', 'C', '/test-sys/menus',     'sys/menu/index',         'sys:menu:list',     'Menu',    3, 1, 1),
(91008, 91001, '隐藏菜单',     'C', '/test-sys/hidden',    'sys/hidden/index',       'sys:hidden:list',   '',        4, 0, 1),
(91009, 91001, '停用菜单',     'C', '/test-sys/disabled',  'sys/disabled/index',     'sys:disabled:list', '',        5, 1, 0);

-- =============================================================================
-- 9. 角色菜单关联（sys_role_menu）
-- =============================================================================
-- 区域经理(91002): 只有用户管理查看+新增
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(91002, 91001),
(91002, 91002),
(91002, 91003);

-- 项目经理(91003): 更多权限（用户管理全部+角色管理）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(91003, 91001),
(91003, 91002),
(91003, 91003),
(91003, 91004),
(91003, 91005),
(91003, 91006);

-- =============================================================================
-- 10. 字典类型（sys_dict_type）
-- =============================================================================
INSERT IGNORE INTO sys_dict_type (id, dict_name, dict_type, status, remark, is_deleted, created_at, updated_at) VALUES
(91001, '项目状态', 'test_project_status', 1, '测试用-项目状态字典', 0, NOW(), NOW()),
(91002, '商铺状态', 'test_shop_status',    1, '测试用-商铺状态字典', 0, NOW(), NOW());

-- =============================================================================
-- 11. 字典数据（sys_dict_data）
-- =============================================================================
INSERT IGNORE INTO sys_dict_data (id, dict_type, dict_label, dict_value, css_class, sort_order, status, is_deleted, created_at, updated_at) VALUES
(91001, 'test_project_status', '筹备中', '1', 'info',    1, 1, 0, NOW(), NOW()),
(91002, 'test_project_status', '运营中', '2', 'success', 2, 1, 0, NOW(), NOW()),
(91003, 'test_project_status', '已关闭', '3', 'danger',  3, 1, 0, NOW(), NOW()),
(91004, 'test_shop_status',    '空置',   '1', 'info',    1, 1, 0, NOW(), NOW()),
(91005, 'test_shop_status',    '在租',   '2', 'success', 2, 1, 0, NOW(), NOW());

-- =============================================================================
-- 12. 编码规则（sys_code_rule）
-- =============================================================================
INSERT IGNORE INTO sys_code_rule (id, rule_key, rule_name, prefix, date_format, sep, seq_length, reset_type, current_seq, current_period, status, remark, is_deleted, created_at, updated_at) VALUES
(91001, 'TEST_CONTRACT', '测试合同编码', 'HT', 'yyyyMMdd', '-', 4, 2, 0, '', 1, '测试用-合同编码规则，按月重置', 0, NOW(), NOW()),
(91002, 'TEST_RECEIPT',  '测试收据编码', 'SK', 'yyyyMMdd', '-', 4, 0, 0, '', 1, '测试用-收据编码规则，不重置',   0, NOW(), NOW());

-- =============================================================================
-- 13. 分类管理（sys_category）
-- =============================================================================
INSERT IGNORE INTO sys_category (id, category_type, parent_id, ancestors, category_code, category_name, level, sort_order, status, is_deleted, created_at, updated_at) VALUES
(91001, 'format', 0,     '0',          'TEST-FMT',      '测试业态分类', 1, 0, 1, 0, NOW(), NOW()),
(91002, 'format', 91001, '0,91001',    'TEST-FMT-CY',   '餐饮',        2, 1, 1, 0, NOW(), NOW()),
(91003, 'format', 91001, '0,91001',    'TEST-FMT-LS',   '零售',        2, 2, 1, 0, NOW(), NOW());

-- =============================================================================
-- 14. 租费算法（sys_rent_algorithm）— 3种算法类型
-- =============================================================================
-- algo_type: 1固定租金 2浮动租金 3取高差额 ...
INSERT IGNORE INTO sys_rent_algorithm (id, algo_name, algo_code, algo_type, calc_mode, month_rule, formula, status, remark, is_deleted, created_at, updated_at) VALUES
(91001, '测试固定租金算法', 'TEST_ALG_FIXED',    1, 1, 1, 'unit_price * area * months',                           1, '测试用-固定租金', 0, NOW(), NOW()),
(91002, '测试浮动租金算法', 'TEST_ALG_FLOATING', 2, 1, 1, 'revenue * rate / 100',                                 1, '测试用-浮动租金', 0, NOW(), NOW()),
(91003, '测试阶梯提成算法', 'TEST_ALG_STEP',     3, 1, 1, 'SUM(each_step_amount * step_rate / 100)',              1, '测试用-阶梯提成', 0, NOW(), NOW());

-- =============================================================================
-- 15. 租费算法阶梯配置（sys_rent_algo_step）— 算法91003的3个档位
-- =============================================================================
-- 阶梯: [0,10万)→5%, [10万,30万)→8%, [30万,+∞)→12%
INSERT IGNORE INTO sys_rent_algo_step (id, algo_id, step_start, step_end, rate, fixed_amount, sort_order, is_deleted, created_at, updated_at) VALUES
(91001, 91003, 0.00,       100000.00, 5.00,  NULL, 1, 0, NOW(), NOW()),
(91002, 91003, 100000.00,  300000.00, 8.00,  NULL, 2, 0, NOW(), NOW()),
(91003, 91003, 300000.00,  NULL,      12.00, NULL, 3, 0, NOW(), NOW());

-- =============================================================================
-- 16. 系统配置（sys_config）
-- =============================================================================
INSERT IGNORE INTO sys_config (id, config_name, config_key, config_value, config_type, remark, is_deleted, created_at, updated_at) VALUES
(91001, '登录失败最大次数', 'test.login.maxRetry',        '5',    1, '测试用-系统内置配置', 0, NOW(), NOW()),
(91002, '密码最小长度',     'test.password.minLength',    '8',    1, '测试用-系统内置配置', 0, NOW(), NOW()),
(91003, '通知开关',         'test.notice.enabled',        'true', 2, '测试用-自定义配置',   0, NOW(), NOW());

-- =============================================================================
SET FOREIGN_KEY_CHECKS = 1;

-- 验证：输出各表测试数据计数
SELECT '--- 系统管理模块测试数据统计 ---' AS info;
SELECT 'sys_dept'           AS table_name, COUNT(*) AS cnt FROM sys_dept           WHERE id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_post',                         COUNT(*)        FROM sys_post           WHERE id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_role',                         COUNT(*)        FROM sys_role           WHERE id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_role_data',                    COUNT(*)        FROM sys_role_data      WHERE id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_user',                         COUNT(*)        FROM sys_user           WHERE id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_user_role',                    COUNT(*)        FROM sys_user_role      WHERE user_id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_user_post',                    COUNT(*)        FROM sys_user_post      WHERE user_id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_menu',                         COUNT(*)        FROM sys_menu           WHERE id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_role_menu',                    COUNT(*)        FROM sys_role_menu      WHERE role_id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_dict_type',                    COUNT(*)        FROM sys_dict_type      WHERE id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_dict_data',                    COUNT(*)        FROM sys_dict_data      WHERE id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_code_rule',                    COUNT(*)        FROM sys_code_rule      WHERE id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_category',                     COUNT(*)        FROM sys_category       WHERE id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_rent_algorithm',               COUNT(*)        FROM sys_rent_algorithm WHERE id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_rent_algo_step',               COUNT(*)        FROM sys_rent_algo_step WHERE id BETWEEN 91001 AND 91199
UNION ALL
SELECT 'sys_config',                       COUNT(*)        FROM sys_config         WHERE id BETWEEN 91001 AND 91199;
