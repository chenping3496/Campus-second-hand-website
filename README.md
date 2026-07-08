# 校园二手交易平台 (Campus Second-Hand Trading Platform)

一个专为大学生打造的校园二手物品交易平台，支持商品发布、浏览、搜索、交易、即时通讯、身份认证、投诉反馈等完整功能。

---

## 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速启动](#快速启动)
- [功能列表](#功能列表)
- [用户角色与权限](#用户角色与权限)
- [API 接口概览](#api-接口概览)
- [数据库表结构](#数据库表结构)
- [部署配置](#部署配置)
- [测试账号](#测试账号)

---

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端框架** | Spring Boot 3.5 + Maven |
| **ORM** | MyBatis-Plus 3.5.9 (LambdaQueryWrapper + 分页插件 + 逻辑删除 + 自动填充 + 枚举映射) |
| **数据库** | MySQL 8.0 |
| **缓存** | Redis (Lettuce) |
| **消息队列** | RabbitMQ |
| **认证** | JWT (JSON Web Token) |
| **加密** | BCrypt |
| **实时通讯** | WebSocket |
| **API文档** | SpringDoc OpenAPI (Swagger) |
| **前端框架** | Vue 3 (Composition API) |
| **构建工具** | Vite |
| **状态管理** | Pinia |
| **路由** | Vue Router 4 |
| **HTTP客户端** | Axios |

---

## 项目结构

```
ershou/
├── README.md
├── backend/                              # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/backend/
│       │   ├── BackendApplication.java   # 启动类
│       │   ├── config/                   # 配置类 (8个)
│       │   │   ├── DataInitializer.java  # 初始数据填充
│       │   │   ├── PasswordConfig.java   # BCrypt 密码加密
│       │   │   ├── RabbitMQConfig.java   # 消息队列配置
│       │   │   ├── RedisConfig.java      # Redis 缓存配置
│       │   │   ├── WebConfig.java        # CORS + 拦截器
│       │   │   ├── WebSocketConfig.java  # 实时通讯
│       │   │   ├── MyBatisPlusConfig.java
│       │   │   ├── MyMetaObjectHandler.java
│       │   │   └── OpenAPIConfig.java    # Swagger
│       │   ├── consumer/                 # RabbitMQ 消费者 (4个)
│       │   ├── controller/               # REST 控制器 (13个)
│       │   ├── dto/                      # 数据传输对象 (16个)
│       │   ├── entity/                   # 数据库实体 (10个)
│       │   ├── exception/                # 全局异常处理
│       │   ├── interceptor/              # JWT 认证拦截器
│       │   ├── mapper/                   # MyBatis Mapper (10个)
│       │   ├── service/                  # 业务逻辑层 (11个)
│       │   ├── util/                     # JWT 工具类
│       │   └── websocket/                # WebSocket 处理器
│       └── resources/
│           ├── application.yml           # 应用配置
│           ├── schema.sql                # 建库 SQL
│           ├── lua/                      # Redis Lua 脚本 (4个)
│           └── static/images/            # 图片资源
└── front/                                # Vue 3 前端
    ├── index.html
    ├── package.json
    ├── vite.config.js                    # Vite 构建配置 + API 代理
    └── src/
        ├── main.js                       # 入口
        ├── App.vue                       # 根组件
        ├── api/                          # API 请求层 (13个模块)
        │   ├── request.js                # Axios 实例 (JWT 拦截器)
        │   ├── admin.js                  # 管理员接口
        │   ├── auth.js                   # 认证接口
        │   ├── category.js               # 分类接口
        │   ├── chat.js                   # 聊天接口
        │   ├── complaint.js              # 投诉接口
        │   ├── favorite.js               # 收藏接口
        │   ├── file.js                   # 文件上传
        │   ├── notification.js           # 通知接口
        │   ├── order.js                  # 订单接口
        │   ├── product.js                # 商品接口
        │   ├── user.js                   # 用户接口
        │   └── verification.js           # 认证接口
        ├── components/
        │   ├── admin/AdminSidebar.vue     # 管理员侧边栏
        │   ├── common/                    # 通用组件 (7个)
        │   │   ├── ProductCard.vue        # 商品卡片
        │   │   ├── LoadingSpinner.vue     # 加载动画
        │   │   ├── EmptyState.vue         # 空状态
        │   │   ├── Pagination.vue         # 分页器
        │   │   ├── ConfirmDialog.vue      # 确认对话框
        │   │   ├── ImageUpload.vue        # 图片上传
        │   │   └── ToastNotification.vue  # Toast 通知
        │   └── layout/
        │       ├── AppHeader.vue          # 导航栏 (用户/管理员双模式)
        │       └── AppFooter.vue          # 页脚
        ├── router/index.js               # 路由配置 + 权限守卫
        ├── stores/auth.js                 # Pinia 认证状态
        ├── styles/global.css              # 全局样式系统
        └── views/                         # 页面组件 (20个)
            ├── Home.vue                   # 首页 (搜索+分类+季节性入口)
            ├── Login.vue                  # 登录
            ├── Register.vue               # 注册
            ├── ProductDetail.vue          # 商品详情
            ├── PublishProduct.vue          # 发布/编辑商品
            ├── MyProducts.vue             # 我的发布
            ├── Favorites.vue              # 我的收藏
            ├── Orders.vue                 # 订单列表
            ├── OrderDetail.vue            # 订单详情
            ├── ChatList.vue               # 消息列表
            ├── ChatDetail.vue             # 聊天详情 (WebSocket)
            ├── Notifications.vue          # 通知中心
            ├── UserProfile.vue            # 个人中心 (含身份认证)
            ├── Verification.vue           # 身份认证页面
            ├── Complaints.vue             # 投诉页面
            ├── NotFound.vue               # 404
            └── admin/                     # 管理员页面 (7个)
                ├── Dashboard.vue          # 数据面板
                ├── UserManagement.vue     # 用户管理
                ├── ProductManagement.vue   # 商品审核
                ├── CategoryManagement.vue # 分类管理
                ├── OrderManagement.vue    # 订单管理
                ├── VerificationManagement.vue # 认证管理
                └── ComplaintManagement.vue    # 投诉管理
```

---

## 快速启动

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis (默认端口 6379)
- RabbitMQ (默认端口 5672)
- Node.js 18+
- Maven (内置 wrapper)

### 1. 数据库准备

```sql
CREATE DATABASE IF NOT EXISTS ershou DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

数据库表由 MyBatis-Plus 自动创建，首次启动时会自动建表。

### 2. 启动 Redis

```bash
redis-server
```

### 3. 启动 RabbitMQ

```bash
rabbitmq-server
```

### 4. 启动后端 (端口 8081)

```bash
cd backend
./mvnw spring-boot:run -DskipTests
```

Swagger API 文档：http://localhost:8081/swagger-ui.html

### 5. 启动前端 (端口 3000)

```bash
cd front
npm install
npm run dev
```

浏览器打开：http://localhost:3000

---

## 功能列表

### 用户端功能

| 模块 | 功能 |
|------|------|
| 🏠 **首页** | 商品浏览、关键词搜索、分类筛选、价格排序、最新/最热排序 |
| 🎒🎓 **季节性板块** | 毕业急出 (5-7月)、开学急用 (8-10月)，非开放期提示开放时间 |
| 📦 **商品发布** | 发布/编辑商品、图片上传、分类选择、普通/急出/急用标签 |
| 🔍 **商品详情** | 图片浏览、卖家信息、收藏、立即购买、联系卖家 |
| ❤️ **收藏管理** | 添加/取消收藏、收藏列表 |
| 📋 **订单系统** | 创建订单、买到的/卖出的、发货、确认收货、取消订单 |
| 💬 **即时通讯** | 会话列表、消息收发、WebSocket 实时推送、未读计数 |
| 🔔 **通知中心** | 系统通知、订单状态通知、商品审核通知、已读/全部已读 |
| 🛡️ **身份认证** | 学生/教师认证、证件照上传、认证状态查看、驳回申诉 |
| 📝 **投诉反馈** | 提交投诉（用户/商品/订单/其他）、查看处理结果 |
| 👤 **个人中心** | 基本信息编辑、修改密码、认证状态展示 |
| 📧 **注册登录** | 用户注册、JWT 登录、未认证提醒 |

### 管理员功能

| 模块 | 功能 |
|------|------|
| 📊 **数据面板** | 用户/商品/订单统计、总交易金额 |
| 👥 **用户管理** | 用户列表、封禁/解禁 |
| 📦 **商品管理** | 商品审核（通过/驳回）、强制下架 |
| 📂 **分类管理** | 分类 CRUD、排序、启用/禁用 |
| 📋 **订单管理** | 全部订单查看、状态筛选 |
| 🛡️ **认证管理** | 认证审核（通过/驳回/冻结/解冻/复核）、申诉处理 |
| 📝 **投诉管理** | 投诉列表、标记处理中、解决并填写处理结果 |

### 后端基础设施

| 组件 | 功能 |
|------|------|
| 🔐 **JWT 认证** | Token 生成/验证/刷新、角色权限校验 |
| 🗄️ **Redis 缓存** | 分类缓存、商品详情缓存、用户会话 |
| 📨 **RabbitMQ** | 订单事件队列、通知消息队列（异步扩展预留） |
| 💬 **WebSocket** | 实时聊天推送 |
| 📁 **文件上传** | 图片上传（Base64 + Multipart） |
| 🛡️ **权限拦截** | 未认证拦截、认证状态校验（交易功能需已认证） |

---

## 用户角色与权限

| 角色 | 浏览商品 | 发布/交易/聊天 | 后台管理 | 审核认证 | 处理投诉 |
|------|---------|--------------|---------|---------|---------|
| **未登录** | ✅ | ❌ | ❌ | ❌ | ❌ |
| **普通用户 (未认证)** | ✅ | ❌ (需先认证) | ❌ | ❌ | ❌ |
| **普通用户 (已认证)** | ✅ | ✅ | ❌ | ❌ | ❌ |
| **管理员** | ❌ (仅后台) | ❌ | ✅ | ✅ | ✅ |

### 认证状态流转

```
UNVERIFIED ──提交认证──▶ PENDING ──管理员通过──▶ APPROVED (可使用全部功能)
                ▲          │                         │
                │   管理员驳回    │                    │
                │          ▼                    │ 管理员冻结
                └──重新提交── REJECTED ──申诉──▶      ▼
                                     │         FROZEN ──解冻──▶ APPROVED
                                     ▼
                               管理员处理申诉
                              (通过/驳回)
```

### 季节性标签权限

| 标签 | 开放时间 | 非开放期行为 |
|------|---------|------------|
| 🎓 毕业急出 | 5月 - 7月 | 入口可见但弹窗提示，发布页置灰 |
| 🎒 开学急用 | 8月 - 10月 | 同上 |
| 📦 普通 | 全年 | 始终可用 |

---

## API 接口概览

### 公开接口（无需登录）

| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/register` | 用户注册 |
| GET | `/api/products/list` | 商品列表 |
| GET | `/api/products/search` | 商品搜索（支持关键词/分类/价格/标签） |
| GET | `/api/products/detail/{id}` | 商品详情 |
| GET | `/api/products/tags/periods` | 获取季节性标签开放时段 |
| GET | `/api/categories/list` | 分类列表 |

### 需登录接口

| Method | Path | 说明 |
|--------|------|------|
| GET/POST | `/api/products/**` | 商品发布/编辑/删除/下架/我的商品 |
| GET/POST | `/api/orders/**` | 订单创建/列表/详情/发货/收货/取消 |
| POST/DELETE | `/api/favorites/**` | 收藏/取消/列表/检查 |
| GET/POST | `/api/chat/**` | 会话/消息/已读/未读数 |
| GET/POST | `/api/notifications/**` | 通知列表/已读/全部已读 |
| GET/PUT/POST | `/api/user/**` | 个人信息/修改密码 |
| GET/POST | `/api/verification/**` | 认证状态/提交认证/申诉 |
| GET/POST | `/api/complaints/**` | 投诉提交/我的投诉 |
| POST | `/api/files/upload` | 图片上传 |

### 管理员接口（需 ADMIN 角色）

| Method | Path | 说明 |
|--------|------|------|
| GET/POST | `/api/admin/users/**` | 用户管理/封禁/解禁 |
| GET/POST | `/api/admin/products/**` | 商品审核/强制下架 |
| GET/POST/PUT/DELETE | `/api/admin/categories/**` | 分类CRUD |
| GET | `/api/admin/orders` | 订单管理 |
| GET | `/api/admin/statistics` | 数据统计 |
| GET/POST | `/api/admin/verifications/**` | 认证审核/冻结/解冻/复核/申诉处理 |
| GET/POST | `/api/admin/complaints/**` | 投诉处理/解决 |

### 统一响应格式

```json
{
  "code": 200,        // 200=成功, 400=参数错误, 401=未登录, 403=无权限, 500=服务器错误
  "message": "success",
  "data": { ... }
}
```

分页响应：
```json
{
  "code": 200,
  "data": {
    "list": [ ... ],
    "total": 100,
    "page": 0,
    "size": 10,
    "totalPages": 10
  }
}
```

---

## 数据库表结构

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `users` | 用户表 | username, password, role, status, verification_status, identity_type, real_name |
| `products` | 商品表 | title, price, category_id, seller_id, status, product_tag, images |
| `categories` | 分类表 | name, icon, sort_order, enabled |
| `orders` | 订单表 | order_no, product_id, buyer_id, seller_id, price, status |
| `favorites` | 收藏表 | user_id, product_id |
| `conversations` | 会话表 | user1_id, user2_id, product_id, last_message |
| `messages` | 消息表 | conversation_id, sender_id, receiver_id, content, type, is_read |
| `notifications` | 通知表 | user_id, title, content, type, related_id, is_read |
| `verification_appeals` | 认证申诉表 | user_id, reason, status, admin_response |
| `complaints` | 投诉表 | user_id, target_user_id, order_id, type, title, content, status, admin_response |

---

## MyBatis-Plus 配置详情

### 依赖
```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.9</version>
</dependency>
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
    <version>3.5.9</version>
</dependency>
```

### 应用配置 (`application.yml`)
```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-enums-package: com.example.backend.entity    # 枚举自动映射
  global-config:
    db-config:
      id-type: auto                                  # 主键自增
      logic-delete-field: deleted                    # 逻辑删除字段
      logic-delete-value: true
      logic-not-delete-value: false
  configuration:
    map-underscore-to-camel-case: true               # 驼峰自动转换
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL 日志
```

### 分页插件 (`MyBatisPlusConfig.java`)
```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
}
```

### 典型用法示例

**Mapper 层** — 继承 BaseMapper 获得 CRUD：
```java
@Mapper
public interface ProductMapper extends BaseMapper<Product> {}
```

**Service 层** — 类型安全的 LambdaQueryWrapper + 分页：
```java
LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Product::getStatus, Product.ProductStatus.ON_SALE)
       .like(Product::getTitle, keyword)
       .ge(minPrice != null, Product::getPrice, minPrice);
Page<Product> mpPage = productMapper.selectPage(new Page<>(page + 1, size), wrapper);
```

**Entity 层** — 逻辑删除 + 自动填充：
```java
@Data
@TableName("products")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableLogic
    private Boolean deleted = false;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

---

## 部署配置

### 后端配置 (`application.yml`)

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ershou?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
  data:
    redis:
      host: localhost
      port: 6379
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

jwt:
  secret: ershou-platform-secret-key-2024
  expiration: 86400000  # 24 hours
```

### 前端配置 (`vite.config.js`)

```js
server: {
  port: 5173,
  proxy: {
    '/api': 'http://localhost:8081',
    '/images': 'http://localhost:8081',
    '/uploads': 'http://localhost:8081'
  }
}
```

---

## 测试账号

首次启动时 `DataInitializer` 自动创建以下测试数据：

| 用户名 | 密码 | 角色 | 认证状态 | 说明 |
|--------|------|------|---------|------|
| `admin` | `admin123` | 管理员 | APPROVED | 后台管理 |
| `user1` | `123456` | 普通用户 | APPROVED | 小明，已认证 |
| `user2` | `123456` | 普通用户 | UNVERIFIED | 小红，未认证 |
| `user3` | `123456` | 普通用户 | UNVERIFIED | 小刚，未认证 |
| `user4` ~ `user10` | `123456` | 普通用户 | UNVERIFIED | 其他测试用户 |

预置 **8个商品分类**、**15件示例商品**（覆盖各分类）。

---

## 开发者说明

- 后端启动后 Swagger 文档地址：`http://localhost:8081/swagger-ui.html`
- 前端使用 Vite HMR 热更新，代码修改即时生效
- Redis 缓存 TTL 可在 `application.yml` 的 `cache.ttl` 配置
- RabbitMQ 队列名定义在 `RabbitMQConfig.java`
- 认证拦截规则定义在 `WebConfig.java` 的 `excludePathPatterns`
- 交易功能权限（需已认证）定义在 `AuthInterceptor.java` 的 `requiresVerification
