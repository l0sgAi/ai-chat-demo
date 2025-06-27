# 项目说明：

### 在线考试系统

1. 前端使用VUE3+NaiveUI框架开发

2. 服务器端采用 Redis+MySQL+SpringBoot框架进行开发
   需求说明：

- 用户信息管理功能：实现考生基本信息的录入与管理，包括姓名、学号、班级等，并将数据保存到对应数据库中。

- 试题库管理功能：支持管理员创建、编辑和删除试题，形成标准化的试题库，供后续考试使用。

- 在线考试功能：允许考生在指定时间内进行在线考试，系统自动随机出题，确保考试的公平性与多样性。

- 成绩评估功能：考试结束后，系统自动评阅试卷并生成成绩报告，考生可随时查询自己的考试成绩和详细分析。

- 数据统计与分析功能：提供考试参与人数、平均成绩、通过率等基础统计数据，此外，使用图表显示成绩分布、班级平均对比、用时分布统计和考试趋势分析。帮助教师和管理者评估教学效果和考试质量。


### 逻辑流程

1. 只能登录，不能主动注册，由管理员统一管理，目前管理员id为21。

2. 管理员可以管理用户信息、试题库、考试信息、成绩信息，还可以查看统计数据。

3. 学生可以修改密码、进行考试、查看历史成绩。

4. 其中，出题的时候，对于每个学生，系统随机抽取题库，按照题型抽取，选择题10道，每题3分，3容易5中等2困难；判断题10道，每题2分，3容易5中等2困难；简答题5道，每题10分，1容易2中等2困难。

5. 考试结束的时候，学生手动提交或到时间系统自动提交，选择判断题直接出成绩，简答题交给AI判断分数，先插入选择判断题的结果，打分完成后更新分数，并可以查看报告：学生点击”查看详情“显示对应的报告，使用json字符串储存对应的数据，在前端进行美观输出。

6. 通过各个学生的分数结果，进行数据统计。


### 架构设计

**表结构：**

```sql
-- 导出  表 AIDB.ai_config 结构
CREATE TABLE IF NOT EXISTS `ai_config`
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
    `create_time`      datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 4
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='AI配置信息表';

-- 导出  表 AIDB.question_bank 结构
CREATE TABLE IF NOT EXISTS `question_bank`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键，自增ID',
    `title`         varchar(255) NOT NULL COMMENT '试题标题',
    `type`          int          NOT NULL COMMENT '试题类型，选择题0,判断题1,简答题2',
    `level`         int          NOT NULL COMMENT '难度等级：易0、中等1、难2',
    `content`       text         NOT NULL COMMENT '试题内容',
    `answer`        text         NOT NULL COMMENT '正确答案-文本',
    `answer_option` int          NOT NULL COMMENT '正确答案-选项 abcd对应0123 正确/错误 对应 0/1',
    `explanation`   text COMMENT '试题解析说明',
    `created_time`  datetime   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`  datetime   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `deleted`       tinyint(1) DEFAULT '0' COMMENT '逻辑删除标志，0表示未删除，1表示已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 89
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='试题库表';

-- 导出  表 AIDB.test 结构
CREATE TABLE IF NOT EXISTS `test`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT '主键，自增ID',
    `name`             varchar(255) NOT NULL COMMENT '考试名称',
    `created_time`     datetime   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`     datetime   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `start_time`       datetime     NOT NULL COMMENT '考试开始时间',
    `end_time`         datetime     NOT NULL COMMENT '考试结束时间',
    `duration_minutes` int          NOT NULL COMMENT '考试持续时间-秒',
    `deleted`          tinyint(1) DEFAULT '0' COMMENT '逻辑删除标志，0表示未删除，1表示已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 17
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='考试结果表';

-- 导出  表 AIDB.test_result 结构
CREATE TABLE IF NOT EXISTS `test_result`
(
    `id`           bigint NOT NULL AUTO_INCREMENT COMMENT '主键，自增ID',
    `user_id`      bigint NOT NULL COMMENT '对应用户ID',
    `test_id`      bigint NOT NULL COMMENT '对应考试ID',
    `content`      text   NOT NULL COMMENT '考试结果报告内容-json格式,即为具体题目数据列表',
    `time_used`    int    NOT NULL COMMENT '考试用时(单位-秒)',
    `score`        int    NOT NULL COMMENT '最终得分',
    `status`       int    NOT NULL DEFAULT '0' COMMENT '状态0-初始 1-已经保存 2-已经提交 3-评分异常',
    `created_time` datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`      tinyint(1)      DEFAULT '0' COMMENT '逻辑删除标志，0表示未删除，1表示已删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 36
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='考试结果表';

-- 导出  表 AIDB.user 结构
CREATE TABLE IF NOT EXISTS `user`
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
  AUTO_INCREMENT = 34
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户信息表';

```