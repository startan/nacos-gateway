# Nacos Gateway - 完整项目文档

## 目录

1. [项目概述](#1-项目概述)
2. [需求文档](#2-需求文档)
3. [架构设计](#3-架构设计)
4. [模块详细设计](#4-模块详细设计)
5. [API参考](#5-api参考)
6. [配置指南](#6-配置指南)
7. [开发指南](#7-开发指南)
8. [部署指南](#8-部署指南)
9. [测试指南](#9-测试指南)

---

## 1. 项目概述

### 1.1 项目简介

**Nacos Gateway** 是一个基于 Java + Vert.x 构建的高性能 HTTP 反向代理网关，专门为 Nacos 服务发现场景设计。

### 1.2 核心特性

- **多协议支持**: HTTP/1、HTTP/2、gRPC（完全透传，无需 proto 文件）
- **智能路由**: 基于 Host 的通配符路由匹配
- **负载均衡**: 轮询、随机、最少连接三种策略
- **优先级分组**: 端点优先级机制，高优先级全挂才降级
- **健康检查**: HTTP/1 和 HTTP/2 探针，连续阈值机制
- **限流保护**: QPS + 并发连接数限流，全局+路由级
- **配置热更新**: 文件监听实现配置动态加载，无需重启
- **连接管理**: 每个客户端连接对应一个后端连接

### 1.3 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17+ | 开发语言 |
| Vert.x | 5.0.6 | 异步事件驱动框架 |
| Jackson | 2.16.0 | YAML 解析 |
| Logback | 1.4.11 | 日志框架 |
| SLF4J | 2.0.9 | 日志门面 |
| Maven | 3.6+ | 构建工具 |

### 1.4 项目结构

```
nacos-gateway-java/
├── pom.xml                          # 父 POM
├── gateway-api/                     # 公共 API 模块
├── gateway-core/                    # 核心实现模块
│   └── src/main/java/pans/gateway/
│       ├── config/                  # 配置加载和管理
│       ├── route/                   # 路由匹配
│       ├── loadbalance/             # 负载均衡
│       ├── health/                  # 健康检查
│       ├── ratelimit/               # 限流
│       ├── proxy/                   # 代理核心
│       ├── management/              # 管理接口
│       ├── logging/                 # 日志
│       ├── server/                  # 服务器启动
│       └── model/                   # 数据模型
└── gateway-launcher/                # 启动器模块
    └── src/main/resources/
        ├── config.yaml              # 默认配置
        └── logback.xml              # 日志配置
```

---

## 2. 需求文档

### 2.1 功能需求

#### 2.1.1 HTTP/HTTPS 代理

**需求描述**: 网关应能够接收 HTTP/1 和 HTTP/2 请求，并转发到后端服务。

**详细说明**:
- 支持 HTTP/1.1 协议
- 支持 HTTP/2 协议（包括 h2c）
- 正确转发请求头（排除 hop-by-hop 头）
- 正确转发请求体
- 正确转发响应头和响应体
- 保持客户端连接

**验收标准**:
- HTTP/1.1 请求能够正确转发
- HTTP/2 请求能够正确转发
- 响应能够正确返回给客户端
- 连接正常关闭

#### 2.1.2 gRPC 代理

**需求描述**: 网关应支持 gRPC 流通信代理，无需解析 proto 文件。

**详细说明**:
- 完全透传 gRPC 消息（不解析内容）
- 检测 Content-Type: application/grpc
- 使用 HTTP/2 传输
- 支持四种 gRPC 模式：
  - Unary RPC
  - Server-side streaming RPC
  - Client-side streaming RPC
  - Bidirectional streaming RPC
- 正确处理背压（backpressure）

**验收标准**:
- gRPC 请求能够正确透传
- 流式通信正常工作
- 不会因背压导致内存溢出

#### 2.1.3 路由匹配

**需求描述**: 网关应支持灵活的路由匹配规则。

**详细说明**:
- Host 匹配支持通配符 `*.example.com`
- Path 匹配支持通配符：
  - `/api/*` - 单级路径匹配
  - `/api/**` - 多级路径匹配
- 精确匹配优先级高于通配符匹配

**验收标准**:
- 通配符路由正常工作
- 精确匹配优先
- 路由匹配性能满足要求（< 1ms）

#### 2.1.4 负载均衡

**需求描述**: 网关应支持多种负载均衡策略。

**详细说明**:
- **轮询 (Round Robin)**: 按顺序轮流选择
- **随机 (Random)**: 随机选择可用端点
- **最少连接 (Least Connection)**: 选择当前连接数最少的端点
- **优先级分组**: 端点按优先级分组，高优先级全挂才降级

**验收标准**:
- 三种策略正确工作
- 优先级机制正常工作
- 负载均衡性能满足要求

#### 2.1.5 健康检查

**需求描述**: 网关应能够自动检测后端端点的健康状态。

**详细说明**:
- 支持 HTTP/1 和 HTTP/2 探针
- 可配置探针路径、周期、超时
- 连续成功阈值（successThreshold）
- 连续失败阈值（failureThreshold）
- 仅选择健康端点进行负载均衡

**验收标准**:
- 不健康端点被自动剔除
- 恢复健康的端点自动加回
- 阈值机制防止误判

#### 2.1.6 限流

**需求描述**: 网关应支持 QPS 和并发连接数限流。

**详细说明**:
- **QPS 限流**: 使用滑动窗口算法
- **连接数限流**: 使用原子计数器
- **全局限流**: 保护网关整体
- **路由级限流**: 路由可覆盖全局配置
- 超限返回 HTTP 429

**验收标准**:
- 限流准确生效
- 性能开销小（< 5%）
- 限流粒度正确

#### 2.1.7 配置热更新

**需求描述**: 网关应支持配置文件热更新，无需重启服务。

**详细说明**:
- 监听配置文件修改（轮询方式，兼容 Windows）
- 配置变化时自动重新加载
- 路由变化时立即断开不符合新路由的连接
- 后端变化时更新健康检查
- 配置验证失败不应用新配置

**验收标准**:
- 配置修改自动生效
- 不符合新路由的连接被断开
- 配置错误不影响当前运行

#### 2.1.8 管理接口

**需求描述**: 网关应提供健康检查管理接口。

**详细说明**:
- HTTP 端点（默认 `/health`）
- 返回网关自身健康状态（UP/DOWN）
- JSON 格式响应

**验收标准**:
- 健康检查端点可访问
- 返回正确的健康状态

### 2.2 非功能需求

#### 2.2.1 性能要求

- 吞吐量: > 10000 RPS（单机）
- 延迟: P99 < 10ms（代理转发）
- 并发连接: > 10000

#### 2.2.2 可靠性要求

- 可用性: 99.9%
- 故障恢复时间: < 30s
- 无单点故障

#### 2.2.3 可扩展性要求

- 支持水平扩展
- 配置驱动的灵活扩展

#### 2.2.4 可维护性要求

- 代码可读性好
- 日志完整且可配置
- 监控指标齐全

---

## 3. 架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端请求                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                     Nacos Gateway                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  HTTP Server │──│  路由匹配器   │──│  限流检查     │      │
│  │  (Vert.x)    │  │ (RouteMatcher)│  │(RateLimiter) │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         │                                    │               │
│         ▼                                    ▼               │
│  ┌──────────────┐                   ┌──────────────┐        │
│  │ 管理接口      │                   │ 负载均衡器    │        │
│  │(/health)     │                   │(LoadBalancer)│        │
│  └──────────────┘                   └──────────────┘        │
│                                             │                │
│         ┌─────────────────────────────────┼──────────┐      │
│         ▼                                 ▼          ▼      │
│  ┌───────────┐  ┌─────────────┐  ┌──────────────────┐     │
│  │ 健康检查   │  │  连接管理器  │  │  代理处理器       │     │
│  │(HealthCheck)│ │(ConnectionMgr)│ │ (ProxyHandler)  │     │
│  └───────────┘  └─────────────┘  └──────────────────┘     │
│                                       │                     │
│         ┌─────────────────────────────┼─────────────┐     │
│         ▼                             ▼             ▼     │
│  ┌──────────┐  ┌───────────┐  ┌──────────────┐          │
│  │HTTP/1 代理│  │HTTP/2 代理 │  │  gRPC 代理    │          │
│  └──────────┘  └───────────┘  └──────────────┘          │
└────────────────────────┬──────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ 后端服务 1   │  │ 后端服务 2   │  │ 后端服务 N   │
│ (Priority 1) │  │ (Priority 1) │  │ (Priority 2) │
└─────────────┘  └─────────────┘  └─────────────┘
```

### 3.2 数据流设计

#### 3.2.1 请求处理流程

```
1. 接收请求
   ├─ HTTP Server (Vert.x) 接收客户端请求
   └─ 提取 Host、Path、Method 等信息

2. 路由匹配
   ├─ 检查是否为管理接口请求（/health）
   ├─ 根据 Host 匹配路由规则
   └─ 找到对应的后端服务配置

3. 限流检查
   ├─ 检查全局限流（QPS + 连接数）
   ├─ 检查路由级限流
   └─ 超限返回 429，否则继续

4. 端点选择
   ├─ 获取后端服务的所有健康端点
   ├─ 按优先级分组
   ├─ 选择最高优先级组
   └─ 在组内使用负载均衡策略选择端点

5. 代理转发
   ├─ 检测请求类型（HTTP/1、HTTP/2、gRPC）
   ├─ 创建对应类型的代理处理器
   ├─ 建立与后端的连接
   ├─ 转发请求头和请求体
   └─ 流式转发响应

6. 连接清理
   ├─ 请求完成后释放限流配额
   ├─ 通知负载均衡器连接关闭
   └─ 清理连接管理器中的记录
```

#### 3.2.2 配置热更新流程

```
1. 文件监听
   └─ ConfigWatcher 每秒检查配置文件修改时间

2. 检测到变化
   └─ 文件修改时间更新，触发重载

3. 加载新配置
   ├─ ConfigLoader 解析 YAML 文件
   ├─ 验证配置有效性
   └─ 创建 GatewayConfig 对象

4. 更新组件
   ├─ 更新路由表（RouteMatcher）
   ├─ 更新后端服务（Backend）
   ├─ 更新健康检查（HealthCheckManager）
   └─ 更新限流配置（RateLimitManager）

5. 连接处理
   └─ 断开不符合新路由的现有连接

6. 应用完成
   └─ 记录日志，继续服务
```

### 3.3 核心组件交互

```
┌────────────────┐
│ ServerBootstrap│
│   启动器        │
└────────┬───────┘
         │
         │ 创建
         ▼
┌────────────────┐
│ GatewayServer  │
│   网关服务器     │
└────────┬───────┘
         │
         │ 初始化
         ▼
┌────────────────────────────────────┐
│      核心组件初始化                  │
│  ┌─────────────────────────────┐   │
│  │ RouteMatcher (路由匹配器)     │   │
│  │  - 匹配 Host 和 Path         │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ LoadBalancer (负载均衡器)    │   │
│  │  - RoundRobin/Random/LeastConn│  │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ HealthCheckManager (健康检查)│  │
│  │  - 定时探测端点健康状态        │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ RateLimitManager (限流)     │   │
│  │  - QPS + 连接数限流          │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │ ConnectionManager (连接管理) │   │
│  │  - 跟踪活动连接               │   │
│  └─────────────────────────────┘   │
└────────────────────────────────────┘
         │
         │ 处理请求
         ▼
┌────────────────────────────────────┐
│      请求处理流程                    │
│  1. 路由匹配 → RouteMatcher        │
│  2. 限流检查 → RateLimitManager    │
│  3. 端点选择 → LoadBalancer        │
│  4. 代理转发 → ProxyHandler        │
│  5. 连接清理 → ConnectionManager   │
└────────────────────────────────────┘
```

### 3.4 设计原则

#### 3.4.1 异步非阻塞

- 基于 Vert.x 的事件循环模型
- 所有 I/O 操作都是异步的
- 避免阻塞事件循环

#### 3.4.2 线程安全

- 使用 ConcurrentHashMap 等并发集合
- 使用 AtomicInteger 等原子类
- 使用 Copy-on-Write 模式更新路由表

#### 3.4.3 资源管理

- 及时释放连接
- 清理过期限流窗口
- 限制缓冲区大小

#### 3.4.4 可观测性

- 详细的日志记录
- 健康检查接口
- 结构化日志输出

---

## 4. 模块详细设计

### 4.1 配置模块 (config)

#### 4.1.1 类图

```
GatewayConfig
    ├── ServerConfig
    ├── List<RouteConfig>
    ├── List<BackendConfig>
    ├── RateLimitConfig
    ├── TimeoutConfig
    ├── LoggingConfig
    └── ManagementConfig
            └── HealthEndpointConfig

RouteConfig
    ├── host (String)
    ├── path (String)
    ├── backend (String)
    ├── qpsLimit (Integer)
    └── maxConnections (Integer)

BackendConfig
    ├── name (String)
    ├── loadBalance (String)
    ├── HealthProbeConfig
    └── List<EndpointConfig>

EndpointConfig
    ├── host (String)
    ├── port (int)
    └── priority (int)
```

#### 4.1.2 关键类说明

**ConfigLoader**
- 职责：加载和验证配置文件
- 关键方法：
  - `load(String configPath)`: 从文件路径加载
  - `load(Path configPath)`: 从 Path 对象加载
  - `validate(GatewayConfig config)`: 验证配置
- 配置验证规则：
  - 端口范围：1-65535
  - 路由引用的后端必须存在
  - 负载均衡策略必须是三种之一

**ConfigWatcher**
- 职责：监听配置文件变化
- 实现方式：轮询文件修改时间（兼容 Windows）
- 检查周期：1秒（可配置）
- 关键方法：
  - `start()`: 启动监听
  - `stop()`: 停止监听

**ConfigReloader**
- 职责：执行配置热更新
- 更新策略：
  - Copy-on-Write 更新路由表
  - 立即断开不符合新路由的连接
  - 更新健康检查和限流配置

### 4.2 路由模块 (route)

#### 4.2.1 类图

```
RouteMatcher (interface)
    └── RouteMatcherImpl
            ├── List<RouteMatchEntry>
            └── match(host, path): Optional<Route>

Route
    ├── id (String)
    ├── hostPattern (String)
    ├── pathPattern (String)
    ├── backendName (String)
    ├── qpsLimit (Integer)
    └── maxConnections (Integer)

HostMatcher
    ├── pattern (String)
    ├── regex (Pattern)
    └── matches(host): boolean

PathMatcher
    ├── pattern (String)
    ├── isDoubleStar (boolean)
    ├── isSingleStar (boolean)
    ├── prefix (String)
    ├── regex (Pattern)
    └── matches(path): boolean
```

#### 4.2.2 匹配算法

**Host 匹配**
```
精确匹配：example.com
通配符匹配：*.example.com → [^.]+\.example\.com
```

**Path 匹配**
```
精确匹配：/api/users
单级通配：/api/* → /api/[^/]+
多级通配：/api/** → /api/.*
```

### 4.3 负载均衡模块 (loadbalance)

#### 4.3.1 类图

```
LoadBalancer (interface)
    ├── select(endpoints): Endpoint
    ├── onConnectionOpen(endpoint)
    └── onConnectionClose(endpoint)
            │
            ├── RoundRobinLoadBalancer
            │       └── currentIndex (AtomicInteger)
            │
            ├── RandomLoadBalancer
            │       └── random (Random)
            │
            └── LeastConnectionLoadBalancer
                    └── connections (ConcurrentMap)

EndpointSelector
    └── select(backend): Endpoint
            ├── 按优先级分组
            ├── 选择最高优先级组
            └── 在组内使用负载均衡策略
```

#### 4.3.2 策略说明

**轮询 (Round Robin)**
- 按顺序依次选择
- 使用原子计数器保证线程安全
- 不追踪连接数

**随机 (Random)**
- 随机选择可用端点
- 使用 Random 生成器
- 适合均匀分布的场景

**最少连接 (Least Connection)**
- 选择当前连接数最少的端点
- 使用 ConcurrentHashMap 追踪连接数
- 需要通知连接打开/关闭

**优先级分组**
- 按 priority 字段分组
- 数字越小优先级越高
- 高优先级组全不可用时才降级

### 4.4 健康检查模块 (health)

#### 4.4.1 类图

```
HealthCheckManager
    ├── Vertx vertx
    ├── Map<Endpoint, HealthCheckTask> tasks
    ├── startBackendChecking(backendConfig, endpoints)
    ├── startChecking(endpoint, config)
    ├── stopChecking(endpoint)
    └── isHealthy(endpoint): boolean

HealthCheckTask
    ├── Vertx vertx
    ├── Endpoint endpoint
    ├── HealthProbeConfig config
    ├── healthy (AtomicBoolean)
    ├── consecutiveSuccesses (AtomicInteger)
    ├── consecutiveFailures (AtomicInteger)
    ├── start()
    ├── stop()
    └── checkHealth(long id)
```

#### 4.4.2 健康检查流程

```
1. 启动定时任务
   └─ vertx.setPeriodic(periodSeconds * 1000)

2. 执行探针
   ├─ 创建 HTTP 客户端
   ├─ 发送 GET 请求到 /health
   └─ 等待响应（timeoutSeconds）

3. 判断结果
   ├─ 状态码 2xx：成功
   │   └─ consecutiveSuccesses++
   └─ 其他：失败
       └─ consecutiveFailures++

4. 更新状态
   ├─ consecutiveSuccesses >= successThreshold
   │   └─ 标记为健康
   └─ consecutiveFailures >= failureThreshold
       └─ 标记为不健康
```

### 4.5 限流模块 (ratelimit)

#### 4.5.1 类图

```
RateLimitManager
    ├── QpsRateLimiter globalQpsLimiter
    ├── ConnectionRateLimiter globalConnectionLimiter
    ├── Map<String, RouteRateLimiter> routeLimiters
    ├── tryAcquire(routeId): boolean
    └── release(routeId)

QpsRateLimiter
    ├── int maxQps
    ├── long windowSizeMs
    ├── ConcurrentMap<Long, AtomicInteger> windows
    └── tryAcquire(): boolean

ConnectionRateLimiter
    ├── int maxConnections
    ├── AtomicInteger currentConnections
    ├── tryAcquire(): boolean
    └── release()

RouteRateLimiter
    ├── QpsRateLimiter qpsLimiter
    ├── ConnectionRateLimiter connectionLimiter
    ├── tryAcquire(): boolean
    └── release()
```

#### 4.5.2 限流算法

**QPS 限流（滑动窗口）**
```
1. 获取当前时间窗口
   window = currentTime / windowSizeMs

2. 原子递增计数器
   count = windows[window].incrementAndGet()

3. 清理旧窗口
   remove windows < currentWindow - 1

4. 检查是否超限
   if count > maxQps:
       decrementAndGet()
       return false
   return true
```

**连接数限流**
```
1. CAS 操作递增
   do {
       current = currentConnections.get()
       if current >= maxConnections:
           return false
   } while (!compareAndSet(current, current + 1))

2. 释放时递减
   currentConnections.decrementAndGet()
```

### 4.6 代理模块 (proxy)

#### 4.6.1 类图

```
ProxyHandler (interface)
    └── handle(request): void

HttpProxyHandler
    ├── HttpClient httpClient
    ├── Endpoint endpoint
    ├── TimeoutConfig timeoutConfig
    └── handle(request)

GrpcProxyHandler
    ├── HttpClient httpClient
    ├── Endpoint endpoint
    ├── TimeoutConfig timeoutConfig
    ├── isGrpcRequest(request): boolean
    └── handle(request)

ConnectionManager
    ├── Map<HttpConnection, ProxyConnection> connections
    ├── addConnection(connection, proxyConnection)
    ├── removeConnection(connection)
    ├── getConnection(connection): ProxyConnection
    ├── disconnectInvalidConnections(routeMatcher)
    └── closeAll()

ProxyConnection (连接级资源容器)
    ├── HttpConnection clientConnection    # 客户端连接
    ├── Route route                       # 路由信息
    ├── Endpoint endpoint                 # 选定的后端端点
    ├── Backend backend                   # 后端服务
    ├── HttpClient httpClient             # 专属 HttpClient
    ├── long createTime                   # 创建时间
    ├── close()                           # 统一释放所有资源
    └── getDuration(): long               # 连接持续时间
```

#### 4.6.2 连接管理架构

**核心设计理念**
- **连接级别资源管理**：每个客户端 TCP 连接对应一个专属的 ProxyConnection
- **资源生命周期绑定**：HttpClient、Endpoint、Backend 与 ProxyConnection 生命周期一致
- **自动清理机制**：客户端断开时自动释放所有后端资源，防止连接泄漏

**架构图**
```
客户端 TCP 连接 (1:1) HttpConnection (1:1) ProxyConnection
                                                   ↓
                                            ┌──────────────┐
                                            │ HttpClient   │ 专属后端连接
                                            │ Endpoint     │ 选定的后端
                                            │ Backend      │ 后端服务
                                            └──────────────┘

连接断开时：
  connection.closeHandler() → ConnectionManager.removeConnection()
    → ProxyConnection.close()
      → httpClient.close()           # 关闭后端连接
      → backend.loadBalancer.onConnectionClose()  # 通知负载均衡器
```

**关键特性**

1. **连接级负载均衡**
   - 每个客户端连接首次请求时选择后端端点
   - 同一连接上的所有后续请求复用同一后端
   - 支持连接数统计（least-connection 策略）

2. **自动资源清理**
   ```java
   // 正常关闭
   connection.closeHandler(v -> {
       connectionManager.removeConnection(connection);
   });

   // 异常关闭（网络中断、进程被杀）
   connection.exceptionHandler(t -> {
       connectionManager.removeConnection(connection);
   });
   ```

3. **配置热更新支持**
   - 配置变更时检查现有连接是否符合新路由
   - 自动断开不符合新路由的连接
   - 保持符合新路由的连接继续使用

#### 4.6.3 代理流程

**连接级初始化（首次请求）**
```
1. 获取 HttpConnection
   connection = request.connection()

2. 检查是否已有 ProxyConnection
   proxyConnection = connectionManager.getConnection(connection)

3. 如果为 null（首次请求）：
   a. 选择后端端点（负载均衡）
      backend = backends.get(route.backendName)
      endpoint = endpointSelector.select(backend)

   b. 创建专属 HttpClient
      httpClient = vertx.createHttpClient(createHttp2ClientOptions())

   c. 通知负载均衡器（连接级）
      backend.loadBalancer.onConnectionOpen(endpoint)

   d. 创建 ProxyConnection
      proxyConnection = new ProxyConnection(
          connection, route, endpoint, backend, httpClient
      )

   e. 注册到 ConnectionManager
      connectionManager.addConnection(connection, proxyConnection)

   f. 注册清理处理器
      connection.closeHandler(...)
      connection.exceptionHandler(...)

4. 如果不为 null（后续请求）：
   - 直接复用现有 ProxyConnection
   - 无需重复负载均衡、创建 HttpClient 等
```

**HTTP/1 代理**
```
1. 创建 HttpClientRequest
2. 复制请求头（排除 hop-by-hop 头）
3. 转发请求体
4. 接收响应
5. 复制响应头
6. 转发响应体
7. 结束请求
```

**gRPC 代理**
```
1. 检测 Content-Type: application/grpc
2. 创建 HTTP/2 客户端请求
3. 完全复制请求头
4. 流式转发请求体（处理背压）
5. 流式转发响应体（处理背压）
6. 不解析 gRPC 消息
```

### 4.7 服务器模块 (server)

#### 4.7.1 类图

```
GatewayServer
    ├── Vertx vertx
    ├── GatewayConfig config
    ├── HttpServer server
    ├── HttpClient httpClient
    ├── HttpClient http2Client
    ├── RouteMatcher routeMatcher
    ├── Map<String, Backend> backends
    ├── EndpointSelector endpointSelector
    ├── ConnectionManager connectionManager
    ├── HealthCheckManager healthCheckManager
    ├── RateLimitManager rateLimitManager
    ├── HealthEndpoint healthEndpoint
    ├── start()
    ├── stop()
    └── handleRequest(request)

ServerBootstrap
    ├── String configPath
    ├── Vertx vertx
    ├── GatewayServer gatewayServer
    ├── ConfigWatcher configWatcher
    ├── start()
    └── stop()
```

---

## 5. API参考

### 5.1 配置类

#### GatewayConfig

根配置类，包含所有配置项。

```java
public class GatewayConfig {
    private ServerConfig server;              // 服务器配置
    private List<RouteConfig> routes;         // 路由配置
    private List<BackendConfig> backends;     // 后端配置
    private RateLimitConfig rateLimit;        // 限流配置
    private TimeoutConfig timeout;            // 超时配置
    private LoggingConfig logging;            // 日志配置
    private ManagementConfig management;      // 管理接口配置
}
```

**方法**:
- `getServer()`: 获取服务器配置
- `getRoutes()`: 获取路由列表
- `getBackends()`: 获取后端列表
- `getRateLimit()`: 获取限流配置
- `getTimeout()`: 获取超时配置
- `getLogging()`: 获取日志配置
- `getManagement()`: 获取管理接口配置

#### RouteConfig

路由配置类。

```java
public class RouteConfig {
    private String host;              // Host 匹配模式（支持 *.example.com）
    private String path;              // Path 匹配模式（支持 * 和 **）
    private String backend;           // 后端服务名称
    private Integer qpsLimit;         // QPS 限流（可选，覆盖全局配置）
    private Integer maxConnections;   // 连接数限流（可选，覆盖全局配置）
}
```

**方法**:
- `getHost()`: 获取 Host 模式
- `getPath()`: 获取 Path 模式
- `getBackend()`: 获取后端名称
- `getQpsLimit()`: 获取 QPS 限流
- `getMaxConnections()`: 获取连接数限流

#### BackendConfig

后端服务配置类。

```java
public class BackendConfig {
    private String name;                         // 后端名称
    private String loadBalance;                  // 负载均衡策略
    private HealthProbeConfig probe;             // 健康检查配置
    private List<EndpointConfig> endpoints;      // 端点列表
}
```

**方法**:
- `getName()`: 获取后端名称
- `getLoadBalance()`: 获取负载均衡策略
- `getProbe()`: 获取健康检查配置
- `getEndpoints()`: 获取端点列表

#### EndpointConfig

端点配置类。

```java
public class EndpointConfig {
    private String host;      // 主机地址
    private int port;         // 端口
    private int priority;     // 优先级（数字越小优先级越高）
}
```

**方法**:
- `getHost()`: 获取主机地址
- `getPort()`: 获取端口
- `getPriority()`: 获取优先级

### 5.2 核心类

#### RouteMatcher

路由匹配器接口。

```java
public interface RouteMatcher {
    /**
     * 匹配路由
     * @param host Host 头
     * @return 匹配的路由
     */
    Optional<Route> match(String host);
}
```

#### LoadBalancer

负载均衡器接口。

```java
public interface LoadBalancer {
    /**
     * 选择一个端点
     * @param endpoints 可用端点列表
     * @return 选中的端点
     */
    Endpoint select(List<Endpoint> endpoints);

    /**
     * 连接打开时通知
     * @param endpoint 端点
     */
    default void onConnectionOpen(Endpoint endpoint) {}

    /**
     * 连接关闭时通知
     * @param endpoint 端点
     */
    default void onConnectionClose(Endpoint endpoint) {}
}
```

#### RateLimiter

限流器接口。

```java
public interface RateLimiter {
    /**
     * 尝试获取许可
     * @return 是否获取成功
     */
    boolean tryAcquire();
}
```

#### ProxyHandler

代理处理器接口。

```java
public interface ProxyHandler {
    /**
     * 处理代理请求
     * @param request 客户端请求
     */
    void handle(HttpServerRequest request);
}
```

### 5.3 模型类

#### Endpoint

端点模型。

```java
public class Endpoint {
    private final String host;                    // 主机地址
    private final int port;                       // 端口
    private final int priority;                   // 优先级
    private final AtomicBoolean healthy;          // 健康状态

    // 构造函数
    public Endpoint(EndpointConfig config);
    public Endpoint(String host, int port, int priority);

    // 方法
    public String getAddress();                   // 获取地址（host:port）
    public boolean isHealthy();                   // 是否健康
    public void setHealthy(boolean healthy);      // 设置健康状态
}
```

#### Backend

后端服务模型。

```java
public class Backend {
    private final String name;                    // 名称
    private final LoadBalancer loadBalancer;      // 负载均衡器
    private final List<Endpoint> endpoints;       // 端点列表

    // 方法
    public List<Endpoint> getHealthyEndpoints();  // 获取健康端点
}
```

#### Route

路由模型。

```java
public class Route {
    private final String id;                      // 路由 ID
    private final String hostPattern;             // Host 模式
    private final String pathPattern;             // Path 模式
    private final String backendName;             // 后端名称
    private final Integer qpsLimit;               // QPS 限流
    private final Integer maxConnections;         // 连接数限流
}
```

---

## 6. 配置指南

### 6.1 配置文件结构

配置文件采用 YAML 格式，默认路径为 `config.yaml`。

```yaml
# 服务器配置
server:
  port: 9848                    # 监听端口

# 路由配置
routes:
  - host: "*.nacos.io"          # Host 模式（支持通配符）
    path: "/**"                 # Path 模式（支持 * 和 **）
    backend: example-service    # 后端服务名称
    qpsLimit: 1000              # 可选：QPS 限流
    maxConnections: 500         # 可选：连接数限流

# 后端服务配置
backends:
  - name: example-service       # 后端名称
    loadBalance: round-robin    # 负载均衡策略
    probe:                      # 健康检查配置
      path: /health
      periodSeconds: 10         # 检查周期（秒）
      timeoutSeconds: 1         # 超时时间（秒）
      successThreshold: 1       # 成功阈值
      failureThreshold: 3       # 失败阈值
    endpoints:                  # 端点列表
      - host: 10.12.23.1
        port: 9848
        priority: 1             # 优先级（数字越小越高）
      - host: 10.12.23.2
        port: 9848
        priority: 2

# 限流配置
rateLimit:
  globalQpsLimit: 10000         # 全局 QPS 限制
  globalMaxConnections: 5000    # 全局连接数限制
  defaultQpsLimit: 1000         # 默认 QPS 限制
  defaultMaxConnections: 500    # 默认连接数限制

# 超时配置
timeout:
  connectTimeoutSeconds: 10     # 连接超时
  requestTimeoutSeconds: 30     # 请求超时
  idleTimeoutSeconds: 60        # 空闲超时

# 日志配置
logging:
  level: INFO                   # 日志级别
  verbose: true                 # 详细日志
  format: json                  # 格式：text 或 json

# 管理接口配置
management:
  health:
    enabled: true               # 是否启用
    path: /health               # 健康检查路径
```

### 6.2 配置项说明

#### 服务器配置 (server)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| port | int | 9848 | 监听端口（1-65535） |

#### 路由配置 (routes)

| 配置项 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| host | string | 是 | Host 匹配模式，支持 `*.example.com` 通配符 |
| path | string | 是 | Path 匹配模式，支持 `*` 和 `**` 通配符 |
| backend | string | 是 | 后端服务名称 |
| qpsLimit | int | 否 | QPS 限流，覆盖全局配置 |
| maxConnections | int | 否 | 连接数限流，覆盖全局配置 |

#### 后端配置 (backends)

| 配置项 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | string | 是 | 后端服务名称 |
| loadBalance | string | 是 | 负载均衡策略：round-robin、random、least-connection |
| probe | object | 否 | 健康检查配置 |
| endpoints | array | 是 | 端点列表 |

#### 健康检查配置 (probe)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| path | string | /health | 探针路径 |
| periodSeconds | int | 10 | 检查周期（秒） |
| timeoutSeconds | int | 1 | 超时时间（秒） |
| successThreshold | int | 1 | 连续成功多少次标记为健康 |
| failureThreshold | int | 3 | 连续失败多少次标记为不健康 |

#### 端点配置 (endpoints)

| 配置项 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| host | string | 是 | 主机地址 |
| port | int | 是 | 端口（1-65535） |
| priority | int | 否 | 优先级（数字越小越高） |

#### 限流配置 (rateLimit)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| globalQpsLimit | int | 10000 | 全局 QPS 限制 |
| globalMaxConnections | int | 5000 | 全局连接数限制 |
| defaultQpsLimit | int | 1000 | 默认 QPS 限制 |
| defaultMaxConnections | int | 500 | 默认连接数限制 |

#### 超时配置 (timeout)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| connectTimeoutSeconds | int | 10 | 连接超时（秒） |
| requestTimeoutSeconds | int | 30 | 请求超时（秒） |
| idleTimeoutSeconds | int | 60 | 空闲超时（秒） |

#### 日志配置 (logging)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| level | string | INFO | 日志级别：TRACE、DEBUG、INFO、WARN、ERROR |
| verbose | boolean | false | 是否记录详细的请求/响应信息 |
| format | string | text | 日志格式：text 或 json |

#### 管理接口配置 (management)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| health.enabled | boolean | true | 是否启用健康检查端点 |
| health.path | string | /health | 健康检查路径 |

### 6.3 配置示例

#### 示例 1：简单代理

```yaml
server:
  port: 8080

routes:
  - host: "example.com"
    path: "/**"
    backend: backend1

backends:
  - name: backend1
    loadBalance: round-robin
    endpoints:
      - host: localhost
        port: 3000
        priority: 1
```

#### 示例 2：多路由 + 限流

```yaml
server:
  port: 8080

routes:
  - host: "api.example.com"
    path: "/api/**"
    backend: api-service
    qpsLimit: 5000
    maxConnections: 1000

  - host: "static.example.com"
    path: "/**"
    backend: static-service
    qpsLimit: 10000

backends:
  - name: api-service
    loadBalance: least-connection
    probe:
      path: /health
      periodSeconds: 5
      failureThreshold: 2
    endpoints:
      - host: api1.internal
        port: 8080
        priority: 1
      - host: api2.internal
        port: 8080
        priority: 1

  - name: static-service
    loadBalance: random
    endpoints:
      - host: static1.internal
        port: 8080
        priority: 1

rateLimit:
  globalQpsLimit: 50000
  globalMaxConnections: 10000
  defaultQpsLimit: 1000
  defaultMaxConnections: 500
```

#### 示例 3：gRPC 服务代理

```yaml
server:
  port: 9090

routes:
  - host: "grpc.example.com"
    path: "/**"
    backend: grpc-service

backends:
  - name: grpc-service
    loadBalance: round-robin
    probe:
      path: /grpc.health.v1.Health/Check
      periodSeconds: 30
    endpoints:
      - host: grpc1.internal
        port: 6565
        priority: 1
      - host: grpc2.internal
        port: 6565
        priority: 2
```

### 6.4 配置验证

配置加载时会进行以下验证：

1. **端口验证**
   - 端口范围：1-65535
   - 不能为 0 或负数

2. **路由验证**
   - Host 不能为空
   - Path 不能为空
   - 引用的后端必须存在

3. **后端验证**
   - 名称不能为空
   - 至少有一个端点
   - 负载均衡策略必须是三种之一

4. **端点验证**
   - Host 不能为空
   - 端口范围：1-65535

5. **限流验证**
   - 所有限制必须为正数

---

## 7. 开发指南

### 7.1 环境准备

#### 7.1.1 必需软件

| 软件 | 版本要求 | 下载地址 |
|------|----------|----------|
| JDK | 17+ | https://adoptium.net/ |
| Maven | 3.6+ | https://maven.apache.org/ |
| IDE | 推荐 IntelliJ IDEA | https://www.jetbrains.com/idea/ |

#### 7.1.2 克隆项目

```bash
git clone <repository-url>
cd nacos-gateway-java
```

#### 7.1.3 构建项目

```bash
mvn clean install
```

### 7.2 开发流程

#### 7.2.1 分支策略

- `main`: 主分支，保持稳定
- `develop`: 开发分支
- `feature/*`: 功能分支
- `bugfix/*`: 修复分支

#### 7.2.2 代码规范

**命名规范**
- 类名：PascalCase（如 `RouteMatcher`）
- 方法名：camelCase（如 `matchRoute`）
- 常量：UPPER_SNAKE_CASE（如 `MAX_CONNECTIONS`）
- 包名：全小写（如 `pans.gateway.config`）

**注释规范**
- 公共 API 必须有 Javadoc
- 复杂逻辑必须有行内注释
- 注释使用中文

**代码风格**
- 使用 4 空格缩进
- 大括号不换行
- 每行不超过 120 字符

#### 7.2.3 提交规范

提交消息格式：
```
<type>(<scope>): <subject>

<body>

<footer>
```

类型（type）：
- `feat`: 新功能
- `fix`: 修复
- `docs`: 文档
- `style`: 格式
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建

示例：
```
feat(proxy): add gRPC streaming support

Implement complete passthrough for gRPC streaming RPCs.
Handle backpressure properly.

Closes #123
```

### 7.3 测试

#### 7.3.1 单元测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=RouteMatcherTest

# 运行特定测试方法
mvn test -Dtest=RouteMatcherTest#testWildcardMatch
```

#### 7.3.2 集成测试

```bash
# 运行集成测试
mvn verify
```

#### 7.3.3 测试覆盖率

```bash
# 生成覆盖率报告
mvn jacoco:report
```

### 7.4 调试

#### 7.4.1 远程调试

启动时添加调试参数：
```bash
mvn exec:java -Dexec.args="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

#### 7.4.2 日志调试

修改 `logback.xml`：
```xml
<logger name="pans.gateway" level="DEBUG"/>
```

### 7.5 性能优化

#### 7.5.1 JVM 参数

```bash
java -Xms2g -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar gateway-launcher-1.0.0.jar
```

#### 7.5.2 Vert.x 优化

```java
VertxOptions options = new VertxOptions()
    .setEventLoopPoolSize(Runtime.getRuntime().availableProcessors() * 2)
    .setWorkerPoolSize(20)
    .setPreferNativeTransport(true);  // 使用原生传输
```

### 7.6 故障排查

#### 7.6.1 常见问题

**问题 1：配置文件未生效**
- 检查文件路径是否正确
- 检查文件格式是否正确
- 查看日志中的配置加载信息

**问题 2：路由匹配失败**
- 检查 Host 头是否正确
- 检查 Path 是否匹配
- 查看路由匹配日志

**问题 3：后端连接失败**
- 检查端点地址是否可达
- 检查健康检查状态
- 查看代理处理器日志

**问题 4：性能问题**
- 检查限流配置
- 检查线程池大小
- 使用 JVM 监控工具分析

---

## 8. 部署指南

### 8.1 打包

```bash
# 打包（跳过测试）
mvn package -DskipTests

# 生成的文件
gateway-launcher/target/gateway-launcher-1.0.0.jar
```

### 8.2 部署方式

#### 8.2.1 直接运行

```bash
java -jar gateway-launcher-1.0.0.jar
```

#### 8.2.2 使用自定义配置

```bash
java -jar gateway-launcher-1.0.0.jar /path/to/config.yaml
```

#### 8.2.3 后台运行

```bash
nohup java -jar gateway-launcher-1.0.0.jar > gateway.log 2>&1 &
```

#### 8.2.4 Systemd 服务

创建 `/etc/systemd/system/nacos-gateway.service`：

```ini
[Unit]
Description=Nacos Gateway
After=network.target

[Service]
Type=simple
User=gateway
WorkingDirectory=/opt/nacos-gateway
ExecStart=/usr/bin/java -jar /opt/nacos-gateway/gateway-launcher-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
sudo systemctl start nacos-gateway
sudo systemctl enable nacos-gateway
```

#### 8.2.5 Docker 部署

创建 `Dockerfile`：

```dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY gateway-launcher/target/gateway-launcher-1.0.0.jar app.jar

COPY gateway-launcher/src/main/resources/config.yaml config.yaml

EXPOSE 9848

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["config.yaml"]
```

构建镜像：
```bash
docker build -t nacos-gateway:1.0.0 .
```

运行容器：
```bash
docker run -d \
  --name nacos-gateway \
  -p 9848:9848 \
  -v /path/to/config.yaml:/app/config.yaml \
  nacos-gateway:1.0.0
```

### 8.3 健康检查

```bash
# 检查网关健康状态
curl http://localhost:9848/health

# 响应示例
{
  "status": "UP",
  "timestamp": 1704662400000
}
```

### 8.4 日志

日志文件位置：`logs/gateway.log`

实时查看：
```bash
tail -f logs/gateway.log
```

### 8.5 监控

#### 8.5.1 JVM 监控

```bash
# JVM 状态
jps -l | grep gateway
jstat -gcutil <pid> 1000

# 线程 dump
jstack <pid> > thread_dump.txt

# 堆 dump
jmap -dump:live,format=b,file=heap_dump.hprof <pid>
```

#### 8.5.2 应用监控

使用 JMX：
```bash
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9010 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -jar gateway-launcher-1.0.0.jar
```

### 8.6 性能调优

#### 8.6.1 JVM 参数调优

```bash
java -server \
     -Xms4g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/gateway/ \
     -jar gateway-launcher-1.0.0.jar
```

#### 8.6.2 系统参数调优

```bash
# 增加文件描述符限制
ulimit -n 65535

# 优化 TCP 参数
sysctl -w net.ipv4.tcp_tw_reuse=1
sysctl -w net.ipv4.tcp_fin_timeout=30
```

---

## 9. 测试指南

### 9.1 单元测试

#### 9.1.1 路由匹配测试

```java
@Test
public void testWildcardHostMatch() {
    HostMatcher matcher = new HostMatcher("*.example.com");
    assertTrue(matcher.matches("api.example.com"));
    assertTrue(matcher.matches("www.example.com"));
    assertFalse(matcher.matches("example.com"));
    assertFalse(matcher.matches("api.other.com"));
}
```

#### 9.1.2 负载均衡测试

```java
@Test
public void testRoundRobin() {
    LoadBalancer lb = new RoundRobinLoadBalancer();
    List<Endpoint> endpoints = createTestEndpoints(3);

    Endpoint e1 = lb.select(endpoints);
    Endpoint e2 = lb.select(endpoints);
    Endpoint e3 = lb.select(endpoints);
    Endpoint e4 = lb.select(endpoints);

    assertEquals(e1, e4); // 循环回第一个
}
```

### 9.2 集成测试

#### 9.2.1 端到端测试

```java
@Test
public void testHttpProxy() throws Exception {
    // 启动网关
    GatewayServer server = startTestGateway();

    // 启动测试后端
    Vertx vertx = Vertx.vertx();
    vertx.createHttpServer()
        .requestHandler(req -> req.response().end("Hello"))
        .listen(8080);

    // 发送请求
    HttpClient client = Vertx.vertx().createHttpClient();
    client.request(HttpMethod.GET, 9848, "localhost", "/")
        .onSuccess(req -> req.send()
            .onSuccess(resp -> {
                assertEquals(200, resp.statusCode());
                testComplete();
            }));
}
```

### 9.3 性能测试

#### 9.3.1 使用 JMeter

1. 创建测试计划
2. 添加 HTTP 请求
3. 配置线程组
4. 运行测试
5. 分析结果

#### 9.3.2 使用 wrk

```bash
wrk -t12 -c400 -d30s http://localhost:9848/api/test
```

### 9.4 压力测试

#### 9.4.1 并发连接测试

```bash
# 10000 并发连接
wrk -t12 -c10000 -d30s http://localhost:9848/
```

#### 9.4.2 限流测试

```bash
# 测试 QPS 限流
ab -n 10000 -c 100 http://localhost:9848/api/test
```

---

## 附录

### A. 配置文件完整示例

```yaml
# Nacos Gateway Configuration

server:
  port: 9848

# Routes configuration
routes:
  - host: "*.nacos.io"
    path: "/**"
    backend: nacos-service
    qpsLimit: 5000
    maxConnections: 1000

  - host: "api.example.com"
    path: "/api/**"
    backend: api-service

  - host: "static.example.com"
    path: "/**"
    backend: static-service

# Backends configuration
backends:
  - name: nacos-service
    loadBalance: round-robin
    probe:
      path: /nacos/v1/console/health/readiness
      periodSeconds: 10
      timeoutSeconds: 2
      successThreshold: 1
      failureThreshold: 3
    endpoints:
      - host: nacos1.internal
        port: 8848
        priority: 1
      - host: nacos2.internal
        port: 8848
        priority: 1
      - host: nacos3.internal
        port: 8848
        priority: 2

  - name: api-service
    loadBalance: least-connection
    probe:
      path: /health
      periodSeconds: 5
      timeoutSeconds: 1
      successThreshold: 2
      failureThreshold: 3
    endpoints:
      - host: api1.internal
        port: 8080
        priority: 1
      - host: api2.internal
        port: 8080
        priority: 1
      - host: api3.internal
        port: 8080
        priority: 1

  - name: static-service
    loadBalance: random
    probe:
      path: /ping
      periodSeconds: 30
      timeoutSeconds: 1
    endpoints:
      - host: cdn1.internal
        port: 80
        priority: 1
      - host: cdn2.internal
        port: 80
        priority: 2

# Rate limiting configuration
rateLimit:
  globalQpsLimit: 50000
  globalMaxConnections: 20000
  defaultQpsLimit: 2000
  defaultMaxConnections: 1000

# Timeout configuration
timeout:
  connectTimeoutSeconds: 10
  requestTimeoutSeconds: 60
  idleTimeoutSeconds: 120

# Logging configuration
logging:
  level: INFO
  verbose: true
  format: json

# Management endpoint configuration
management:
  health:
    enabled: true
    path: /health
```

### B. 故障排查清单

- [ ] 检查配置文件路径是否正确
- [ ] 检查配置文件格式是否正确（YAML 语法）
- [ ] 检查端口是否被占用
- [ ] 检查后端服务是否可访问
- [ ] 检查健康检查端点是否正常
- [ ] 检查防火墙规则
- [ ] 检查 JVM 内存是否充足
- [ ] 检查日志文件是否有错误
- [ ] 检查系统资源（CPU、内存、网络）

### C. 性能基准

| 指标 | 单机 | 集群 |
|------|------|------|
| 吞吐量 | 10000+ RPS | 50000+ RPS |
| 延迟 (P99) | < 10ms | < 20ms |
| 并发连接 | 10000+ | 50000+ |
| 内存占用 | 512MB - 2GB | - |
| CPU 占用 | 20% - 80% | - |

### D. 相关资源

- [Vert.x 官方文档](https://vertx.io/docs/)
- [Jackson YAML 文档](https://github.com/FasterXML/jackson-dataformat-yaml)
- [Logback 文档](http://logback.qos.ch/documentation.html)

### E. 贡献指南

欢迎贡献代码！请遵循以下流程：

1. Fork 项目
2. 创建功能分支
3. 提交代码
4. 推送到分支
5. 创建 Pull Request

---

**文档版本**: 1.0.0
**最后更新**: 2025-01-08
**维护者**: Nacos Gateway Team
