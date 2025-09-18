# Milvus向量数据库配置指南

本项目已集成Milvus向量数据库，实现文档向量的持久化存储，避免每次启动项目时重新嵌入文档。

## 1. 版本配置

### 当前版本组合
- **Milvus Java SDK**: 2.6.2
- **langchain4j-milvus**: 1.1.0-beta7
- **Milvus服务器**: v2.6.2

### 功能特性
- ✅ **向量存储**：使用Milvus进行持久化存储
- ✅ **RAG功能**：完全正常工作
- ✅ **持久化**：向量数据持久化到Milvus，重启后无需重新嵌入
- ✅ **性能**：启动和运行性能良好

## 2. 项目配置

### 2.1 依赖已添加

项目已自动添加以下依赖：

```xml
<!-- Milvus Java SDK -->
<dependency>
    <groupId>io.milvus</groupId>
    <artifactId>milvus-sdk-java</artifactId>
    <version>2.6.2</version>
</dependency>

<!-- LangChain4j Milvus Embedding Store -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-milvus</artifactId>
    <version>1.1.0-beta7</version>
</dependency>
```

### 2.2 配置文件

在 `application.properties` 中已添加Milvus配置：

```properties
# Milvus Configuration
milvus.host=localhost
milvus.port=19530
milvus.database=default
milvus.collection=default
milvus.dimension=1536
```

**注意**：确保Milvus服务端已禁用身份验证，因为langchain4j-milvus不支持用户名密码认证。

## 3. 功能特性

### 3.1 自动初始化

- 项目启动时会自动连接到Milvus服务
- **自动创建Collection**：langchain4j-milvus会自动创建名为`ai4education_vectors`的collection
- 自动将 `src/main/resources/questions/` 目录下的文档进行向量化并存储
- 如果collection已存在，会直接使用现有的collection

### 3.2 文档管理API

新增了文档管理接口：

#### 重新初始化文档向量
```http
POST /api/document/reinitialize
```

#### 上传新文档
```http
POST /api/document/upload
Content-Type: multipart/form-data

file: [文档文件]
```

#### 获取向量存储状态
```http
GET /api/document/status
```

### 3.3 持久化存储

- 向量数据存储在Milvus中，重启项目后无需重新嵌入
- 支持增量添加新文档
- 自动管理向量集合的创建和索引

## 4. 使用说明

### 4.1 启动Milvus服务

1. 启动Milvus服务：
   ```bash
   docker-compose -f docker-compose-milvus.yml up -d
   ```

2. 验证Milvus服务：
   ```bash
   curl http://localhost:9091/healthz
   ```

### 4.2 启动应用

1. 启动Spring Boot应用：
   ```bash
   mvn spring-boot:run
   ```

2. 应用会自动：
   - 连接到Milvus数据库
   - 初始化向量存储
   - 加载并向量化 `src/main/resources/questions/` 目录下的文档
   - 提供完整的RAG功能

### 4.3 添加新文档

1. 将新文档放入 `src/main/resources/questions/` 目录
2. 调用重新初始化接口：`POST /api/document/reinitialize`
3. 或者通过上传接口直接添加：`POST /api/document/upload`

### 4.4 监控和维护

- 使用 `GET /api/document/status` 检查向量存储状态
- 通过Milvus控制台（http://localhost:9001）管理向量数据
- 默认用户名/密码：minioadmin/minioadmin
- 向量数据已持久化，重启应用后无需重新嵌入

## 5. 故障排除

### 5.1 连接问题

如果遇到连接Milvus失败：

1. 检查Milvus服务是否运行：`docker ps`
2. 检查端口是否开放：`netstat -tlnp | grep 19530`
3. 检查防火墙设置

### 5.2 向量维度问题

如果遇到向量维度不匹配：

1. 确认 `milvus.dimension` 配置与嵌入模型输出维度一致
2. 通义千问text-embedding-v1模型的输出维度是1536

### 5.3 Collection管理

**自动创建Collection**：
- langchain4j-milvus会自动创建名为`ai4education_vectors`的collection
- 你不需要手动创建collection
- 如果collection已存在，会直接使用

**重新创建Collection**：
如果需要重新创建向量集合：

1. 通过Milvus控制台删除集合：`ai4education_vectors`
2. 重启应用，会自动重新创建

**Collection配置**：
- Collection名称在`application.properties`中配置：`milvus.collection=ai4education_vectors`
- 向量维度：1536（通义千问text-embedding-v4的输出维度）

## 6. 性能优化

### 6.1 索引配置

可以根据需要调整Milvus的索引配置，在 `MilvusConfig.java` 中修改：

```java
.indexType(IndexType.IVF_FLAT)
.indexParameter(IndexParameter.ivfFlat().nlist(1536))
```

### 6.2 批量操作

对于大量文档，建议使用批量上传接口而不是单个文件上传。

## 7. 注意事项

1. 确保Milvus服务在应用启动前已经运行
2. 向量数据会占用磁盘空间，注意监控存储使用情况
3. 定期备份Milvus数据目录
4. 生产环境建议使用集群版本的Milvus
