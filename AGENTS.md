# AGENTS.md

本文件为在本仓库中工作的 AI 编程代理提供项目约定和操作指南。

## 项目概览

这是一个 Java 17 + Spring Boot 2.4.5 的教育 AI 后端项目。核心能力包括学生账号、错题管理、AI 解题/错因分析、SSE 流式对话、学生画像、OSS 文件上传和基于 Milvus 的 RAG 文档检索。

主入口：`src/main/java/org/musi/AI4Education/GreenFarmApplication.java`

## 重要目录

```text
src/main/java/org/musi/AI4Education/controller   # REST API
src/main/java/org/musi/AI4Education/service      # 服务接口
src/main/java/org/musi/AI4Education/service/impl # 服务实现
src/main/java/org/musi/AI4Education/mapper       # MyBatis-Plus Mapper
src/main/java/org/musi/AI4Education/domain       # 实体和 DTO
src/main/java/org/musi/AI4Education/config       # Spring 配置
src/main/java/org/musi/AI4Education/utils        # 工具类
src/main/resources/application.properties        # 应用配置
src/main/resources/mysql/education.sql           # MySQL 初始化 SQL
```

## 构建与测试

使用本机 Maven：

```bash
mvn clean test
mvn clean package
mvn spring-boot:run
```

## 运行环境

默认服务地址：`http://localhost:8443/api`

本地服务依赖：

- MySQL：`127.0.0.1:3306/education1`
- MongoDB：`127.0.0.1:27017/my_db`
- Redis：按 `RedisConfig` 和 Spring Boot 默认配置连接
- Milvus：RAG 功能需要，需根据本机环境自行启动服务
- 阿里云 OSS、DeepSeek、通义千问、SimpleTex OCR：通过环境变量覆盖密钥

## 数据库

`src/main/resources/mysql/education.sql` 是当前 MySQL 初始化脚本。更新数据库脚本时，应从 `education1` 的实际表结构和数据导出，避免手写字段导致实体、Mapper 和 SQL 不一致。

常用导入命令：

```bash
mysql -h127.0.0.1 -uroot -p education1 < src/main/resources/mysql/education.sql
```

## 代码约定

- 继续使用现有分层：Controller -> Service -> ServiceImpl -> Mapper -> Domain。
- 新增数据库访问优先使用 MyBatis-Plus Mapper。
- Controller 返回值尽量沿用 `CommonResponse` 风格。
- 新增配置放入 `config/`，配置项放入 `application.properties`，敏感值使用 `${ENV_NAME:default}` 形式。
- 不要把密钥、真实 token、账号密码写死到新代码或文档中。
- 不要提交 `target/` 下的编译产物。
- 不要为简单修复引入新的抽象层或大规模重构。

## API 约定

项目设置了统一上下文路径：

```properties
server.servlet.context-path=/api
```

文档和测试接口时需要带 `/api` 前缀。多个控制器使用 `/student` 作为一级路径，例如：

- `/api/student/login`
- `/api/student/bigModel`
- `/api/student/question/base`
- `/api/student/chat/inspiration`

## AI 与 RAG 注意事项

- 大模型相关实现集中在 `ChatGPTServiceImpl`、`GptServiceImpl`、`QuestionCreationServiceImpl`、模型配置类和 prompt 文件中。
- RAG 相关实现集中在 `rag/`、`DocumentVectorService`、`DocumentController`、`MilvusConfig`。
- 修改流式接口时注意 `MediaType.TEXT_EVENT_STREAM_VALUE` 和 SSE 输出格式。
- 提示词模板集中放在 `src/main/resources/prompts/`，按 `math/`、`analysis/`、`chat/`、`student/` 分类。
- Java 侧提示词 key 统一维护在 `src/main/java/org/musi/AI4Education/prompt/PromptKeys.java`。
- 业务代码应通过 `PromptTemplateService` 读取或渲染模板，不要散落硬编码长提示词。

## 安全注意事项

- 现有配置中可能包含历史默认密钥；新增内容必须使用环境变量占位，不要扩散真实密钥。
- 文件上传接口应保持边界检查，避免路径穿越和不受控文件写入。
- 调用外部命令或 Python 脚本时，参数必须来自受控输入或经过边界校验。
- 修改认证、登录态、CORS、Cookie 或 Sa-Token 配置时，需要额外检查跨域和会话安全影响。

## 变更前检查

开始修改前建议先检查：

```bash
git status --short
```

仓库可能包含用户本地未提交改动，尤其是 `target/` 编译输出。不要删除或覆盖不属于当前任务的改动。

## 完成前检查

根据改动范围运行最小必要验证：

```bash
mvn test
```

如果改动涉及启动配置、Bean 装配、Controller 或数据库映射，优先运行：

```bash
mvn spring-boot:run
```

如因本地 MySQL、MongoDB、Redis、Milvus 或外部 API 密钥缺失无法验证，应在最终回复中明确说明。
