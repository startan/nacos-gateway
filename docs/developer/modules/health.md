# 健康检查模块设计

## 1. 概述

健康检查模块负责定时探测后端端点的健康状态，只将健康端点提供给负载均衡器。支持 HTTP 和 TCP 两种探测方式。

## 2. 核心组件

### 2.1 类图

```
HealthCheckTask
    ├── vertx (Vertx)
    ├── endpoint (Endpoint)
    ├── config (HealthProbeConfig)
    ├── consecutiveSuccesses (AtomicInteger)
    ├── consecutiveFailures (AtomicInteger)
    ├── tcpHealthChecker (TcpHealthChecker)
    ├── timerId (long)
    ├── start(): void
    ├── stop(): void
    ├── checkHealth(timerId): void
    ├── performTcpHealthCheck(): void
    ├── performHttpHealthCheck(): void
    ├── handleResponse(HttpClientResponse): void
    ├── handleSuccess(): void
    ├── handleFailure(): void
    └── createHttpClient(): HttpClient

TcpHealthChecker
    ├── vertx (Vertx)
    ├── endpoint (Endpoint)
    ├── config (HealthProbeConfig)
    └── check(): Future<Boolean>

HealthCheckManager
    ├── vertx (Vertx)
    ├── registry (GatewayRegistry)
    ├── Map<Endpoint, HealthCheckTask> tasks
    ├── startChecking(endpoint, config): void
    ├── stopChecking(endpoint): void
    ├── startBackendChecking(): void
    ├── stopAll(): void
    └── getActiveCheckCount(): int
```

### 2.2 设计说明

- HTTP 探测逻辑直接在 `HealthCheckTask` 中实现，没有独立的 `HttpHealthChecker` 类
- TCP 探测使用独立的 `TcpHealthChecker` 类
- 健康状态直接更新到 `Endpoint` 对象，不使用事件驱动
- 健康检查始终使用后端服务的 `apiV1` 端口

## 3. 探测类型

### 3.1 HTTP 探测

- **协议**: HTTP/1
- **方法**: GET
- **路径**: 可配置（默认 `/health`）
- **判定**: HTTP 状态码 2xx 为健康
- **端口**: 使用端点的 `apiV1Port`

### 3.2 TCP 探测

- **协议**: TCP 连接
- **判定**: 连接成功为健康
- **端口**: 使用端点的 `apiV1Port`
- **连接**: 连接成功后立即关闭

## 4. 阈值机制

### 4.1 配置参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| enabled | 是否启用健康检查 | true |
| type | 探测类型（http/tcp） | http |
| periodSeconds | 探测周期（秒） | 10 |
| timeoutSeconds | 探测超时（秒，当前未使用） | 1 |
| path | HTTP 探测路径 | `/health` |
| successThreshold | 成功阈值 | 1 |
| failureThreshold | 失败阈值 | 3 |

### 4.2 状态转换

```
[初始状态] → [探测中]
              ↓
         连续 successThreshold 次成功
              ↓
         [健康状态] ←→ [不健康状态]
              ↑                ↑
         连续 failureThreshold 次失败
```

## 5. 探测流程

```
定时触发（periodSeconds）
    ↓
根据 type 选择探测方式
    ├─ tcp → performTcpHealthCheck()
    └─ http → performHttpHealthCheck()
    ↓
收集探测结果
    ↓
更新端点健康状态
    ├─ 成功 → consecutiveSuccesses++, consecutiveFailures = 0
    └─ 失败 → consecutiveFailures++, consecutiveSuccesses = 0
    ↓
判定状态
    ├─ consecutiveSuccesses >= successThreshold → endpoint.setHealthy(true)
    └─ consecutiveFailures >= failureThreshold → endpoint.setHealthy(false)
```

## 6. 调度管理

### 6.1 启动探测

```java
void startChecking(Endpoint endpoint, HealthProbeConfig config) {
    HealthCheckTask task = new HealthCheckTask(vertx, endpoint, config);
    tasks.put(endpoint, task);
    task.start();
}
```

### 6.2 停止探测

```java
void stopChecking(Endpoint endpoint) {
    HealthCheckTask task = tasks.remove(endpoint);
    if (task != null) {
        task.stop();
    }
}
```

### 6.3 批量启动

`startBackendChecking()` 方法会遍历注册中心的所有后端服务：
- 如果健康检查未配置或禁用：标记所有端点为健康
- 如果健康检查启用：为每个端点启动独立的健康检查任务
