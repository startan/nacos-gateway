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
配置文件路径默认为classpath下的 `nacos-gateway.yaml`或`nacos-gateway.yml`，您可以在启动时通过命令行参数`-c path/to/config.yaml`指定自定义配置文件位置。

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
  # 日志配置
  logging:
    level: INFO                      # 日志级别: TRACE, DEBUG, INFO, WARN, ERROR
    verbose: false                   # 是否记录详细的请求/响应信息

# 路由规则（仅在 router 模式下使用）
routes:
  - host: "group1.nacos.io"          # 请求域名，星号为通配符
    path: "/**"                      # 请求路径，单个星号为对单端路径的通配符，双星号为全匹配
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
```

## 系统要求

- Java 17+
- Maven 3.6+

## 依赖

- Vert.x 5.0.6
- Jackson
- Logback
- SLF4J
