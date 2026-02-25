-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: asset_db
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `biz_brand`
--

DROP TABLE IF EXISTS `biz_brand`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_brand` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `brand_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品牌编码',
  `brand_name_cn` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '品牌名(中)',
  `brand_name_en` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品牌名(英)',
  `format_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属业态',
  `brand_level` tinyint DEFAULT NULL COMMENT '品牌等级: 1高端 2中端 3大众',
  `cooperation_type` tinyint DEFAULT NULL COMMENT '合作关系: 1直营 2加盟 3代理',
  `business_nature` tinyint DEFAULT NULL COMMENT '经营性质: 1餐饮 2零售 3娱乐 4服务',
  `chain_type` tinyint DEFAULT NULL COMMENT '连锁类型: 1连锁 2单店',
  `project_stage` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '项目阶段',
  `group_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '集团名称',
  `hq_address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '总部地址',
  `main_cities` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主要分布城市',
  `website` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '网址',
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系电话',
  `brand_type` tinyint DEFAULT NULL COMMENT '品牌类型: 1MALL 2商街',
  `avg_rent` decimal(14,2) DEFAULT NULL COMMENT '平均租金(元/㎡·月)',
  `min_customer_price` decimal(14,2) DEFAULT NULL COMMENT '最低客单价(元)',
  `brand_intro` text COLLATE utf8mb4_unicode_ci COMMENT '品牌简介',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_brand_code_deleted` (`brand_code`,`is_deleted`),
  KEY `idx_brand_name_cn` (`brand_name_cn`),
  KEY `idx_format_type` (`format_type`),
  KEY `idx_brand_level` (`brand_level`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='品牌表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_brand`
--

LOCK TABLES `biz_brand` WRITE;
/*!40000 ALTER TABLE `biz_brand` DISABLE KEYS */;
INSERT INTO `biz_brand` (`id`, `brand_code`, `brand_name_cn`, `brand_name_en`, `format_type`, `brand_level`, `cooperation_type`, `business_nature`, `chain_type`, `project_stage`, `group_name`, `hq_address`, `main_cities`, `website`, `phone`, `brand_type`, `avg_rent`, `min_customer_price`, `brand_intro`, `is_deleted`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (1,'01-232-1224','星巴克','xingbake','喝酒234',2,2,1,1,'正在3夺','礵模压东走西顾 柘城','礵模压东走西顾 柘城礵模压东走西顾 柘城','济南、青岛','www.ssrg.com.cn','18663772731',2,123.00,1.00,'礵模压东走西顾 柘城',0,1,'2026-02-22 08:31:48',1,'2026-02-22 08:31:48'),(2,NULL,'测试品牌改名',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,1,'2026-02-22 08:39:16',1,'2026-02-22 08:39:50');
/*!40000 ALTER TABLE `biz_brand` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_brand_contact`
--

DROP TABLE IF EXISTS `biz_brand_contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_brand_contact` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `brand_id` bigint unsigned NOT NULL COMMENT '品牌ID',
  `contact_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人姓名',
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电话',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `position` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职位',
  `is_primary` tinyint DEFAULT '0' COMMENT '是否主要联系人: 0否 1是',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_brand_id` (`brand_id`),
  KEY `idx_is_primary` (`is_primary`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `biz_brand_contact_ibfk_1` FOREIGN KEY (`brand_id`) REFERENCES `biz_brand` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='品牌联系人表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_brand_contact`
--

LOCK TABLES `biz_brand_contact` WRITE;
/*!40000 ALTER TABLE `biz_brand_contact` DISABLE KEYS */;
INSERT INTO `biz_brand_contact` (`id`, `brand_id`, `contact_name`, `phone`, `email`, `position`, `is_primary`, `is_deleted`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (1,1,'cao','18663772731','cybok@163.com','jingli',1,1,1,'2026-02-22 08:31:49',1,'2026-02-22 08:39:58'),(2,2,'张三','13800000000',NULL,NULL,1,1,1,'2026-02-22 08:39:16',1,'2026-02-22 08:39:16'),(3,2,'李四','13911111111',NULL,NULL,1,0,1,'2026-02-22 08:39:17',1,'2026-02-22 08:39:17'),(4,1,'cao','18663772731','cybok@163.com','jingli345',1,1,1,'2026-02-22 08:39:59',1,'2026-02-22 08:40:37'),(5,1,'cao','18663772731','cybok@163.com','jingli345',1,1,1,'2026-02-22 08:40:37',1,'2026-02-22 08:40:56'),(6,1,'cao','18663772731','cybok@163.com','jingli345',1,0,1,'2026-02-22 08:40:57',1,'2026-02-22 08:40:57'),(7,1,'123','18663772732','cybok@163.com','ssdf',0,0,1,'2026-02-22 08:40:57',1,'2026-02-22 08:40:57');
/*!40000 ALTER TABLE `biz_brand_contact` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_building`
--

DROP TABLE IF EXISTS `biz_building`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_building` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint unsigned NOT NULL COMMENT '所属项目ID',
  `building_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '楼栋编码',
  `building_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '楼栋名称',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0停用 1启用',
  `building_area` decimal(14,2) DEFAULT '0.00' COMMENT '建筑面积(㎡)',
  `operating_area` decimal(14,2) DEFAULT '0.00' COMMENT '营业面积(㎡)',
  `above_floors` int DEFAULT '0' COMMENT '地上楼层数',
  `below_floors` int DEFAULT '0' COMMENT '地下楼层数',
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '楼栋平面图URL',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_building_code_project` (`project_id`,`building_code`,`is_deleted`),
  KEY `idx_building_name` (`building_name`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `biz_building_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `biz_project` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='楼栋表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_building`
--

LOCK TABLES `biz_building` WRITE;
/*!40000 ALTER TABLE `biz_building` DISABLE KEYS */;
INSERT INTO `biz_building` (`id`, `project_id`, `building_code`, `building_name`, `status`, `building_area`, `operating_area`, `above_floors`, `below_floors`, `image_url`, `is_deleted`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (1,4,'12','23423',1,3223.00,234.00,4,1,'',0,1,'2026-02-22 00:08:20',1,'2026-02-22 00:08:20');
/*!40000 ALTER TABLE `biz_building` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_floor`
--

DROP TABLE IF EXISTS `biz_floor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_floor` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint unsigned NOT NULL COMMENT '所属项目ID',
  `building_id` bigint unsigned NOT NULL COMMENT '所属楼栋ID',
  `floor_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '楼层编码',
  `floor_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '楼层名称',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0停用 1启用',
  `building_area` decimal(14,2) DEFAULT '0.00' COMMENT '建筑面积(㎡)',
  `operating_area` decimal(14,2) DEFAULT '0.00' COMMENT '营业面积(㎡)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '楼层平面图URL',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_floor_code_building` (`building_id`,`floor_code`,`is_deleted`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_building_id` (`building_id`),
  KEY `idx_status` (`status`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `biz_floor_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `biz_project` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `biz_floor_ibfk_2` FOREIGN KEY (`building_id`) REFERENCES `biz_building` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='楼层表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_floor`
--

LOCK TABLES `biz_floor` WRITE;
/*!40000 ALTER TABLE `biz_floor` DISABLE KEYS */;
INSERT INTO `biz_floor` (`id`, `project_id`, `building_id`, `floor_code`, `floor_name`, `status`, `building_area`, `operating_area`, `remark`, `image_url`, `is_deleted`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (1,4,1,'b2','sdfsdf',1,234.00,200.00,'sadfsf','',0,1,'2026-02-22 00:08:49',1,'2026-02-22 00:08:49');
/*!40000 ALTER TABLE `biz_floor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_merchant`
--

DROP TABLE IF EXISTS `biz_merchant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_merchant` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint unsigned NOT NULL COMMENT '所属项目ID',
  `merchant_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商家编号',
  `merchant_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商家名称',
  `merchant_attr` tinyint DEFAULT NULL COMMENT '商家属性: 1个体户 2企业',
  `merchant_nature` tinyint DEFAULT NULL COMMENT '商家性质: 1民营 2国营 3外资 4合资',
  `format_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '经营业态',
  `natural_person` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '自然人姓名',
  `id_card` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '身份证号(SM4加密存储)',
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '地址',
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机',
  `merchant_level` tinyint DEFAULT '3' COMMENT '商家评级: 1优秀 2良好 3一般 4差',
  `audit_status` tinyint DEFAULT '0' COMMENT '审核状态: 0待审核 1通过 2驳回',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_code_project` (`project_id`,`merchant_code`,`is_deleted`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_merchant_name` (`merchant_name`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_merchant_level` (`merchant_level`),
  KEY `idx_is_deleted` (`is_deleted`),
  KEY `idx_merchant_audit_query` (`project_id`,`audit_status`,`created_at`),
  CONSTRAINT `biz_merchant_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `biz_project` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_merchant`
--

LOCK TABLES `biz_merchant` WRITE;
/*!40000 ALTER TABLE `biz_merchant` DISABLE KEYS */;
INSERT INTO `biz_merchant` (`id`, `project_id`, `merchant_code`, `merchant_name`, `merchant_attr`, `merchant_nature`, `format_type`, `natural_person`, `id_card`, `address`, `phone`, `merchant_level`, `audit_status`, `is_deleted`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (1,4,'wre','sdfsfd',1,2,'sdfsf','wqew','','wwer werwrwrw wrewerw','12334',2,0,0,1,'2026-02-22 08:42:53',1,'2026-02-22 08:42:53');
/*!40000 ALTER TABLE `biz_merchant` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_merchant_attachment`
--

DROP TABLE IF EXISTS `biz_merchant_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_merchant_attachment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_id` bigint unsigned NOT NULL COMMENT '商家ID',
  `file_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件名称',
  `file_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件URL',
  `file_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件类型',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `upload_by` bigint unsigned DEFAULT NULL COMMENT '上传人ID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `biz_merchant_attachment_ibfk_1` FOREIGN KEY (`merchant_id`) REFERENCES `biz_merchant` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家附件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_merchant_attachment`
--

LOCK TABLES `biz_merchant_attachment` WRITE;
/*!40000 ALTER TABLE `biz_merchant_attachment` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_merchant_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_merchant_contact`
--

DROP TABLE IF EXISTS `biz_merchant_contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_merchant_contact` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_id` bigint unsigned NOT NULL COMMENT '商家ID',
  `contact_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系人姓名',
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电话',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `position` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职位',
  `is_primary` tinyint DEFAULT '0' COMMENT '是否主要联系人',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `biz_merchant_contact_ibfk_1` FOREIGN KEY (`merchant_id`) REFERENCES `biz_merchant` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家联系人表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_merchant_contact`
--

LOCK TABLES `biz_merchant_contact` WRITE;
/*!40000 ALTER TABLE `biz_merchant_contact` DISABLE KEYS */;
INSERT INTO `biz_merchant_contact` (`id`, `merchant_id`, `contact_name`, `phone`, `email`, `position`, `is_primary`, `is_deleted`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (1,1,'wrwwer','werwr','cybok@163.com','xiaoshou',1,1,1,'2026-02-22 08:42:53',1,'2026-02-23 09:18:29'),(2,1,'wrwwer','werwr','cybok@163.com','xiaoshou',1,1,1,'2026-02-23 09:18:30',1,'2026-02-23 09:18:50'),(3,1,'wrwwer','werwr','cybok@163.com','xiaoshou',1,0,1,'2026-02-23 09:18:50',1,'2026-02-23 09:18:50');
/*!40000 ALTER TABLE `biz_merchant_contact` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_merchant_credit`
--

DROP TABLE IF EXISTS `biz_merchant_credit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_merchant_credit` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_id` bigint unsigned NOT NULL COMMENT '商家ID',
  `record_type` tinyint DEFAULT NULL COMMENT '记录类型: 1好评 2差评 3违约 4其他',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '记录内容',
  `record_date` date DEFAULT NULL COMMENT '记录日期',
  `operator_id` bigint unsigned DEFAULT NULL COMMENT '操作人ID',
  `attachment_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附件URL',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_record_type` (`record_type`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `biz_merchant_credit_ibfk_1` FOREIGN KEY (`merchant_id`) REFERENCES `biz_merchant` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家诚信记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_merchant_credit`
--

LOCK TABLES `biz_merchant_credit` WRITE;
/*!40000 ALTER TABLE `biz_merchant_credit` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_merchant_credit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_merchant_invoice`
--

DROP TABLE IF EXISTS `biz_merchant_invoice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_merchant_invoice` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `merchant_id` bigint unsigned NOT NULL COMMENT '商家ID',
  `invoice_title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '发票抬头',
  `tax_number` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '税号',
  `bank_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '开户银行',
  `bank_account` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '银行账号',
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '注册地址',
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '注册电话',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `biz_merchant_invoice_ibfk_1` FOREIGN KEY (`merchant_id`) REFERENCES `biz_merchant` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家开票信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_merchant_invoice`
--

LOCK TABLES `biz_merchant_invoice` WRITE;
/*!40000 ALTER TABLE `biz_merchant_invoice` DISABLE KEYS */;
INSERT INTO `biz_merchant_invoice` (`id`, `merchant_id`, `invoice_title`, `tax_number`, `bank_name`, `bank_account`, `address`, `phone`, `is_default`, `is_deleted`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (1,1,'mmmmm','9137ma334232312','gonghang','1123435354','','05312323233',1,1,1,'2026-02-22 08:42:54',1,'2026-02-23 09:18:29'),(2,1,'mmmmm','9137ma334232312','gonghang','1123435354','','05312323233',1,1,1,'2026-02-23 09:18:30',1,'2026-02-23 09:18:50'),(3,1,'mmmmm','9137ma334232312','gonghang','1123435354','','05312323233',1,0,1,'2026-02-23 09:18:50',1,'2026-02-23 09:18:50');
/*!40000 ALTER TABLE `biz_merchant_invoice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_news`
--

DROP TABLE IF EXISTS `biz_news`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_news` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `content` longtext COLLATE utf8mb4_unicode_ci COMMENT '内容(富文本HTML)',
  `category` tinyint DEFAULT NULL COMMENT '分类: 1新闻 2政策 3招商 4服务指南',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0草稿 1上架 2下架',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `created_by` bigint unsigned DEFAULT NULL COMMENT 'åˆ›å»ºäººID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_create_by` (`created_by`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻资讯表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_news`
--

LOCK TABLES `biz_news` WRITE;
/*!40000 ALTER TABLE `biz_news` DISABLE KEYS */;
INSERT INTO `biz_news` (`id`, `title`, `content`, `category`, `status`, `publish_time`, `created_by`, `is_deleted`, `created_at`, `updated_by`, `updated_at`) VALUES (1,'模压苛','顶戴 錒茜',1,1,'2026-02-23 09:28:10',1,0,'2026-02-23 09:28:04',1,'2026-02-23 09:28:04');
/*!40000 ALTER TABLE `biz_news` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_notice`
--

DROP TABLE IF EXISTS `biz_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_notice` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `content` longtext COLLATE utf8mb4_unicode_ci COMMENT '内容(富文本HTML)',
  `notice_type` tinyint DEFAULT NULL COMMENT '类型: 1通知 2公告 3政策',
  `status` tinyint DEFAULT '0' COMMENT '状态: 0草稿 1已发布 2下架',
  `scheduled_time` datetime DEFAULT NULL COMMENT '定时发送时间',
  `publish_time` datetime DEFAULT NULL COMMENT '实际发布时间',
  `created_by` bigint unsigned DEFAULT NULL COMMENT 'åˆ›å»ºäººID',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_notice_type` (`notice_type`),
  KEY `idx_status` (`status`),
  KEY `idx_scheduled_time` (`scheduled_time`),
  KEY `idx_create_by` (`created_by`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_notice`
--

LOCK TABLES `biz_notice` WRITE;
/*!40000 ALTER TABLE `biz_notice` DISABLE KEYS */;
INSERT INTO `biz_notice` (`id`, `title`, `content`, `notice_type`, `status`, `scheduled_time`, `publish_time`, `created_by`, `is_deleted`, `created_at`, `updated_by`, `updated_at`) VALUES (1,'两次失败','模压模压革',1,1,'2026-02-24 00:00:00','2026-02-23 09:28:17',1,0,'2026-02-23 09:27:30',1,'2026-02-23 09:27:30'),(2,'再次测试一个公告1','柘城 Task 1.2 - 后端脚手架 ✅\n  - 95 个 Java 文件，0 编译错误\n  - 8 个枚举类（ChargeType, PaymentCycle, BillingMode, IntentionStatus 等）\n  - 5 个业务模块（config/intention/contract/opening/policy/decomposition）\n  - 每模块：Entity → Mapper → Service → ServiceImpl → Controller\n  - OpenApiGroupConfig（5 个 Knife4j 分组）\n  - 计租引擎 Strategy 骨架（engine 包）\n',2,0,'2026-02-25 00:00:00',NULL,1,0,'2026-02-23 19:00:42',1,'2026-02-23 19:00:42');
/*!40000 ALTER TABLE `biz_notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_notice_read`
--

DROP TABLE IF EXISTS `biz_notice_read`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_notice_read` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `notice_id` bigint unsigned NOT NULL COMMENT '公告ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `read_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`,`user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `biz_notice_read_ibfk_1` FOREIGN KEY (`notice_id`) REFERENCES `biz_notice` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告阅读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_notice_read`
--

LOCK TABLES `biz_notice_read` WRITE;
/*!40000 ALTER TABLE `biz_notice_read` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_notice_read` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_project`
--

DROP TABLE IF EXISTS `biz_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_project` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目编号',
  `project_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目名称',
  `company_id` bigint unsigned NOT NULL COMMENT '所属公司ID',
  `province` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所在省份',
  `city` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所在城市',
  `address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '项目地址',
  `property_type` tinyint DEFAULT NULL COMMENT '产权性质: 1国有 2集体 3私有 4其他',
  `business_type` tinyint DEFAULT NULL COMMENT '经营类型: 1自持 2租赁 3合作',
  `building_area` decimal(14,2) DEFAULT '0.00' COMMENT '建筑面积(㎡)',
  `operating_area` decimal(14,2) DEFAULT '0.00' COMMENT '经营面积(㎡)',
  `operation_status` tinyint DEFAULT '0' COMMENT '运营状态: 0筹备 1开业 2停业',
  `opening_date` date DEFAULT NULL COMMENT '开业时间',
  `manager_id` bigint unsigned DEFAULT NULL COMMENT '负责人ID',
  `image_urls` json DEFAULT NULL COMMENT '项目图片URL数组(JSON)',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0正常 1删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_code_deleted` (`project_code`,`is_deleted`),
  KEY `idx_company_id` (`company_id`),
  KEY `idx_manager_id` (`manager_id`),
  KEY `idx_operation_status` (`operation_status`),
  KEY `idx_project_name` (`project_name`),
  KEY `idx_is_deleted` (`is_deleted`),
  KEY `idx_province_city` (`province`,`city`,`is_deleted`),
  KEY `idx_project_manager` (`manager_id`,`is_deleted`,`operation_status`),
  CONSTRAINT `biz_project_ibfk_1` FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `biz_project_ibfk_2` FOREIGN KEY (`manager_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_project`
--

LOCK TABLES `biz_project` WRITE;
/*!40000 ALTER TABLE `biz_project` DISABLE KEYS */;
INSERT INTO `biz_project` (`id`, `project_code`, `project_name`, `company_id`, `province`, `city`, `address`, `property_type`, `business_type`, `building_area`, `operating_area`, `operation_status`, `opening_date`, `manager_id`, `image_urls`, `is_deleted`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (4,'234-243sdfsf-df','23rwerwrewe',5,'sdf','sfd','asdfas',2,NULL,21341.00,132.00,0,'2026-02-10',1,NULL,0,1,'2026-02-22 00:07:53',1,'2026-02-22 00:15:12');
/*!40000 ALTER TABLE `biz_project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_project_bank`
--

DROP TABLE IF EXISTS `biz_project_bank`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_project_bank` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint unsigned NOT NULL COMMENT '项目ID',
  `bank_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '银行名称',
  `bank_account` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '银行账号',
  `account_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '户名',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认: 0否 1是',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `biz_project_bank_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `biz_project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='银行账号表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_project_bank`
--

LOCK TABLES `biz_project_bank` WRITE;
/*!40000 ALTER TABLE `biz_project_bank` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_project_bank` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_project_contract`
--

DROP TABLE IF EXISTS `biz_project_contract`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_project_contract` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint unsigned NOT NULL COMMENT '项目ID',
  `party_a_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '合同甲方抬头',
  `party_a_abbr` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '合同甲方缩写',
  `party_a_address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '甲方地址',
  `party_a_phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '甲方电话',
  `business_license` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '营业执照号',
  `legal_representative` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '法人代表',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `biz_project_contract_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `biz_project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目合同甲方信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_project_contract`
--

LOCK TABLES `biz_project_contract` WRITE;
/*!40000 ALTER TABLE `biz_project_contract` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_project_contract` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_project_finance_contact`
--

DROP TABLE IF EXISTS `biz_project_finance_contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_project_finance_contact` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint unsigned NOT NULL COMMENT '项目ID',
  `contact_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '联系人姓名',
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '电话',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `credit_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '社会信用代码',
  `seal_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用章类型',
  `seal_desc` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用章说明',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `biz_project_finance_contact_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `biz_project` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务联系人表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_project_finance_contact`
--

LOCK TABLES `biz_project_finance_contact` WRITE;
/*!40000 ALTER TABLE `biz_project_finance_contact` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_project_finance_contact` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_shop`
--

DROP TABLE IF EXISTS `biz_shop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_shop` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_id` bigint unsigned NOT NULL COMMENT '所属项目ID',
  `building_id` bigint unsigned NOT NULL COMMENT '所属楼栋ID',
  `floor_id` bigint unsigned NOT NULL COMMENT '所在楼层ID',
  `shop_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '铺位号',
  `shop_type` tinyint DEFAULT NULL COMMENT '商铺类型: 1临街 2内铺 3专柜',
  `rent_area` decimal(14,2) DEFAULT '0.00' COMMENT '计租面积(㎡)',
  `measured_area` decimal(14,2) DEFAULT '0.00' COMMENT '实测面积(㎡)',
  `building_area` decimal(14,2) DEFAULT '0.00' COMMENT '建筑面积(㎡)',
  `operating_area` decimal(14,2) DEFAULT '0.00' COMMENT '经营面积(㎡)',
  `shop_status` tinyint DEFAULT '0' COMMENT '商铺状态: 0空置 1在租 2自用 3预留',
  `count_leasing_rate` tinyint DEFAULT '1' COMMENT '计入招商率: 0否 1是',
  `count_rental_rate` tinyint DEFAULT '1' COMMENT '计入出租率: 0否 1是',
  `count_opening_rate` tinyint DEFAULT '1' COMMENT '计入开业率: 0否 1是',
  `signed_format` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签约业态',
  `planned_format` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '规划业态',
  `owner_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业主名称',
  `owner_contact` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业主联系人',
  `owner_phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业主电话',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0正常 1删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_code_project` (`project_id`,`shop_code`,`is_deleted`),
  KEY `idx_building_id` (`building_id`),
  KEY `idx_floor_id` (`floor_id`),
  KEY `idx_shop_status` (`shop_status`),
  KEY `idx_project_status` (`project_id`,`shop_status`,`is_deleted`),
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `biz_shop_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `biz_project` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `biz_shop_ibfk_2` FOREIGN KEY (`building_id`) REFERENCES `biz_building` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `biz_shop_ibfk_3` FOREIGN KEY (`floor_id`) REFERENCES `biz_floor` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商铺表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_shop`
--

LOCK TABLES `biz_shop` WRITE;
/*!40000 ALTER TABLE `biz_shop` DISABLE KEYS */;
INSERT INTO `biz_shop` (`id`, `project_id`, `building_id`, `floor_id`, `shop_code`, `shop_type`, `rent_area`, `measured_area`, `building_area`, `operating_area`, `shop_status`, `count_leasing_rate`, `count_rental_rate`, `count_opening_rate`, `signed_format`, `planned_format`, `owner_name`, `owner_contact`, `owner_phone`, `is_deleted`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (1,4,1,1,'13-123',1,15.00,12.00,16.00,11.00,0,1,1,1,'wer','13','sfdsf','123','18663772731',0,1,'2026-02-22 00:09:50',1,'2026-02-22 00:09:50');
/*!40000 ALTER TABLE `biz_shop` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_shop_relation`
--

DROP TABLE IF EXISTS `biz_shop_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_shop_relation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_shop_id` bigint unsigned NOT NULL COMMENT '源商铺ID（拆分前/合并前）',
  `target_shop_id` bigint unsigned NOT NULL COMMENT '目标商铺ID（拆分后/合并后）',
  `relation_type` tinyint NOT NULL COMMENT '关系类型: 1拆分 2合并',
  `area_before` decimal(14,2) DEFAULT '0.00' COMMENT '变更前面积(㎡)',
  `area_after` decimal(14,2) DEFAULT '0.00' COMMENT '变更后面积(㎡)',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作备注',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '操作人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_source_shop` (`source_shop_id`),
  KEY `idx_target_shop` (`target_shop_id`),
  KEY `idx_relation_type` (`relation_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_shop_relation_query` (`source_shop_id`,`target_shop_id`,`relation_type`),
  KEY `idx_shop_relation_time` (`created_at`,`relation_type`),
  CONSTRAINT `biz_shop_relation_ibfk_1` FOREIGN KEY (`source_shop_id`) REFERENCES `biz_shop` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `biz_shop_relation_ibfk_2` FOREIGN KEY (`target_shop_id`) REFERENCES `biz_shop` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商铺拆合关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_shop_relation`
--

LOCK TABLES `biz_shop_relation` WRITE;
/*!40000 ALTER TABLE `biz_shop_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_shop_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cfg_fee_item`
--

DROP TABLE IF EXISTS `cfg_fee_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cfg_fee_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `item_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目编码',
  `item_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目名称(租金/保证金/物管费等)',
  `item_type` tinyint DEFAULT NULL COMMENT '类型(1租金类/2保证金类/3服务费类)',
  `is_required` tinyint DEFAULT '0' COMMENT '是否必填(0否/1是)',
  `sort_order` int DEFAULT NULL COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '启用状态(1启用/0停用)',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_code_deleted` (`item_code`,`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收款项目配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cfg_fee_item`
--

LOCK TABLES `cfg_fee_item` WRITE;
/*!40000 ALTER TABLE `cfg_fee_item` DISABLE KEYS */;
INSERT INTO `cfg_fee_item` (`id`, `item_code`, `item_name`, `item_type`, `is_required`, `sort_order`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `is_deleted`) VALUES (1,'FI001','租金',1,1,1,1,NULL,'2026-02-23 16:05:05',NULL,'2026-02-23 16:05:05',0),(2,'FI002','租赁保证金',2,1,2,1,NULL,'2026-02-23 16:05:05',NULL,'2026-02-23 16:05:05',0),(3,'FI003','物业管理费',3,0,3,1,NULL,'2026-02-23 16:05:05',NULL,'2026-02-23 16:05:05',0),(4,'FI004','装修保证金',2,0,4,1,NULL,'2026-02-23 16:05:05',NULL,'2026-02-23 16:05:05',0),(5,'FI005','推广服务费',3,0,5,1,NULL,'2026-02-23 16:05:05',NULL,'2026-02-23 16:05:05',0),(6,'FI006','水电费押金',2,0,6,1,NULL,'2026-02-23 16:05:05',NULL,'2026-02-23 16:05:05',0),(7,'FI007','其他费用',3,0,7,1,NULL,'2026-02-23 16:05:05',NULL,'2026-02-23 16:05:05',0);
/*!40000 ALTER TABLE `cfg_fee_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cfg_rent_scheme`
--

DROP TABLE IF EXISTS `cfg_rent_scheme`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cfg_rent_scheme` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scheme_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方案编码',
  `scheme_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '方案名称',
  `charge_type` tinyint DEFAULT '1' COMMENT '默认收费方式(1固定/2固定提成/3阶梯提成/4取高/5一次性)',
  `payment_cycle` tinyint DEFAULT '1' COMMENT '默认支付周期(1月付/2两月付/3季付/4四月付/5半年付/6年付)',
  `billing_mode` tinyint DEFAULT '1' COMMENT '默认账期模式(1预付/2当期/3后付)',
  `formula_json` json DEFAULT NULL COMMENT '租金计算公式配置(JSON格式，支持动态参数)',
  `strategy_bean_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '策略Bean名称(用于Spring策略路由)',
  `status` tinyint DEFAULT '1' COMMENT '状态(1启用/0停用)',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '方案说明',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scheme_code_version_deleted` (`scheme_code`,`is_deleted`,`id`) COMMENT '编码+删除标记唯一(支持重建)'
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计租方案配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cfg_rent_scheme`
--

LOCK TABLES `cfg_rent_scheme` WRITE;
/*!40000 ALTER TABLE `cfg_rent_scheme` DISABLE KEYS */;
INSERT INTO `cfg_rent_scheme` (`id`, `scheme_code`, `scheme_name`, `charge_type`, `payment_cycle`, `billing_mode`, `formula_json`, `strategy_bean_name`, `status`, `description`, `created_by`, `created_at`, `updated_by`, `updated_at`, `is_deleted`) VALUES (1,'RS001','固定租金方案',1,1,1,'{\"type\": \"fixed\", \"params\": [\"unit_price\", \"area\", \"months\"]}','fixedRentStrategy',1,'按固定单价×面积×月数计算，适用于长期稳定租户',NULL,'2026-02-23 16:05:05',NULL,'2026-02-23 16:05:05',0),(2,'RS002','固定提成方案',2,1,1,'{\"type\": \"commission\", \"params\": [\"commission_rate\", \"revenue\", \"min_commission_amount\"]}','fixedCommissionStrategy',1,'按营业额提成，设定最低保底金额，适用于餐饮娱乐类',NULL,'2026-02-23 16:05:05',NULL,'2026-02-23 16:05:05',0),(3,'RS003','阶梯提成方案',3,3,1,'{\"type\": \"step_commission\", \"params\": [\"stages\", \"commission_rate\", \"min_commission_amount\"]}','stepCommissionStrategy',1,'分阶段设置不同提成比例，适用于合同期较长、阶段性调整的租户',NULL,'2026-02-23 16:05:05',NULL,'2026-02-23 16:05:05',0),(4,'RS004','两者取高方案',4,3,1,'{\"type\": \"higher_of\", \"params\": [\"fixed_amount\", \"commission_rate\", \"revenue\", \"min_commission_amount\"]}','higherOfStrategy',1,'固定租金与提成金额取较高者，保障最低收益',NULL,'2026-02-23 16:05:05',NULL,'2026-02-23 16:05:05',0),(5,'RS005','一次性收费方案',5,6,1,'{\"type\": \"one_time\", \"params\": [\"amount\"]}','oneTimeStrategy',1,'一次性收取全部费用，适用于短期展览、特殊活动场地',NULL,'2026-02-23 16:05:05',NULL,'2026-02-23 16:05:05',0);
/*!40000 ALTER TABLE `cfg_rent_scheme` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES (1,'0','<< Flyway Baseline >>','BASELINE','<< Flyway Baseline >>',NULL,'root','2026-02-20 21:49:08',0,1),(2,'1.0.0','init schema','SQL','V1.0.0__init_schema.sql',-1820536885,'root','2026-02-22 23:43:37',951,0);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_intention`
--

DROP TABLE IF EXISTS `inv_intention`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_intention` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `intention_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '意向协议编号(系统自动生成)',
  `intention_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '意向协议名称',
  `project_id` bigint NOT NULL COMMENT '所属项目ID',
  `merchant_id` bigint DEFAULT NULL COMMENT '商家ID',
  `brand_id` bigint DEFAULT NULL COMMENT '意向品牌ID',
  `signing_entity` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签约主体',
  `rent_scheme_id` bigint DEFAULT NULL COMMENT '计租方案ID',
  `delivery_date` date DEFAULT NULL COMMENT '交付日',
  `decoration_start` date DEFAULT NULL COMMENT '装修开始日期',
  `decoration_end` date DEFAULT NULL COMMENT '装修结束日期',
  `opening_date` date DEFAULT NULL COMMENT '开业日',
  `contract_start` date DEFAULT NULL COMMENT '合同开始日期',
  `contract_end` date DEFAULT NULL COMMENT '合同结束日期',
  `payment_cycle` tinyint DEFAULT NULL COMMENT '支付周期(1月付/2两月付/3季付/4四月付/5半年付/6年付)',
  `billing_mode` tinyint DEFAULT NULL COMMENT '账期模式(1预付/2当期/3后付)',
  `status` tinyint DEFAULT '0' COMMENT '状态(0草稿/1审批中/2审批通过/3驳回/4已转合同/5已删除)',
  `total_amount` decimal(14,2) DEFAULT NULL COMMENT '费用总额',
  `agreement_text` longtext COLLATE utf8mb4_unicode_ci COMMENT '协议文本内容',
  `approval_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批流程实例ID',
  `version` int DEFAULT '1' COMMENT '版本号',
  `is_current` tinyint DEFAULT '1' COMMENT '是否当前版本(1是/0否)',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_intention_code_version_deleted` (`intention_code`,`version`,`is_deleted`) COMMENT '编码+版本+删除标记唯一',
  KEY `idx_project_status` (`project_id`,`status`,`is_deleted`) COMMENT '项目状态查询',
  KEY `idx_merchant` (`merchant_id`,`is_deleted`) COMMENT '商家查询',
  KEY `idx_rent_scheme` (`rent_scheme_id`),
  KEY `idx_intention_multi` (`project_id`,`status`,`is_deleted`,`created_at`) COMMENT '覆盖项目+状态+时间排序查询'
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='意向协议主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_intention`
--

LOCK TABLES `inv_intention` WRITE;
/*!40000 ALTER TABLE `inv_intention` DISABLE KEYS */;
INSERT INTO `inv_intention` (`id`, `intention_code`, `intention_name`, `project_id`, `merchant_id`, `brand_id`, `signing_entity`, `rent_scheme_id`, `delivery_date`, `decoration_start`, `decoration_end`, `opening_date`, `contract_start`, `contract_end`, `payment_cycle`, `billing_mode`, `status`, `total_amount`, `agreement_text`, `approval_id`, `version`, `is_current`, `created_by`, `created_at`, `updated_by`, `updated_at`, `is_deleted`) VALUES (1,'INV2026020001','测试意向协议001',1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-01','2027-02-28',3,1,2,NULL,NULL,'MOCK-INV-1-1771898517252',1,1,1,'2026-02-24 10:01:48',1,'2026-02-24 10:02:08',0),(2,'INV2026020002','测试4.2流程',1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-03-01','2028-02-28',3,1,0,788762.93,NULL,NULL,1,1,1,'2026-02-24 10:15:03',1,'2026-02-24 10:19:34',0);
/*!40000 ALTER TABLE `inv_intention` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_intention_billing`
--

DROP TABLE IF EXISTS `inv_intention_billing`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_intention_billing` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `intention_id` bigint NOT NULL COMMENT '意向协议ID',
  `fee_item_id` bigint DEFAULT NULL COMMENT '收款项目ID',
  `billing_start` date DEFAULT NULL COMMENT '账期开始',
  `billing_end` date DEFAULT NULL COMMENT '账期结束',
  `due_date` date DEFAULT NULL COMMENT '应收日期',
  `amount` decimal(14,2) DEFAULT NULL COMMENT '应收金额',
  `billing_type` tinyint DEFAULT NULL COMMENT '账期类型(1首账期/2正常账期)',
  `status` tinyint DEFAULT '0' COMMENT '收款状态(0未收/1部分/2已收)',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_intention_billing` (`intention_id`,`billing_start`,`billing_end`),
  KEY `idx_due_date` (`due_date`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='意向协议-账期表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_intention_billing`
--

LOCK TABLES `inv_intention_billing` WRITE;
/*!40000 ALTER TABLE `inv_intention_billing` DISABLE KEYS */;
INSERT INTO `inv_intention_billing` (`id`, `intention_id`, `fee_item_id`, `billing_start`, `billing_end`, `due_date`, `amount`, `billing_type`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `is_deleted`) VALUES (1,2,1,'2026-03-01','2026-05-31','2026-03-01',90225.00,1,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(2,2,1,'2026-06-01','2026-08-31','2026-06-01',90225.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(3,2,1,'2026-09-01','2026-11-30','2026-09-01',90225.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(4,2,1,'2026-12-01','2027-02-28','2026-12-01',90225.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(5,2,1,'2027-03-01','2027-05-31','2027-03-01',90225.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(6,2,1,'2027-06-01','2027-08-31','2027-06-01',90225.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(7,2,1,'2027-09-01','2027-11-30','2027-09-01',90225.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(8,2,1,'2027-12-01','2028-02-28','2027-12-01',89187.93,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(9,2,2,'2026-03-01','2028-02-28','2026-03-01',50000.00,1,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(10,2,3,'2026-03-01','2026-05-31','2026-03-01',2250.00,1,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(11,2,3,'2026-06-01','2026-08-31','2026-06-01',2250.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(12,2,3,'2026-09-01','2026-11-30','2026-09-01',2250.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(13,2,3,'2026-12-01','2027-02-28','2026-12-01',2250.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(14,2,3,'2027-03-01','2027-05-31','2027-03-01',2250.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(15,2,3,'2027-06-01','2027-08-31','2027-06-01',2250.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(16,2,3,'2027-09-01','2027-11-30','2027-09-01',2250.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0),(17,2,3,'2027-12-01','2028-02-28','2027-12-01',2250.00,2,0,1,'2026-02-24 10:20:27',1,'2026-02-24 10:20:27',0);
/*!40000 ALTER TABLE `inv_intention_billing` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_intention_fee`
--

DROP TABLE IF EXISTS `inv_intention_fee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_intention_fee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `intention_id` bigint NOT NULL COMMENT '意向协议ID',
  `fee_item_id` bigint DEFAULT NULL COMMENT '收款项目ID',
  `fee_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '费项名称',
  `charge_type` tinyint DEFAULT NULL COMMENT '收费方式(1固定/2固定提成/3阶梯提成/4取高/5一次性)',
  `unit_price` decimal(14,2) DEFAULT NULL COMMENT '单价(元/平方米/月)',
  `area` decimal(14,2) DEFAULT NULL COMMENT '面积(平方米)',
  `amount` decimal(14,2) DEFAULT NULL COMMENT '金额(元)',
  `start_date` date DEFAULT NULL COMMENT '费项开始日期',
  `end_date` date DEFAULT NULL COMMENT '费项结束日期',
  `period_index` int DEFAULT NULL COMMENT '租期阶段序号(拆分租期用)',
  `formula_params` json DEFAULT NULL COMMENT '计算公式参数(JSON)',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_intention_fee` (`intention_id`,`fee_item_id`),
  KEY `idx_charge_type` (`charge_type`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='意向协议-费项明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_intention_fee`
--

LOCK TABLES `inv_intention_fee` WRITE;
/*!40000 ALTER TABLE `inv_intention_fee` DISABLE KEYS */;
INSERT INTO `inv_intention_fee` (`id`, `intention_id`, `fee_item_id`, `fee_name`, `charge_type`, `unit_price`, `area`, `amount`, `start_date`, `end_date`, `period_index`, `formula_params`, `created_by`, `created_at`, `updated_by`, `updated_at`, `is_deleted`) VALUES (1,2,1,'租金',1,150.00,200.50,NULL,'2026-03-01','2028-02-28',NULL,NULL,1,'2026-02-24 10:19:13',1,'2026-02-24 10:19:26',1),(2,2,2,'保证金',5,NULL,NULL,NULL,'2026-03-01','2028-02-28',NULL,'{\"amount\": 50000}',1,'2026-02-24 10:19:13',1,'2026-02-24 10:19:26',1),(3,2,1,'租金',1,150.00,200.50,720762.93,'2026-03-01','2028-02-28',NULL,NULL,1,'2026-02-24 10:19:27',1,'2026-02-24 10:19:27',0),(4,2,2,'保证金',5,NULL,NULL,50000.00,'2026-03-01','2028-02-28',NULL,'{\"amount\": 50000}',1,'2026-02-24 10:19:27',1,'2026-02-24 10:19:27',0),(5,2,3,'营业额提成',3,NULL,200.50,18000.00,'2026-03-01','2028-02-28',NULL,'{\"commission_rate\": 5.0, \"min_commission_amount\": 8000}',1,'2026-02-24 10:19:27',1,'2026-02-24 10:19:27',0);
/*!40000 ALTER TABLE `inv_intention_fee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_intention_fee_stage`
--

DROP TABLE IF EXISTS `inv_intention_fee_stage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_intention_fee_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `intention_fee_id` bigint NOT NULL COMMENT '费项明细ID',
  `shop_id` bigint DEFAULT NULL COMMENT '商铺ID',
  `stage_start` date DEFAULT NULL COMMENT '阶段开始日期',
  `stage_end` date DEFAULT NULL COMMENT '阶段结束日期',
  `unit_price` decimal(14,2) DEFAULT NULL COMMENT '该阶段单价',
  `commission_rate` decimal(5,2) DEFAULT NULL COMMENT '提成比例(%)',
  `min_commission_amount` decimal(14,2) DEFAULT NULL COMMENT '最低提成金额',
  `amount` decimal(14,2) DEFAULT NULL COMMENT '该阶段金额',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_fee_stage` (`intention_fee_id`,`stage_start`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='意向协议-分铺计租阶段表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_intention_fee_stage`
--

LOCK TABLES `inv_intention_fee_stage` WRITE;
/*!40000 ALTER TABLE `inv_intention_fee_stage` DISABLE KEYS */;
INSERT INTO `inv_intention_fee_stage` (`id`, `intention_fee_id`, `shop_id`, `stage_start`, `stage_end`, `unit_price`, `commission_rate`, `min_commission_amount`, `amount`, `created_by`, `created_at`, `updated_by`, `updated_at`, `is_deleted`) VALUES (1,5,1,'2026-03-01','2027-02-28',NULL,5.00,8000.00,8000.00,1,'2026-02-24 10:19:27',1,'2026-02-24 10:19:27',0),(2,5,1,'2027-03-01','2028-02-28',NULL,6.00,10000.00,10000.00,1,'2026-02-24 10:19:27',1,'2026-02-24 10:19:27',0);
/*!40000 ALTER TABLE `inv_intention_fee_stage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_intention_shop`
--

DROP TABLE IF EXISTS `inv_intention_shop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_intention_shop` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `intention_id` bigint NOT NULL COMMENT '意向协议ID',
  `shop_id` bigint NOT NULL COMMENT '商铺ID',
  `building_id` bigint DEFAULT NULL COMMENT '楼栋ID',
  `floor_id` bigint DEFAULT NULL COMMENT '楼层ID',
  `format_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业态',
  `area` decimal(14,2) DEFAULT NULL COMMENT '租赁面积(平方米)',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_intention_shop` (`intention_id`,`shop_id`,`is_deleted`) COMMENT '意向+商铺唯一',
  KEY `idx_shop_intention` (`shop_id`,`intention_id`) COMMENT '商铺查询意向',
  KEY `idx_building_floor` (`building_id`,`floor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='意向协议-商铺关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_intention_shop`
--

LOCK TABLES `inv_intention_shop` WRITE;
/*!40000 ALTER TABLE `inv_intention_shop` DISABLE KEYS */;
INSERT INTO `inv_intention_shop` (`id`, `intention_id`, `shop_id`, `building_id`, `floor_id`, `format_type`, `area`, `created_by`, `created_at`, `updated_by`, `updated_at`, `is_deleted`) VALUES (1,2,1,1,1,'餐饮',120.50,1,'2026-02-24 10:19:03',1,'2026-02-24 10:19:03',0),(2,2,2,1,1,'零售',80.00,1,'2026-02-24 10:19:03',1,'2026-02-24 10:19:03',0);
/*!40000 ALTER TABLE `inv_intention_shop` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_lease_contract`
--

DROP TABLE IF EXISTS `inv_lease_contract`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_lease_contract` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租赁合同编码',
  `contract_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '合同名称',
  `project_id` bigint NOT NULL COMMENT '所属项目ID',
  `merchant_id` bigint DEFAULT NULL COMMENT '商家ID',
  `brand_id` bigint DEFAULT NULL COMMENT '品牌ID',
  `intention_id` bigint DEFAULT NULL COMMENT '来源意向协议ID(意向转合同时)',
  `signing_entity` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签约主体',
  `contract_type` tinyint DEFAULT NULL COMMENT '合同类型',
  `rent_scheme_id` bigint DEFAULT NULL COMMENT '计租方案ID',
  `delivery_date` date DEFAULT NULL COMMENT '交付日',
  `decoration_start` date DEFAULT NULL COMMENT '装修开始日期',
  `decoration_end` date DEFAULT NULL COMMENT '装修结束日期',
  `opening_date` date DEFAULT NULL COMMENT '开业日',
  `contract_start` date NOT NULL COMMENT '合同开始日期',
  `contract_end` date NOT NULL COMMENT '合同结束日期',
  `payment_cycle` tinyint DEFAULT NULL COMMENT '支付周期(1月付/2两月付/3季付/4四月付/5半年付/6年付)',
  `billing_mode` tinyint DEFAULT NULL COMMENT '账期模式(1预付/2当期/3后付)',
  `status` tinyint DEFAULT '0' COMMENT '状态(0草稿/1审批中/2生效/3到期/4终止/5已删除)',
  `total_amount` decimal(14,2) DEFAULT NULL COMMENT '合同总金额',
  `contract_text` longtext COLLATE utf8mb4_unicode_ci COMMENT '合同文本',
  `approval_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批流程实例ID',
  `version` int DEFAULT '1' COMMENT '版本号(每次变更+1)',
  `is_current` tinyint DEFAULT '1' COMMENT '是否当前有效版本(1是/0否)',
  `lock_token` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分布式锁Token(防止并发转合同)',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_code_version_deleted` (`contract_code`,`version`,`is_deleted`) COMMENT '编码+版本+删除标记唯一(支持删除后重建)',
  KEY `idx_project_status` (`project_id`,`status`,`is_current`,`is_deleted`) COMMENT '项目状态查询',
  KEY `idx_intention` (`intention_id`),
  KEY `idx_merchant` (`merchant_id`),
  KEY `idx_date_range` (`contract_start`,`contract_end`) COMMENT '合同期限查询',
  KEY `idx_contract_end` (`contract_end`,`status`,`is_current`,`is_deleted`) COMMENT '支持合同到期前自动提醒',
  KEY `idx_lock_token` (`lock_token`) COMMENT '分布式锁Token查询（MySQL普通索引）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租赁合同主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_lease_contract`
--

LOCK TABLES `inv_lease_contract` WRITE;
/*!40000 ALTER TABLE `inv_lease_contract` DISABLE KEYS */;
/*!40000 ALTER TABLE `inv_lease_contract` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_lease_contract_billing`
--

DROP TABLE IF EXISTS `inv_lease_contract_billing`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_lease_contract_billing` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_id` bigint NOT NULL COMMENT '租赁合同ID',
  `fee_item_id` bigint DEFAULT NULL COMMENT '收款项目ID',
  `billing_start` date DEFAULT NULL COMMENT '账期开始',
  `billing_end` date DEFAULT NULL COMMENT '账期结束',
  `due_date` date DEFAULT NULL COMMENT '应收日期',
  `amount` decimal(14,2) DEFAULT NULL COMMENT '应收金额',
  `billing_type` tinyint DEFAULT NULL COMMENT '账期类型(1首账期/2正常账期)',
  `status` tinyint DEFAULT '0' COMMENT '收款状态(0未收/1部分/2已收)',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_contract_billing` (`contract_id`,`billing_start`),
  KEY `idx_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租赁合同-账期表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_lease_contract_billing`
--

LOCK TABLES `inv_lease_contract_billing` WRITE;
/*!40000 ALTER TABLE `inv_lease_contract_billing` DISABLE KEYS */;
/*!40000 ALTER TABLE `inv_lease_contract_billing` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_lease_contract_fee`
--

DROP TABLE IF EXISTS `inv_lease_contract_fee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_lease_contract_fee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_id` bigint NOT NULL COMMENT '租赁合同ID',
  `fee_item_id` bigint DEFAULT NULL COMMENT '收款项目ID',
  `fee_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '费项名称',
  `charge_type` tinyint DEFAULT NULL COMMENT '收费方式(1固定/2固定提成/3阶梯提成/4取高/5一次性)',
  `unit_price` decimal(14,2) DEFAULT NULL COMMENT '单价',
  `area` decimal(14,2) DEFAULT NULL COMMENT '面积',
  `amount` decimal(14,2) DEFAULT NULL COMMENT '金额',
  `start_date` date DEFAULT NULL COMMENT '费项开始日期',
  `end_date` date DEFAULT NULL COMMENT '费项结束日期',
  `period_index` int DEFAULT NULL COMMENT '租期阶段序号',
  `formula_params` json DEFAULT NULL COMMENT '计算公式参数(JSON，与意向费项保持对称，支持转合同后参数追溯)',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_contract_fee` (`contract_id`,`fee_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租赁合同-费项明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_lease_contract_fee`
--

LOCK TABLES `inv_lease_contract_fee` WRITE;
/*!40000 ALTER TABLE `inv_lease_contract_fee` DISABLE KEYS */;
/*!40000 ALTER TABLE `inv_lease_contract_fee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_lease_contract_fee_stage`
--

DROP TABLE IF EXISTS `inv_lease_contract_fee_stage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_lease_contract_fee_stage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_fee_id` bigint NOT NULL COMMENT '合同费项ID',
  `shop_id` bigint DEFAULT NULL COMMENT '商铺ID',
  `stage_start` date DEFAULT NULL COMMENT '阶段开始日期',
  `stage_end` date DEFAULT NULL COMMENT '阶段结束日期',
  `unit_price` decimal(14,2) DEFAULT NULL COMMENT '该阶段单价',
  `commission_rate` decimal(5,2) DEFAULT NULL COMMENT '提成比例(%)',
  `min_commission_amount` decimal(14,2) DEFAULT NULL COMMENT '最低提成金额(与意向阶段表对称，用于"两者取高"收费方式)',
  `amount` decimal(14,2) DEFAULT NULL COMMENT '该阶段金额',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_fee_stage` (`contract_fee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租赁合同-分铺计租阶段表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_lease_contract_fee_stage`
--

LOCK TABLES `inv_lease_contract_fee_stage` WRITE;
/*!40000 ALTER TABLE `inv_lease_contract_fee_stage` DISABLE KEYS */;
/*!40000 ALTER TABLE `inv_lease_contract_fee_stage` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_lease_contract_shop`
--

DROP TABLE IF EXISTS `inv_lease_contract_shop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_lease_contract_shop` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_id` bigint NOT NULL COMMENT '租赁合同ID',
  `shop_id` bigint NOT NULL COMMENT '商铺ID',
  `building_id` bigint DEFAULT NULL COMMENT '楼栋ID',
  `floor_id` bigint DEFAULT NULL COMMENT '楼层ID',
  `format_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业态',
  `area` decimal(14,2) DEFAULT NULL COMMENT '租赁面积',
  `rent_unit_price` decimal(14,2) DEFAULT NULL COMMENT '租金单价',
  `property_unit_price` decimal(14,2) DEFAULT NULL COMMENT '物管费单价',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_shop` (`contract_id`,`shop_id`,`is_deleted`),
  KEY `idx_shop_contract` (`shop_id`,`contract_id`),
  KEY `idx_shop_status` (`shop_id`,`is_deleted`) COMMENT '商铺状态查询',
  KEY `idx_shop_contract_active` (`shop_id`,`is_deleted`) COMMENT '商铺有效合同查询（查询时需携带 is_deleted=0 条件）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租赁合同-商铺关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_lease_contract_shop`
--

LOCK TABLES `inv_lease_contract_shop` WRITE;
/*!40000 ALTER TABLE `inv_lease_contract_shop` DISABLE KEYS */;
/*!40000 ALTER TABLE `inv_lease_contract_shop` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_lease_contract_version`
--

DROP TABLE IF EXISTS `inv_lease_contract_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_lease_contract_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_id` bigint NOT NULL COMMENT '合同ID',
  `version` int NOT NULL COMMENT '版本号',
  `snapshot_data` json NOT NULL COMMENT '完整合同数据快照(JSON格式)',
  `change_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '变更原因',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_version` (`contract_id`,`version`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合同版本快照表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_lease_contract_version`
--

LOCK TABLES `inv_lease_contract_version` WRITE;
/*!40000 ALTER TABLE `inv_lease_contract_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `inv_lease_contract_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_opening_approval`
--

DROP TABLE IF EXISTS `inv_opening_approval`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_opening_approval` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `approval_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批单号',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `building_id` bigint DEFAULT NULL COMMENT '楼栋ID',
  `floor_id` bigint DEFAULT NULL COMMENT '楼层ID',
  `shop_id` bigint DEFAULT NULL COMMENT '商铺ID',
  `contract_id` bigint DEFAULT NULL COMMENT '关联合同ID',
  `merchant_id` bigint DEFAULT NULL COMMENT '商家ID',
  `planned_opening_date` date DEFAULT NULL COMMENT '计划开业日期',
  `actual_opening_date` date DEFAULT NULL COMMENT '实际开业日期',
  `status` tinyint DEFAULT '0' COMMENT '状态(0待提交/1审批中/2通过/3驳回)',
  `approval_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批流程实例ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `previous_approval_id` bigint DEFAULT NULL COMMENT '被驳回原单ID(用于数据快照恢复)',
  `snapshot_data` json DEFAULT NULL COMMENT '驳回时数据快照(JSON)',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_approval_code_deleted` (`approval_code`,`is_deleted`),
  KEY `idx_project_status` (`project_id`,`status`),
  KEY `idx_contract` (`contract_id`),
  KEY `idx_shop` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='开业审批主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_opening_approval`
--

LOCK TABLES `inv_opening_approval` WRITE;
/*!40000 ALTER TABLE `inv_opening_approval` DISABLE KEYS */;
/*!40000 ALTER TABLE `inv_opening_approval` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_opening_attachment`
--

DROP TABLE IF EXISTS `inv_opening_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_opening_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `opening_approval_id` bigint NOT NULL COMMENT '开业审批ID',
  `file_name` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件名',
  `file_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件地址',
  `file_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件类型',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_opening_approval` (`opening_approval_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='开业审批附件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_opening_attachment`
--

LOCK TABLES `inv_opening_attachment` WRITE;
/*!40000 ALTER TABLE `inv_opening_attachment` DISABLE KEYS */;
/*!40000 ALTER TABLE `inv_opening_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_rent_decomp_detail`
--

DROP TABLE IF EXISTS `inv_rent_decomp_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_rent_decomp_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `decomp_id` bigint NOT NULL COMMENT '租金分解ID',
  `shop_category` tinyint DEFAULT NULL COMMENT '商铺类别(1主力/2次主力/3一般)',
  `format_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业态',
  `rent_unit_price` decimal(14,2) DEFAULT NULL COMMENT '租金单价',
  `property_unit_price` decimal(14,2) DEFAULT NULL COMMENT '物管费单价',
  `area` decimal(14,2) DEFAULT NULL COMMENT '面积',
  `annual_rent` decimal(14,2) DEFAULT NULL COMMENT '标准年租金',
  `annual_fee` decimal(14,2) DEFAULT NULL COMMENT '标准年物管费',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_decomp_category` (`decomp_id`,`shop_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租金分解明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_rent_decomp_detail`
--

LOCK TABLES `inv_rent_decomp_detail` WRITE;
/*!40000 ALTER TABLE `inv_rent_decomp_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `inv_rent_decomp_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_rent_decomposition`
--

DROP TABLE IF EXISTS `inv_rent_decomposition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_rent_decomposition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `decomp_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租金分解编号',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `policy_id` bigint NOT NULL COMMENT '关联租决政策ID',
  `total_annual_rent` decimal(14,2) DEFAULT NULL COMMENT '标准年租金汇总',
  `total_annual_fee` decimal(14,2) DEFAULT NULL COMMENT '标准年物管费汇总',
  `status` tinyint DEFAULT '0' COMMENT '状态(0草稿/1审批中/2通过/3驳回)',
  `approval_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批流程实例ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_decomp_code_deleted` (`decomp_code`,`is_deleted`),
  KEY `idx_project_status` (`project_id`,`status`),
  KEY `idx_policy` (`policy_id`),
  KEY `idx_decomp_business` (`project_id`,`policy_id`,`status`) COMMENT '关联租决政策查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租金分解主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_rent_decomposition`
--

LOCK TABLES `inv_rent_decomposition` WRITE;
/*!40000 ALTER TABLE `inv_rent_decomposition` DISABLE KEYS */;
/*!40000 ALTER TABLE `inv_rent_decomposition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_rent_policy`
--

DROP TABLE IF EXISTS `inv_rent_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_rent_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `policy_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '租决编号',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `policy_type` tinyint DEFAULT NULL COMMENT '租金决策类型',
  `year1_rent` decimal(14,2) DEFAULT NULL COMMENT '第一年租金指标',
  `year2_rent` decimal(14,2) DEFAULT NULL COMMENT '第二年租金指标',
  `year1_property_fee` decimal(14,2) DEFAULT NULL COMMENT '第一年物业指标',
  `year2_property_fee` decimal(14,2) DEFAULT NULL COMMENT '第二年物业指标',
  `shop_attr` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '适用铺位属性',
  `format_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '适用业态',
  `min_lease_term` int DEFAULT NULL COMMENT '租期范围-最小(月)',
  `max_lease_term` int DEFAULT NULL COMMENT '租期范围-最大(月)',
  `rent_growth_rate` decimal(5,2) DEFAULT NULL COMMENT '租金增长率(%)',
  `fee_growth_rate` decimal(5,2) DEFAULT NULL COMMENT '管理费增长率(%)',
  `free_rent_period` int DEFAULT NULL COMMENT '免租期(月)',
  `deposit_months` int DEFAULT NULL COMMENT '租赁保证金月数',
  `payment_cycle` tinyint DEFAULT NULL COMMENT '支付周期',
  `status` tinyint DEFAULT '0' COMMENT '状态(0草稿/1审批中/2通过/3驳回)',
  `approval_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批流程实例ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_policy_code_deleted` (`policy_code`,`is_deleted`),
  KEY `idx_project_status` (`project_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租决政策主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_rent_policy`
--

LOCK TABLES `inv_rent_policy` WRITE;
/*!40000 ALTER TABLE `inv_rent_policy` DISABLE KEYS */;
/*!40000 ALTER TABLE `inv_rent_policy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inv_rent_policy_indicator`
--

DROP TABLE IF EXISTS `inv_rent_policy_indicator`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inv_rent_policy_indicator` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `policy_id` bigint NOT NULL COMMENT '租决政策ID',
  `shop_category` tinyint NOT NULL COMMENT '商铺类别(1主力店/2次主力店/3一般商铺)',
  `rent_price` decimal(14,2) DEFAULT NULL COMMENT '租金单价(元/㎡·月)',
  `property_fee_price` decimal(14,2) DEFAULT NULL COMMENT '物管费单价(元/㎡·月)',
  `format_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业态',
  `rent_growth_rate` decimal(5,2) DEFAULT NULL COMMENT '租金增长率(覆盖政策级)',
  `fee_growth_rate` decimal(5,2) DEFAULT NULL COMMENT '管理费增长率',
  `free_rent_months` int DEFAULT NULL COMMENT '免租期(月)',
  `deposit_months` int DEFAULT NULL COMMENT '保证金月数',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_policy_category` (`policy_id`,`shop_category`,`is_deleted`),
  KEY `idx_policy_id` (`policy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租决政策-分类指标表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inv_rent_policy_indicator`
--

LOCK TABLES `inv_rent_policy_indicator` WRITE;
/*!40000 ALTER TABLE `inv_rent_policy_indicator` DISABLE KEYS */;
/*!40000 ALTER TABLE `inv_rent_policy_indicator` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_company`
--

DROP TABLE IF EXISTS `sys_company`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_company` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `company_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公司编码',
  `company_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公司名称',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0停用 1启用',
  `created_by` bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `company_code` (`company_code`),
  KEY `idx_company_code` (`company_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_company`
--

LOCK TABLES `sys_company` WRITE;
/*!40000 ALTER TABLE `sys_company` DISABLE KEYS */;
INSERT INTO `sys_company` (`id`, `company_code`, `company_name`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`) VALUES (4,'CC001','产城（总部）投资管理有限公司',1,NULL,'2026-02-22 00:14:21',NULL,'2026-02-22 00:14:21'),(5,'CC002','产城（北京）资产管理有限公司',1,NULL,'2026-02-22 00:14:21',NULL,'2026-02-22 00:14:21'),(6,'CC003','产城（上海）资产管理有限公司',1,NULL,'2026-02-22 00:14:21',NULL,'2026-02-22 00:14:21');
/*!40000 ALTER TABLE `sys_company` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型',
  `dict_label` varchar(100) NOT NULL COMMENT '字典标签',
  `dict_value` varchar(100) NOT NULL COMMENT '字典值',
  `sort_order` int DEFAULT '0',
  `status` tinyint DEFAULT '1',
  `remark` varchar(500) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_name` varchar(100) NOT NULL COMMENT '字典名称',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型',
  `status` tinyint DEFAULT '1',
  `remark` varchar(500) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_type`
--

LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `menu_name` varchar(100) NOT NULL COMMENT '菜单名称',
  `menu_type` char(1) NOT NULL COMMENT '类型(M目录 C菜单 F按钮)',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) DEFAULT '' COMMENT '组件路径',
  `perms` varchar(200) DEFAULT '' COMMENT '权限标识',
  `icon` varchar(100) DEFAULT '' COMMENT '图标',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `visible` tinyint DEFAULT '1' COMMENT '是否可见',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module` varchar(50) DEFAULT '' COMMENT '模块',
  `biz_type` varchar(50) DEFAULT '' COMMENT '业务类型',
  `method` varchar(200) DEFAULT '' COMMENT '方法名',
  `request_method` varchar(10) DEFAULT '' COMMENT 'HTTP方法',
  `request_url` varchar(500) DEFAULT '' COMMENT '请求URL',
  `request_param` text COMMENT '请求参数',
  `response_result` text COMMENT '响应结果',
  `oper_user` varchar(50) DEFAULT '' COMMENT '操作人',
  `oper_ip` varchar(128) DEFAULT '' COMMENT '操作IP',
  `status` tinyint DEFAULT '1' COMMENT '状态(1成功 0失败)',
  `error_msg` text COMMENT '错误消息',
  `cost_time` bigint DEFAULT '0' COMMENT '耗时(ms)',
  `oper_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `tenant_id` bigint DEFAULT '0' COMMENT '租户ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `data_scope` tinyint DEFAULT '1' COMMENT '数据权限(1全部 2自定义 3本部门 4本部门及以下 5本人)',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `create_by` varchar(50) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(50) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'SM3å¯†ç å“ˆå¸Œ',
  `real_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '真实姓名',
  `status` tinyint DEFAULT '1' COMMENT '状态: 0停用 1启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `status`, `created_at`, `updated_at`) VALUES (1,'admin','667c756cf9334e328a56e44e906245c8e214c655a160f18fdb84d79c209c49cf','系统管理员',1,'2026-02-21 06:30:21','2026-02-22 00:15:40');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'asset_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-26  6:02:58
