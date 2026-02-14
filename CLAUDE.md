# Nacos Gateway

基于 Java + Vert.x 构建的高性能 HTTP 反向代理，支持 HTTP/1 和 HTTP/2，支持基于 HTTP/2 的 gRPC 流通信代理（不解析 gRPC 报文内容无需 proto 文件），支持连接级的负载均衡。

## 特性

- **流通信支持**: 完整支持 gRPC 的各种通信模式（Unary RPC、Client-side streaming RPC、Server-side streaming RPC 和 Bidirectional streaming RPC）
- **负载均衡**: 支持多种负载均衡策略（轮询 Round Robin、随机 Random、最少连接 Least Connection）
- **灵活路由**: 基于 YAML 配置的路由策略
- **动态配置**: 配置发生变化，实现动态加载更新无需重启服务
- **动态路由**: 配置重新加载如路由信息发生变化，自动断开不符合新路由策略的连接
- **路由转发**: 基于路由匹配将请求转发至不同的后端服务
- **健康检查**: 支持 HTTP/1 和 HTTP/2 请求后端实现健康检查，响应 status 为 2xx 即为健康
- **连接管理**: 采用连接级别资源管理架构，每个客户端连接对应一个专属的 ProxyConnection，内部管理专属 HttpClient、Endpoint 和 Backend。客户端断开时自动清理后端资源，有效防止连接泄漏

## 快速链接

- [用户手册](docs/user/) - 安装、配置、部署指南
- [设计文档](docs/developer/) - 架构设计、模块实现
- [决策记录](docs/decisions/) - 架构决策记录 (ADR)
- [文档索引](docs/INDEX.md) - 完整文档目录

## 架构概览

```
客户端(nacos-client) -> 网关(nacos-gateway) -> 后端服务(nacos-server)
```

网关工作流程:
1. 接收传入的 HTTP 调用
2. 根据配置路由到适当的后端
3. 在多个后端端点之间进行负载均衡
4. 为流式通信维护持久连接
5. 返回响应给客户端

详细架构设计请参阅 [架构设计文档](docs/developer/architecture.md)。

## 配置概览

配置文件默认从当前目录查找 `nacos-gateway.yaml` 或 `nacos-gateway.yml`。

### 配置来源

| 协议 | 说明 | 热更新 |
|------|------|--------|
| `file://` | 本地文件系统（默认） | ✅ |
| `classpath://` | 应用类路径 | ❌ |
| `nacos://` | Nacos 配置中心 | ✅ |

### 启动示例

```bash
# 使用默认配置
java -jar nacos-gateway.jar

# 指定配置文件
java -jar nacos-gateway.jar -c /path/to/config.yaml

# 使用 Nacos 配置中心
java -jar nacos-gateway.jar -c "nacos://config.yaml?serverAddr=127.0.0.1:8848"
```

详细配置说明请参阅 [配置指南](docs/user/configuration.md)。

## 系统要求

- Java 17+
- Maven 3.6+

## 技术栈

- Vert.x 5.0.6
- Jackson
- Logback
- SLF4J
- Nacos Client 2.3.2（配置中心集成）

---

## 文档维护规范

### 文档结构

本项目文档按受众和用途分为三类：

1. **用户手册** ([`docs/user/`](docs/user/)) - 面向网关用户/运维人员
2. **设计文档** ([`docs/developer/`](docs/developer/)) - 面向项目开发者/AI Agent
3. **决策记录** ([`docs/decisions/`](docs/decisions/)) - 面向项目历史追溯

#### 目录索引文件

各目录使用 `INDEX.md` 作为索引文件：

- `docs/INDEX.md` - 总索引
- `docs/user/INDEX.md` - 用户手册索引
- `docs/developer/INDEX.md` - 设计文档索引
- `docs/decisions/INDEX.md` - 决策记录索引
- `docs/progress/INDEX.md` - 实施记录索引

### 设计变更时文档更新流程

当修改设计时，按以下步骤更新文档：

1. **更新设计文档** ([`docs/developer/`](docs/developer/))
   - 修改对应的模块设计文档 ([`docs/developer/modules/`](docs/developer/modules/))
   - 更新架构图和流程图
   - 更新 API 参考

2. **创建/更新 ADR** ([`docs/decisions/`](docs/decisions/))
   - 对于重要架构决策，创建新的 ADR
   - 使用 [ADR 模板](docs/decisions/template.md) 记录背景、决策、后果
   - 更新 [ADR 索引](docs/decisions/INDEX.md)

3. **更新用户手册** ([`docs/user/`](docs/user/))
   - 如果变更影响用户使用，更新配置指南或 API 参考
   - 添加迁移指南（如需要）

4. **更新 CLAUDE.md**
   - 如果变更影响项目特性列表，更新概述部分

5. **同步目录索引**
   - 更新各目录的 INDEX.md
   - 确保索引与实际文件一致

### 文档命名约定

- 用户手册：`kebab-case.md`（小写-连字符）
- 设计文档：`kebab-case.md`
- ADR：`NNN-title.md`（NNN 为序号）
- 进度记录：`kebab-case.md`
