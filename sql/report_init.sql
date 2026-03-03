-- ==================================================
-- 报表管理模块数据库初始化脚本
-- 模块: asset-report (端口: 8005)
-- 版本: v1.0.0
-- 创建时间: 2026-03-02
-- 说明:
--   1. 创建12张 rpt_* 报表层数据表（5张事实表 + 4张配置表 + 3张运营支撑表）
--   2. 建立覆盖索引（高频"项目+时间"查询场景）
--   3. 初始化44条 rpt_config 报表配置基础数据
--   4. 初始化主要P0报表的维度/指标子表数据
--   5. 初始化核心报表的钻取路径配置
-- 唯一键约定:
--   - 事实表去除NULL歧义，用 0 替代NULL的数值ID，空串 替代NULL的字符串
--   - 配置表采用 (code, version, is_deleted) 复合唯一索引，支持多版本并存
-- ==================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE asset_report;

-- ==================================================
-- 一、事实表（5张）
-- ==================================================

-- 1. 资产日汇总表
-- ETL每日T+1更新，汇总粒度：项目/楼栋/楼层/业态
-- 聚合层级约定：building_id=0 表示项目级汇总，floor_id=0 表示楼栋级汇总，format_type='' 表示全业态汇总
DROP TABLE IF EXISTS `rpt_asset_daily`;
CREATE TABLE `rpt_asset_daily` (
  `id`               bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date`        date          NOT NULL                COMMENT '统计日期（ETL日期=T-1）',
  `project_id`       bigint        NOT NULL                COMMENT '项目ID',
  `building_id`      bigint        NOT NULL DEFAULT '0'    COMMENT '楼栋ID（0=项目级汇总）',
  `floor_id`         bigint        NOT NULL DEFAULT '0'    COMMENT '楼层ID（0=楼栋级汇总）',
  `format_type`      varchar(100)  NOT NULL DEFAULT ''     COMMENT '业态类型（空串=全业态汇总）',

  -- 商铺数量指标
  `total_shops`      int           NOT NULL DEFAULT '0'    COMMENT '商铺总数',
  `rented_shops`     int           NOT NULL DEFAULT '0'    COMMENT '已租商铺数（合同状态=已签约）',
  `vacant_shops`     int           NOT NULL DEFAULT '0'    COMMENT '空置商铺数（合同状态=未出租）',
  `decorating_shops` int           NOT NULL DEFAULT '0'    COMMENT '装修中商铺数（合同状态=装修期）',
  `opened_shops`     int           NOT NULL DEFAULT '0'    COMMENT '已开业商铺数（实际经营=营业中）',

  -- 面积指标（DECIMAL(14,2)统一标准）
  `total_area`       decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '总面积（㎡）',
  `rented_area`      decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '已租面积（㎡）',
  `vacant_area`      decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '空置面积（㎡）',
  `decoration_area`  decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '装修中面积（㎡）',

  -- 比率指标
  `vacancy_rate`     decimal(5,2)  NOT NULL DEFAULT '0.00' COMMENT '空置率（%）= vacant_area/total_area*100',
  `rental_rate`      decimal(5,2)  NOT NULL DEFAULT '0.00' COMMENT '出租率（%）= rented_area/total_area*100',
  `opening_rate`     decimal(5,2)  NOT NULL DEFAULT '0.00' COMMENT '开业率（%）= opened_shops/total_shops*100',

  -- 审计字段
  `created_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '创建人ID',
  `created_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `updated_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '更新人ID',
  `updated_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  tinyint(1)  NOT NULL DEFAULT '0'                              COMMENT '逻辑删除（0:否,1:是）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_date_prj_bld_flr_fmt` (`stat_date`,`project_id`,`building_id`,`floor_id`,`format_type`),
  KEY `idx_stat_date`       (`stat_date`),
  KEY `idx_project_id`      (`project_id`),
  KEY `idx_building_floor`  (`building_id`,`floor_id`),
  -- 覆盖索引：支持"项目+时间范围+核心三率"高频看板查询
  KEY `idx_proj_date_rates` (`project_id`,`stat_date`,`vacancy_rate`,`rental_rate`,`opening_rate`,`total_area`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产日汇总表（ETL每日T+1更新）';


-- 2. 招商日汇总表
-- ETL每日T+1更新，汇总粒度：项目/业态/招商负责人
-- 聚合层级约定：format_type='' 表示全业态，investment_manager_id=0 表示全员
DROP TABLE IF EXISTS `rpt_investment_daily`;
CREATE TABLE `rpt_investment_daily` (
  `id`                    bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date`             date          NOT NULL                COMMENT '统计日期',
  `project_id`            bigint        NOT NULL                COMMENT '项目ID',
  `format_type`           varchar(100)  NOT NULL DEFAULT ''     COMMENT '业态类型（空串=全业态汇总）',
  `investment_manager_id` bigint        NOT NULL DEFAULT '0'    COMMENT '招商负责人ID（0=全员汇总）',

  -- 意向指标
  `intention_count`  int  NOT NULL DEFAULT '0' COMMENT '意向协议数（累计有效）',
  `intention_signed` int  NOT NULL DEFAULT '0' COMMENT '已签意向数（已缴意向金）',
  `new_intention`    int  NOT NULL DEFAULT '0' COMMENT '当日新增意向',

  -- 合同指标
  `contract_count`   int           NOT NULL DEFAULT '0'    COMMENT '租赁合同数（累计有效）',
  `contract_amount`  decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '合同总金额（元，合同期总应收）',
  `contract_area`    decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '签约面积（㎡）',
  `new_contract`     int           NOT NULL DEFAULT '0'    COMMENT '当日新增合同',

  -- 转化指标
  `conversion_rate`  decimal(5,2)  NOT NULL DEFAULT '0.00' COMMENT '意向转化率（%）= contract_count/intention_count*100',
  `avg_rent_price`   decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '平均租金单价（元/㎡/月）',

  -- 审计字段
  `created_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '创建人ID',
  `created_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `updated_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '更新人ID',
  `updated_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  tinyint(1)  NOT NULL DEFAULT '0'                              COMMENT '逻辑删除（0:否,1:是）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_date_prj_fmt_mgr`     (`stat_date`,`project_id`,`format_type`,`investment_manager_id`),
  KEY `idx_stat_date`                      (`stat_date`),
  KEY `idx_project_id`                     (`project_id`),
  KEY `idx_investment_manager`             (`investment_manager_id`),
  -- 覆盖索引：支持"转化率排序"高频查询
  KEY `idx_proj_date_conversion`           (`project_id`,`stat_date`,`conversion_rate`,`contract_amount`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招商日汇总表（ETL每日T+1更新）';


-- 3. 营运月汇总表
-- ETL每月T+1更新，汇总粒度：项目/楼栋/业态
-- 聚合层级约定：building_id=0 表示项目级，format_type='' 表示全业态
DROP TABLE IF EXISTS `rpt_operation_monthly`;
CREATE TABLE `rpt_operation_monthly` (
  `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_month`  varchar(7)    NOT NULL                COMMENT '统计月份（YYYY-MM格式）',
  `project_id`  bigint        NOT NULL                COMMENT '项目ID',
  `building_id` bigint        NOT NULL DEFAULT '0'    COMMENT '楼栋ID（0=项目级汇总）',
  `format_type` varchar(100)  NOT NULL DEFAULT ''     COMMENT '业态类型（空串=全业态汇总）',

  -- 营收指标
  `revenue_amount`       decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '月营收总额（元）',
  `floating_rent_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '浮动租金总额（元）',
  `avg_revenue_per_sqm`  decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '坪效（元/㎡）',

  -- 客流指标
  `passenger_flow`       bigint NOT NULL DEFAULT '0' COMMENT '月客流总量（人次）',
  `avg_daily_passenger`  int    NOT NULL DEFAULT '0' COMMENT '日均客流（人次）',

  -- 合同变动指标
  `change_count`         int           NOT NULL DEFAULT '0'    COMMENT '合同变更次数',
  `change_rent_impact`   decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '变更租金影响额（元，正增负减）',
  `expiring_contracts`   int           NOT NULL DEFAULT '0'    COMMENT '即将到期合同数（90天内）',
  `terminated_contracts` int           NOT NULL DEFAULT '0'    COMMENT '本月解约合同数',

  -- 审计字段
  `created_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '创建人ID',
  `created_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `updated_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '更新人ID',
  `updated_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  tinyint(1)  NOT NULL DEFAULT '0'                              COMMENT '逻辑删除（0:否,1:是）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_opr_month_prj_bld_fmt` (`stat_month`,`project_id`,`building_id`,`format_type`),
  KEY `idx_stat_month`  (`stat_month`),
  KEY `idx_project_id`  (`project_id`),
  KEY `idx_building_id` (`building_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营运月汇总表（ETL每月T+1更新）';


-- 4. 财务月汇总表
-- ETL每月T+1更新，汇总粒度：项目/费项
-- 聚合层级约定：fee_item_id=0 表示所有费项汇总
DROP TABLE IF EXISTS `rpt_finance_monthly`;
CREATE TABLE `rpt_finance_monthly` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_month`    varchar(7)    NOT NULL                COMMENT '统计月份（YYYY-MM格式）',
  `project_id`    bigint        NOT NULL                COMMENT '项目ID',
  `fee_item_id`   bigint        NOT NULL DEFAULT '0'    COMMENT '费项ID（0=所有费项汇总）',
  `fee_item_type` varchar(50)   NOT NULL DEFAULT ''     COMMENT '费项类型（租金/物业费/推广费等）',

  -- 应收实收
  `receivable_amount`  decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '应收总额（元，权责发生制当月应收）',
  `received_amount`    decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '已收总额（元，实收含往期补缴）',
  `outstanding_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '欠款总额（元）= receivable - received',

  -- 调整减免
  `deduction_amount`   decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '减免总额（元，审批通过的减免）',
  `adjustment_amount`  decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '调整总额（元，账务调整）',

  -- 逾期指标
  `overdue_amount`     decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '逾期总额（元，超付款期限的欠款）',
  `overdue_rate`       decimal(5,2)  NOT NULL DEFAULT '0.00' COMMENT '逾期率（%）= overdue/receivable*100',

  -- 资金余额（月末快照）
  `deposit_balance`    decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '保证金余额（元，月末在押）',
  `prepay_balance`     decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '预收款余额（元，月末余额）',

  -- 计算指标
  `collection_rate`    decimal(5,2)  NOT NULL DEFAULT '0.00' COMMENT '收缴率（%）= received/receivable*100',

  -- 审计字段
  `created_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '创建人ID',
  `created_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `updated_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '更新人ID',
  `updated_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  tinyint(1)  NOT NULL DEFAULT '0'                              COMMENT '逻辑删除（0:否,1:是）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fin_month_prj_fee`  (`stat_month`,`project_id`,`fee_item_id`),
  KEY `idx_stat_month`   (`stat_month`),
  KEY `idx_project_id`   (`project_id`),
  KEY `idx_fee_item_id`  (`fee_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务月汇总表（ETL每月T+1更新）';


-- 5. 账龄分析表
-- ETL每日T+1预计算，汇总粒度：项目/商家/合同/费项
-- 聚合层级约定：fee_item_id=0 表示所有费项汇总
DROP TABLE IF EXISTS `rpt_aging_analysis`;
CREATE TABLE `rpt_aging_analysis` (
  `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date`   date   NOT NULL               COMMENT '统计日期（ETL日期=T-1）',
  `project_id`  bigint NOT NULL               COMMENT '项目ID',
  `merchant_id` bigint NOT NULL               COMMENT '商家ID',
  `contract_id` bigint NOT NULL               COMMENT '合同ID',
  `fee_item_id` bigint NOT NULL DEFAULT '0'   COMMENT '费项ID（0=所有费项汇总）',

  -- 账龄分档（DECIMAL(14,2)统一标准）
  `within_30`       decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '30天内欠款（元）',
  `days_31_60`      decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '31-60天欠款（元）',
  `days_61_90`      decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '61-90天欠款（元）',
  `days_91_180`     decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '91-180天欠款（元）',
  `days_181_365`    decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '181-365天欠款（元）',
  `over_365`        decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '365天以上欠款（元）',
  `total_outstanding` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '欠款合计（元）',

  -- 审计字段
  `created_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '创建人ID',
  `created_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `updated_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '更新人ID',
  `updated_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  tinyint(1)  NOT NULL DEFAULT '0'                              COMMENT '逻辑删除（0:否,1:是）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aging_date_prj_mer_con_fee` (`stat_date`,`project_id`,`merchant_id`,`contract_id`,`fee_item_id`),
  KEY `idx_stat_date`          (`stat_date`),
  KEY `idx_project_id`         (`project_id`),
  KEY `idx_merchant_id`        (`merchant_id`),
  KEY `idx_contract_id`        (`contract_id`),
  KEY `idx_total_outstanding`  (`total_outstanding`),
  -- 覆盖索引：支持"项目+日期+账龄"快速统计，用于看板排行榜
  KEY `idx_proj_date_aging`    (`project_id`,`stat_date`,`total_outstanding`,`over_365`,`days_181_365`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账龄分析表（ETL每日T+1预计算）';


-- ==================================================
-- 二、报表配置表（4张）
-- ==================================================

-- 6. 报表配置主表
-- 存储44个报表/功能的元数据，支持多版本并存（uk含version+is_deleted）
DROP TABLE IF EXISTS `rpt_config`;
CREATE TABLE `rpt_config` (
  `id`               bigint        NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
  `report_code`      varchar(50)   NOT NULL                 COMMENT '报表编码（全局唯一，如AST_VACANCY_RATE）',
  `version`          int           NOT NULL DEFAULT '1'     COMMENT '版本号（修改后递增，旧版本逻辑删除）',
  `report_name`      varchar(200)  NOT NULL                 COMMENT '报表名称',
  `report_category`  tinyint(1)    NOT NULL                 COMMENT '分类（1:资产,2:招商,3:营运,4:财务,5:通用）',
  `report_type`      varchar(20)   NOT NULL DEFAULT 'TABLE' COMMENT '报表类型（TABLE/CHART/DASHBOARD）',
  `priority`         tinyint(1)    NOT NULL DEFAULT '0'     COMMENT '优先级（0:P0,1:P1,2:P2）',

  `data_source`        varchar(200) DEFAULT NULL COMMENT '数据来源表/视图（多表用逗号分隔）',
  `default_dimensions` json         DEFAULT NULL COMMENT '默认维度字段列表（JSON数组）',
  `default_metrics`    json         DEFAULT NULL COMMENT '默认指标字段列表（JSON数组）',
  `chart_type`         varchar(50)  DEFAULT NULL COMMENT '图表类型（LINE/BAR/PIE/FUNNEL/RADAR/HEATMAP/DASHBOARD）',
  `refresh_cron`       varchar(50)  DEFAULT NULL COMMENT '数据刷新Cron（NULL=实时查询）',

  `enable_drill`   tinyint(1) NOT NULL DEFAULT '1' COMMENT '启用钻取（0:否,1:是）',
  `enable_export`  tinyint(1) NOT NULL DEFAULT '1' COMMENT '启用导出（0:否,1:是）',
  `sort_order`     int        NOT NULL DEFAULT '0'  COMMENT '显示排序（同分类内升序）',
  `status`         tinyint(1) NOT NULL DEFAULT '1'  COMMENT '状态（0:禁用,1:启用）',
  `remark`         varchar(500) DEFAULT NULL         COMMENT '备注说明',

  -- 审计字段
  `created_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '创建人ID',
  `created_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `updated_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '更新人ID',
  `updated_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  tinyint(1)  NOT NULL DEFAULT '0'                              COMMENT '逻辑删除（0:否,1:是）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_code_version_deleted` (`report_code`,`version`,`is_deleted`),
  KEY `idx_report_category` (`report_category`),
  KEY `idx_priority`        (`priority`),
  KEY `idx_status`          (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表配置主表（44条基础报表定义）';


-- 7. 报表维度配置子表
DROP TABLE IF EXISTS `rpt_config_dimension`;
CREATE TABLE `rpt_config_dimension` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id`       bigint       NOT NULL               COMMENT '报表配置ID（关联rpt_config.id）',
  `dimension_code`  varchar(50)  NOT NULL               COMMENT '维度编码（如PROJECT/STAT_DATE/FORMAT_TYPE）',
  `version`         int          NOT NULL DEFAULT '1'   COMMENT '版本号',
  `dimension_name`  varchar(100) NOT NULL               COMMENT '维度名称（显示用）',
  `field_name`      varchar(50)  NOT NULL               COMMENT '对应数据库字段名',
  `data_type`       varchar(20)  NOT NULL DEFAULT 'STRING' COMMENT '数据类型（STRING/DATE/NUMBER）',
  `sort_order`      int          NOT NULL DEFAULT '0'   COMMENT '显示排序',
  `is_default_show` tinyint(1)   NOT NULL DEFAULT '1'   COMMENT '是否默认显示（0:否,1:是）',

  -- 审计字段
  `created_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '创建人ID',
  `created_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `updated_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '更新人ID',
  `updated_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  tinyint(1)  NOT NULL DEFAULT '0'                              COMMENT '逻辑删除（0:否,1:是）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dim_report_code_ver` (`report_id`,`dimension_code`,`version`,`is_deleted`),
  KEY `idx_report_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表维度配置子表';


-- 8. 报表指标配置子表
DROP TABLE IF EXISTS `rpt_config_metric`;
CREATE TABLE `rpt_config_metric` (
  `id`                bigint       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
  `report_id`         bigint       NOT NULL                 COMMENT '报表配置ID（关联rpt_config.id）',
  `metric_code`       varchar(50)  NOT NULL                 COMMENT '指标编码（如VACANCY_RATE/TOTAL_SHOPS）',
  `version`           int          NOT NULL DEFAULT '1'     COMMENT '版本号',
  `metric_name`       varchar(100) NOT NULL                 COMMENT '指标名称（显示用）',
  `field_name`        varchar(100) NOT NULL                 COMMENT '对应字段名或计算表达式',
  `calculation_logic` text         DEFAULT NULL             COMMENT '计算逻辑说明（如"vacant_area/total_area*100"）',
  `data_type`         varchar(20)  NOT NULL DEFAULT 'DECIMAL' COMMENT '数据类型（DECIMAL/INTEGER/PERCENT/TEXT）',
  `unit`              varchar(20)  DEFAULT NULL             COMMENT '单位（元/㎡/%/人次/间）',
  `sort_order`        int          NOT NULL DEFAULT '0'     COMMENT '显示排序',
  `is_default_show`   tinyint(1)   NOT NULL DEFAULT '1'     COMMENT '是否默认显示（0:否,1:是）',
  `is_calculated`     tinyint(1)   NOT NULL DEFAULT '0'     COMMENT '是否计算字段（0:直接字段,1:需二次计算）',

  -- 审计字段
  `created_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '创建人ID',
  `created_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `updated_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '更新人ID',
  `updated_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  tinyint(1)  NOT NULL DEFAULT '0'                              COMMENT '逻辑删除（0:否,1:是）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_metric_report_code_ver` (`report_id`,`metric_code`,`version`,`is_deleted`),
  KEY `idx_report_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表指标配置子表';


-- 9. 钻取配置表
-- 定义各报表的下钻路径，由CMN_DRILL_DOWN接口动态解析
DROP TABLE IF EXISTS `rpt_drill_config`;
CREATE TABLE `rpt_drill_config` (
  `id`                 bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id`          bigint NOT NULL               COMMENT '源报表ID（关联rpt_config.id）',
  `version`            int    NOT NULL DEFAULT '1'   COMMENT '版本号',
  `drill_level`        tinyint(1) NOT NULL           COMMENT '当前层级（1:项目,2:楼栋,3:楼层,4:商铺,5:合同,6:商家）',
  `target_report_code` varchar(50)  NOT NULL         COMMENT '下钻目标报表编码',
  `drill_condition`    json         NOT NULL          COMMENT '钻取条件参数映射（JSON对象：paramName/sourceField/levelName）',
  `sort_order`         int    NOT NULL DEFAULT '0'   COMMENT '同报表内多钻取路径排序',

  -- 审计字段
  `created_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '创建人ID',
  `created_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `updated_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '更新人ID',
  `updated_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  tinyint(1)  NOT NULL DEFAULT '0'                              COMMENT '逻辑删除（0:否,1:是）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_drill_report_level_ver` (`report_id`,`drill_level`,`version`,`is_deleted`),
  KEY `idx_report_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表钻取配置表';


-- ==================================================
-- 三、运营支撑表（3张）
-- ==================================================

-- 10. 用户报表收藏表
DROP TABLE IF EXISTS `rpt_user_favorite`;
CREATE TABLE `rpt_user_favorite` (
  `id`           bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`      bigint NOT NULL               COMMENT '用户ID（关联sys_user.id）',
  `report_id`    bigint NOT NULL               COMMENT '报表配置ID（关联rpt_config.id）',
  `version`      int    NOT NULL DEFAULT '1'   COMMENT '报表版本号',
  `sort_order`   int    NOT NULL DEFAULT '0'   COMMENT '排序（支持拖拽排序，前端维护）',
  `quick_access` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否快捷入口（0:否,1:是）',

  -- 审计字段
  `created_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '创建人ID',
  `created_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `updated_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '更新人ID',
  `updated_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  tinyint(1)  NOT NULL DEFAULT '0'                              COMMENT '逻辑删除（0:否,1:是）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fav_user_report_ver` (`user_id`,`report_id`,`version`,`is_deleted`),
  KEY `idx_user_id`   (`user_id`),
  KEY `idx_report_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户报表收藏表（支持拖拽排序）';


-- 11. 报表定时推送任务表
DROP TABLE IF EXISTS `rpt_schedule_task`;
CREATE TABLE `rpt_schedule_task` (
  `id`              bigint       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
  `task_code`       varchar(50)  NOT NULL                 COMMENT '任务编码（全局唯一）',
  `version`         int          NOT NULL DEFAULT '1'     COMMENT '版本号',
  `task_name`       varchar(100) NOT NULL                 COMMENT '任务名称',
  `report_id`       bigint       NOT NULL                 COMMENT '报表配置ID',
  `cron_expression` varchar(50)  NOT NULL                 COMMENT 'Cron表达式（标准5/6位）',
  `recipients`      json         NOT NULL                 COMMENT '收件人邮箱列表（JSON数组）',
  `cc_recipients`   json         DEFAULT NULL             COMMENT '抄送人邮箱列表（JSON数组）',
  `export_format`   varchar(10)  NOT NULL DEFAULT 'EXCEL' COMMENT '导出格式（EXCEL/PDF）',
  `filter_params`   json         DEFAULT NULL             COMMENT '固定筛选参数快照（JSON）',
  `last_run_time`   datetime     DEFAULT NULL             COMMENT '上次执行时间',
  `next_run_time`   datetime     DEFAULT NULL             COMMENT '下次执行时间（XXL-Job调度更新）',
  `run_count`       int          NOT NULL DEFAULT '0'     COMMENT '累计执行次数',
  `fail_count`      int          NOT NULL DEFAULT '0'     COMMENT '连续失败次数（超过3次自动禁用）',
  `status`          tinyint(1)   NOT NULL DEFAULT '1'     COMMENT '状态（0:禁用,1:启用）',

  -- 审计字段
  `created_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '创建人ID',
  `created_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `updated_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '更新人ID',
  `updated_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  tinyint(1)  NOT NULL DEFAULT '0'                              COMMENT '逻辑删除（0:否,1:是）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_code_ver_deleted` (`task_code`,`version`,`is_deleted`),
  KEY `idx_report_id`    (`report_id`),
  KEY `idx_next_run_time`(`next_run_time`),
  KEY `idx_status`       (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表定时推送任务表（XXL-Job动态注册）';


-- 12. 报表生成日志表
DROP TABLE IF EXISTS `rpt_generation_log`;
CREATE TABLE `rpt_generation_log` (
  `id`              bigint       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
  `log_code`        varchar(64)  NOT NULL                 COMMENT '日志流水号（全局唯一，如LOG_20260302143000_001）',
  `report_id`       bigint       NOT NULL                 COMMENT '报表ID（关联rpt_config.id）',
  `task_id`         bigint       DEFAULT NULL             COMMENT '定时任务ID（手动生成为NULL）',
  `generation_type` varchar(20)  NOT NULL                 COMMENT '生成类型（MANUAL:手动,SCHEDULE:定时）',
  `triggered_by`    bigint       NOT NULL DEFAULT '0'     COMMENT '触发人ID（系统自动触发为0）',
  `file_format`     varchar(10)  NOT NULL                 COMMENT '文件格式（EXCEL/PDF）',
  `file_name`       varchar(200) DEFAULT NULL             COMMENT '文件名称',
  `file_path`       varchar(500) DEFAULT NULL             COMMENT '文件存储路径（OSS或本地路径）',
  `file_size`       bigint       DEFAULT '0'              COMMENT '文件大小（字节）',
  `file_md5`        varchar(32)  DEFAULT NULL             COMMENT '文件MD5校验值',
  `filter_params`   json         DEFAULT NULL             COMMENT '查询参数快照（用于重复生成）',
  `data_count`      int          DEFAULT '0'              COMMENT '导出数据条数',
  `status`          tinyint(1)   NOT NULL DEFAULT '2'     COMMENT '状态（0:失败,1:成功,2:进行中）',
  `error_msg`       text         DEFAULT NULL             COMMENT '错误信息（失败时记录）',
  `duration_ms`     int          DEFAULT '0'              COMMENT '耗时（毫秒）',

  -- 审计字段
  `created_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '创建人ID',
  `created_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  `updated_by`  bigint      NOT NULL DEFAULT '0'                              COMMENT '更新人ID',
  `updated_at`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  tinyint(1)  NOT NULL DEFAULT '0'                              COMMENT '逻辑删除（0:否,1:是）',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_log_code`  (`log_code`),
  KEY `idx_report_id`  (`report_id`),
  KEY `idx_task_id`    (`task_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_status`     (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表生成日志表（记录手动/定时导出历史）';

SET FOREIGN_KEY_CHECKS = 1;


-- ==================================================
-- 四、初始化44条 rpt_config 报表基础配置
-- 分类说明：1=资产 2=招商 3=营运 4=财务 5=通用
-- 优先级说明：0=P0(必做) 1=P1(重要) 2=P2(可延后)
-- refresh_cron说明：
--   '0 2 * * *'   = 每日02:00（T+1日表）
--   '0 3 1 * *'   = 每月1日03:00（月表）
--   NULL           = 实时查询，不依赖ETL
-- ==================================================

INSERT INTO `rpt_config` (
  `report_code`, `version`, `report_name`, `report_category`, `report_type`, `priority`,
  `data_source`, `default_dimensions`, `default_metrics`, `chart_type`, `refresh_cron`,
  `enable_drill`, `enable_export`, `sort_order`, `status`, `remark`
) VALUES

-- ============================================================
-- 资产类报表（10条，report_category=1，sort_order: 10~100）
-- ============================================================
('AST_SHOP_RENTAL', 1, '商铺租赁信息报表', 1, 'TABLE', 0,
 'rpt_asset_daily',
 '["project_id","building_id","floor_id","format_type"]',
 '["total_shops","rented_shops","vacant_shops","decorating_shops","rented_area","vacancy_rate","rental_rate","opening_rate"]',
 'TABLE', '0 2 * * *', 1, 1, 10, 1,
 '展示商铺当前租赁状态明细，支持按项目/楼栋/楼层/业态筛选，状态分组，年/月/周切换'),

('AST_VACANCY_RATE', 1, '空置率统计', 1, 'CHART', 0,
 'rpt_asset_daily',
 '["project_id","stat_date","building_id","floor_id","format_type"]',
 '["vacancy_rate","vacant_shops","vacant_area","total_area"]',
 'LINE', '0 2 * * *', 1, 1, 20, 1,
 '空置率趋势折线图，支持同比/环比分析，可下钻至楼栋→楼层→商铺明细'),

('AST_RENTAL_RATE', 1, '出租率统计', 1, 'CHART', 0,
 'rpt_asset_daily',
 '["project_id","stat_date","building_id","floor_id","format_type"]',
 '["rental_rate","rented_shops","rented_area","total_area"]',
 'LINE', '0 2 * * *', 1, 1, 30, 1,
 '出租率趋势分析，支持年/月/周维度切换，同期对比折线图'),

('AST_OPENING_RATE', 1, '开业率统计', 1, 'CHART', 0,
 'rpt_asset_daily',
 '["project_id","stat_date","building_id","floor_id","format_type"]',
 '["opening_rate","opened_shops","total_shops"]',
 'LINE', '0 2 * * *', 1, 1, 40, 1,
 '开业率趋势，多指标切换Tab（出租率/开业率），同期对比'),

('AST_DASHBOARD', 1, '资产数据看板', 1, 'DASHBOARD', 0,
 'rpt_asset_daily',
 '["project_id","stat_date"]',
 '["vacancy_rate","rental_rate","opening_rate","total_area","rented_area","vacant_area","total_shops"]',
 'DASHBOARD', '0 2 * * *', 1, 0, 50, 1,
 '资产数据聚合看板：核心三率指标卡片+趋势箭头+折线图+项目对比柱状图'),

('AST_BRAND_DIST', 1, '品牌分布报表', 1, 'CHART', 0,
 'rpt_asset_daily',
 '["project_id","format_type","building_id","floor_id"]',
 '["rented_shops","rented_area","rental_rate"]',
 'PIE', '0 2 * * *', 1, 1, 60, 1,
 '品牌业态分布热力图/树状图，楼层平面分布可视化'),

('AST_SHOP_SPLIT_MERGE', 1, '商铺拆分合并报表', 1, 'TABLE', 1,
 'biz_shop',
 '["project_id","building_id","floor_id"]',
 '["shop_code","original_area","current_area","split_count","merge_count","last_change_date"]',
 'TABLE', NULL, 0, 1, 70, 1,
 '统计商铺拆分/合并历史记录，面积变更明细，不依赖ETL实时查询'),

('AST_MERCHANT_DIST', 1, '商家分布报表', 1, 'CHART', 1,
 'rpt_asset_daily',
 '["project_id","format_type","building_id"]',
 '["rented_shops","total_shops","rental_rate"]',
 'BAR', '0 2 * * *', 1, 1, 80, 1,
 '商家分布柱状图，按楼栋/业态维度分析商家占比'),

('AST_REGION_SUMMARY', 1, '区域归属报表', 1, 'TABLE', 1,
 'rpt_asset_daily',
 '["project_id","building_id"]',
 '["total_area","rented_area","vacant_area","vacancy_rate","rental_rate","total_shops","rented_shops"]',
 'TABLE', '0 2 * * *', 1, 1, 90, 1,
 '按区域/楼栋汇总资产数据，支持导出'),

('AST_AREA_SUMMARY', 1, '经营面积统计', 1, 'CHART', 1,
 'rpt_asset_daily',
 '["project_id","format_type","building_id","floor_id"]',
 '["total_area","rented_area","vacant_area","decoration_area"]',
 'BAR', '0 2 * * *', 1, 1, 100, 1,
 '经营面积堆叠柱状图，分业态/楼栋展示面积构成，辅助资产规划'),

-- ============================================================
-- 招商类报表（8条，report_category=2，sort_order: 10~80）
-- ============================================================
('INV_INTENTION_STATS', 1, '意向客户统计', 2, 'TABLE', 0,
 'rpt_investment_daily',
 '["project_id","stat_date","format_type","investment_manager_id"]',
 '["intention_count","intention_signed","new_intention","conversion_rate"]',
 'TABLE', '0 2 * * *', 1, 1, 10, 1,
 '意向协议数量统计，按招商人员/项目/业态/时间维度分析，显示当日新增'),

('INV_FUNNEL', 1, '客户跟进漏斗数据', 2, 'CHART', 0,
 'rpt_investment_daily',
 '["project_id","stat_date","format_type"]',
 '["intention_count","intention_signed","contract_count","conversion_rate"]',
 'FUNNEL', '0 2 * * *', 0, 1, 20, 1,
 '招商漏斗图：意向登记→签约意向→签约合同，各阶段转化率，支持年/月/日维度切换'),

('INV_CONTRACT_STATS', 1, '合同租赁情况', 2, 'TABLE', 0,
 'rpt_investment_daily',
 '["project_id","stat_date","format_type","investment_manager_id"]',
 '["contract_count","contract_amount","contract_area","avg_rent_price","new_contract"]',
 'TABLE', '0 2 * * *', 1, 1, 30, 1,
 '合同签约情况统计，支持业态/项目/招商人员维度，平均单价分析'),

('INV_PERFORMANCE', 1, '招商业绩显差看板', 2, 'DASHBOARD', 0,
 'rpt_investment_daily',
 '["project_id","investment_manager_id","stat_date"]',
 '["contract_count","contract_amount","contract_area","conversion_rate","new_contract"]',
 'BAR', '0 2 * * *', 0, 1, 40, 1,
 '招商人员业绩对比柱状图，目标vs完成对比，支持项目/人员维度切换'),

('INV_DASHBOARD', 1, '招商数据看板', 2, 'DASHBOARD', 0,
 'rpt_investment_daily',
 '["project_id","stat_date"]',
 '["intention_count","contract_count","conversion_rate","avg_rent_price","contract_amount","new_intention","new_contract"]',
 'DASHBOARD', '0 2 * * *', 0, 0, 50, 1,
 '招商数据聚合看板：漏斗图+业绩显差柱状图+转化率卡片+品牌签约排行'),

('INV_RENT_LEVEL', 1, '租金水平分析', 2, 'CHART', 1,
 'rpt_investment_daily',
 '["project_id","format_type","building_id","stat_date"]',
 '["avg_rent_price","contract_amount","contract_area"]',
 'HEATMAP', '0 2 * * *', 1, 1, 60, 1,
 '租金均价热力图，楼层/业态分组柱状图，辅助定价策略制定'),

('INV_POLICY_EXEC', 1, '租决政策执行报表', 2, 'TABLE', 1,
 'inv_rent_policy',
 '["project_id"]',
 '["policy_name","policy_type","applicable_count","discount_rate","total_discount_amount","approved_count"]',
 'TABLE', NULL, 0, 1, 70, 1,
 '租决政策执行情况统计，优惠额度及审批数量分析，实时查询'),

('INV_BRAND_RANKING', 1, '品牌签约排行', 2, 'CHART', 1,
 'rpt_investment_daily',
 '["project_id","format_type","stat_date"]',
 '["contract_count","contract_area","contract_amount","avg_rent_price"]',
 'BAR', '0 2 * * *', 0, 1, 80, 1,
 '品牌签约排行榜，支持按签约面积/金额/数量排序'),

-- ============================================================
-- 营运类报表（10条，report_category=3，sort_order: 10~100）
-- ============================================================
('OPS_CONTRACT_CHANGES', 1, '合同变更统计', 3, 'TABLE', 0,
 'rpt_operation_monthly',
 '["project_id","stat_month","building_id","format_type"]',
 '["change_count","change_rent_impact","terminated_contracts","expiring_contracts"]',
 'TABLE', '0 3 1 * *', 1, 1, 10, 1,
 '合同变更次数统计，变更类型分布饼图，租金影响额柱状图分析'),

('OPS_RENT_CHANGES', 1, '租金变更分析', 3, 'CHART', 0,
 'rpt_operation_monthly',
 '["project_id","stat_month","format_type","building_id"]',
 '["change_rent_impact","change_count","avg_revenue_per_sqm"]',
 'LINE', '0 3 1 * *', 1, 1, 20, 1,
 '租金变更趋势折线图，变更金额影响柱状图，同比/环比分析'),

('OPS_REVENUE_SUMMARY', 1, '营收填报汇总', 3, 'CHART', 0,
 'rpt_operation_monthly',
 '["project_id","stat_month","building_id","format_type"]',
 '["revenue_amount","floating_rent_amount","avg_revenue_per_sqm","passenger_flow","change_count"]',
 'LINE', '0 3 1 * *', 1, 1, 30, 1,
 '月营收汇总同比/环比折线图，业态饼图分析，支持下钻至商铺级明细'),

('OPS_EXPIRING_CONTRACTS', 1, '合同到期预警', 3, 'TABLE', 0,
 'inv_lease_contract',
 '["project_id","building_id","format_type"]',
 '["contract_code","merchant_name","shop_code","contract_end","days_to_expire","rent_amount","contract_status"]',
 'TABLE', '0 1 * * *', 0, 1, 40, 1,
 '合同到期预警清单，30/60/90天分档，实时查询inv_lease_contract，不经ETL'),

('OPS_REGION_COMPARE', 1, '地区业务对比', 3, 'CHART', 0,
 'rpt_operation_monthly',
 '["project_id","stat_month"]',
 '["revenue_amount","passenger_flow","avg_revenue_per_sqm","change_count","terminated_contracts"]',
 'RADAR', '0 3 1 * *', 0, 1, 50, 1,
 '多项目地区对比雷达图，支持多维柱状图对比，辅助区域业务决策'),

('OPS_DASHBOARD', 1, '营运数据看板', 3, 'DASHBOARD', 0,
 'rpt_operation_monthly',
 '["project_id","stat_month"]',
 '["revenue_amount","passenger_flow","change_count","terminated_contracts","expiring_contracts","avg_revenue_per_sqm"]',
 'DASHBOARD', '0 3 1 * *', 0, 0, 60, 1,
 '营运数据聚合看板：营收/客流趋势折线图+变更/解约统计+合同到期预警列表'),

('OPS_LEDGER_CHANGES', 1, '合同台账变动', 3, 'TABLE', 1,
 'rpt_operation_monthly',
 '["project_id","stat_month","building_id","format_type"]',
 '["change_count","change_rent_impact","expiring_contracts","terminated_contracts"]',
 'TABLE', '0 3 1 * *', 0, 1, 70, 1,
 '合同台账月度变动汇总，新增/变更/解约数量统计'),

('OPS_FLOATING_RENT', 1, '浮动租金统计', 3, 'TABLE', 1,
 'rpt_operation_monthly',
 '["project_id","stat_month","format_type","building_id"]',
 '["floating_rent_amount","revenue_amount","avg_revenue_per_sqm"]',
 'TABLE', '0 3 1 * *', 0, 1, 80, 1,
 '浮动租金计算结果月度汇总，按业态/项目维度展示，坪效对比'),

('OPS_PASSENGER_FLOW', 1, '客流数据分析', 3, 'CHART', 1,
 'rpt_operation_monthly',
 '["project_id","stat_month","building_id","format_type"]',
 '["passenger_flow","avg_daily_passenger","avg_revenue_per_sqm"]',
 'LINE', '0 3 1 * *', 0, 1, 90, 1,
 '月度客流趋势折线图，日均客流，客流与营收相关性分析，同比/环比'),

('OPS_TERMINATION_STATS', 1, '解约统计', 3, 'TABLE', 1,
 'rpt_operation_monthly',
 '["project_id","stat_month","format_type","building_id"]',
 '["terminated_contracts","change_count","change_rent_impact"]',
 'TABLE', '0 3 1 * *', 0, 1, 100, 1,
 '解约合同月度统计，解约类型分布，租金损失金额分析'),

-- ============================================================
-- 财务类报表（11条，report_category=4，sort_order: 10~110）
-- ============================================================
('FIN_RECEIVABLE_SUMMARY', 1, '应收汇总报表', 4, 'TABLE', 0,
 'rpt_finance_monthly',
 '["project_id","stat_month","fee_item_id","fee_item_type"]',
 '["receivable_amount","received_amount","outstanding_amount","collection_rate","deduction_amount","adjustment_amount"]',
 'TABLE', '0 3 1 * *', 1, 1, 10, 1,
 '月度应收汇总，按费项/项目维度，支持账单打印导出，权责发生制口径'),

('FIN_RECEIPT_SUMMARY', 1, '收款汇总报表', 4, 'TABLE', 0,
 'rpt_finance_monthly',
 '["project_id","stat_month","fee_item_id"]',
 '["received_amount","receivable_amount","collection_rate","outstanding_amount"]',
 'TABLE', '0 3 1 * *', 0, 1, 20, 1,
 '月度收款汇总，收款方式分布，实收vs应收对比'),

('FIN_OUTSTANDING_SUMMARY', 1, '欠款统计报表', 4, 'TABLE', 0,
 'rpt_finance_monthly',
 '["project_id","stat_month","fee_item_id"]',
 '["outstanding_amount","overdue_amount","overdue_rate","deposit_balance","prepay_balance"]',
 'TABLE', '0 3 1 * *', 1, 1, 30, 1,
 '欠款汇总多维筛选，账龄分布堆叠柱状图，保证金/预收款余额对照'),

('FIN_AGING_ANALYSIS', 1, '账龄分析报表', 4, 'CHART', 0,
 'rpt_aging_analysis',
 '["project_id","stat_date","merchant_id","contract_id","fee_item_id"]',
 '["within_30","days_31_60","days_61_90","days_91_180","days_181_365","over_365","total_outstanding"]',
 'BAR', '0 2 * * *', 1, 1, 40, 1,
 '账龄分档堆叠柱状图，商家欠款排行榜TOP10，支持下钻至应收明细'),

('FIN_OVERDUE_RATE', 1, '逾期率统计', 4, 'CHART', 0,
 'rpt_finance_monthly',
 '["project_id","stat_month","fee_item_id"]',
 '["overdue_rate","overdue_amount","receivable_amount"]',
 'LINE', '0 3 1 * *', 0, 1, 50, 1,
 '月度逾期率趋势折线，多项目对比柱状图，预警阈值红线标注'),

('FIN_COLLECTION_RATE', 1, '收缴率统计', 4, 'CHART', 0,
 'rpt_finance_monthly',
 '["project_id","stat_month","fee_item_id"]',
 '["collection_rate","received_amount","receivable_amount"]',
 'LINE', '0 3 1 * *', 0, 1, 60, 1,
 '月度收缴率折线图，项目对比柱状图，仪表盘展示当期收缴率'),

('FIN_DASHBOARD', 1, '财务数据看板', 4, 'DASHBOARD', 0,
 'rpt_finance_monthly',
 '["project_id","stat_month"]',
 '["receivable_amount","received_amount","outstanding_amount","overdue_amount","collection_rate","overdue_rate","deposit_balance","prepay_balance"]',
 'DASHBOARD', '0 3 1 * *', 0, 0, 70, 1,
 '财务数据聚合看板：应收/已收/欠款/逾期四大指标卡+趋势图+收缴率仪表盘'),

('FIN_VOUCHER_STATS', 1, '凭证统计', 4, 'TABLE', 1,
 'fin_voucher',
 '["project_id","stat_month"]',
 '["voucher_count","voucher_amount","pending_count","approved_count","uploaded_count"]',
 'TABLE', NULL, 0, 1, 80, 1,
 '财务凭证生成及审核状态统计，实时查询fin_voucher表'),

('FIN_DEPOSIT_SUMMARY', 1, '保证金汇总', 4, 'TABLE', 1,
 'fin_deposit_account',
 '["project_id"]',
 '["deposit_balance","total_deposit","total_refund","total_forfeit","account_count"]',
 'TABLE', NULL, 1, 1, 90, 1,
 '保证金余额汇总，收取/退款/罚没金额统计，实时查询'),

('FIN_PREPAY_SUMMARY', 1, '预收款汇总', 4, 'TABLE', 1,
 'fin_prepay_account',
 '["project_id"]',
 '["prepay_balance","total_prepay","total_writeoff","total_refund","account_count"]',
 'TABLE', NULL, 0, 1, 100, 1,
 '预收款余额汇总，冲抵/退款统计，实时查询fin_prepay_account表'),

('FIN_DEDUCTION_ADJ', 1, '减免/调整统计', 4, 'TABLE', 1,
 'fin_receivable',
 '["project_id","stat_month","fee_item_id"]',
 '["deduction_amount","adjustment_amount","deduction_count","adjustment_count","net_change"]',
 'TABLE', '0 3 1 * *', 0, 1, 110, 1,
 '应收减免和调整金额统计，按项目/费项维度，按月汇总审批数量'),

-- ============================================================
-- 通用类报表（5条，report_category=5，sort_order: 10~50）
-- ============================================================
('CMN_RPT_HOME', 1, '报表中心首页', 5, 'DASHBOARD', 0,
 NULL, NULL, NULL,
 'DASHBOARD', NULL, 0, 0, 10, 1,
 '四大类报表入口卡片，收藏报表快捷区，最近浏览记录（localStorage），报表搜索框'),

('CMN_EXPORT', 1, '报表导出任务', 5, 'TABLE', 1,
 'rpt_generation_log',
 '["report_id","generation_type","status"]',
 '["file_name","file_format","file_size","data_count","duration_ms","status","created_at"]',
 'TABLE', NULL, 0, 0, 20, 1,
 'EasyExcel流式写入（百万行不OOM），Puppeteer截图PDF，异步任务+进度轮询，30分钟缓存复用'),

('CMN_DRILL_DOWN', 1, '数据钻取', 5, 'TABLE', 0,
 NULL,
 '["reportCode","dimension","dimensionId","targetLevel"]',
 NULL,
 'TABLE', NULL, 0, 0, 30, 1,
 '通用钻取路由接口，基于rpt_drill_config动态解析规则，支持四层穿透（项目→楼栋→楼层→商铺）'),

('CMN_FAVORITES', 1, '报表收藏管理', 5, 'TABLE', 2,
 'rpt_user_favorite',
 '["user_id"]',
 '["report_name","sort_order","quick_access","created_at"]',
 'TABLE', NULL, 0, 0, 40, 1,
 '用户报表收藏CRUD，支持拖拽排序（sort_order），快捷入口配置'),

('CMN_SCHEDULE', 1, '报表定时推送', 5, 'TABLE', 2,
 'rpt_schedule_task',
 '["report_id","status"]',
 '["task_name","cron_expression","recipients","export_format","last_run_time","next_run_time","fail_count"]',
 'TABLE', NULL, 0, 0, 50, 1,
 'XXL-Job动态任务注册，Spring Mail邮件发送，最多3次失败重试，失败写入rpt_generation_log');


-- ==================================================
-- 五、初始化维度配置（rpt_config_dimension）
-- 覆盖主要P0报表，使用子查询动态关联report_id
-- ==================================================

-- -------------------------------------------------------
-- AST_VACANCY_RATE（空置率统计）维度
-- -------------------------------------------------------
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id,  'PROJECT',     1, '项目',   'project_id',   'NUMBER', 1, 1 FROM rpt_config WHERE report_code='AST_VACANCY_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id,  'STAT_DATE',   1, '统计日期', 'stat_date',  'DATE',   2, 1 FROM rpt_config WHERE report_code='AST_VACANCY_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id,  'BUILDING',    1, '楼栋',   'building_id',  'NUMBER', 3, 0 FROM rpt_config WHERE report_code='AST_VACANCY_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id,  'FLOOR',       1, '楼层',   'floor_id',     'NUMBER', 4, 0 FROM rpt_config WHERE report_code='AST_VACANCY_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id,  'FORMAT_TYPE', 1, '业态',   'format_type',  'STRING', 5, 1 FROM rpt_config WHERE report_code='AST_VACANCY_RATE' AND is_deleted=0;

-- AST_VACANCY_RATE 指标
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'VACANCY_RATE', 1, '空置率',   'vacancy_rate', 'vacant_area / total_area * 100', 'PERCENT',  '%',  1, 1, 1 FROM rpt_config WHERE report_code='AST_VACANCY_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'VACANT_SHOPS', 1, '空置商铺数', 'vacant_shops', NULL, 'INTEGER', '间', 2, 1, 0 FROM rpt_config WHERE report_code='AST_VACANCY_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'VACANT_AREA',  1, '空置面积',   'vacant_area',  NULL, 'DECIMAL', '㎡', 3, 1, 0 FROM rpt_config WHERE report_code='AST_VACANCY_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'TOTAL_AREA',   1, '总面积',     'total_area',   NULL, 'DECIMAL', '㎡', 4, 1, 0 FROM rpt_config WHERE report_code='AST_VACANCY_RATE' AND is_deleted=0;

-- -------------------------------------------------------
-- AST_RENTAL_RATE（出租率统计）维度
-- -------------------------------------------------------
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'PROJECT',     1, '项目',   'project_id',  'NUMBER', 1, 1 FROM rpt_config WHERE report_code='AST_RENTAL_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'STAT_DATE',   1, '统计日期', 'stat_date', 'DATE',   2, 1 FROM rpt_config WHERE report_code='AST_RENTAL_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'BUILDING',    1, '楼栋',   'building_id', 'NUMBER', 3, 0 FROM rpt_config WHERE report_code='AST_RENTAL_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'FORMAT_TYPE', 1, '业态',   'format_type', 'STRING', 4, 1 FROM rpt_config WHERE report_code='AST_RENTAL_RATE' AND is_deleted=0;

-- AST_RENTAL_RATE 指标
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'RENTAL_RATE',  1, '出租率',   'rental_rate',  'rented_area / total_area * 100', 'PERCENT',  '%',  1, 1, 1 FROM rpt_config WHERE report_code='AST_RENTAL_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'RENTED_SHOPS', 1, '已租商铺数', 'rented_shops', NULL, 'INTEGER', '间', 2, 1, 0 FROM rpt_config WHERE report_code='AST_RENTAL_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'RENTED_AREA',  1, '已租面积',   'rented_area',  NULL, 'DECIMAL', '㎡', 3, 1, 0 FROM rpt_config WHERE report_code='AST_RENTAL_RATE' AND is_deleted=0;

-- -------------------------------------------------------
-- AST_OPENING_RATE（开业率统计）维度 + 指标
-- -------------------------------------------------------
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'PROJECT',     1, '项目',   'project_id',  'NUMBER', 1, 1 FROM rpt_config WHERE report_code='AST_OPENING_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'STAT_DATE',   1, '统计日期', 'stat_date', 'DATE',   2, 1 FROM rpt_config WHERE report_code='AST_OPENING_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'FORMAT_TYPE', 1, '业态',   'format_type', 'STRING', 3, 1 FROM rpt_config WHERE report_code='AST_OPENING_RATE' AND is_deleted=0;

INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'OPENING_RATE',  1, '开业率',   'opening_rate',  'opened_shops / total_shops * 100', 'PERCENT',  '%', 1, 1, 1 FROM rpt_config WHERE report_code='AST_OPENING_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'OPENED_SHOPS',  1, '已开业商铺数', 'opened_shops', NULL, 'INTEGER', '间', 2, 1, 0 FROM rpt_config WHERE report_code='AST_OPENING_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'TOTAL_SHOPS',   1, '商铺总数',     'total_shops',  NULL, 'INTEGER', '间', 3, 1, 0 FROM rpt_config WHERE report_code='AST_OPENING_RATE' AND is_deleted=0;

-- -------------------------------------------------------
-- INV_FUNNEL（客户跟进漏斗）维度 + 指标
-- -------------------------------------------------------
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'PROJECT',     1, '项目',   'project_id',  'NUMBER', 1, 1 FROM rpt_config WHERE report_code='INV_FUNNEL' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'STAT_DATE',   1, '统计日期', 'stat_date', 'DATE',   2, 1 FROM rpt_config WHERE report_code='INV_FUNNEL' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'FORMAT_TYPE', 1, '业态',   'format_type', 'STRING', 3, 0 FROM rpt_config WHERE report_code='INV_FUNNEL' AND is_deleted=0;

INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'INTENTION_COUNT',  1, '意向协议数', 'intention_count',  NULL, 'INTEGER', '个', 1, 1, 0 FROM rpt_config WHERE report_code='INV_FUNNEL' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'INTENTION_SIGNED', 1, '已签意向数', 'intention_signed', NULL, 'INTEGER', '个', 2, 1, 0 FROM rpt_config WHERE report_code='INV_FUNNEL' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'CONTRACT_COUNT',   1, '签约合同数', 'contract_count',   NULL, 'INTEGER', '个', 3, 1, 0 FROM rpt_config WHERE report_code='INV_FUNNEL' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'CONVERSION_RATE',  1, '意向转化率', 'conversion_rate',  'contract_count / intention_count * 100', 'PERCENT', '%', 4, 1, 1 FROM rpt_config WHERE report_code='INV_FUNNEL' AND is_deleted=0;

-- -------------------------------------------------------
-- OPS_REVENUE_SUMMARY（营收填报汇总）维度 + 指标
-- -------------------------------------------------------
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'PROJECT',     1, '项目',   'project_id',  'NUMBER', 1, 1 FROM rpt_config WHERE report_code='OPS_REVENUE_SUMMARY' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'STAT_MONTH',  1, '统计月份', 'stat_month', 'STRING', 2, 1 FROM rpt_config WHERE report_code='OPS_REVENUE_SUMMARY' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'BUILDING',    1, '楼栋',   'building_id', 'NUMBER', 3, 0 FROM rpt_config WHERE report_code='OPS_REVENUE_SUMMARY' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'FORMAT_TYPE', 1, '业态',   'format_type', 'STRING', 4, 1 FROM rpt_config WHERE report_code='OPS_REVENUE_SUMMARY' AND is_deleted=0;

INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'REVENUE_AMOUNT',       1, '月营收总额', 'revenue_amount',       NULL, 'DECIMAL', '元',   1, 1, 0 FROM rpt_config WHERE report_code='OPS_REVENUE_SUMMARY' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'FLOATING_RENT',        1, '浮动租金',   'floating_rent_amount', NULL, 'DECIMAL', '元',   2, 1, 0 FROM rpt_config WHERE report_code='OPS_REVENUE_SUMMARY' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'AVG_REVENUE_PER_SQM',  1, '坪效',       'avg_revenue_per_sqm',  NULL, 'DECIMAL', '元/㎡', 3, 1, 0 FROM rpt_config WHERE report_code='OPS_REVENUE_SUMMARY' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'PASSENGER_FLOW',        1, '月客流量',   'passenger_flow',       NULL, 'INTEGER', '人次', 4, 1, 0 FROM rpt_config WHERE report_code='OPS_REVENUE_SUMMARY' AND is_deleted=0;

-- -------------------------------------------------------
-- FIN_AGING_ANALYSIS（账龄分析报表）维度 + 指标
-- -------------------------------------------------------
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'PROJECT',   1, '项目',   'project_id',  'NUMBER', 1, 1 FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'STAT_DATE', 1, '统计日期', 'stat_date', 'DATE',   2, 1 FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'MERCHANT',  1, '商家',   'merchant_id', 'NUMBER', 3, 1 FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'CONTRACT',  1, '合同',   'contract_id', 'NUMBER', 4, 0 FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'FEE_ITEM',  1, '费项',   'fee_item_id', 'NUMBER', 5, 0 FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;

INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'WITHIN_30',        1, '30天内',    'within_30',        NULL, 'DECIMAL', '元', 1, 1, 0 FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'DAYS_31_60',       1, '31-60天',   'days_31_60',       NULL, 'DECIMAL', '元', 2, 1, 0 FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'DAYS_61_90',       1, '61-90天',   'days_61_90',       NULL, 'DECIMAL', '元', 3, 1, 0 FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'DAYS_91_180',      1, '91-180天',  'days_91_180',      NULL, 'DECIMAL', '元', 4, 1, 0 FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'DAYS_181_365',     1, '181-365天', 'days_181_365',     NULL, 'DECIMAL', '元', 5, 1, 0 FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'OVER_365',         1, '365天以上', 'over_365',         NULL, 'DECIMAL', '元', 6, 1, 0 FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'TOTAL_OUTSTANDING', 1, '欠款合计',  'total_outstanding', NULL, 'DECIMAL', '元', 7, 1, 0 FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;

-- -------------------------------------------------------
-- FIN_COLLECTION_RATE（收缴率统计）维度 + 指标
-- -------------------------------------------------------
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'PROJECT',    1, '项目',   'project_id',  'NUMBER', 1, 1 FROM rpt_config WHERE report_code='FIN_COLLECTION_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'STAT_MONTH', 1, '统计月份', 'stat_month', 'STRING', 2, 1 FROM rpt_config WHERE report_code='FIN_COLLECTION_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_dimension` (`report_id`,`dimension_code`,`version`,`dimension_name`,`field_name`,`data_type`,`sort_order`,`is_default_show`)
SELECT id, 'FEE_ITEM',   1, '费项',   'fee_item_id', 'NUMBER', 3, 0 FROM rpt_config WHERE report_code='FIN_COLLECTION_RATE' AND is_deleted=0;

INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'COLLECTION_RATE',   1, '收缴率',   'collection_rate',   'received_amount / receivable_amount * 100', 'PERCENT', '%', 1, 1, 1 FROM rpt_config WHERE report_code='FIN_COLLECTION_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'RECEIVED_AMOUNT',   1, '已收金额', 'received_amount',   NULL, 'DECIMAL', '元', 2, 1, 0 FROM rpt_config WHERE report_code='FIN_COLLECTION_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'RECEIVABLE_AMOUNT', 1, '应收金额', 'receivable_amount', NULL, 'DECIMAL', '元', 3, 1, 0 FROM rpt_config WHERE report_code='FIN_COLLECTION_RATE' AND is_deleted=0;
INSERT INTO `rpt_config_metric` (`report_id`,`metric_code`,`version`,`metric_name`,`field_name`,`calculation_logic`,`data_type`,`unit`,`sort_order`,`is_default_show`,`is_calculated`)
SELECT id, 'OUTSTANDING_AMOUNT', 1, '欠款金额', 'outstanding_amount', NULL, 'DECIMAL', '元', 4, 0, 0 FROM rpt_config WHERE report_code='FIN_COLLECTION_RATE' AND is_deleted=0;


-- ==================================================
-- 六、初始化钻取配置（rpt_drill_config）
-- 钻取规则：drill_level 表示当前层级，target_report_code 为下钻目标
-- drill_condition 字段说明：
--   paramName   = 传给目标报表的参数名
--   sourceField = 当前层级数据中用来获取参数值的字段
--   levelName   = 该层级的中文名称（面包屑显示用）
-- ==================================================

-- -------------------------------------------------------
-- AST_VACANCY_RATE 四层钻取：项目→楼栋→楼层→商铺明细
-- -------------------------------------------------------
INSERT INTO `rpt_drill_config` (`report_id`,`version`,`drill_level`,`target_report_code`,`drill_condition`,`sort_order`)
SELECT id, 1, 1, 'AST_VACANCY_RATE',
  '{"paramName":"building_id","sourceField":"building_id","levelName":"楼栋"}', 1
FROM rpt_config WHERE report_code='AST_VACANCY_RATE' AND is_deleted=0;

INSERT INTO `rpt_drill_config` (`report_id`,`version`,`drill_level`,`target_report_code`,`drill_condition`,`sort_order`)
SELECT id, 1, 2, 'AST_VACANCY_RATE',
  '{"paramName":"floor_id","sourceField":"floor_id","levelName":"楼层"}', 2
FROM rpt_config WHERE report_code='AST_VACANCY_RATE' AND is_deleted=0;

INSERT INTO `rpt_drill_config` (`report_id`,`version`,`drill_level`,`target_report_code`,`drill_condition`,`sort_order`)
SELECT id, 1, 3, 'AST_SHOP_RENTAL',
  '{"paramName":"floor_id","sourceField":"floor_id","levelName":"商铺明细","targetTable":"rpt_asset_daily"}', 3
FROM rpt_config WHERE report_code='AST_VACANCY_RATE' AND is_deleted=0;

-- -------------------------------------------------------
-- FIN_AGING_ANALYSIS 三层钻取：项目→商家→合同→应收明细
-- -------------------------------------------------------
INSERT INTO `rpt_drill_config` (`report_id`,`version`,`drill_level`,`target_report_code`,`drill_condition`,`sort_order`)
SELECT id, 1, 1, 'FIN_AGING_ANALYSIS',
  '{"paramName":"merchant_id","sourceField":"merchant_id","levelName":"商家"}', 1
FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;

INSERT INTO `rpt_drill_config` (`report_id`,`version`,`drill_level`,`target_report_code`,`drill_condition`,`sort_order`)
SELECT id, 1, 2, 'FIN_AGING_ANALYSIS',
  '{"paramName":"contract_id","sourceField":"contract_id","levelName":"合同"}', 2
FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;

INSERT INTO `rpt_drill_config` (`report_id`,`version`,`drill_level`,`target_report_code`,`drill_condition`,`sort_order`)
SELECT id, 1, 3, 'FIN_RECEIVABLE_SUMMARY',
  '{"paramName":"contract_id","sourceField":"contract_id","levelName":"应收明细","targetTable":"fin_receivable"}', 3
FROM rpt_config WHERE report_code='FIN_AGING_ANALYSIS' AND is_deleted=0;

-- -------------------------------------------------------
-- OPS_REVENUE_SUMMARY 两层钻取：项目→楼栋→业态明细
-- -------------------------------------------------------
INSERT INTO `rpt_drill_config` (`report_id`,`version`,`drill_level`,`target_report_code`,`drill_condition`,`sort_order`)
SELECT id, 1, 1, 'OPS_REVENUE_SUMMARY',
  '{"paramName":"building_id","sourceField":"building_id","levelName":"楼栋"}', 1
FROM rpt_config WHERE report_code='OPS_REVENUE_SUMMARY' AND is_deleted=0;

INSERT INTO `rpt_drill_config` (`report_id`,`version`,`drill_level`,`target_report_code`,`drill_condition`,`sort_order`)
SELECT id, 1, 2, 'OPS_REVENUE_SUMMARY',
  '{"paramName":"format_type","sourceField":"format_type","levelName":"业态明细"}', 2
FROM rpt_config WHERE report_code='OPS_REVENUE_SUMMARY' AND is_deleted=0;

-- -------------------------------------------------------
-- FIN_OUTSTANDING_SUMMARY 两层钻取：项目→商家→应收明细
-- -------------------------------------------------------
INSERT INTO `rpt_drill_config` (`report_id`,`version`,`drill_level`,`target_report_code`,`drill_condition`,`sort_order`)
SELECT id, 1, 1, 'FIN_AGING_ANALYSIS',
  '{"paramName":"project_id","sourceField":"project_id","levelName":"账龄明细"}', 1
FROM rpt_config WHERE report_code='FIN_OUTSTANDING_SUMMARY' AND is_deleted=0;

-- ==================================================
-- 脚本执行完成，验收检查语句
-- ==================================================
-- SELECT '表数量', COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name LIKE 'rpt_%';
-- SELECT '报表配置总数', COUNT(*) FROM rpt_config WHERE is_deleted=0;
-- SELECT '维度配置总数', COUNT(*) FROM rpt_config_dimension WHERE is_deleted=0;
-- SELECT '指标配置总数', COUNT(*) FROM rpt_config_metric WHERE is_deleted=0;
-- SELECT '钻取配置总数', COUNT(*) FROM rpt_drill_config WHERE is_deleted=0;
-- SELECT report_category, COUNT(*) cnt FROM rpt_config WHERE is_deleted=0 GROUP BY report_category;
