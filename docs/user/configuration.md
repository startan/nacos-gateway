# 配置指南

## 1. 配置文件结构

```yaml
# 网关服务配置
server:
  ports:
    apiV1: 18848                   # Nacos V1 接口端口
    apiV2: 19848                   # Nacos V2 接口端口
    apiConsole: 18080              # 控制台端口
  rateLimit:
    maxQps: 2000                   # 全局 QPS 限制
    maxConnections: 10000           # 全局连接数限制
    maxQpsPerClient: 10            # 单客户端 QPS 限制
    maxConnectionsPerClient: 5     # 单客户端连接数限制

# 路由规则
routes:
  - host: "group1.nacos.io"        # 请求域名（支持通配符）
    backend: group1-service         # 后端服务名称

# 后端服务配置
backends:
  - name: group1-service
    ports:
      apiV1: 8848
      apiV2: 9848
      apiConsole: 8080
    probe:
      enabled: true
      type: http                    # http 或 tcp
      path: /health
      periodSeconds: 10
      timeoutSeconds: 1
      successThreshold: 1
      failureThreshold: 3
    loadBalance: round-robin         # round-robin/random/least-connection
    rateLimit:
      maxQps: 1000
      maxConnections: 2000
      maxQpsPerClient: 10
      maxConnectionsPerClient: 5
    endpoints:
      - host: 10.12.23.1
        priority: 10
      - host: 10.12.23.2
        priority: 10

# 访问日志配置（可选）
accessLog:
  enabled: false
  format: pattern                   # pattern 或 json
  pattern: "%h - - [%t] \"%m %U %H\" %s %b %D"
  output:
    path: logs/access.log
  rotation:
    policy: daily
    maxHistory: 30
```

## 2. 限流配置

### 2.1 配置级联

```
Route → Backend → Server → -1 (无限制)
```

### 2.2 值语义

| 值 | 含义 |
|----|------|
| -1 | 无限制 |
| 0 | 拒绝所有访问 |
| > 0 | 正常限流 |

### 2.3 优先级示例

```yaml
server:
  rateLimit:
    maxQpsPerClient: 10          # 优先级 3（最低）

backends:
  - name: api-service
    rateLimit:
      maxQpsPerClient: 50        # 优先级 2（中等）

routes:
  - host: "api.example.com"
    backend: api-service
    rateLimit:
      maxQpsPerClient: 100       # 优先级 1（最高）→ 实际生效值
```

## 3. 负载均衡策略

| 策略 | 说明 |
|------|------|
| round-robin | 轮询 |
| random | 随机 |
| least-connection | 最少连接 |

## 4. 端点优先级

- 数值越小，优先级越高
- 最小值：1
- 默认值：10
- 高优先级组有健康端点时，不使用低优先级组

## 5. 变量替换

### 5.1 语法

| 语法 | 说明 |
|------|------|
| `${env:VAR_NAME}` | 从环境变量读取 |
| `${env:VAR_NAME:-default}` | 从环境变量读取，未找到则使用默认值 |
| `${sys:property.name}` | 从系统属性读取 |
| `${sys:property.name:-default}` | 从系统属性读取，未找到则使用默认值 |
| `${VAR_NAME}` | 先尝试环境变量，再尝试系统属性 |
| `${VAR_NAME:-default}` | 先尝试环境变量，再尝试系统属性，都未找到则使用默认值 |

### 5.2 使用示例

```yaml
server:
  ports:
    apiV1: ${env:API_V1_PORT:-18848}

accessLog:
  output:
    path: ${sys:log.path:-./logs}/access.log

backends:
  - name: my-service
    endpoints:
      - host: ${env:BACKEND_HOST:-localhost}
```

## 6. 访问日志配置

### 6.1 日志格式

**Pattern 格式**（适合人类阅读）：
```
%h - - [%t] "%m %U %H" %s %b %D "%{User-Agent}i" "%{Referer}i"%n
```

**JSON 格式**（适合日志分析工具）：
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "clientIp": "192.168.1.1",
  "method": "GET",
  "uri": "/nacos/v1/ns/instance/list",
  "protocol": "HTTP/1.1",
  "status": 200,
  "bytesSent": 512,
  "durationMs": 15
}
```

### 6.2 占位符

| 占位符 | 说明 | 示例 |
|--------|------|------|
| `%h` | 客户端 IP | 192.168.1.1 |
| `%m` | HTTP 方法 | GET, POST |
| `%U` | URI 路径 | /nacos/v1/ns/instance/list |
| `%s` | 响应状态码 | 200, 404 |
| `%b` | 发送字节数 | 512, - |
| `%D` | 请求耗时（毫秒） | 15 |
| `%t` | 时间戳 | 07/Feb/2026:14:30:00 +0800 |
| `%H` | HTTP 协议 | HTTP/1.1, HTTP/2 |
| `%{User-Agent}i` | 请求头 | Mozilla/5.0... |
| `%{Referer}i` | 请求头 | http://example.com |
