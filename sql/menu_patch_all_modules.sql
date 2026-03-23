-- ============================================================
-- 菜单补丁：补全所有业务模块菜单数据
-- 执行前提: system_init.sql 已执行
-- 模块覆盖: 工作台、基础数据管理、招商管理、营运管理、
--           财务管理、报表管理、系统管理（补全）
-- ============================================================

USE asset_db;
SET NAMES utf8mb4;

-- ============================================================
-- 1. 工作台
-- ============================================================
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order) VALUES
(1,  0, '工作台', 'C', '/dashboard', 'dashboard/index', '', 'Monitor', 0);

-- ============================================================
-- 2. 基础数据管理
-- ============================================================
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order) VALUES
-- 目录
(10,  0,  '基础数据管理', 'M', '/base',                    '',                          '',                   'Grid',          1),
-- 菜单
(11,  10, '项目管理',     'C', '/base/projects',           'base/project/index',        'base:project:list',  'OfficeBuilding', 1),
(12,  10, '楼栋管理',     'C', '/base/buildings',          'base/building/index',       'base:building:list', 'House',          2),
(13,  10, '楼层管理',     'C', '/base/floors',             'base/floor/index',          'base:floor:list',    'Management',     3),
(14,  10, '商铺管理',     'C', '/base/shops',              'base/shop/index',           'base:shop:list',     'Shop',           4),
(15,  10, '品牌管理',     'C', '/base/brands',             'base/brand/index',          'base:brand:list',    'Star',           5),
(16,  10, '商家管理',     'C', '/base/merchants',          'base/merchant/index',       'base:merchant:list', 'User',           6),
(17,  10, '通知公告',     'C', '/base/notices',            'base/notice/index',         'base:notice:list',   'Bell',           7),
(18,  10, '新闻资讯',     'C', '/base/news',               'base/news/index',           'base:news:list',     'Document',       8),
-- 按钮权限
(111, 11, '新增项目', 'F', '', '', 'base:project:create', '', 1),
(112, 11, '编辑项目', 'F', '', '', 'base:project:update', '', 2),
(113, 11, '删除项目', 'F', '', '', 'base:project:delete', '', 3),
(121, 12, '新增楼栋', 'F', '', '', 'base:building:create', '', 1),
(122, 12, '编辑楼栋', 'F', '', '', 'base:building:update', '', 2),
(123, 12, '删除楼栋', 'F', '', '', 'base:building:delete', '', 3),
(131, 13, '新增楼层', 'F', '', '', 'base:floor:create', '', 1),
(132, 13, '编辑楼层', 'F', '', '', 'base:floor:update', '', 2),
(133, 13, '删除楼层', 'F', '', '', 'base:floor:delete', '', 3),
(141, 14, '新增商铺', 'F', '', '', 'base:shop:create',  '', 1),
(142, 14, '编辑商铺', 'F', '', '', 'base:shop:update',  '', 2),
(143, 14, '删除商铺', 'F', '', '', 'base:shop:delete',  '', 3),
(144, 14, '导入商铺', 'F', '', '', 'base:shop:import',  '', 4),
(151, 15, '新增品牌', 'F', '', '', 'base:brand:create', '', 1),
(152, 15, '编辑品牌', 'F', '', '', 'base:brand:update', '', 2),
(153, 15, '删除品牌', 'F', '', '', 'base:brand:delete', '', 3),
(154, 15, '导入品牌', 'F', '', '', 'base:brand:import', '', 4),
(161, 16, '新增商家', 'F', '', '', 'base:merchant:create', '', 1),
(162, 16, '编辑商家', 'F', '', '', 'base:merchant:update', '', 2),
(163, 16, '删除商家', 'F', '', '', 'base:merchant:delete', '', 3),
(164, 16, '审核商家', 'F', '', '', 'base:merchant:audit',  '', 4),
(171, 17, '新增公告', 'F', '', '', 'base:notice:create', '', 1),
(172, 17, '编辑公告', 'F', '', '', 'base:notice:update', '', 2),
(173, 17, '删除公告', 'F', '', '', 'base:notice:delete', '', 3),
(181, 18, '新增资讯', 'F', '', '', 'base:news:create', '', 1),
(182, 18, '编辑资讯', 'F', '', '', 'base:news:update', '', 2),
(183, 18, '删除资讯', 'F', '', '', 'base:news:delete', '', 3);

-- ============================================================
-- 3. 招商管理
-- ============================================================
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order) VALUES
-- 目录
(20,  0,  '招商管理',   'M', '/inv',                       '',                               '',                    'Briefcase',  2),
-- 菜单
(21,  20, '计租方案',   'C', '/inv/config/rent-schemes',   'inv/config/rent-scheme/index',   'inv:rentScheme:list', 'Setting',    1),
(22,  20, '收款费项',   'C', '/inv/config/fee-items',      'inv/config/fee-item/index',      'inv:feeItem:list',   'Coin',       2),
(23,  20, '意向协议',   'C', '/inv/intentions',            'inv/intention/index',            'inv:intention:list', 'EditPen',    3),
(24,  20, '招商合同',   'C', '/inv/contracts',             'inv/contract/index',             'inv:contract:list',  'Tickets',    4),
(25,  20, '开业审批',   'C', '/inv/opening-approvals',     'inv/opening-approval/index',     'inv:opening:list',   'Checked',    5),
(26,  20, '租决政策',   'C', '/inv/rent-policies',         'inv/rent-policy/index',          'inv:rentPolicy:list','DataLine',   6),
(27,  20, '租金分解',   'C', '/inv/rent-decomps',          'inv/rent-decomp/index',          'inv:rentDecomp:list','PieChart',   7),
-- 按钮权限
(211, 21, '新增方案', 'F', '', '', 'inv:rentScheme:create', '', 1),
(212, 21, '编辑方案', 'F', '', '', 'inv:rentScheme:update', '', 2),
(213, 21, '删除方案', 'F', '', '', 'inv:rentScheme:delete', '', 3),
(221, 22, '新增费项', 'F', '', '', 'inv:feeItem:create', '', 1),
(222, 22, '编辑费项', 'F', '', '', 'inv:feeItem:update', '', 2),
(223, 22, '删除费项', 'F', '', '', 'inv:feeItem:delete', '', 3),
(231, 23, '新增意向', 'F', '', '', 'inv:intention:create', '', 1),
(232, 23, '编辑意向', 'F', '', '', 'inv:intention:update', '', 2),
(233, 23, '删除意向', 'F', '', '', 'inv:intention:delete', '', 3),
(234, 23, '转为合同', 'F', '', '', 'inv:intention:convert', '', 4),
(241, 24, '新增合同', 'F', '', '', 'inv:contract:create', '', 1),
(242, 24, '编辑合同', 'F', '', '', 'inv:contract:update', '', 2),
(243, 24, '删除合同', 'F', '', '', 'inv:contract:delete', '', 3),
(251, 25, '新增审批', 'F', '', '', 'inv:opening:create', '', 1),
(252, 25, '审批操作', 'F', '', '', 'inv:opening:approve', '', 2),
(261, 26, '新增政策', 'F', '', '', 'inv:rentPolicy:create', '', 1),
(262, 26, '编辑政策', 'F', '', '', 'inv:rentPolicy:update', '', 2),
(263, 26, '删除政策', 'F', '', '', 'inv:rentPolicy:delete', '', 3),
(271, 27, '新增分解', 'F', '', '', 'inv:rentDecomp:create', '', 1),
(272, 27, '编辑分解', 'F', '', '', 'inv:rentDecomp:update', '', 2),
(273, 27, '删除分解', 'F', '', '', 'inv:rentDecomp:delete', '', 3);

-- ============================================================
-- 4. 营运管理
-- ============================================================
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order) VALUES
-- 目录
(30,  0,  '营运管理',       'M', '/opr',                         '',                                '',                       'Operation',   3),
-- 菜单
(31,  30, '合同台账',       'C', '/opr/ledgers',                 'opr/ledger/index',                'opr:ledger:list',        'Memo',        1),
(32,  30, '合同到期预警',   'C', '/opr/alerts/contract-expiry',  'opr/alerts/contract-expiry',      'opr:alert:list',         'Warning',     2),
(33,  30, '合同变更',       'C', '/opr/contract-changes',        'opr/change/index',                'opr:change:list',        'Switch',      3),
(34,  30, '营收填报',       'C', '/opr/revenue-reports',         'opr/revenue/index',               'opr:revenue:list',       'TrendCharts', 4),
(35,  30, '浮动租金',       'C', '/opr/floating-rent',           'opr/revenue/floating-rent',       'opr:floatRent:list',     'DataLine',    5),
(36,  30, '客流填报',       'C', '/opr/passenger-flows',         'opr/flow/index',                  'opr:flow:list',          'User',        6),
(37,  30, '合同解约',       'C', '/opr/terminations',            'opr/termination/index',           'opr:termination:list',   'CircleClose', 7),
-- 按钮权限
(311, 31, '查看台账', 'F', '', '', 'opr:ledger:query',      '', 1),
(321, 32, '查看预警', 'F', '', '', 'opr:alert:query',       '', 1),
(331, 33, '新增变更', 'F', '', '', 'opr:change:create',     '', 1),
(332, 33, '编辑变更', 'F', '', '', 'opr:change:update',     '', 2),
(333, 33, '删除变更', 'F', '', '', 'opr:change:delete',     '', 3),
(334, 33, '审批变更', 'F', '', '', 'opr:change:approve',    '', 4),
(341, 34, '新增填报', 'F', '', '', 'opr:revenue:create',    '', 1),
(342, 34, '编辑填报', 'F', '', '', 'opr:revenue:update',    '', 2),
(343, 34, '删除填报', 'F', '', '', 'opr:revenue:delete',    '', 3),
(361, 36, '新增客流', 'F', '', '', 'opr:flow:create',       '', 1),
(362, 36, '编辑客流', 'F', '', '', 'opr:flow:update',       '', 2),
(371, 37, '新增解约', 'F', '', '', 'opr:termination:create', '', 1),
(372, 37, '审批解约', 'F', '', '', 'opr:termination:approve','', 2),
(373, 37, '删除解约', 'F', '', '', 'opr:termination:delete', '', 3);

-- ============================================================
-- 5. 财务管理
-- ============================================================
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order) VALUES
-- 目录
(40,  0,  '财务管理',   'M', '/fin',                '',                         '',                      'Money',         4),
-- 菜单
(41,  40, '财务看板',   'C', '/fin/dashboard',      'fin/dashboard/index',      'fin:dashboard:query',   'DataAnalysis',  1),
(42,  40, '应收管理',   'C', '/fin/receivables',    'fin/receivable/index',     'fin:receivable:list',   'List',          2),
(43,  40, '收款管理',   'C', '/fin/receipts',       'fin/receipt/index',        'fin:receipt:list',      'CreditCard',    3),
(44,  40, '核销管理',   'C', '/fin/write-offs',     'fin/write-off/index',      'fin:writeOff:list',     'CircleCheck',   4),
(45,  40, '凭证管理',   'C', '/fin/vouchers',       'fin/voucher/index',        'fin:voucher:list',      'Postcard',      5),
(46,  40, '保证金管理', 'C', '/fin/deposits',       'fin/deposit/index',        'fin:deposit:list',      'Wallet',        6),
(47,  40, '预收款管理', 'C', '/fin/prepayments',    'fin/prepayment/index',     'fin:prepayment:list',   'CollectionTag', 7),
-- 按钮权限
(421, 42, '新增应收', 'F', '', '', 'fin:receivable:create', '', 1),
(422, 42, '编辑应收', 'F', '', '', 'fin:receivable:update', '', 2),
(423, 42, '删除应收', 'F', '', '', 'fin:receivable:delete', '', 3),
(431, 43, '新增收款', 'F', '', '', 'fin:receipt:create',    '', 1),
(432, 43, '编辑收款', 'F', '', '', 'fin:receipt:update',    '', 2),
(433, 43, '删除收款', 'F', '', '', 'fin:receipt:delete',    '', 3),
(441, 44, '新增核销', 'F', '', '', 'fin:writeOff:create',   '', 1),
(442, 44, '删除核销', 'F', '', '', 'fin:writeOff:delete',   '', 2),
(451, 45, '新增凭证', 'F', '', '', 'fin:voucher:create',    '', 1),
(452, 45, '删除凭证', 'F', '', '', 'fin:voucher:delete',    '', 2),
(461, 46, '存入保证金', 'F', '', '', 'fin:deposit:create',   '', 1),
(462, 46, '退还保证金', 'F', '', '', 'fin:deposit:refund',   '', 2),
(471, 47, '存入预收款', 'F', '', '', 'fin:prepayment:create', '', 1),
(472, 47, '退还预收款', 'F', '', '', 'fin:prepayment:refund', '', 2);

-- ============================================================
-- 6. 报表管理
-- ============================================================
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order) VALUES
-- 目录
(50,  0,  '报表管理',       'M', '/rpt',                       '',                             '',                    'DataAnalysis', 5),
-- 通用
(51,  50, '报表中心',       'C', '/rpt/home',                  'rpt/home',                     'rpt:home:query',      'HomeFilled',   1),
-- 资产类报表
(52,  50, '资产数据看板',   'C', '/rpt/asset/dashboard',       'rpt/asset/dashboard',          'rpt:asset:dashboard', 'DataAnalysis', 2),
(53,  50, '空置率统计',     'C', '/rpt/asset/vacancy',         'rpt/asset/vacancy',            'rpt:asset:vacancy',   'TrendCharts',  3),
(54,  50, '出租率/开业率',  'C', '/rpt/asset/rates',           'rpt/asset/rates',              'rpt:asset:rates',     'DataLine',     4),
(55,  50, '品牌业态分布',   'C', '/rpt/asset/brand-dist',      'rpt/asset/brand-dist',         'rpt:asset:brandDist', 'PieChart',     5),
(56,  50, '商铺租赁信息',   'C', '/rpt/asset/shop-rental',     'rpt/asset/shop-rental',        'rpt:asset:shopRental','Shop',         6),
-- 招商类报表
(57,  50, '招商数据看板',   'C', '/rpt/inv/dashboard',         'rpt/inv/dashboard',            'rpt:inv:dashboard',   'DataAnalysis', 7),
(58,  50, '客户漏斗分析',   'C', '/rpt/inv/funnel',            'rpt/inv/funnel',               'rpt:inv:funnel',      'Filter',       8),
(59,  50, '招商业绩对比',   'C', '/rpt/inv/performance',       'rpt/inv/performance',          'rpt:inv:performance', 'TrendCharts',  9),
(60,  50, '租金水平分析',   'C', '/rpt/inv/rent-level',        'rpt/inv/rent-level',           'rpt:inv:rentLevel',   'Money',        10),
-- 营运类报表
(61,  50, '营运数据看板',   'C', '/rpt/opr/dashboard',         'rpt/opr/dashboard',            'rpt:opr:dashboard',   'DataAnalysis', 11),
(62,  50, '营收汇总分析',   'C', '/rpt/opr/revenue',           'rpt/opr/revenue',              'rpt:opr:revenue',     'TrendCharts',  12),
(63,  50, '合同变更分析',   'C', '/rpt/opr/changes',           'rpt/opr/changes',              'rpt:opr:changes',     'Document',     13),
(64,  50, '地区业务对比',   'C', '/rpt/opr/region-compare',    'rpt/opr/region-compare',       'rpt:opr:region',      'MapLocation',  14),
-- 财务类报表
(65,  50, '财务数据看板',   'C', '/rpt/fin/dashboard',         'rpt/fin/dashboard',            'rpt:fin:dashboard',   'DataAnalysis', 15),
(66,  50, '欠款统计分析',   'C', '/rpt/fin/outstanding',       'rpt/fin/outstanding',          'rpt:fin:outstanding', 'TrendCharts',  16),
(67,  50, '账龄分析',       'C', '/rpt/fin/aging',             'rpt/fin/aging',                'rpt:fin:aging',       'DataLine',     17),
(68,  50, '收缴率趋势',     'C', '/rpt/fin/collection',        'rpt/fin/collection',           'rpt:fin:collection',  'Histogram',    18),
-- 定时推送
(69,  50, '定时推送管理',   'C', '/rpt/schedule',              'rpt/schedule/index',           'rpt:schedule:list',   'Timer',        19);

-- ============================================================
-- 7. 系统管理 - 补全缺失菜单项
--    （100-107 已在 system_init.sql 中存在，补充 108-113）
-- ============================================================
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort_order) VALUES
(108, 100, '编码规则',   'C', '/sys/code-rules',    'sys/code/index',       'sys:codeRule:list',    'Tickets',      8),
(109, 100, '分类管理',   'C', '/sys/categories',   'sys/category/index',   'sys:category:list',    'FolderOpened', 9),
(110, 100, '租费算法',   'C', '/sys/fee-algorithms','sys/algorithm/index',  'sys:algorithm:list',   'Coin',         10),
-- 注意：180/185/186 避开了 基础数据管理按钮 id 段（111-183）
(180, 100, '系统配置',   'C', '/sys/config',        'sys/config/index',     'sys:config:list',      'Setting',      11),
(185, 100, '扩展字段',   'C', '/sys/ext-fields',    'sys/ext-field/index',  'sys:extField:list',    'Setting',      12),
(186, 100, '在线用户',   'C', '/sys/online',         'sys/online/index',     'sys:online:list',      'Monitor',      13),
-- 按钮权限
(1081, 108, '新增规则', 'F', '', '', 'sys:codeRule:create',  '', 1),
(1082, 108, '编辑规则', 'F', '', '', 'sys:codeRule:update',  '', 2),
(1083, 108, '删除规则', 'F', '', '', 'sys:codeRule:delete',  '', 3),
(1091, 109, '新增分类', 'F', '', '', 'sys:category:create',  '', 1),
(1092, 109, '编辑分类', 'F', '', '', 'sys:category:update',  '', 2),
(1093, 109, '删除分类', 'F', '', '', 'sys:category:delete',  '', 3),
(1101, 110, '新增算法', 'F', '', '', 'sys:algorithm:create', '', 1),
(1102, 110, '编辑算法', 'F', '', '', 'sys:algorithm:update', '', 2),
(1103, 110, '删除算法', 'F', '', '', 'sys:algorithm:delete', '', 3),
(1801, 180, '修改配置', 'F', '', '', 'sys:config:update',    '', 1),
(1851, 185, '新增字段', 'F', '', '', 'sys:extField:create',  '', 1),
(1852, 185, '编辑字段', 'F', '', '', 'sys:extField:update',  '', 2),
(1853, 185, '删除字段', 'F', '', '', 'sys:extField:delete',  '', 3),
(1861, 186, '强制下线', 'F', '', '', 'sys:online:kickout',   '', 1);

-- ============================================================
-- 8. 超级管理员授权所有新增菜单
-- ============================================================
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu
WHERE id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);

-- ============================================================
-- 验证
-- ============================================================
SELECT
    CASE m.parent_id WHEN 0 THEN '【目录】' ELSE '  └─' END AS level_mark,
    m.id,
    m.menu_name,
    m.menu_type,
    m.path,
    m.sort_order
FROM sys_menu m
WHERE m.menu_type IN ('M','C')
ORDER BY
    CASE WHEN m.parent_id = 0 THEN m.id ELSE m.parent_id END,
    m.parent_id,
    m.sort_order;
