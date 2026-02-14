# 安装指南

## 1. 系统要求

| 组件 | 版本要求 |
|------|----------|
| Java | 17+ |
| Maven | 3.6+ |

## 2. 构建项目

```bash
# 克隆仓库
git clone <repository-url>
cd nacos-gateway

# 构建
mvn clean package

# 输出
gateway-launcher/target/nacos-gateway.jar
```

## 3. 运行

### 3.1 使用默认配置

配置文件默认从当前目录查找 `nacos-gateway.yaml` 或 `nacos-gateway.yml`。

```bash
java -jar gateway-launcher/target/nacos-gateway.jar
```

### 3.2 指定配置文件

```bash
# 使用 -c 参数指定配置文件
java -jar nacos-gateway.jar -c /path/to/config.yaml
```

## 4. 配置文件来源

支持三种配置来源：

### 4.1 本地文件系统（默认）

```bash
# 默认行为
java -jar nacos-gateway.jar

# 明确指定 file:// 协议
java -jar nacos-gateway.jar -c file:///etc/nacos-gateway/config.yaml
```

### 4.2 类路径

```bash
java -jar nacos-gateway.jar -c classpath://nacos-gateway.yaml
```

### 4.3 Nacos 配置中心

```bash
# 基础配置（使用默认 group）
java -jar nacos-gateway.jar -c "nacos://nacos-gateway.yaml?serverAddr=127.0.0.1:8848"

# AK/SK 认证
java -jar nacos-gateway.jar -c "nacos://nacos-gateway.yaml:gateway-group?namespace=dev&serverAddr=127.0.0.1:8848&accessKey=yourKey&secretKey=yourSecret"

# 用户名/密码认证
java -jar nacos-gateway.jar -c "nacos://nacos-gateway.yaml:prod?namespace=prod&serverAddr=192.168.1.100:8848,192.168.1.101:8848&username=nacos&password=nacos"
```

## 5. 验证安装

### 5.1 健康检查端点

```bash
curl http://localhost:18080/health
```

返回示例：
```json
{
  "status": "UP"
}
```

### 5.2 测试代理

```bash
# 假设配置了后端服务
curl http://localhost:18848/nacos/v1/ns/instance/list
```
