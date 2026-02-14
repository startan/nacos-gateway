# 监控与故障排查

## 1. 健康检查

### 1.1 网关健康检查

```bash
curl http://localhost:18080/health
```

返回示例：
```json
{
  "status": "UP"
}
```

### 1.2 后端健康状态

后端端点健康状态由健康检查模块自动管理：
- 健康的端点参与负载均衡
- 不健康的端点自动剔除
- 恢复健康的端点自动加回

## 2. 日志

### 2.1 日志位置

| 日志类型 | 默认位置 |
|----------|----------|
| 应用日志 | `logs/gateway.log` |
| 访问日志 | `logs/access.log`（需启用）|

### 2.2 启用访问日志

```yaml
accessLog:
  enabled: true
  format: pattern
  output:
    path: logs/access.log
  rotation:
    policy: daily
    maxHistory: 30
```

### 2.3 日志级别配置

编辑 `gateway-launcher/src/main/resources/logback.xml`：

```xml
<logger name="nextf.nacos.gateway" level="DEBUG"/>
```

## 3. 常见问题

### 3.1 启动失败

**症状**: 网关无法启动

**排查步骤**:
1. 检查 Java 版本：`java -version`（需要 17+）
2. 检查端口占用：`netstat -tlnp | grep -E '18848|19848|18080'`
3. 检查配置文件语法：`cat nacos-gateway.yaml`
4. 查看日志：`tail -f logs/gateway.log`

### 3.2 请求返回 429

**症状**: 客户端收到 HTTP 429 状态码

**原因**: 触发限流保护

**排查步骤**:
1. 检查限流配置
2. 查看 QPS 是否超过限制
3. 查看连接数是否超过限制
4. 考虑调整限流参数

### 3.3 后端不可用

**症状**: 请求返回 502 或 503

**排查步骤**:
1. 检查后端服务是否运行
2. 检查健康检查配置
3. 查看后端端点健康状态
4. 检查网络连通性

### 3.4 配置更新未生效

**症状**: 修改配置文件后行为未变化

**排查步骤**:
1. 确认使用的是 `file://` 或 `nacos://` 协议（classpath 不支持热更新）
2. 检查文件权限
3. 查看日志中的配置更新记录
4. 验证配置文件语法是否正确

### 3.5 连接数持续超出限制

**症状**: 收紧连接数限制后，新连接仍被拒绝

**原因**: 旧连接自然消亡需要时间

**解决方案**:
1. 查看日志中的警告信息
2. 监控当前连接数
3. 等待旧连接自然断开
4. 如需立即生效，可临时提高限制再逐步降低

## 4. 监控指标

### 4.1 关键指标

| 指标 | 说明 |
|------|------|
| 请求 QPS | 每秒请求数 |
| 活跃连接数 | 当前连接数 |
| 限流拒绝数 | 被限流拒绝的请求数 |
| 后端健康度 | 健康端点占比 |
| 请求延迟 | P50/P95/P99 延迟 |

### 4.2 日志查看

```bash
# 实时查看日志
tail -f logs/gateway.log

# 查看错误日志
grep ERROR logs/gateway.log

# 查看限流日志
grep "Rate limit" logs/gateway.log

# 查看配置更新日志
grep "Config.*updated" logs/gateway.log
```
