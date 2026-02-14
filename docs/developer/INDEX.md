# 设计文档索引

本目录包含 Nacos Gateway 项目的设计文档，面向项目开发者和 AI Agent。

## 目录定位

面向**项目开发者**和 **AI Agent**，提供架构设计、模块实现和开发规范。

## 基本信息

| 文档类型 | 数量 | 说明 |
|----------|------|------|
| 核心设计 | 2 | 整体架构、AI Agent 指南 |
| 模块设计 | 6 | 配置、路由、负载均衡、健康检查、限流、代理 |
| 开发指南 | 3 | AI Agent 指南、代码规范、测试指南 |

## 文档索引

### 核心设计

| 文档 | 说明 |
|------|------|
| [architecture.md](architecture.md) | 项目整体架构设计 |

### 模块设计

| 模块 | 说明 |
|------|------|
| [modules/config.md](modules/config.md) | 配置管理模块 |
| [modules/route.md](modules/route.md) | 路由匹配模块 |
| [modules/loadbalance.md](modules/loadbalance.md) | 负载均衡模块 |
| [modules/health.md](modules/health.md) | 健康检查模块 |
| [modules/ratelimit.md](modules/ratelimit.md) | 限流模块 |
| [modules/proxy.md](modules/proxy.md) | 代理处理模块 |

### 开发指南

| 文档 | 说明 |
|------|------|
| [ai-agent-guide.md](ai-agent-guide.md) | AI Agent 编程指南 |
| [coding-guide.md](coding-guide.md) | 代码规范 |
| [testing-guide.md](testing-guide.md) | 测试指南 |

## 设计变更时文档更新流程

当修改设计时，按以下步骤更新文档：

1. **更新模块设计文档** (`modules/*.md`)
   - 修改对应的模块设计文档
   - 更新架构图和流程图
   - 更新 API 参考

2. **创建/更新 ADR** (`../decisions/`)
   - 对于重要架构决策，创建新的 ADR
   - 使用 ADR 模板记录背景、决策、后果

3. **更新用户手册** (`../user/`)
   - 如果变更影响用户使用，更新配置指南或 API 参考
   - 添加迁移指南（如需要）

4. **更新 CLAUDE.md**
   - 如果变更影响项目特性列表，更新概述部分

5. **同步目录索引**
   - 更新本目录的 INDEX.md
   - 确保索引与实际文件一致

## 文档维护注意事项

- 模块设计文档应与代码实现保持一致
- 代码结构变更时及时更新对应文档
- AI Agent 指南应包含最新的项目元数据
