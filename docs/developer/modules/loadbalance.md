# 负载均衡模块设计

## 1. 概述

负载均衡模块负责从后端服务的多个端点中选择一个目标端点。支持三种负载均衡策略和端点优先级机制。

## 2. 核心组件

### 2.1 类图

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
                    └── connections (ConcurrentHashMap<Endpoint, AtomicInteger>)

EndpointSelector
    └── select(backend): Endpoint

LoadBalancerFactory
    └── create(strategy): LoadBalancer
```

### 2.2 LoadBalancerFactory

工厂类负责创建对应策略的负载均衡器：

```java
public LoadBalancer create(String strategy) {
    if (strategy == null) {
        return new RoundRobinLoadBalancer(); // 默认策略
    }

    switch (strategy.toLowerCase()) {
        case "round-robin":
            return new RoundRobinLoadBalancer();
        case "random":
            return new RandomLoadBalancer();
        case "least-connection":
            return new LeastConnectionLoadBalancer();
        default:
            // 未知策略时使用默认策略
            return new RoundRobinLoadBalancer();
    }
}
```

## 3. 负载均衡策略

### 3.1 轮询 (Round Robin)

- **算法**: 按顺序依次选择
- **实现**: 使用 `AtomicInteger` 作为索引
- **线程安全**: `getAndIncrement()` 原子操作
- **连接追踪**: 不追踪连接数

### 3.2 随机 (Random)

- **算法**: 随机选择可用端点
- **实现**: 使用 `Random.nextInt(size)`
- **线程安全**: 无状态，天然线程安全
- **连接追踪**: 不追踪连接数

### 3.3 最少连接 (Least Connection)

- **算法**: 选择当前连接数最少的端点
- **实现**: 使用 `ConcurrentHashMap<Endpoint, AtomicInteger>` 追踪
- **线程安全**: 原子操作保证
- **连接追踪**: 需要调用 `onConnectionOpen/Close`
- **自动清理**: 连接数 <= 0 时自动移除条目，防止内存泄漏

## 4. 端点优先级

### 4.1 优先级分组

端点按 `priority` 字段分组：
- **数值越小，优先级越高**
- 建议最小值为 1（最高优先级）
- 默认值：10

### 4.2 选择逻辑

```
1. 按优先级分组端点
2. 找到数值最小的优先级组（最高优先级）
3. 从该组内选择健康端点
4. 在组内使用负载均衡策略
```

### 4.3 示例

```
后端服务: example-service
端点列表:
  - 10.0.0.1 (priority: 10) ← 健康
  - 10.0.0.2 (priority: 10) ← 健康
  - 10.0.0.3 (priority: 20) ← 健康
  - 10.0.0.4 (priority: 20) ← 不健康

选择结果:
  → 只从 priority=10 组选择 (10.0.0.1 或 10.0.0.2)
  → 10.0.0.3 不被选中（低优先级组有健康端点）
```

## 5. 选择流程

```
请求到达后端服务
    ↓
获取后端所有端点
    ↓
过滤健康端点（与 HealthCheckManager 集成）
    ↓
按优先级分组
    ↓
选择数值最小的优先级组
    ↓
在组内使用负载均衡策略选择
    ↓
返回选中的端点
```
