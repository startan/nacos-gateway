# RateLimit 配置热加载功能

## 功能概述

网关现在支持 **rateLimit 配置的热加载**，无需重启服务即可动态更新限流配置。

### 支持的配置级别

- **Server 级别配置** (`server.rateLimit`)
  - `maxQps`: 全局 QPS 限制
  - `maxConnections`: 全局连接数限制
  - `maxQpsPerClient`: 单客户端默认 QPS 限制
  - `maxConnectionsPerClient`: 单客户端默认连接数限制

- **Backend 级别配置** (`backends[].rateLimit`)
  - 所有字段均支持热加载（已有功能）

- **Route 级别配置** (`routes[].rateLimit`)
  - `maxQps`: 路由级 QPS 限制（最高优先级）
  - `maxConnections`: 路由级连接数限制
  - `maxQpsPerClient`: 路由级单客户端 QPS 限制（最高优先级）
  - `maxConnectionsPerClient`: 路由级单客户端连接数限制（最高优先级）

## 配置更新策略

### 全局限流器

| 限流器 | 更新策略 | 说明 |
|--------|---------|------|
| QPS 限流器 | 创建新实例，计数器重置为 0 | 更新瞬间可能允许超出新限制的请求通过，属正常行为 |
| 连接限流器 | 创建新实例，保留当前连接数 | 如果新限制 < 当前连接数，旧连接自然消亡，仅限制新连接 |

### 客户端限流器

- **配置变更时**：自动清理所有客户端限流器
- **生效时机**：下次请求时使用新配置创建
- **QPS 限制**：在下一秒统计窗口开始生效
- **连接数限制**：现有客户端限流器继续使用，新客户端使用新配置

### 线程安全保证

- 使用 `AtomicReference` 保证配置切换的原子性
- 使用 `ConcurrentHashMap` 保证并发访问安全
- 配置更新过程中，现有请求继续使用旧配置，新请求使用新配置

## 使用示例

### 示例 1：提高全局 QPS 限制

**修改配置文件** `nacos-gateway.yaml`:
```yaml
server:
  rateLimit:
    maxQps: 5000  # 从 2000 提高到 5000
```

**预期行为**：
- 新配置立即生效
- QPS 计数器重置为 0
- 全局 QPS 限制从 2000 提升到 5000

### 示例 2：收紧连接数限制

**修改配置文件**:
```yaml
server:
  rateLimit:
    maxConnections: 5000  # 从 10000 降低到 5000
```

**预期行为**：
- 新配置立即生效
- 当前连接数保留（假设当前有 6000 个连接）
- 记录警告日志：`New maxConnections (5000) is less than current connections (6000), existing connections will be allowed to decay naturally`
- 旧连接继续保留直到断开，新连接请求被拒绝（因为当前连接数 > 新限制）
- 随着旧连接断开，新连接逐步可用

### 示例 3：调整单客户端限制

**修改配置文件**:
```yaml
server:
  rateLimit:
    maxQpsPerClient: 20   # 从 10 提高到 20
    maxConnectionsPerClient: 10  # 从 5 提高到 10
```

**预期行为**：
- 新配置立即生效
- 所有现有客户端限流器被清理
- 下次请求时，使用新配置创建客户端限流器
- 新客户端和重新连接的客户端立即享受更高的限制

### 示例 4：仅修改 Backend 级别配置

**修改配置文件**:
```yaml
backends:
  - name: local-nacos
    rateLimit:
      maxQps: 2000  # 从 1000 提高到 2000
      maxQpsPerClient: 50  # 从 10 提高到 50
```

**预期行为**：
- 仅 `local-nacos` 后端的限流器更新
- 其他后端和全局配置不受影响

### 示例 5：配置 Route 级别限流

**修改配置文件**:
```yaml
routes:
  - host: "api.example.com"
    backend: api-service
    rateLimit:
      maxQpsPerClient: 100       # Route 级覆盖：100 QPS/客户端
      maxConnectionsPerClient: 50 # Route 级覆盖：50 连接/客户端
```

**预期行为**：
- 仅 `api.example.com` 路由的限流配置更新
- 其他路由使用 Backend/Server 配置
- 新配置立即生效
- 客户端限流器清理，下次请求使用新配置

**优先级示例**：
```yaml
# 配置优先级演示
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

  - host: "admin.example.com"
    backend: api-service
    # 无 rateLimit → 使用 backend 配置 → 实际生效 50 QPS
```

## 注意事项和限制

### 1. QPS 计数器重置

更新全局或后端 QPS 限制时，计数器会重置为 0。这可能导致更新瞬间的瞬时流量超出新限制。

**影响**：可接受，符合设计预期

### 2. 连接数限制收紧

当新限制小于当前连接数时，旧连接不会主动断开，而是自然消亡。在此期间，新连接请求将被拒绝。

**影响**：可能短暂影响新连接建立，符合软限制策略

### 3. 客户端限流器清理

更新 server 级别的 per-client 限制时，所有现有客户端限流器会被清理。这可能导致：

- 正在运行的客户端立即使用新配置
- 如果限制从宽松变严格，现有客户端可能立即被限流

**影响**：符合即时生效需求

### 4. 配置验证

配置更新前会验证配置的合法性，如果验证失败，更新会回滚，保持旧配置不变。

### 5. 端口配置变更

端口配置变更仍需重启服务，不在热加载范围内。

## 配置文件位置

- **默认位置**：`classpath:nacos-gateway.yaml`
- **自定义位置**：启动时通过 `-c` 参数指定，如 `java -jar gateway.jar -c /path/to/config.yaml`

## 监控和日志

### 配置更新日志

```
INFO  n.n.g.ratelimit.RateLimitManager - Server rate limit config updated: QPS 2000 -> 5000, Connections 10000 -> 5000, Per-client QPS 10 -> 20, Per-client connections 5 -> 10
INFO  n.n.g.ratelimit.RateLimitManager - Cleared 15 client rate limiters due to server rate limit configuration change
```

### 连接数收紧警告

```
WARN  n.n.g.ratelimit.RateLimitManager - New maxConnections (5000) is less than current connections (6000), existing connections will be allowed to decay naturally
```

### 配置未变更

```
DEBUG n.n.g.ratelimit.RateLimitManager - Server rate limit config unchanged, skipping update
```

## 与现有功能的对比

| 功能 | 之前 | 现在 |
|-----|------|------|
| Server 级别 rateLimit | ❌ 不支持热加载 | ✅ 支持热加载 |
| Backend 级别 rateLimit | ✅ 支持热加载 | ✅ 支持热加载 |
| Route 级别 rateLimit | ❌ 不存在 | ✅ 支持热加载（最高优先级） |
| 配置更新时重启 | ❌ 需要重启 | ✅ 无需重启 |
| 连接数状态保留 | N/A | ✅ 保留当前连接数 |
| 客户端限流器更新 | N/A | ✅ 自动清理并重建 |
| 三级级联优先级 | Backend → Server | Route → Backend → Server |

## 实现原理

### 核心设计

采用 **"原子引用替换 + 平滑过渡"** 方案：

1. 将不可变的 `final` 配置字段改为 `AtomicReference`
2. 添加 `updateServerRateLimitConfig()` 方法实现热更新
3. 全局限流器创建新实例替换（连接数保留当前值）
4. 客户端限流器自动清理，下次请求使用新配置

### 关键类

- `RateLimitManager`: 限流管理器，核心热更新逻辑
- `ConfigReloader`: 配置重载器，调用热更新方法
- `ConnectionRateLimiter`: 连接限流器，支持状态保留
- `QpsRateLimiter`: QPS 限流器，滑动窗口算法

### 配置更新流程

```
配置文件变更
  → ConfigWatcher 检测到变更
  → ConfigReloader.reload()
    → ConfigReloader.updateRateLimiters()
      → RateLimitManager.updateServerRateLimitConfig()
        → 创建新限流器实例
        → 原子性替换配置
        → 清理客户端限流器（如需要）
  → 新配置生效
```

## 故障排查

### 配置更新未生效

1. 检查配置文件语法是否正确
2. 查看日志中是否有配置更新记录
3. 确认 `ConfigWatcher` 是否正常运行

### 连接数持续超出限制

1. 查看警告日志，确认是否因收紧限制导致
2. 监控当前连接数，等待旧连接自然消亡
3. 如需立即生效，可临时提高限制再逐步降低

### 客户端限流不生效

1. 检查 server 级别的 per-client 配置
2. 检查 backend 级别的 per-client 配置（优先级更高）
3. 查看日志确认客户端限流器是否已清理

## 版本历史

- **v1.1** (当前版本): 新增 Route 级别限流配置
  - Route 级别限流支持（最高优先级）
  - 三级级联：Route → Backend → Server → -1
  - 支持 Route 级别配置热加载
  - 客户端限流器支持 Route 级配置优先级

- **v1.0**: 首次实现 rateLimit 配置热加载功能
  - Server 级别配置热加载支持
  - 连接数状态保留
  - 客户端限流器自动清理
