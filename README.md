# Nacos Gateway

> 基于 Java + Vert.x 的高性能 HTTP 反向代理网关，支持 HTTP/1、HTTP/2 和 gRPC 代理。

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net/)
[![Vert.x](https://img.shields.io/badge/Vert.x-5.0.6-purple.svg)](https://vertx.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

## 特性

- **多协议支持**: HTTP/1、HTTP/2、gRPC（完全透传，无需 proto 文件）
- **智能路由**: 基于 Host 和 Path 的通配符路由匹配
- **负载均衡**: 轮询、随机、最少连接三种策略
- **优先级分组**: 端点优先级机制，高优先级全挂才降级
- **健康检查**: HTTP/1 和 HTTP/2 探针，连续阈值机制
- **限流保护**: QPS + 并发连接数限流，全局+路由级
- **配置热更新**: 文件监听实现配置动态加载，无需重启
- **连接管理**: 每个客户端连接对应一个专属后端连接，客户端断开自动清理后端资源，防止连接泄漏

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+

### 构建项目

```bash
git clone <repository-url>
cd nacos-gateway-java
mvn clean install
```

### 运行

```bash
# 使用默认配置
java -jar gateway-launcher/target/gateway-launcher-1.0.0.jar

# 使用自定义配置
java -jar gateway-launcher/target/gateway-launcher-1.0.0.jar /path/to/config.yaml
```

### 配置示例

```yaml
server:
  port: 9848

routes:
  - host: "*.nacos.io"
    path: "/**"
    backend: example-service

backends:
  - name: example-service
    loadBalance: round-robin
    probe:
      path: /health
      periodSeconds: 10
      timeoutSeconds: 1
      successThreshold: 1
      failureThreshold: 3
    endpoints:
      - host: localhost
        port: 8080
        priority: 1
```

## 配置指南

### 路由匹配

- `*.example.com` - 通配符子域
- `/api/*` - 单级路径通配
- `/api/**` - 多级路径全匹配

### 负载均衡策略

- `round-robin` - 轮询
- `random` - 随机
- `least-connection` - 最少连接

### 优先级

端点按 `priority` 分组，数字越小优先级越高。高优先级组全部不可用时才降级到低优先级。

### 限流

- 全局默认限制
- 路由级可覆盖配置
- 先检查全局，再检查路由级
- 超限返回 HTTP 429

## 健康检查

```bash
curl http://localhost:9848/health
```

响应：
```json
{
  "status": "UP",
  "timestamp": 1704662400000
}
```

## 性能

| 指标 | 值 |
|------|-----|
| 吞吐量 | > 10000 RPS |
| 延迟 (P99) | < 10ms |
| 并发连接 | > 10000 |

## 项目结构

```
nacos-gateway-java/
├── gateway-api/          # 公共 API 模块
├── gateway-core/         # 核心实现模块
└── gateway-launcher/     # 启动器模块
```

## 文档

详细文档请查看 [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md)，包含：

- 完整需求文档
- 详细架构设计
- 模块设计说明
- API 参考
- 配置指南
- 开发指南
- 部署指南
- 测试指南

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vert.x | 5.0.6 | 异步事件驱动框架 |
| Jackson | 2.16.0 | YAML 解析 |
| Logback | 1.4.11 | 日志框架 |
| SLF4J | 2.0.9 | 日志门面 |

## 许可证

Apache License 2.0

## 贡献

欢迎贡献！请查看 [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md) 了解详情。
