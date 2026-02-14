# 代理模块设计

## 1. 概述

代理模块负责将客户端请求转发到后端服务。支持 HTTP/1、HTTP/2（透传）和 gRPC 三种协议类型。

## 2. 核心组件

### 2.1 类图

```
ProxyHandler (interface)
    ├── handle(request): void
            │
            ├── HttpProxyHandler
            │       ├── httpClient (HttpClient)
            │       ├── host (String)
            │       ├── port (int)
            │       ├── accessLogger (AccessLogger)
            │       ├── backend (Backend)
            │       └── endpoint (Endpoint)
            │
            └── GrpcProxyHandler
                    ├── httpClient (HttpClient)
                    ├── host (String)
                    ├── port (int)
                    ├── accessLogger (AccessLogger)
                    ├── backend (Backend)
                    └── endpoint (Endpoint)

ConnectionManager
    ├── Map<ClientConnection, ProxyConnection> connections
    ├── register(clientConn, proxyConn)
    ├── unregister(clientConn)
    └── getProxyConnection(clientConn): ProxyConnection

ProxyConnection
    ├── clientConnection (HttpConnection)
    ├── httpClient (HttpClient)
    ├── backend (Backend)
    ├── endpoint (Endpoint)
    ├── portType (PortType)
    ├── clientIp (String)
    ├── createTime (long)
    ├── route (Route)
    ├── getBackendPort(): int
    ├── getDuration(): long
    └── close(): void
```

## 3. 协议处理

### 3.1 HTTP/1 和 HTTP/2 代理

`HttpProxyHandler` 同时处理 HTTP/1 和 HTTP/2 请求：

- **请求方法**: 所有 HTTP 方法
- **请求头**: 过滤 hop-by-hop 头后转发
- **请求体**: 流式转发，支持背压处理
- **响应**: 流式转发响应体

### 3.2 gRPC 代理

`GrpcProxyHandler` 专门处理 gRPC 请求，用于 Nacos API V2 (gRPC over HTTP/2)：

- **检测**: `Content-Type: application/grpc`
- **传输**: 基于 HTTP/2 完全透传
- **模式**: 不解析 proto，支持所有 gRPC 通信模式
  - Unary RPC
  - Server-side streaming RPC
  - Client-side streaming RPC
  - Bidirectional streaming RPC

## 4. 代理流程

### 4.1 请求转发流程

```
客户端请求到达
    ↓
创建 ProxyConnection
    ├─ 绑定客户端连接
    ├─ 记录后端和端点信息
    └─ 注册到 ConnectionManager
    ↓
检测请求协议类型
    ├─ Content-Type: application/grpc → GrpcProxyHandler
    └─ 其他（HTTP/1 和 HTTP/2）→ HttpProxyHandler
    ↓
建立后端连接
    ├─ 负载均衡选择端点
    ├─ 创建 HttpClientRequest
    └─ 连接到后端
    ↓
转发请求头
    └─ 过滤 hop-by-hop 头
    ↓
转发请求体
    ├─ Pump 流式转发
    └─ 处理背压
    ↓
接收后端响应
    ├─ 转发响应头
    └─ 流式转发响应体
    ↓
请求完成
    ├─ 关闭后端连接
    ├─ 释放限流配额
    ├─ 通知负载均衡器
    └─ 从 ConnectionManager 移除
```

### 4.2 连接管理

每个客户端连接对应一个独立的 ProxyConnection：
- 独立的后端 HttpClient
- 独立的端点选择
- 独立的限流计数

客户端断开时自动清理：
- 关闭后端连接
- 释放限流配额
- 更新负载均衡器计数

## 5. 头部处理

### 5.1 过滤的请求头（hop-by-hop）

```
Connection
Keep-Alive
Proxy-Authenticate
Proxy-Authorization
Te
Trailers
Transfer-Encoding
Upgrade
```

### 5.2 过滤的响应头

```
Connection
Keep-Alive
Transfer-Encoding
Upgrade
```

### 5.3 gRPC 特殊处理

在 `GrpcProxyHandler` 中，`te` 和 `trailers` 头仍然被过滤（与其他 hop-by-hop 头一起），trailer 通过响应的 `putTrailer()` 方法单独传递。

## 6. 协议检测

协议检测基于端口类型（PortType）进行：

```java
switch (portType) {
    case API_V1, API_CONSOLE -> {
        // 使用 HttpProxyHandler
    }
    case API_V2 -> {
        // 使用 GrpcProxyHandler
    }
}
```

| 端口类型 | 代理处理器 | 说明 |
|---------|-----------|------|
| API_V1 | HttpProxyHandler | Nacos V1 API |
| API_CONSOLE | HttpProxyHandler | 控制台 API |
| API_V2 | GrpcProxyHandler | Nacos V2 gRPC API |

注意：虽然 `GrpcProxyHandler` 中保留了基于 Content-Type 的 `isGrpcRequest()` 方法，但实际代理处理流程由端口类型决定。
