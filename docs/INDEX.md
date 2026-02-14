# Nacos Gateway 文档索引

本目录是 Nacos Gateway 项目的文档中心。

## 目录定位

本目录作为项目文档的统一入口，面向所有文档受众：
- **用户/运维人员** → 查看 [用户手册](user/)
- **开发者/AI Agent** → 查看 [设计文档](developer/)
- **历史追溯** → 查看 [决策记录](decisions/)
- **进度跟踪** → 查看 [实施记录](progress/)

## 基本信息

| 类型 | 目录 | 文档数量 | 说明 |
|------|------|----------|------|
| 用户手册 | [user/](user/) | 5 | 安装、配置、部署、监控指南 |
| 设计文档 | [developer/](developer/) | 7 | 架构设计、模块实现、开发规范 |
| 决策记录 | [decisions/](decisions/) | 3 | 架构决策记录 (ADR) |
| 实施记录 | [progress/](progress/) | 2 | 测试进度、变更历史 |
| 归档文档 | [archive/](archive/) | 4 | 旧版本文档，保留参考 |

## 快速导航

| 文档类型 | 面向对象 | 目录 |
|----------|----------|------|
| **用户手册** | 网关用户/运维人员 | [user/](user/) |
| **设计文档** | 项目开发者/AI Agent | [developer/](developer/) |
| **决策记录** | 项目历史追溯 | [decisions/](decisions/) |
| **实施记录** | 项目进度跟踪 | [progress/](progress/) |

## 用户手册

面向网关用户和运维人员，涵盖安装、配置、部署和运维。

| 文档 | 说明 |
|------|------|
| [安装指南](user/installation.md) | 环境要求、构建、运行 |
| [配置指南](user/configuration.md) | 配置结构、限流、负载均衡 |
| [部署指南](user/deployment.md) | systemd、Docker、Kubernetes 部署 |
| [监控与故障排查](user/monitoring.md) | 健康检查、日志、常见问题 |
| [API 参考](user/api-reference.md) | 管理接口、配置协议 |

## 设计文档

面向项目开发者和 AI Agent，涵盖架构设计和模块实现。

### 核心设计

| 文档 | 说明 |
|------|------|
| [架构设计](developer/architecture.md) | 整体架构、数据流、设计原则 |
| [AI Agent 指南](developer/ai-agent-guide.md) | 项目元数据、类依赖关系、代码规范 |

### 模块设计

| 模块 | 说明 |
|------|------|
| [配置模块](developer/modules/config.md) | 配置加载、验证、热更新 |
| [路由模块](developer/modules/route.md) | Host 匹配、路由表 |
| [负载均衡模块](developer/modules/loadbalance.md) | 负载均衡策略、优先级 |
| [健康检查模块](developer/modules/health.md) | HTTP/TCP 探测、阈值机制 |
| [限流模块](developer/modules/ratelimit.md) | QPS/连接数限流、级联配置 |
| [代理模块](developer/modules/proxy.md) | HTTP/1、HTTP/2、gRPC 代理 |

## 决策记录 (ADR)

架构决策记录，记录重要的架构决策及其背景和后果。

| 编号 | 标题 | 状态 |
|------|------|------|
| 001 | 限流配置热加载 | 已接受 |
| 002 | 多协议配置源支持 | 已接受 |
| 003 | 连接级资源管理 | 已接受 |

## 实施记录

项目实施进度和变更历史。

| 文档 | 说明 |
|------|------|
| [测试进度](progress/test-progress.md) | 单元测试进度总结 |
| [变更历史](progress/changelog.md) | 项目变更历史 |

## 归档文档

[archive/](archive/) 目录包含旧版本文档，保留作为历史参考。

## 文档维护

### 文档结构规范

本项目文档使用 **INDEX.md** 作为各目录的索引文件：

- `docs/INDEX.md` - 总索引（本文件）
- `docs/user/INDEX.md` - 用户手册索引
- `docs/developer/INDEX.md` - 设计文档索引
- `docs/decisions/INDEX.md` - 决策记录索引
- `docs/progress/INDEX.md` - 实施记录索引

### 文档命名约定

- **索引文件**: `INDEX.md`（各目录入口）
- **用户手册**: `kebab-case.md`（小写-连字符）
- **设计文档**: `kebab-case.md`
- **ADR**: `NNN-title.md`（NNN 为序号）
- **进度记录**: `kebab-case.md`
