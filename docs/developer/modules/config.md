# 配置模块设计

## 1. 概述

配置模块负责网关配置的加载、验证和热更新。支持三种配置来源：本地文件系统、应用类路径、Nacos 配置中心。

## 2. 配置模型

### 2.1 配置类层次

```
GatewayConfig
    ├── ServerConfig
    │   ├── PortsConfig
    │   │   ├── apiV1 (int)
    │   │   ├── apiV2 (int)
    │   │   └── apiConsole (int)
    │   └── RateLimitConfig
    ├── List<RouteConfig>
    ├── List<BackendConfig>
    ├── TimeoutConfig
    ├── AccessLogConfig
    └── ManagementConfig
```

### 2.2 RouteConfig

```
RouteConfig
    ├── host (String)              # Host 匹配模式
    ├── backend (String)           # 后端服务名称
    └── rateLimit (RateLimitConfig)
```

### 2.3 BackendConfig

```
BackendConfig
    ├── name (String)
    ├── loadBalance (String)      # round-robin/random/least-connection
    ├── ports (BackendPortsConfig)
    │   ├── apiV1 (int)
    │   ├── apiV2 (int)
    │   └── apiConsole (int)
    ├── probe (HealthProbeConfig)
    ├── rateLimit (RateLimitConfig)
    └── endpoints (List<EndpointConfig>)
```

### 2.4 RateLimitConfig

```
RateLimitConfig
    ├── maxQps (int = -1)              # -1: 无限制, 0: 拒绝所有, >0: 正常限流
    ├── maxConnections (int = -1)
    ├── maxQpsPerClient (int = -1)
    └── maxConnectionsPerClient (int = -1)
```

### 2.5 AccessLogConfig

```
AccessLogConfig
    ├── enabled (boolean = false)
    ├── format (String = "pattern")    # pattern or json
    ├── pattern (String)               # 日志格式模式
    ├── output (AccessLogOutputConfig)
    ├── rotation (AccessLogRotationConfig)
    └── async (AccessLogAsyncConfig)

AccessLogOutputConfig
    ├── path (String = "logs/access.log")
    └── encoding (String = "UTF-8")

AccessLogRotationConfig
    ├── policy (String = "daily")      # daily, size, both
    ├── maxFileSize (String = "100MB")
    ├── maxHistory (int = 30)
    └── fileNamePattern (String = "access.%d{yyyy-MM-dd}.log")

AccessLogAsyncConfig
    ├── enabled (boolean = true)
    ├── queueSize (int = 512)
    ├── discardingThreshold (int = 20)
    └── neverBlock (boolean = true)
```

## 3. 配置读取器

### 3.1 接口定义

```
ConfigFileReader (interface)
    ├── readConfig(): String
    ├── watchConfig(Runnable callback)
    ├── stopWatching()
    └── getSourceDescription(): String
```

### 3.2 实现类

| 实现类 | 协议 | 说明 |
|--------|------|------|
| **FileConfigReader** | `file://` | 从本地文件系统读取，支持轮询热更新 |
| **ClasspathConfigReader** | `classpath://` | 从类路径读取，不支持热更新 |
| **NacosConfigReader** | `nacos://` | 从 Nacos 配置中心读取，支持实时推送 |

### 3.3 协议识别

配置路径通过协议前缀自动识别：
- `file://` 或无前缀 → FileConfigReader（默认）
- `classpath://` → ClasspathConfigReader
- `nacos://<dataId>?<params>` → NacosConfigReader

## 4. 配置变量解析

### 4.1 变量语法

配置支持变量替换，语法如下：

| 语法 | 说明 |
|------|------|
| `${env:VAR_NAME}` | 环境变量 |
| `${sys:property.name}` | 系统属性 |
| `${VAR_NAME}` | 先尝试环境变量，再尝试系统属性 |
| `${VAR_NAME:-default}` | 带默认值的变量 |

### 4.2 递归解析

变量支持递归解析（变量中包含变量），最大递归深度为 10 层。

## 5. 端口类型

### 5.1 PortType 枚举

```
PortType
    ├── API_V1 ("apiV1", "Nacos V1 API")
    ├── API_V2 ("apiV2", "Nacos V2 gRPC API")
    └── API_CONSOLE ("apiConsole", "Console API")
```

## 6. 配置验证

### 6.1 验证规则

| 规则 | 说明 |
|------|------|
| 端口范围 | 1-65535 |
| 后端引用 | 路由引用的后端必须存在 |
| 负载均衡策略 | 必须是 round-robin/random/least-connection 之一 |

### 6.2 验证失败处理

配置验证失败时：
- 记录错误日志
- 保持旧配置继续运行
- 不应用新配置

## 7. 配置热更新

### 7.1 更新流程

```
配置变更检测
    ├─ 文件系统：文件修改时间变化
    ├─ Nacos：Listener.receiveConfigInfo() 回调
    └─ 类路径：不支持

    → 触发回调
      → ConfigReloader.reload()
        → 读取新配置
        → 解析变量
        → 验证配置
        → 更新各组件
```

### 7.2 组件更新策略

| 组件 | 更新策略 |
|------|----------|
| RouteMatcher | Copy-on-Write 替换 |
| BackendRegistry | 原子引用替换 |
| RateLimitManager | 保留连接数，重建限流器 |
| HealthCheckManager | 重新启动健康检查任务 |
| AccessLogger | 重新配置日志记录器 |

## 8. Nacos 配置

### 8.1 URL 格式

```
nacos://<dataId>[:<group>]?<query-parameters>
```

- `dataId`：必需，配置文件标识符
- `group`：可选，用 `:` 分隔，默认 `DEFAULT_GROUP`
- `query-parameters`：所有参数透传给 Nacos Client SDK

### 8.2 支持的参数

所有查询参数直接透传给 Nacos Client SDK，支持的参数取决于 Nacos 版本。

| 参数 | 说明 |
|------|------|
| serverAddr | Nacos 服务器地址（如 `127.0.0.1:8848`） |
| namespace | 命名空间 ID |
| username | 认证用户名 |
| password | 认证密码 |
| accessKey | 阿里云 AK 认证 |
| secretKey | 阿里云 SK 认证 |
