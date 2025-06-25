# 项目说明：

### 在线考试系统

1. 前端使用`VUE3`+`NaiveUI`框架开发

2. 服务器端采用 `Redis`+`MySQL`+`SpringBoot`框架进行开发
   需求说明：

- 用户信息管理功能：实现考生基本信息的录入与管理，包括姓名、学号、班级等，并将数据保存到对应数据库中。

- 试题库管理功能：支持管理员创建、编辑和删除试题，形成标准化的试题库，供后续考试使用。

- 在线考试功能：允许考生在指定时间内进行在线考试，系统自动随机出题，确保考试的公平性与多样性。

- 成绩评估功能：考试结束后，系统自动评阅试卷并生成成绩报告，考生可随时查询自己的考试成绩和详细分析。

- 数据统计与分析功能：提供考试参与人数、平均成绩、通过率等统计数据，帮助教师和管理者评估教学效果和考试质量。

### 逻辑流程

1. 只能登录，不能主动注册，由管理员统一管理，目前管理员`id`为`21`。

2. 管理员可以管理用户信息、试题库、考试信息、成绩信息，还可以查看统计数据。

3. 学生可以修改密码、进行考试、查看历史成绩。

4. 其中，出题的时候，对于每个学生，系统随机抽取题库，按照题型抽取，选择题10道，每题`3`分，`3`容易`5`中等`2`困难；判断题`10`道，每题
   `2`分，`3`容易`5`中等`2`困难；简答题`5`道，每题`10`分，`1`容易`2`中等`2`困难。

5. 考试结束的时候，学生手动提交或到时间系统自动提交，选择判断题直接出成绩，简答题交给`AI`
   判断分数，先插入选择判断题的结果，打分完成后更新分数，并可以查看报告：学生点击”查看详情“显示对应的报告，使用`json`
   字符串储存对应的数据，在前端进行美观输出。

6. 通过各个学生的分数结果，进行数据统计。

### 架构设计

##### 用户表：

```sql
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
  AUTO_INCREMENT = 4
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户信息表';
```

##### 题目表：

```sql
CREATE TABLE question_bank
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增ID',
    title         VARCHAR(255) NOT NULL COMMENT '试题标题',
    type          int          NOT NULL COMMENT '试题类型，选择题0,判断题1,简答题2',
    level         int          NOT NULL COMMENT '难度等级：易0、中等1、难2',
    content       TEXT         NOT NULL COMMENT '试题内容',
    answer        TEXT         NOT NULL COMMENT '正确答案-文本',
    answer_option int          NOT NULL COMMENT '正确答案-选项 abcd对应0123 正确/错误 对应 0/1',
    explanation   TEXT COMMENT '试题解析说明',
    created_time  DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time  DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    deleted       TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标志，0表示未删除，1表示已删除'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='试题库表';
```

##### 考试信息表:

```sql
CREATE TABLE test
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增ID',
    name             VARCHAR(255) NOT NULL COMMENT '考试名称',
    created_time     DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time     DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    start_time       DATETIME     NOT NULL COMMENT '考试开始时间',
    end_time         DATETIME     NOT NULL COMMENT '考试结束时间',
    duration_minutes INT          NOT NULL COMMENT '考试持续时间-秒',
    deleted          TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标志，0表示未删除，1表示已删除'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='考试结果表';
```

##### 考试结果表:

```sql
CREATE TABLE test_result
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键，自增ID',
    user_id      BIGINT NOT NULL COMMENT '对应用户ID',
    test_id      BIGINT NOT NULL COMMENT '对应考试ID',
    content      TEXT   NOT NULL COMMENT '考试结果报告内容-json格式,即为具体题目数据列表',
    time_used    INT    NOT NULL COMMENT '考试用时(单位-秒)',
    score        INT    NOT NULL COMMENT '最终得分',
    created_time DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted      TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标志，0表示未删除，1表示已删除'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='考试结果表';
```