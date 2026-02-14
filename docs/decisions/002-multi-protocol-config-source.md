# ADR 002: 多协议配置源支持

## 状态
已接受

## 背景

网关最初仅支持从本地文件系统读取配置。随着部署环境的多样化，需要支持更多配置来源：

1. **容器化部署**：配置文件可能打包在 JAR 中，需要从类路径读取
2. **配置中心**：企业环境使用 Nacos 配置中心集中管理配置
3. **动态更新**：期望配置变更后能够实时生效，无需重启

## 决策

实现基于协议前缀的配置源自动识别机制，支持 file://、classpath://、nacos:// 三种协议。

### 协议设计

| 协议 | 格式 | 热更新 | 说明 |
|------|------|--------|------|
| file:// | `file:///path/to/config.yaml` | ✅ | 本地文件系统（默认） |
| classpath:// | `classpath://config.yaml` | ❌ | 应用类路径资源 |
| nacos:// | `nacos://dataId[:group]?params` | ✅ | Nacos 配置中心 |

### Nacos URL 格式

```
nacos://<dataId>[:<group>]?<query-parameters>
```

- `dataId`：必需，配置文件标识符
- `group`：可选，用 `:` 分隔，默认 `DEFAULT_GROUP`
- `query-parameters`：所有参数直接透传给 Nacos Client SDK

### 架构设计

```
ConfigFileReader (interface)
    ├── readConfig(): String
    ├── watchConfig(Runnable callback)
    ├── stopWatching()
    └── getSourceDescription(): String
            │
            ├── FileConfigReader
            │       ├── Vert.x 定时器轮询文件变更
            │       └── 支持热更新
            │
            ├── ClasspathConfigReader
            │       ├── getResourceAsStream() 读取
            │       └── 不支持热更新（资源不可变）
            │
            └── NacosConfigReader
                    ├── Nacos Client SDK
                    ├── gRPC 长连接实时推送
                    └── 支持热更新
```

### 向后兼容

- 未指定协议前缀时，默认使用 `file://`
- 原有配置路径无需修改即可继续工作

## 后果

### 正面影响

- **部署灵活**：支持多种部署场景（本地、容器、配置中心）
- **集中管理**：通过 Nacos 配置中心实现配置的统一管理
- **实时生效**：Nacos 协议支持配置实时推送更新

### 负面影响

- **类路径限制**：classpath:// 协议不支持热更新，可能造成困惑
- **依赖增加**：nacos:// 协议引入 Nacos Client 依赖

### 风险

- Nacos 配置中心故障时，网关启动可能失败
- 配置更新频率过高可能影响网关性能

## 替代方案

### 方案 A：单一配置源
- **优点**：实现简单，无需协议识别
- **缺点**：无法适应多种部署场景

### 方案 B：启动参数区分
- **优点**：协议明确
- **缺点**：需要多个启动参数，使用复杂

## 相关文档

- [配置模块设计](../developer/modules/config.md)
- [NacosUrlParser.java](../../gateway-core/src/main/java/nextf/nacos/gateway/config/reader/NacosUrlParser.java)
- [NacosConfigReader.java](../../gateway-core/src/main/java/nextf/nacos/gateway/config/reader/NacosConfigReader.java)
