# Nacos 网关
基于 Java + Vert.x 构建的高性能 http 反向代理，支持http/1 和 http/2 ，支持基于http/2的gRPC流通信代理（不解析gRPC报文内容无需proto文件），支持连接级的负载均衡。

## 特性
- **流通信支持**: 完整支持 gRPC的各种通信模式（Unary RPC、Client-side streaming RPC、Server-side streaming RPC 和 Bidirectional streaming RPC）
- **负载均衡**: 支持多种负载均衡策略:
  - 轮询 (Round Robin)
  - 随机 (Random)
  - 最少连接 (Least Connection)
- **灵活路由**: 基于 YAML 配置的路由策略
- **动态配置**: 配置发生变化，实现动态加载更新无需重启服务
- **动态路由**: 配置重新加载如路由信息发生变化，自动断开不符合新路由策略的连接
- **路由转发**: 基于路由匹配将请求转发至不同的后端服务
- **健康检查**: 支持http/1和http/2请求后端实现健康检查，响应status为2xx即为健康
- **连接管理**: 采用连接级别资源管理架构，每个客户端连接对应一个专属的 ProxyConnection，内部管理专属 HttpClient、Endpoint 和 Backend。客户端断开时自动清理后端资源，有效防止连接泄漏

## 配置读取模式
- **多协议支持**:
  - `file://` - 本地文件系统读取（默认，支持热更新）
  - `classpath://` - 类路径读取（不支持热更新，需通过 -c 参数明确指定）
  - `nacos://` - Nacos 配置中心读取（支持实时推送更新，需通过 -c 参数明确指定）
- **向后兼容**: 未指定协议时默认使用 file://
- **灵活配置**: 通过 URL 参数配置 Nacos 认证信息

## 架构

```
客户端 -> 网关 -> (负载均衡器) -> 后端服务
```

网关工作流程:
1. 接收传入的 http 调用
2. 根据配置路由到适当的后端
3. 在多个后端端点之间进行负载均衡
4. 为流式通信维护持久连接
5. 返回响应给客户端


## 配置
配置文件默认从当前目录查找 `nacos-gateway.yaml` 或 `nacos-gateway.yml`。
您可以在启动时通过命令行参数 `-c path/to/config.yaml` 指定自定义配置文件位置，
支持 `file://`、`classpath://`、`nacos://` 三种协议前缀。

### 配置文件路径协议

配置文件路径支持三种协议，通过协议前缀自动识别：

1. **file:// 协议**（默认）
   - 从本地文件系统读取配置
   - 支持文件变更监听和热更新
   - 示例：
     ```bash
     java -jar nacos-gateway.jar -c nacos-gateway.yaml
     java -jar nacos-gateway.jar -c file:///etc/nacos-gateway/config.yaml
     ```

2. **classpath:// 协议**
   - 从应用类路径读取配置
   - 不支持热更新（资源通常打包在 jar 中）
   - 示例：
     ```bash
     java -jar nacos-gateway.jar -c classpath://nacos-gateway.yaml
     ```

3. **nacos:// 协议**
   - 从 Nacos 配置中心读取配置
   - 支持实时推送更新（基于 gRPC 长连接）
   - URL 格式：
     ```
     nacos://<dataId>[:<group>]?<query-parameters>
     ```
   - 说明：
     - `dataId`：必填
     - `group`：可选，使用 `:` 分隔。未指定时使用默认值 `DEFAULT_GROUP`
     - 所有其他参数直接透传给 Nacos Client
   - 示例：
     ```bash
     # 基础配置（使用默认 group）
     java -jar nacos-gateway.jar -c "nacos://nacos-gateway.yaml?serverAddr=127.0.0.1:8848"

     # AK/SK 认证
     java -jar nacos-gateway.jar -c "nacos://nacos-gateway.yaml:gateway-group?namespace=dev&serverAddr=127.0.0.1:8848&accessKey=yourKey&secretKey=yourSecret"

     # 用户名/密码认证
     java -jar nacos-gateway.jar -c "nacos://nacos-gateway.yaml:prod?namespace=prod&serverAddr=192.168.1.100:8848,192.168.1.101:8848&username=nacos&password=nacos"
     ```

### 配置模板变量替换

支持在配置文件中使用变量占位符，运行时自动从环境变量或系统属性中获取值进行替换。

**变量语法**：

| 语法格式 | 说明 | 示例 |
|---------|------|------|
| `${env:VAR_NAME}` | 从环境变量读取 | `${env:HOME}` |
| `${env:VAR_NAME:-default}` | 从环境变量读取，未找到则使用默认值 | `${env:PORT:-8080}` |
| `${sys:property.name}` | 从系统属性读取 | `${sys:user.home}` |
| `${sys:property.name:-default}` | 从系统属性读取，未找到则使用默认值 | `${sys:log.path:-/var/log}` |
| `${VAR_NAME}` | 先尝试环境变量，再尝试系统属性 | `${PORT}` |
| `${VAR_NAME:-default}` | 先尝试环境变量，再尝试系统属性，都未找到则使用默认值 | `${PORT:-8080}` |

**使用示例**：

```yaml
# 使用环境变量配置端口
server:
  ports:
    apiV1: ${env:API_V1_PORT:-18848}
    apiV2: ${env:API_V2_PORT:-19848}
    apiConsole: ${env:CONSOLE_PORT:-18080}

# 使用系统属性配置日志路径
accessLog:
  output:
    path: ${sys:log.path:-./logs}/access.log

# 路径拼接示例
backends:
  - name: my-service
    endpoints:
      - host: ${env:BACKEND_HOST:-localhost}
```

**特性**：
- 支持嵌套变量引用（变量值中可包含其他变量）
- 未找到的变量会保留原样并记录警告日志
- 完全向后兼容，不包含变量的配置不受影响

### 配置文件结构
```yaml
# 网关服务配置：同时监听多个端口，支持路由转发到不同的后端服务组
server:
  ports:
      apiV1: 18848                   # 网关提供的Nacos V1接口的服务端口（协议：HTTP）
      apiV2: 19848                   # 网关提供的Nacos V2接口的服务端口（协议：gRPC）
      apiConsole: 18080              # 网关提供的Nacos控制台API的服务端口（协议：HTTP）
  rateLimit:
    maxQps: 2000                     # 最大请求QPS (网关服务整体限制)
    maxConnections: 10000            # 最大连接数 (网关服务整体限制)
    maxQpsPerClient: 10              # 最大请求QPS (针对单个客户端，可被后端服务组配置覆盖)
    maxConnectionsPerClient: 5       # 最大连接数 (针对单个客户端，可被后端服务组配置覆盖)

# 路由规则（仅在 router 模式下使用）
routes:
  - host: "group1.nacos.io"          # 请求域名，星号为通配符
    backend: group1-service          # 路由转发目标后端服务名称

# 后端服务配置
backends:
  - name: group1-service             # 后端服务组名称
    ports:
      apiV1: 8848                    # 后端的Nacos V1接口服务端口（协议：HTTP）
      apiV2: 9848                    # 后端的Nacos V2接口服务端口（协议：gRPC）
      apiConsole: 8080               # 后端的Nacos控制台端口（协议：HTTP）
    probe:
      enabled: true                  # 是否启用健康检查
      type: http                     # 探测类型，支持类型：http/tcp (默认：tcp)
      path: /health                  # 探测请求路径（仅HTTP类型有效）
      periodSeconds: 10              # 执行探针的时间间隔
      timeoutSeconds: 1              # 探针超时时间
      successThreshold: 1            # 成功状态的阈值
      failureThreshold: 3            # 失败状态的阈值
    loadBalance: round-robin         # 负载均衡策略，支持类型：round-robin/random/least-connection
    rateLimit:
      maxQps: 1000                   # 最大请求QPS (针对该后端服务组)
      maxConnections: 2000           # 最大连接数 (针对该后端服务组)
      maxQpsPerClient: 10            # 最大请求QPS (针对请求该后端服务组的单个客户端)
      maxConnectionsPerClient: 5     # 最大连接数 (针对请求该后端服务组的单个客户端)
    endpoints:                       # 后端服务端点列表
      - host: 10.12.23.1             # 后端服务实例IP地址
        priority: 10                 # 端点优先级，数值越小优先级越高(最小值：1，默认值：10)
      - host: 10.12.23.2
        priority: 10
      - host: 10.12.23.3
        priority: 20
      - host: 10.12.23.4
        priority: 20

# 访问日志配置（可选）
accessLog:
  enabled: false                    # 是否启用访问日志，默认 false
  format: pattern                   # 日志格式: pattern 或 json，默认 pattern
  pattern: "%h - - [%t] \"%m %U %H\" %s %b %D \"%{User-Agent}i\" \"%{Referer}i\"%n"
                                    # 日志格式模式（仅 format=pattern 时有效）
  output:
    path: logs/access.log           # 日志文件路径
    encoding: UTF-8                 # 文件编码
  rotation:
    policy: daily                   # 轮转策略: daily, size, both
    maxFileSize: 100MB              # 最大文件大小（policy=size 或 both 时有效）
    maxHistory: 30                  # 保留历史文件数量
    fileNamePattern: "access.%d{yyyy-MM-dd}.log"  # 轮转文件名模式
  async:
    enabled: true                   # 是否启用异步写入
    queueSize: 512                  # 异步队列大小
    discardingThreshold: 20         # 丢弃阈值
    neverBlock: true                # 是否永不阻塞
```

### 访问日志 Pattern 占位符

当 `accessLog.format` 设置为 `pattern` 时，可以使用以下占位符：

#### 简单占位符
| 占位符 | 说明 | 示例 |
|--------|------|------|
| `%h` | 客户端 IP 地址 | 192.168.1.1 |
| `%m` | HTTP 方法 | GET, POST, etc. |
| `%U` | URI 路径 | /nacos/v1/ns/instance/list |
| `%s` | HTTP 响应状态码 | 200, 404, etc. |
| `%b` | 发送的字节数（无内容时显示 `-`） | 512, - |
| `%D` | 请求耗时（毫秒） | 15 |
| `%t` | 时间戳 | 07/Feb/2026:14:30:00 +0800 |
| `%H` | HTTP 协议 | HTTP/1.1, HTTP/2 |
| `%r` | 请求行（METHOD URI PROTOCOL） | GET /api/v1/config HTTP/1.1 |
| `%u` | 远程用户（未支持，固定显示 `-`） | - |
| `%T` | 请求耗时（秒） | 0.015 |
| `%n` | 换行符 | - |

#### 命名占位符（%{name}type）
| 占位符 | 说明 | 示例 |
|--------|------|------|
| `%{User-Agent}i` | 请求头中的 User-Agent | Mozilla/5.0... |
| `%{Referer}i` | 请求头中的 Referer | http://example.com |
| `%{Content-Type}i` | 任意请求头 | application/json |
| `%{any-header}o` | 任意响应头 | - |

#### 默认 Pattern 示例
默认的 pattern 格式（Apache 风格）：
```
%h - - [%t] "%m %U %H" %s %b %D "%{User-Agent}i" "%{Referer}i"%n
```

输出示例：
```
192.168.1.1 - - [07/Feb/2026:14:30:00 +0800] "GET /nacos/v1/ns/instance/list HTTP/1.1" 200 512 15 "Mozilla/5.0" "http://example.com"
```

### 访问日志格式类型

#### Pattern 格式
- 适合人类阅读的文本格式
- 支持上述所有占位符
- 每条日志一行

#### JSON 格式
- 结构化的 JSON 格式
- 适合日志分析和解析工具
- 每条日志一个 JSON 对象，包含以下字段：
  ```json
  {
    "timestamp": "2024-01-01T12:00:00Z",
    "clientIp": "192.168.1.1",
    "method": "GET",
    "uri": "/nacos/v1/ns/instance/list",
    "protocol": "HTTP/1.1",
    "status": 200,
    "bytesSent": 512,
    "durationMs": 15,
    "backend": "local-nacos",
    "endpoint": "localhost:8848"
  }
  ```

## 系统要求

- Java 17+
- Maven 3.6+

## 依赖

- Vert.x 5.0.6
- Jackson
- Logback
- SLF4J
- Nacos Client 2.3.2（配置中心集成）
