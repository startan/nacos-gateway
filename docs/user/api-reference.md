# API 参考

## 1. 管理接口

### 1.1 健康检查

**端点**: `GET /health`

**响应示例**:
```json
{
  "status": "UP"
}
```

## 2. 配置协议

### 2.1 配置路径格式

| 协议 | 格式 | 热更新 |
|------|------|--------|
| file:// | `file:///path/to/config.yaml` | ✅ |
| classpath:// | `classpath://config.yaml` | ❌ |
| nacos:// | `nacos://dataId:group?params` | ✅ |

### 2.2 Nacos 协议参数

所有查询参数直接透传给 Nacos Client SDK，支持的参数取决于 Nacos 版本。

**常用参数**：

| 参数 | 说明 |
|------|------|
| serverAddr | Nacos 服务器地址（如 `127.0.0.1:8848`） |
| namespace | 命名空间 ID |
| username | 认证用户名 |
| password | 认证密码 |
| accessKey | 阿里云 AK 认证 |
| secretKey | 阿里云 SK 认证 |

**URL 格式**：
```
nacos://<dataId>[:<group>]?<query-parameters>
```

- `dataId`：必需，配置文件标识符
- `group`：可选，用 `:` 分隔，默认 `DEFAULT_GROUP`
- `query-parameters`：所有参数透传给 Nacos Client

**示例**：
```bash
# 基础配置（使用默认 group）
java -jar nacos-gateway.jar -c "nacos://config.yaml?serverAddr=127.0.0.1:8848"

# 指定 group 和命名空间
java -jar nacos-gateway.jar -c "nacos://config.yaml:prod-group?serverAddr=127.0.0.1:8848&namespace=prod"

# AK/SK 认证
java -jar nacos-gateway.jar -c "nacos://config.yaml?serverAddr=127.0.0.1:8848&accessKey=key&secretKey=secret"
```

## 3. 负载均衡策略

| 策略 | 值 |
|------|-----|
| 轮询 | round-robin |
| 随机 | random |
| 最少连接 | least-connection |

## 4. 健康检查类型

| 类型 | 值 | 说明 |
|------|-----|------|
| HTTP | http | HTTP 探测 |
| TCP | tcp | TCP 连接探测 |

## 5. 限流值语义

| 值 | 含义 |
|----|------|
| -1 | 无限制 |
| 0 | 拒绝所有访问 |
| > 0 | 正常限流 |

## 6. HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求错误 |
| 404 | 后端未找到 |
| 429 | 限流拒绝 |
| 502 | 后端不可用 |
| 503 | 后端不健康 |
