# 部署指南

## 1. 系统服务部署

### 1.1 创建系统服务（systemd）

```ini
[Unit]
Description=Nacos Gateway
After=network.target

[Service]
Type=simple
User=nacos
WorkingDirectory=/opt/nacos-gateway
ExecStart=/usr/bin/java -jar /opt/nacos-gateway/nacos-gateway.jar -c /etc/nacos-gateway/config.yaml
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

安装服务：

```bash
# 复制服务文件
sudo cp nacos-gateway.service /etc/systemd/system/

# 重载配置
sudo systemctl daemon-reload

# 启用服务
sudo systemctl enable nacos-gateway

# 启动服务
sudo systemctl start nacos-gateway

# 查看状态
sudo systemctl status nacos-gateway
```

## 2. Docker 部署

### 2.1 Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre

# 安装依赖
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 创建应用目录
WORKDIR /opt/nacos-gateway

# 复制 jar 包
COPY gateway-launcher/target/nacos-gateway.jar app.jar

# 复制配置文件
COPY nacos-gateway.yaml config.yaml

# 暴露端口
EXPOSE 18848 19848 18080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:18080/health || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar", "-c", "config.yaml"]
```

### 2.2 构建镜像

```bash
docker build -t nacos-gateway:latest .
```

### 2.3 运行容器

```bash
docker run -d \
  --name nacos-gateway \
  -p 18848:18848 \
  -p 19848:19848 \
  -p 18080:18080 \
  -v $(pwd)/config.yaml:/opt/nacos-gateway/config.yaml \
  nacos-gateway:latest
```

## 3. Kubernetes 部署

### 3.1 ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: nacos-gateway-config
data:
  nacos-gateway.yaml: |
    server:
      ports:
        apiV1: 18848
        apiV2: 19848
        apiConsole: 18080
    backends:
      - name: backend-service
        endpoints:
          - host: backend-service.default.svc.cluster.local
            port: 8848
            priority: 10
```

### 3.2 Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nacos-gateway
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nacos-gateway
  template:
    metadata:
      labels:
        app: nacos-gateway
    spec:
      containers:
      - name: gateway
        image: nacos-gateway:latest
        ports:
        - containerPort: 18848
          name: api-v1
        - containerPort: 19848
          name: api-v2
        - containerPort: 18080
          name: console
        volumeMounts:
        - name: config
          mountPath: /opt/nacos-gateway/config.yaml
          subPath: nacos-gateway.yaml
        livenessProbe:
          httpGet:
            path: /health
            port: 18080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health
            port: 18080
          initialDelaySeconds: 10
          periodSeconds: 5
      volumes:
      - name: config
        configMap:
          name: nacos-gateway-config
```

### 3.3 Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: nacos-gateway
spec:
  selector:
    app: nacos-gateway
  ports:
  - name: api-v1
    port: 18848
    targetPort: 18848
  - name: api-v2
    port: 19848
    targetPort: 19848
  - name: console
    port: 18080
    targetPort: 18080
  type: LoadBalancer
```

## 4. 配置热更新

### 4.1 文件监听（本地文件）

配置文件修改后自动生效，无需重启。

### 4.2 Nacos 配置中心

```bash
# 启动时使用 Nacos 配置
java -jar nacos-gateway.jar -c "nacos://config.yaml?serverAddr=127.0.0.1:8848"
```

在 Nacos 控制台修改配置后，实时推送到网关。
