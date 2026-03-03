-- ==================================================
-- TASK-6.5 报表定时推送 - 数据库迁移脚本
-- 版本: v3.0 (TASK-6.5)
-- 说明:
--   为 rpt_schedule_task 追加 report_code 冗余字段（避免 JOIN rpt_config）。
--   原 report_id 改为允许 0 默认值（未关联时填 0）。
-- ==================================================

USE asset_report;

ALTER TABLE `rpt_schedule_task`
  MODIFY COLUMN `report_id` bigint NOT NULL DEFAULT '0' COMMENT '报表配置ID（关联rpt_config.id，未关联时为0）',
  ADD COLUMN IF NOT EXISTS `report_code` varchar(50) NOT NULL DEFAULT '' COMMENT '报表编码（与ExportTaskDTO.reportCode一致，冗余存储）' AFTER `report_id`;

-- 为 report_code 建立索引
ALTER TABLE `rpt_schedule_task`
  ADD INDEX IF NOT EXISTS `idx_report_code` (`report_code`);
