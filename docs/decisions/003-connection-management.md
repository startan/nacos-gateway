# ADR 003: 连接级资源管理

## 状态
已接受

## 背景

### 核心问题：nacos gRPC 双向流通讯

nacos 客户端与服务端采用 **gRPC 双向流通讯机制**，客户端需要持续接收来自服务端的事件通知（如配置变更、服务实例变化等）。

在反向代理场景中，如果使用**后端连接池机制**，会导致：
- 多个客户端连接复用同一个后端连接
- 后端服务端的事件通知无法正确路由到对应的 nacos 客户端
- 客户端无法及时接收服务端的推送事件，功能失效

因此，必须采用**连接级资源管理**，确保每个客户端连接与后端连接的一对一绑定。

### 传统反向代理的挑战

除了上述核心问题，在典型的反向代理场景中，一个客户端连接可能产生多个后端请求（如 HTTP/1.1 keep-alive 或 HTTP/2 多路复用）。如何管理后端资源是一个关键设计决策：

1. **资源泄漏风险**：共享后端连接可能导致连接未正确释放
2. **状态追踪困难**：难以关联客户端请求与后端资源
3. **动态路由变更**：配置变更时需要断开不符合新路由的连接

## 决策

采用连接级资源管理架构，每个客户端连接对应一个专属的 ProxyConnection，内部管理专属 HttpClient、Endpoint 和 Backend。

### 架构设计

```
客户端连接
    │
    ▼
ProxyConnection (每个客户端连接一个)
    ├── clientConnection (HttpServerRequest)
    ├── backend (Backend)           # 绑定的后端服务
    ├── endpoint (Endpoint)          # 选择的端点
    ├── backendConnection (HttpClientRequest)
    └── close(): void               # 清理所有资源
```

### 关键特性

1. **一对一绑定**：客户端连接与后端资源的生命周期完全绑定
2. **自动清理**：客户端断开时，自动清理关联的后端连接和限流配额
3. **动态路由支持**：配置变更导致路由失效时，可精确断开受影响的连接

### 资源清理流程

```
客户端断开
    │
    ▼
ConnectionManager.unregister(clientConn)
    │
    ├── proxyConnection.close()
    │   ├── 关闭后端连接
    │   ├── 释放限流配额
    │   └── 通知负载均衡器
    │
    └── 从管理器移除记录
```

## 后果

### 正面影响

- **防止泄漏**：客户端断开时自动清理所有关联资源，有效防止连接泄漏
- **状态清晰**：每个 ProxyConnection 包含完整的请求上下文，便于调试和监控
- **动态路由**：配置变更时可以精确识别和断开受影响的连接
- **线程安全**：每个连接独立管理状态，避免共享状态的并发问题

### 负面影响

- **资源开销**：每个客户端连接都有独立的管理对象，内存占用略高
- **连接数**：后端连接数与客户端连接数成正比，可能增加后端压力

### 风险

- 大量并发连接时，ProxyConnection 对象数量增加
- gRPC 长连接场景下，后端连接可能长时间保持

## 替代方案

### 方案 A：共享连接池
- **优点**：减少后端连接数，降低资源消耗
- **缺点**：状态追踪复杂，容易产生连接泄漏

### 方案 B：请求级管理
- **优点**：资源管理更细粒度
- **缺点**：无法支持连接复用，性能较差

## 相关文档

- [代理模块设计](../developer/modules/proxy.md)
- [架构设计](../developer/architecture.md)
- [ConnectionManager.java](../../gateway-core/src/main/java/nextf/nacos/gateway/proxy/ConnectionManager.java)
- [ProxyConnection.java](../../gateway-core/src/main/java/nextf/nacos/gateway/proxy/ProxyConnection.java)
