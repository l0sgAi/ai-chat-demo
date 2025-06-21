-- --------------------------------------------------------
-- 主机:                           192.168.200.132
-- 服务器版本:                        8.3.0 - MySQL Community Server - GPL
-- 服务器操作系统:                      Linux
-- HeidiSQL 版本:                  12.6.0.6765
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- 导出 AIDB 的数据库结构
CREATE DATABASE IF NOT EXISTS `AIDB` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `AIDB`;

-- 导出  表 AIDB.ai_config 结构
CREATE TABLE IF NOT EXISTS `ai_config` (
                                           `id` int NOT NULL AUTO_INCREMENT COMMENT '主键，自增ID',
                                           `display_name` varchar(64) NOT NULL COMMENT '显示名称',
                                           `api_domain` varchar(255) NOT NULL COMMENT 'API域名',
                                           `model_name` varchar(128) NOT NULL COMMENT '模型名称',
                                           `model_type` tinyint NOT NULL COMMENT '模型类型：0-大模型，1-文本向量，2-视觉模型',
                                           `model_id` varchar(128) NOT NULL COMMENT '模型ID',
                                           `api_key` varchar(255) NOT NULL COMMENT 'API密钥',
                                           `max_context_msgs` int DEFAULT '4096' COMMENT '上下文最大消息数',
                                           `similarity_top_p` float DEFAULT '1' COMMENT '相似度TopP',
                                           `temperature` float DEFAULT '0.2' COMMENT '温度，控制输出随机性，越大随机性越高:0-2.0',
                                           `similarity_top_k` float DEFAULT '0.1' COMMENT '相似度TopK',
                                           `is_default` tinyint(1) DEFAULT '0' COMMENT '是否为默认模型(是0/否1)',
                                           `case_tags` varchar(255) DEFAULT NULL COMMENT '标签',
                                           `case_brief` varchar(255) DEFAULT NULL COMMENT '简介',
                                           `case_remark` varchar(255) DEFAULT NULL COMMENT '备注',
                                           `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI配置信息表';

-- 数据导出被取消选择。

-- 导出  表 AIDB.ai_message_pair 结构
CREATE TABLE IF NOT EXISTS `ai_message_pair` (
                                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，自增ID',
                                                 `session_id` bigint NOT NULL COMMENT '会话ID',
                                                 `sse_session_id` varchar(64) NOT NULL COMMENT 'SSE会话ID',
                                                 `user_content` text NOT NULL COMMENT '用户提问内容',
                                                 `ai_content` mediumtext COMMENT 'AI回复内容',
                                                 `model_used` int DEFAULT NULL COMMENT '使用模型id',
                                                 `status` tinyint DEFAULT '0' COMMENT '状态：0-生成中 1-完成 2-中断',
                                                 `tokens` int unsigned DEFAULT NULL COMMENT '本轮消耗的Token',
                                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '用户提问时间',
                                                 `response_time` datetime DEFAULT NULL COMMENT 'AI回复完成时间',
                                                 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='一轮问答记录表';

-- 数据导出被取消选择。

-- 导出  表 AIDB.ai_session 结构
CREATE TABLE IF NOT EXISTS `ai_session` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，自增ID',
                                            `title` varchar(255) DEFAULT NULL COMMENT '对话主题',
                                            `is_favorite` tinyint(1) DEFAULT '0' COMMENT '是否收藏',
                                            `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `last_message_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后对话时间',
                                            `user_id` bigint NOT NULL COMMENT '用户ID',
                                            `model_id` int DEFAULT NULL COMMENT '使用的模型ID',
                                            `tags` varchar(255) DEFAULT NULL COMMENT '标签，逗号分隔',
                                            `summary` text COMMENT '对话摘要',
                                            PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI会话记录表';

-- 数据导出被取消选择。

-- 导出  表 AIDB.user 结构
CREATE TABLE IF NOT EXISTS `user` (
                                      `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                                      `username` varchar(50) NOT NULL COMMENT '用户名',
                                      `password` varchar(512) NOT NULL COMMENT '加密后的密码',
                                      `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
                                      `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
                                      `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
                                      `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像链接',
                                      `gender` tinyint DEFAULT '0' COMMENT '性别：0=未知，1=男，2=女',
                                      `birthdate` date DEFAULT NULL COMMENT '出生日期',
                                      `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0=禁用，1=启用',
                                      `role` bigint unsigned NOT NULL DEFAULT '0' COMMENT '角色id',
                                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
                                      PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';

-- 数据导出被取消选择。

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
