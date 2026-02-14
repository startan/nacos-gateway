# 限流模块设计

## 1. 概述

限流模块负责保护后端服务，支持 QPS 和连接数两种限流方式，配置级联覆盖 Server/Backend/Route 三级。

## 2. 核心组件

### 2.1 类图

```
RateLimiter (interface)
    ├── tryAcquire(): boolean
            │
            ├── QpsRateLimiter
            │       ├── maxQps (int)
            │       ├── counter (AtomicInteger)
            │       ├── counterWindow (AtomicLong)
            │       └── WINDOW_SIZE_MS (static long) = 1000
            │
            └── ConnectionRateLimiter
                    ├── maxConnections (int)
                    ├── currentConnections (AtomicInteger)
                    ├── release(): void
                    ├── getCurrentConnections(): int
                    └── setCurrentConnections(int): void

ClientRateLimiter
    ├── clientId (String)
    ├── maxQps (int)
    ├── maxConnections (int)
    ├── qpsLimited (boolean)
    ├── connectionLimited (boolean)
    ├── qpsLimiter (QpsRateLimiter)
    ├── connectionLimiter (ConnectionRateLimiter)
    ├── tryAcquireQps(): boolean
    ├── tryAcquireConnection(): boolean
    └── release(): void

BackendRateLimiter
    ├── backendName (String)
    ├── maxQps (int)
    ├── maxConnections (int)
    ├── qpsLimiter (QpsRateLimiter)
    ├── connectionLimiter (ConnectionRateLimiter)
    ├── tryAcquireQps(): boolean
    ├── tryAcquireConnection(): boolean
    └── release(): void

RouteRateLimiter
    ├── routeId (String)
    ├── maxQps (int)
    ├── maxConnections (int)
    ├── qpsLimiter (QpsRateLimiter)
    ├── connectionLimiter (ConnectionRateLimiter)
    ├── tryAcquireQps(): boolean
    ├── tryAcquireConnection(): boolean
    └── releaseConnection(): void

RateLimitManager
    ├── globalQpsLimiter (AtomicReference<QpsRateLimiter>)
    ├── globalConnectionLimiter (AtomicReference<ConnectionRateLimiter>)
    ├── Map<String, BackendRateLimiter> backendLimiters
    ├── Map<String, RouteRateLimiter> routeRateLimiters
    ├── Map<String, ClientRateLimiter> clientLimiters
    ├── Map<String, RateLimitConfig> backendRateLimitConfigs
    ├── Map<String, RateLimitConfig> routeRateLimitConfigs
    ├── AtomicReference<RateLimitConfig> serverRateLimitConfig
    ├── tryAcquire(backendName, clientIp, routeId): boolean
    ├── tryAcquireConnection(...): boolean
    ├── releaseConnection(proxyConnection): void
    ├── updateBackendLimiter(...): void
    ├── updateRouteLimiter(...): void
    ├── updateServerRateLimitConfig(...): boolean
    ├── clearRouteLimiters(): void
    └── clearClientLimiters(): void
```

### 2.2 限流器类型

- `QpsRateLimiter` - QPS 限流器（滑动窗口算法）
- `ConnectionRateLimiter` - 连接数限流器（原子计数器）
- `ClientRateLimiter` - 客户端限流器（包含 QPS 和连接数限流）
- `BackendRateLimiter` - 后端服务级限流器
- `RouteRateLimiter` - 路由级限流器

## 3. 限流类型

### 3.1 QPS 限流

- **算法**: 滑动窗口
- **精度**: 秒级
- **实现**: 维护每秒的请求数计数器
- **清理**: 超过当前秒的计数器自动清理

### 3.2 连接数限流

- **算法**: 原子计数器
- **精度**: 精确计数
- **实现**: `AtomicInteger`
- **特性**: 连接关闭时自动释放

## 4. 级联配置

### 4.1 优先级顺序

客户端级限流采用级联配置，优先级从高到低：

```
Route (!= -1) → Backend (!= -1) → Server (!= -1) → -1 (无限制)
```

只有当配置值不等于 -1（即明确配置了限流）时，该级配置才会生效。

### 4.2 值语义

| 值 | 含义 |
|----|------|
| -1 | 无限制 |
| 0 | 拒绝所有访问 |
| > 0 | 正常限流 |

### 4.3 配置示例

```yaml
server:
  rateLimit:
    maxQps: 2000                 # 全局 QPS 限流
    maxConnections: 10000        # 全局连接数限流
    maxQpsPerClient: 10          # 客户端级 QPS 限流（优先级最低）
    maxConnectionsPerClient: 5    # 客户端级连接数限流

backends:
  - name: api-service
    rateLimit:
      maxQps: 1000               # 后端级 QPS 限流
      maxConnections: 5000        # 后端级连接数限流
      maxQpsPerClient: 50         # 客户端级 QPS 限流（优先级中等）
      maxConnectionsPerClient: 10 # 客户端级连接数限流

routes:
  - host: "api.example.com"
    backend: api-service
    rateLimit:
      maxQps: 500                # 路由级 QPS 限流
      maxConnections: 2000        # 路由级连接数限流
      maxQpsPerClient: 100        # 客户端级 QPS 限流（优先级最高）
      maxConnectionsPerClient: 20 # 客户端级连接数限流
```

## 5. 限流检查流程

### 5.1 QPS 检查流程

```
请求到达
    ↓
检查全局 QPS 限流
    ├─ 未通过 → 返回 HTTP 429
    ↓
检查路由级 QPS 限流（如配置）
    ├─ 未通过 → 返回 HTTP 429
    ↓
检查后端级 QPS 限流（如配置）
    ├─ 未通过 → 返回 HTTP 429
    ↓
检查客户端 QPS 限流（级联配置）
    ├─ 未通过 → 返回 HTTP 429
    ↓
通过 → 放行
```

### 5.2 连接数检查流程

```
创建新连接
    ↓
检查全局连接数限流
    ├─ 未通过 → 返回 HTTP 429
    ↓
检查路由级连接数限流（如配置）
    ├─ 未通过 → 返回 HTTP 429
    ↓
检查后端级连接数限流（如配置）
    ├─ 未通过 → 返回 HTTP 429
    ↓
检查客户端连接数限流（级联配置）
    ├─ 未通过 → 返回 HTTP 429
    ↓
通过 → 创建连接
```

## 6. 热更新支持

### 6.1 更新策略

| 限流器 | 更新策略 |
|--------|----------|
| 全局 QPS 限流器 | 创建新实例，计数器重置为 0 |
| 全局连接限流器 | 创建新实例，保留当前连接数 |
| 客户端限流器 | 清空缓存，下次请求使用新配置 |
| 路由级限流器 | 清空客户端限流器缓存 |
| 后端级限流器 | 直接替换 |

### 6.2 连接数收紧处理

```
新限制 < 当前连接数
    ↓
记录警告日志
    ↓
旧连接自然消亡
    ↓
新连接请求被拒绝
    ↓
随着旧连接断开，新连接逐步可用
```

### 6.3 配置更新检测

更新前会比较新旧配置，只有配置值真正发生变化时才会执行更新操作。

## 7. 管理方法

### 7.1 RateLimitManager 公开方法

```java
/**
 * 更新后端服务级限流器
 * @param backendName 后端服务名称
 * @param backendConfig 后端服务配置
 */
public void updateBackendLimiter(String backendName, BackendConfig backendConfig)

/**
 * 更新路由级限流器
 * @param routeId 路由标识（Route.getId()）
 * @param routeConfig 路由配置
 */
public void updateRouteLimiter(String routeId, RouteConfig routeConfig)

/**
 * 更新服务器级限流配置（热更新支持）
 * @param newConfig 新的服务器限流配置（null 表示重置为无限制）
 * @return 更新成功返回 true，否则返回 false
 */
public boolean updateServerRateLimitConfig(RateLimitConfig newConfig)

/**
 * 清空所有路由级限流器
 * 路由更新时调用
 */
public void clearRouteLimiters()

/**
 * 清空所有客户端限流器
 * 服务器配置更新时可强制重新创建
 */
public void clearClientLimiters()
```

### 7.2 RateLimitManager 私有方法

```java
/**
 * 获取或创建客户端限流器（带正确的配置解析）
 * 级联配置：Route (!= -1) → Backend (!= -1) → Server (!= -1) → -1 (无限制)
 * @return 客户端限流器
 */
private ClientRateLimiter getOrCreateClientLimiter(String clientIp, String backendName, String routeId)

/**
 * 比较两个服务器限流配置是否相等
 * @return 相等返回 true
 */
private boolean configEquals(RateLimitConfig c1, RateLimitConfig c2)
```

## 8. 客户端限流器清理

- 当客户端的所有连接都关闭后，对应的限流器会自动从缓存中移除
- 这确保了长时间不活跃的客户端不会占用内存资源
