# 影院订票系统

一个基于 Spring Boot + Vue 3 的现代化影院订票系统，支持会员管理、在线购票、支付集成等功能。

## 功能特性

- 🎬 电影信息管理
- 🎫 在线购票系统
- 💳 支付宝支付集成
- 👥 会员管理系统
- 📊 订单统计
- 🔐 JWT 身份认证
- 📱 响应式设计

## 技术栈

### 后端
- Spring Boot 2.7+
- Spring Security
- Spring Data JPA
- MySQL 8.0
- Redis
- JWT
- 支付宝 SDK

### 前端
- Vue 3
- Vue Router
- Axios
- Element Plus
- Vite

## 快速开始

### 环境要求
- JDK 8+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+

### 后端启动

1. 克隆项目
```bash
git clone <repository-url>
cd CinemaTicketBookingSystem
```

2. 配置数据库
```bash
# 创建数据库
CREATE DATABASE cinema_ticket_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 修改配置
编辑 `src/main/resources/application.properties`：
```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/cinema_ticket_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
spring.datasource.username=your_username
spring.datasource.password=your_password

# Redis配置
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
```

4. 启动应用
```bash
./mvnw spring-boot:run
```

### 前端启动

1. 进入前端目录
```bash
cd frontend
```

2. 安装依赖
```bash
npm install
```

3. 启动开发服务器
```bash
npm run dev
```

## Natapp 内网穿透配置

为了支持支付宝回调，需要配置 natapp 内网穿透：

### 1. 注册 Natapp 账号
访问 [natapp.cn](https://natapp.cn) 注册账号并获取 authtoken。

### 2. 配置隧道
- 协议：HTTP
- 本地端口：8080
- 记录分配的域名

### 3. 修改应用配置
编辑 `src/main/resources/application.properties`：
```properties
# 支付回调配置 - 替换为您的 natapp 域名
payment.callback.url=https://your-natapp-domain.natapp1.cc/api/payments/callback
payment.return.url=https://your-natapp-domain.natapp1.cc/return_url
```

### 4. 启动 Natapp 客户端
```bash
# Windows
natapp.exe -authtoken=your_auth_token

# Linux/Mac
./natapp -authtoken=your_auth_token
```

### 5. 测试配置
访问 `https://your-natapp-domain.natapp1.cc/test-natapp.html` 测试穿透是否成功。

详细配置说明请参考 [NATAPP_SETUP.md](./NATAPP_SETUP.md)。

## API 文档

启动应用后访问：`http://localhost:8080/swagger-ui.html`

## 支付配置

### 支付宝沙箱环境
系统已配置支付宝沙箱环境，可直接用于测试：

- 网关地址：`https://openapi-sandbox.dl.alipaydev.com/gateway.do`
- 应用ID：`9021000149688252`
- 测试账号：请参考支付宝沙箱文档

### 生产环境配置
修改 `application.properties` 中的支付宝配置：
```properties
payment.alipay.gateway=https://openapi.alipay.com/gateway.do
payment.alipay.app-id=your_app_id
payment.alipay.private-key=your_private_key
payment.alipay.public-key=alipay_public_key
```

## 项目结构

```
CinemaTicketBookingSystem/
├── src/main/java/com/asaki0019/cinematicketbookingsystem/
│   ├── controller/          # 控制器层
│   ├── service/            # 服务层
│   ├── repository/         # 数据访问层
│   ├── entity/            # 实体类
│   ├── dto/               # 数据传输对象
│   ├── config/            # 配置类
│   └── utils/             # 工具类
├── frontend/              # Vue 3 前端
├── src/main/resources/    # 配置文件
└── docs/                  # 文档
```

## 开发指南

### 添加新功能
1. 创建实体类
2. 创建 Repository
3. 创建 Service
4. 创建 Controller
5. 添加前端页面

### 代码规范
- 使用 Java 8+ 特性
- 遵循 RESTful API 设计规范
- 使用 JWT 进行身份认证
- 添加适当的日志记录

## 部署

### Docker 部署
```bash
# 构建镜像
docker build -t cinema-ticket-system .

# 运行容器
docker run -p 8080:8080 cinema-ticket-system
```

### 生产环境注意事项
1. 修改数据库密码
2. 配置 SSL 证书
3. 使用生产环境的支付宝配置
4. 配置日志轮转
5. 设置监控告警

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## 联系方式

如有问题，请提交 Issue 或联系开发团队。 