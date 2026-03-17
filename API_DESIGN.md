# Birthday Video Maker API 设计（含后台管理）

## 1. 数据库设计

- 建表脚本：`backend/sql/schema_mysql.sql`
- 初始化数据：`backend/sql/sample_data_mysql.sql`

业务表：
- `video_template`：视频模板商品表
- `order_info`：订单表

后台权限表（RBAC）：
- `admin_user`：后台用户
- `admin_role`：角色
- `admin_menu`：菜单与权限资源
- `admin_user_role`：用户-角色关系
- `admin_role_menu`：角色-菜单关系

## 2. 前台接口（网站）

基础前缀：`/api`

模板：
- `GET /templates/themes`
- `GET /templates/categories`
- `GET /templates?keyword=&theme=&category=`
- `GET /templates/{templateId}`

订单：
- `GET /orders`
- `POST /orders`
- `GET /orders/{orderId}`
- `PUT /orders/{orderId}/pay`
- `PUT /orders/{orderId}/status?status=`
- `GET /orders/{orderId}/jump`
- `DELETE /orders/{orderId}`

视频生成：
- `POST /video/generate`
- `GET /video/download/{taskId}`

## 3. 后台接口（RESTful）

基础前缀：`/api/admin`

认证方式：
- 登录后返回 `token`
- 后续请求头：`Authorization: Bearer <token>`

### 3.1 会话（登录/登出）
- `POST /sessions`：登录
- `GET /sessions/current`：获取当前登录信息
- `DELETE /sessions/current`：退出登录

默认账号：
- `admin / Admin@123456`
- `operator / Operator@123456`

### 3.2 用户管理
- `GET /users?keyword=&status=`
- `GET /users/{userId}`
- `POST /users`
- `PUT /users/{userId}`
- `DELETE /users/{userId}`

### 3.3 角色管理
- `GET /roles`
- `GET /roles/{roleId}`
- `POST /roles`
- `PUT /roles/{roleId}`
- `DELETE /roles/{roleId}`

### 3.4 菜单管理
- `GET /menus?enabledOnly=`
- `POST /menus`
- `PUT /menus/{menuId}`
- `DELETE /menus/{menuId}`

### 3.5 模板商城管理
- `GET /templates?keyword=&theme=&category=&enabled=`
- `GET /templates/{templateId}`
- `POST /templates`
- `PUT /templates/{templateId}`
- `DELETE /templates/{templateId}`

### 3.6 订单管理
- `GET /orders?keyword=&status=&startDate=&endDate=`
- `GET /orders/{orderId}`
- `PUT /orders/{orderId}`
- `DELETE /orders/{orderId}`

### 3.7 数据统计
- `GET /statistics/overview`
- `GET /statistics/daily-sales?days=`
- `GET /statistics/top-templates?limit=`

## 4. 跨域说明（已处理）

- 统一放行：`/api/**`
- 允许方法：`GET/POST/PUT/PATCH/DELETE/OPTIONS`
- 后台拦截器放行 `OPTIONS` 预检请求，避免浏览器 `CORS error`

## 5. 启动方式

后端：
```bash
cd backend
mvn -s settings.xml spring-boot:run
```

后端（MySQL）：
```bash
cd backend
mvn -s settings.xml spring-boot:run -Dspring-boot.run.profiles=mysql
```

前台：
```bash
cd frontend
npm run dev
```

后台：
```bash
cd admin-frontend
npm install
npm run dev
```
