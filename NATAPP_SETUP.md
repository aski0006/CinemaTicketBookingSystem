# Natapp 内网穿透配置指南

## 概述

本指南将帮助您配置 natapp 内网穿透，以便支付宝回调能够正确访问您的本地开发环境。

## 步骤

### 1. 注册 Natapp 账号

1. 访问 [natapp.cn](https://natapp.cn)
2. 注册账号并登录
3. 购买隧道或使用免费隧道

### 2. 下载 Natapp 客户端

1. 在 natapp 控制台下载对应系统的客户端
2. 解压到本地目录

### 3. 配置隧道

1. 在 natapp 控制台创建新隧道
2. 选择协议：HTTP
3. 本地端口：8080（与您的 Spring Boot 应用端口一致）
4. 记录下分配的域名，例如：`abc123.natapp1.cc`

### 4. 修改应用配置

编辑 `src/main/resources/application.properties` 文件，将以下配置中的域名替换为您的 natapp 域名：

```properties
# 支付回调配置 - 支持natapp内网穿透
# 请将下面的URL替换为您的natapp穿透地址
payment.callback.url=https://your-natapp-domain.natapp1.cc/api/payments/callback
payment.return.url=https://your-natapp-domain.natapp1.cc/return_url
```

例如，如果您的 natapp 域名是 `abc123.natapp1.cc`，则配置为：

```properties
payment.callback.url=https://abc123.natapp1.cc/api/payments/callback
payment.return.url=https://abc123.natapp1.cc/return_url
```

### 5. 启动 Natapp 客户端

1. 在 natapp 客户端目录下运行：
   ```bash
   # Windows
   natapp.exe -authtoken=your_auth_token
   
   # Linux/Mac
   ./natapp -authtoken=your_auth_token
   ```

2. 在控制台获取您的 authtoken，替换上述命令中的 `your_auth_token`

### 6. 启动应用

1. 启动您的 Spring Boot 应用
2. 确保应用运行在 8080 端口

### 7. 测试配置

1. 访问 `https://your-natapp-domain.natapp1.cc` 确认穿透成功
2. 测试会员购买功能，确认支付宝回调正常工作

## 注意事项

1. **HTTPS 支持**：natapp 免费版支持 HTTPS，建议使用 HTTPS 协议
2. **域名稳定性**：natapp 免费版的域名可能会变化，需要及时更新配置
3. **端口冲突**：确保本地 8080 端口没有被其他应用占用
4. **防火墙**：确保本地防火墙允许 8080 端口的访问

## 故障排除

### 问题1：natapp 连接失败
- 检查 authtoken 是否正确
- 确认网络连接正常
- 查看 natapp 客户端日志

### 问题2：支付宝回调失败
- 确认回调 URL 配置正确
- 检查 Spring Boot 应用是否正常运行
- 查看应用日志确认回调接口是否被调用

### 问题3：域名无法访问
- 确认 natapp 客户端正常运行
- 检查本地应用端口是否正确
- 尝试重启 natapp 客户端

## 生产环境建议

在生产环境中，建议：

1. 使用固定的域名和 IP 地址
2. 配置 SSL 证书
3. 使用专业的反向代理服务
4. 配置监控和告警

## 相关文件

- `src/main/resources/application.properties` - 应用配置文件
- `src/main/java/com/asaki0019/cinematicketbookingsystem/configs/PaymentConfig.java` - 支付配置类
- `src/main/java/com/asaki0019/cinematicketbookingsystem/services/MembershipOrderServiceImpl.java` - 会员订单服务
- `src/main/java/com/asaki0019/cinematicketbookingsystem/services/OrderServiceImpl.java` - 订单服务 