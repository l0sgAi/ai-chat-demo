
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI配置信息表';

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
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_sse_session_id` (`sse_session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='一轮问答记录表';

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

