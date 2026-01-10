# AI Agent 编程指南 - Nacos Gateway

> 本文档专门为 AI Agent 编码工具设计，提供结构化的项目信息、代码组织、类依赖关系等，便于 AI 理解和修改代码。

## 项目元数据

```yaml
project:
  name: "Nacos Gateway"
  type: "Java Application"
  build_tool: "Maven"
  jdk_version: "17"
  root_package: "pans.gateway"

modules:
  - name: "gateway-api"
    path: "gateway-api"
    package: "pans.gateway"

  - name: "gateway-core"
    path: "gateway-core"
    package: "pans.gateway"
    depends_on: ["gateway-api"]

  - name: "gateway-launcher"
    path: "gateway-launcher"
    package: "pans.gateway"
    depends_on: ["gateway-core"]
```

## 模块结构树

```
pans.gateway/
├── config/                          # 配置管理
│   ├── GatewayConfig                # 根配置类
│   ├── ServerConfig                 # 服务器配置
│   ├── RouteConfig                  # 路由配置
│   ├── BackendConfig                # 后端配置
│   ├── EndpointConfig               # 端点配置
│   ├── HealthProbeConfig            # 健康检查配置
│   ├── RateLimitConfig              # 限流配置
│   ├── TimeoutConfig                # 超时配置
│   ├── LoggingConfig                # 日志配置
│   ├── ManagementConfig             # 管理接口配置
│   ├── ConfigLoader                 # 配置加载器
│   ├── ConfigWatcher                # 配置文件监听器
│   └── ConfigReloader               # 配置热更新器
│
├── route/                           # 路由匹配
│   ├── Route (interface)            # 路由实体
│   ├── RouteMatcher (interface)     # 路由匹配器
│   ├── RouteMatcherImpl             # 路由匹配器实现
│   ├── RouteTable                   # 路由表管理
│   ├── HostMatcher                  # Host 匹配器
│   └── PathMatcher                  # Path 匹配器
│
├── loadbalance/                     # 负载均衡
│   ├── LoadBalancer (interface)     # 负载均衡器接口
│   ├── RoundRobinLoadBalancer       # 轮询实现
│   ├── RandomLoadBalancer           # 随机实现
│   ├── LeastConnectionLoadBalancer  # 最少连接实现
│   ├── LoadBalancerFactory          # 工厂类
│   └── EndpointSelector             # 端点选择器（含优先级）
│
├── health/                          # 健康检查
│   ├── HealthChecker (interface)    # 健康检查接口
│   ├── HealthCheckManager           # 健康检查管理器
│   ├── HealthCheckTask              # 健康检查定时任务
│   └── HttpHealthChecker            # HTTP 健康检查实现
│
├── ratelimit/                       # 限流
│   ├── RateLimiter (interface)      # 限流器接口
│   ├── RateLimitManager             # 限流管理器
│   ├── RouteRateLimiter             # 路由级限流器
│   ├── QpsRateLimiter               # QPS 限流器（滑动窗口）
│   └── ConnectionRateLimiter        # 连接数限流器
│
├── proxy/                           # 代理核心
│   ├── ProxyHandler (interface)     # 代理处理器接口
│   ├── HttpProxyHandler             # HTTP/1 代理处理器
│   ├── Http2ProxyHandler            # HTTP/2 代理处理器
│   ├── GrpcProxyHandler             # gRPC 代理处理器
│   ├── ConnectionManager            # 连接管理器
│   └── ProxyConnection              # 代理连接实体
│
├── management/                      # 管理接口
│   └── HealthEndpoint               # 健康检查端点
│
├── logging/                         # 日志系统
│   ├── LogFormatter (interface)     # 日志格式化器接口
│   ├── TextLogFormatter             # 文本格式化器
│   ├── JsonLogFormatter             # JSON 格式化器
│   ├── AccessLogger                 # 访问日志
│   └── AccessLog                    # 访问日志实体
│
├── server/                          # 服务器
│   ├── GatewayServer                # 网关服务器核心
│   └── ServerBootstrap              # 服务器启动类
│
└── model/                           # 数据模型
    ├── Endpoint                     # 端点模型
    ├── Backend                      # 后端服务模型
    └── ConnectionState              # 连接状态
```

## 核心类依赖关系图

```
┌─────────────┐
│ Main        │  启动入口
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ServerBootstrap│  服务器启动器
└──────┬──────┘
       │ creates
       ▼
┌─────────────────────────────┐
│   GatewayServer             │  网关服务器核心
├─────────────────────────────┤
│ - RouteMatcher              │  路由匹配
│ - LoadBalancer              │  负载均衡
│ - HealthCheckManager        │  健康检查
│ - RateLimitManager          │  限流
│ - ConnectionManager         │  连接管理
└─────────────────────────────┘
       │
       │ uses
       ▼
┌─────────────────────────────┐
│   Config Module             │  配置管理
├─────────────────────────────┤
│ ConfigLoader                │  加载 YAML
│ ConfigWatcher               │  监听文件变化
│ ConfigReloader              │  热更新
└─────────────────────────────┘
```

## 关键设计模式

### 1. 策略模式 (Strategy Pattern)

**位置**: `loadbalance` 包

**接口**: `LoadBalancer`

**实现类**:
- `RoundRobinLoadBalancer`
- `RandomLoadBalancer`
- `LeastConnectionLoadBalancer`

**工厂**: `LoadBalancerFactory.create(String strategy)`

**使用方式**:
```java
LoadBalancer lb = LoadBalancerFactory.create("round-robin");
Endpoint selected = lb.select(endpoints);
```

### 2. 责任链模式 (Chain of Responsibility)

**位置**: 请求处理流程

**链条**:
```
RouteMatcher → RateLimitManager → LoadBalancer → ProxyHandler
```

### 3. 观察者模式 (Observer Pattern)

**位置**: `config` 包

**主题**: `ConfigWatcher`

**观察者**: `ConfigReloader`

**触发**: 配置文件修改

### 4. 单例模式 (Singleton Pattern)

**位置**: Vert.x 实例管理

**实现**: 通过 `ServerBootstrap` 管理

## 核心流程算法

### 请求处理流程

```
输入: HttpServerRequest

1. 检查管理接口
   IF path == /health THEN
     HealthEndpoint.handle()
     RETURN

2. 路由匹配
   route = RouteMatcher.match(host, path)
   IF route == null THEN
     RETURN 404 Not Found

3. 限流检查（请求级别）
   IF !RateLimitManager.tryAcquire(route.id) THEN
     RETURN 429 Too Many Requests

4. 连接级别初始化
   connection = request.connection()
   proxyConnection = ConnectionManager.getConnection(connection)

   IF proxyConnection == null THEN
     # 首次请求：创建连接级资源
     a. 选择后端端点（负载均衡）
        backend = Backends[route.backendName]
        endpoint = EndpointSelector.select(backend)

     b. 创建专属 HttpClient
        httpClient = vertx.createHttpClient(createHttp2ClientOptions())

     c. 通知负载均衡器（连接级）
        backend.loadBalancer.onConnectionOpen(endpoint)

     d. 创建 ProxyConnection
        proxyConnection = new ProxyConnection(
            connection, route, endpoint, backend, httpClient
        )

     e. 注册到 ConnectionManager
        ConnectionManager.addConnection(connection, proxyConnection)

     f. 注册清理处理器
        connection.closeHandler(v -> ConnectionManager.removeConnection(connection))
        connection.exceptionHandler(t -> ConnectionManager.removeConnection(connection))
   ELSE
     # 后续请求：复用现有资源
     proxyConnection 已包含 httpClient、endpoint、backend

5. 选择代理处理器
   IF GrpcProxyHandler.isGrpcRequest(request) THEN
     handler = GrpcProxyHandler(proxyConnection.getHttpClient(), ...)
   ELSE
     handler = HttpProxyHandler(proxyConnection.getHttpClient(), ...)

6. 执行代理
   handler.handle(request)

7. 连接清理（自动）
   当客户端连接关闭时（正常或异常）：
     ConnectionManager.removeConnection(connection)
       → ProxyConnection.close()
         → httpClient.close()              # 关闭后端连接
         → backend.loadBalancer.onConnectionClose()  # 通知负载均衡器
```

### 路由匹配算法

```
HostMatcher.match(pattern, host):
  IF pattern 不包含 '*' THEN
    RETURN pattern.equalsIgnoreCase(host)

  IF pattern 是 "*.xxx.com" THEN
    regex = pattern.replace("*.", "[^.]+\\.").replace(".", "\\.")
    RETURN host.matches("^" + regex + "$")

PathMatcher.match(pattern, path):
  IF pattern 是 "/abc/**" THEN
    RETURN path.equals("/abc") OR path.startsWith("/abc/")

  IF pattern 是 "/abc/*" THEN
    RETURN path.startsWith("/abc/") AND path.substring(5) 不包含 '/'

  IF pattern 包含 '*' THEN
    regex = convertToRegex(pattern)
    RETURN path.matches("^" + regex + "$")

  RETURN pattern.equals(path)
```

### 负载均衡算法

```
EndpointSelector.select(backend):
  healthyEndpoints = backend.endpoints.filter(healthy)

  groupedByPriority = healthyEndpoints.groupBy(endpoint.priority)

  highestPriority = groupedByPriority.keys.min()

  highestPriorityEndpoints = groupedByPriority[highestPriority]

  RETURN backend.loadBalancer.select(highestPriorityEndpoints)
```

### 限流算法

```
QpsRateLimiter.tryAcquire():
  window = currentTime / 1000
  counter = windows[window].incrementAndGet()

  cleanupOldWindows(window)

  IF counter > maxQps THEN
    counter.decrementAndGet()
    RETURN false

  RETURN true

ConnectionRateLimiter.tryAcquire():
  DO
    current = currentConnections.get()
    IF current >= maxConnections THEN
      RETURN false
  WHILE !currentConnections.compareAndSet(current, current + 1)

  RETURN true
```

## Vert.x API 使用规范

### HTTP 服务器

```java
// 创建服务器
HttpServer server = vertx.createHttpServer(options);

// 设置请求处理器
server.requestHandler(this::handleRequest);

// 启动（返回 Future）
server.listen(port)
  .onSuccess(v -> log.info("Started"))
  .onFailure(t -> log.error("Failed", t));

// 关闭（返回 Future）
server.close()
  .onSuccess(v -> log.info("Stopped"))
  .onFailure(t -> log.error("Failed", t));
```

### HTTP 客户端

```java
// 创建客户端
HttpClient client = vertx.createHttpClient(options);

// 发送请求（返回 Future）
client.request(method, port, host, uri)
  .onSuccess(request -> {
    // 配置请求
    request.headers().set("Content-Type", "application/json");

    // 获取响应（返回 Future）
    request.response()
      .onSuccess(response -> {
        // 处理响应
        response.handler(buffer -> ...);
      })
      .onFailure(t -> ...);

    // 发送请求体
    request.end(body);
  })
  .onFailure(t -> ...);
```

### 定时任务

```java
// 周期性任务
long timerId = vertx.setPeriodic(delay, id -> {
  // 执行任务
});

// 一次性任务
long timerId = vertx.setTimer(delay, id -> {
  // 执行任务
});

// 取消任务
vertx.cancelTimer(timerId);
```

## 代码修改指南

### 添加新的负载均衡策略

1. 在 `loadbalance` 包创建新类：
```java
public class NewStrategyLoadBalancer implements LoadBalancer {
    @Override
    public Endpoint select(List<Endpoint> endpoints) {
        // 实现选择逻辑
    }
}
```

2. 在 `LoadBalancerFactory` 添加分支：
```java
case "new-strategy":
    return new NewStrategyLoadBalancer();
```

3. 更新配置文档和示例

### 添加新的限流算法

1. 实现 `RateLimiter` 接口：
```java
public class NewRateLimiter implements RateLimiter {
    @Override
    public boolean tryAcquire() {
        // 实现限流逻辑
    }
}
```

2. 在 `RateLimitManager` 中集成

3. 更新配置类

### 添加新的管理端点

1. 创建处理器类：
```java
public class NewEndpoint {
    public void handle(HttpServerRequest request) {
        // 处理请求
    }
}
```

2. 在 `GatewayServer` 中注册：
```java
if (newEndpoint.matches(path)) {
    newEndpoint.handle(request);
    return;
}
```

### 修改配置结构

1. 修改对应的 `XxxConfig` 类
2. 更新 `ConfigLoader.validate()` 方法
3. 更新默认 `config.yaml`
4. 更新文档

## 常见修改场景

### 场景 1：添加自定义请求头

位置：`HttpProxyHandler` 或 `GrpcProxyHandler`

```java
private void copyHeaders(HttpServerRequest from, HttpClientRequest to) {
    from.headers().forEach(header -> {
        if (!isHopByHopHeader(header.getKey())) {
            to.putHeader(header.getKey(), header.getValue());
        }
    });

    // 添加自定义头
    to.putHeader("X-Gateway", "Nacos-Gateway/1.0");
}
```

### 场景 2：修改健康检查逻辑

位置：`HealthCheckTask`

```java
private void handleResponse(HttpClientResponse response) {
    int statusCode = response.statusCode();

    // 自定义判断逻辑
    if (isSuccess(statusCode)) {
        handleSuccess();
    }
}

private boolean isSuccess(int statusCode) {
    // 自定义成功条件
    return statusCode >= 200 && statusCode < 300;
}
```

### 场景 3：添加访问日志

位置：`GatewayServer.handleRequest()`

```java
AccessLogger logger = new AccessLogger(config.getLogging());

long startTime = System.currentTimeMillis();

// ... 处理请求 ...

request.response().endHandler(v -> {
    long duration = System.currentTimeMillis() - startTime;
    logger.logRequest(
        request.method().name(),
        request.path(),
        response.getStatusCode(),
        duration,
        request.remoteAddress().host(),
        backend.getName(),
        endpoint.getAddress()
    );
});
```

## 测试指南

### 单元测试模板

```java
public class RouteMatcherTest {

    private RouteMatcher matcher;

    @BeforeEach
    public void setUp() {
        List<Route> routes = List.of(
            new Route(createRouteConfig("*.example.com", "/api/**")),
            new Route(createRouteConfig("example.com", "/**"))
        );
        matcher = new RouteMatcherImpl(routes);
    }

    @Test
    public void testWildcardMatch() {
        Optional<Route> route = matcher.match("api.example.com", "/api/users");
        assertTrue(route.isPresent());
        assertEquals("*.example.com", route.get().getHostPattern());
    }
}
```

### 集成测试模板

```java
public class GatewayIntegrationTest {

    private Vertx vertx;
    private GatewayServer server;

    @BeforeEach
    public void setUp() {
        vertx = Vertx.vertx();
        // 启动网关
    }

    @AfterEach
    public void tearDown() {
        vertx.close();
    }

    @Test
    public void testHttpProxy() {
        // 测试 HTTP 代理
    }
}
```

## 性能优化建议

### 1. 对象池化

```java
// 复用对象
private final ObjectPool<Buffer> bufferPool = new ObjectPool<>(...);
```

### 2. 批量操作

```java
// 批量清理
windows.entrySet().removeIf(entry -> entry.getKey() < threshold);
```

### 3. 避免锁竞争

```java
// 使用无锁数据结构
ConcurrentHashMap<String, Route> routes = new ConcurrentHashMap<>();

// 使用原子类
AtomicInteger counter = new AtomicInteger(0);
```

### 4. 减少对象创建

```java
// 复用对象
private final DateTimeFormatter formatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
```

## 故障排查

### 编译错误

1. **Vert.x API 版本问题**
   - 检查 Vert.x 版本是否为 5.0.6
   - API 返回 Future 而非回调

2. **Jackson 依赖冲突**
   - 确保使用统一的 Jackson 版本

### 运行时错误

1. **配置文件找不到**
   - 检查文件路径
   - 检查工作目录

2. **端口占用**
   - 修改配置文件中的端口
   - 检查是否有其他进程占用

3. **OutOfMemoryError**
   - 增加 JVM 堆内存：`-Xmx2g`
   - 检查是否有内存泄漏

### 性能问题

1. **高延迟**
   - 检查后端服务响应时间
   - 检查健康检查配置
   - 启用详细日志分析

2. **低吞吐**
   - 检查限流配置
   - 检查线程池大小
   - 使用性能分析工具

## 相关文档

- [项目完整文档](PROJECT_DOCUMENTATION.md)
- [README](README.md)
- [Vert.x 官方文档](https://vertx.io/docs/)
- [Jackson YAML 文档](https://github.com/FasterXML/jackson-dataformat-yaml)

---

**文档版本**: 1.0.0
**最后更新**: 2025-01-08
**维护者**: Nacos Gateway Team
