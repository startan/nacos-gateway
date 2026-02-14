# 路由模块设计

## 1. 概述

路由模块负责将客户端请求基于 Host 匹配到对应的后端服务。支持 Host 通配符匹配。

## 2. 核心组件

### 2.1 类图

```
RouteMatcher (interface)
    └── RouteMatcherImpl
            ├── CopyOnWriteArrayList<RouteMatchEntry>
            ├── addRoute(Route): void
            ├── clear(): void
            ├── updateRoutes(List<Route>): void
            └── match(host): Optional<Route>

Route
    ├── hostPattern (String)
    ├── backendName (String)
    ├── rateLimitConfig (RateLimitConfig)
    ├── from(List<RouteConfig>): Map<String, Route> (静态工厂方法)
    └── id(): String (返回 hostPattern)

HostMatcher
    ├── pattern (String)
    ├── regex (Pattern)
    └── matches(host): boolean
```

### 2.2 RouteMatchEntry

`RouteMatchEntry` 是 `RouteMatcherImpl` 的内部类：

```
RouteMatchEntry
    ├── route (Route)
    └── hostMatcher (HostMatcher)
```

## 3. 匹配算法

### 3.1 Host 匹配

| 模式 | 说明 | 示例 |
|------|------|------|
| 精确匹配 | 完全相等 | `example.com` |
| 通配符匹配 | `*` 替代任意子域 | `*.example.com` 匹配 `api.example.com` |
| 通配符转换 | 转为正则表达式 | `*.example.com` → `[^.]+\.example\.com` |

### 3.2 匹配优先级

1. 精确匹配 > 通配符匹配
2. 先匹配的优先

## 4. 路由表

### 4.1 数据结构

`RouteTable` 使用 `ConcurrentHashMap` 保证线程安全：

```
ConcurrentHashMap<String, Route>
    key: hostPattern
    value: Route
```

`RouteMatcherImpl` 使用 `CopyOnWriteArrayList` 存储 `RouteMatchEntry`：

```
CopyOnWriteArrayList<RouteMatchEntry>
```

### 4.2 更新策略

配置热更新时使用 Copy-on-Write：
1. 创建新的路由表
2. 替换原子引用
3. 旧路由自然消亡

## 5. RouteTable API

```java
/**
 * 添加路由
 */
public void addRoute(Route route)

/**
 * 移除路由
 * @param routeId 路由标识（hostPattern）
 */
public void removeRoute(String routeId)

/**
 * 获取路由
 * @param routeId 路由标识（hostPattern）
 * @return 路由对象，不存在返回 null
 */
public Route getRoute(String routeId)

/**
 * 获取所有路由
 * @return 路由列表
 */
public List<Route> getAllRoutes()

/**
 * 清空所有路由
 */
public void clear()

/**
 * 更新路由表
 * @param newRoutes 新路由列表
 */
public void updateRoutes(List<Route> newRoutes)

/**
 * 获取路由数量
 * @return 路由数量
 */
public int size()
```

## 6. 匹配流程

```
请求到达
    ↓
提取 Host
    ↓
遍历路由表进行匹配
    └─ HostMatcher.matches(host)
    ↓
匹配成功 → 返回 Route
匹配失败 → 返回 Optional.empty()
```
