-- ==================================================
-- TASK-6.4 报表收藏与快捷入口 - 数据库迁移脚本
-- 版本: v2.0 (TASK-6.4)
-- 说明:
--   在 rpt_user_favorite 表中追加冗余字段，
--   避免必须 JOIN rpt_config（配置数据可能未完整初始化）。
--   同时删除对 report_id 的强 NOT NULL 约束（改为允许 0 默认值）。
-- 注意: MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS / ADD INDEX IF NOT EXISTS
--       本脚本设计为从 v1 单向迁移，幂等性由调用方保证
-- ==================================================

USE asset_report;

-- 1. 追加冗余字段
ALTER TABLE `rpt_user_favorite`
  MODIFY COLUMN `report_id` bigint NOT NULL DEFAULT '0' COMMENT '报表配置ID（关联rpt_config.id，未初始化时为0）',
  ADD COLUMN `report_code` varchar(50)  NOT NULL DEFAULT '' COMMENT '报表编码，如 AST_VACANCY_DAILY' AFTER `report_id`,
  ADD COLUMN `report_name` varchar(200) NOT NULL DEFAULT '' COMMENT '报表名称，如 空置率统计' AFTER `report_code`,
  ADD COLUMN `route_path`  varchar(200) NOT NULL DEFAULT '' COMMENT '前端路由路径，如 /rpt/asset/vacancy' AFTER `report_name`,
  ADD COLUMN `category`    tinyint      NOT NULL DEFAULT '1' COMMENT '报表分类：1=资产，2=招商，3=营运，4=财务' AFTER `route_path`;

-- 2. 删除旧的唯一键（基于 report_id，改为基于 report_code）
ALTER TABLE `rpt_user_favorite`
  DROP INDEX `uk_fav_user_report_ver`;

-- 3. 创建新的唯一键（用户+报表编码+逻辑删除，允许取消后重新收藏同一报表）
ALTER TABLE `rpt_user_favorite`
  ADD UNIQUE KEY `uk_fav_user_code_deleted` (`user_id`, `report_code`, `is_deleted`);

-- 4. 追加 report_code 的独立索引，便于按编码查找
ALTER TABLE `rpt_user_favorite`
  ADD INDEX `idx_report_code` (`report_code`);
