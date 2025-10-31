-- AIDB.ai_config definition

CREATE TABLE `ai_config`
(
    `id`               int          NOT NULL AUTO_INCREMENT COMMENT '主键，自增ID',
    `display_name`     varchar(64)  NOT NULL COMMENT '显示名称',
    `api_domain`       varchar(255) NOT NULL COMMENT 'API域名',
    `model_name`       varchar(128) NOT NULL COMMENT '模型名称',
    `model_type`       tinyint      NOT NULL COMMENT '模型类型：0-大模型，1-文本向量，2-视觉模型',
    `model_id`         varchar(128) NOT NULL COMMENT '模型ID',
    `api_key`          varchar(255) NOT NULL COMMENT 'API密钥',
    `max_context_msgs` int          DEFAULT '4096' COMMENT '上下文最大消息数',
    `similarity_top_p` float        DEFAULT '1' COMMENT '相似度TopP',
    `temperature`      float        DEFAULT '0.2' COMMENT '温度，控制输出随机性，越大随机性越高:0-2.0',
    `similarity_top_k` float        DEFAULT '0.1' COMMENT '相似度TopK',
    `is_default`       tinyint(1)   DEFAULT '0' COMMENT '是否为默认模型(是0/否1)',
    `case_tags`        varchar(255) DEFAULT NULL COMMENT '标签',
    `case_brief`       varchar(255) DEFAULT NULL COMMENT '简介',
    `case_remark`      varchar(255) DEFAULT NULL COMMENT '备注',
    `is_enabled`       tinyint(1)   DEFAULT '1' COMMENT '是否启用(是0/否1)',
    `create_time`      datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='AI配置信息表';


-- AIDB.ai_message_pair definition

CREATE TABLE `ai_message_pair`
(
    `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '主键，自增ID',
    `session_id`     bigint      NOT NULL COMMENT '会话ID',
    `sse_session_id` varchar(64) NOT NULL COMMENT 'SSE会话ID',
    `user_content`   text        NOT NULL COMMENT '用户提问内容',
    `ai_content`     mediumtext COMMENT 'AI回复内容',
    `model_used`     int                  DEFAULT NULL COMMENT '使用模型id',
    `status`         tinyint              DEFAULT '0' COMMENT '状态：0-生成中 1-完成 2-中断',
    `tokens`         int unsigned         DEFAULT NULL COMMENT '本轮消耗的Token',
    `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '用户提问时间',
    `response_time`  datetime             DEFAULT NULL COMMENT 'AI回复完成时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='一轮问答记录表';


-- AIDB.ai_session definition

CREATE TABLE `ai_session`
(
    `id`                bigint   NOT NULL AUTO_INCREMENT COMMENT '主键，自增ID',
    `title`             varchar(255)      DEFAULT NULL COMMENT '对话主题',
    `is_favorite`       tinyint(1)        DEFAULT '0' COMMENT '是否收藏',
    `created_time`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `last_message_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后对话时间',
    `user_id`           bigint   NOT NULL COMMENT '用户ID',
    `model_id`          int               DEFAULT NULL COMMENT '使用的模型ID',
    `tags`              varchar(255)      DEFAULT NULL COMMENT '标签，逗号分隔',
    `summary`           text COMMENT '对话摘要',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='AI会话记录表';


-- AIDB.`user` definition

CREATE TABLE `user`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    varchar(50)     NOT NULL COMMENT '用户名',
    `password`    varchar(512)    NOT NULL COMMENT '加密后的密码',
    `email`       varchar(100)             DEFAULT NULL COMMENT '邮箱',
    `phone`       varchar(20)              DEFAULT NULL COMMENT '手机号',
    `nickname`    varchar(50)              DEFAULT NULL COMMENT '昵称',
    `avatar_url`  varchar(255)             DEFAULT NULL COMMENT '头像链接',
    `gender`      tinyint                  DEFAULT '0' COMMENT '性别：0=未知，1=男，2=女',
    `birthdate`   date                     DEFAULT NULL COMMENT '出生日期',
    `status`      tinyint         NOT NULL DEFAULT '1' COMMENT '状态：0=禁用，1=启用',
    `role`        bigint unsigned NOT NULL DEFAULT '0' COMMENT '角色id',
    `create_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     tinyint         NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户信息表';

-- AIDB.`rag_store` definition

-- RAG向量检索系统文档存储表
CREATE TABLE `rag_store`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文档唯一标识ID',
    `doc_id`           VARCHAR(64)     NOT NULL COMMENT '文档业务ID（可用于外部关联）',
    `title`            VARCHAR(255)    NOT NULL COMMENT '文档标题',
    `index_name`       VARCHAR(64)    NOT NULL COMMENT '文档索引名',
    `content`          TEXT            NOT NULL COMMENT '文档原始内容',
    `content_summary`  VARCHAR(500)             DEFAULT NULL COMMENT '文档内容摘要',

    -- 向量相关字段
    `embedding_model`  int                      DEFAULT NULL COMMENT '使用的向量模型在ai_config表中对应的id',
    `vector_dimension` INT                      DEFAULT NULL COMMENT '向量维度',

    -- 文档分块信息
    `chunk_index`      INT                      DEFAULT 0 COMMENT '文档块索引（0表示完整文档，>0表示分块）',
    `chunk_total`      INT                      DEFAULT 1 COMMENT '总块数',
    `parent_doc_id`    VARCHAR(64)              DEFAULT NULL COMMENT '父文档ID（用于分块文档关联）',

    -- 文档元信息
    `doc_type`         VARCHAR(50)              DEFAULT NULL COMMENT '文档类型（txt/pdf/docx/md等）',
    `file_size`        BIGINT                   DEFAULT NULL COMMENT '文件大小（字节）',
    `language`         VARCHAR(10)              DEFAULT 'zh' COMMENT '文档语言（zh/en等）',

    -- 用户与权限
    `user_id`          BIGINT UNSIGNED          DEFAULT NULL COMMENT '文档所属用户ID',

    -- 处理状态
    `status`           TINYINT(1)               DEFAULT 0 COMMENT '处理状态（0-待处理，1-已向量化，2-处理失败）',
    `error_message`    VARCHAR(500)             DEFAULT NULL COMMENT '错误信息',

    -- 时间戳
    `created_time`     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`     TIMESTAMP                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT         NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=正常，1=已删除',

    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_doc_type` (`doc_type`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_time`),
    KEY `idx_parent_doc_id` (`parent_doc_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='RAG向量检索文档存储表';