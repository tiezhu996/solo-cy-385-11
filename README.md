# 宝宝成长记录与辅食管理平台

帮助新手父母管理宝宝档案、生长曲线、疫苗计划、辅食食谱和喂养记录，支持家庭成员协作照护。

## 快速启动

```bash
cp .env.example .env
docker compose up -d --build
```

访问地址：前端 http://localhost:18405 ，后端 http://localhost:19405/health 。

## 项目主要功能

- 创建多个宝宝档案，记录出生日期、身高、体重和血型。
- 定期记录身高体重，自动生成成长曲线百分位。
- 内置疫苗计划，支持已接种和未接种状态。
- 按月龄推荐辅食食谱，并支持过敏原筛选。
- **家庭协作**：创建者为宝宝生成一次性邀请码，家人领取后按角色协作照护。

## 家庭协作

### 角色与权限

| 角色 | 说明 | 档案/生长/疫苗/辅食读取 | 生长/疫苗写入 | 邀请码与成员管理 |
| --- | --- | --- | --- | --- |
| OWNER | 创建者 | ✓ | ✓ | ✓ |
| MANAGE | 管理 | ✓ | ✓ | ✓ |
| RECORD | 记录 | ✓ | ✓ | ✗ |
| VIEW | 查看 | ✓ | ✗ | ✗ |

### 协作规则

- 创建者可为某个宝宝生成**一次性邀请码**（可设有效期，默认 72 小时），并指定领取后获得的角色（VIEW / RECORD / MANAGE）。
- 邀请码被**领取、撤销或过期**后不可再次使用；并发领取时数据库原子更新保证只有一个确定结果。
- 角色调整与移除成员**立即生效**：所有数据接口每次请求实时校验成员表，被移除成员立刻无法读取或写入。
- **创建者不能退出家庭、不能被移除或改角色；家庭中最后一名管理者不能被移除或降级。**
- 成员变更（改角色 / 移除 / 退出）通过对宝宝行加排他锁串行化，并发操作只保留一个确定结果；成员与权限列表可随时回读。

### 协作相关接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/users/register · /api/users/login | 注册 / 登录，返回 JWT |
| POST | /api/babies | 创建宝宝档案（创建者自动成为 OWNER） |
| GET | /api/babies | 我加入的宝宝列表（含我的角色） |
| GET · PUT | /api/babies/{babyId} | 查看档案（成员）· 修改档案（管理） |
| POST | /api/babies/{babyId}/invites | 生成一次性邀请码（管理） |
| GET | /api/babies/{babyId}/invites | 邀请码列表回读（管理） |
| POST | /api/babies/{babyId}/invites/{id}/revoke | 撤销邀请码（管理） |
| POST | /api/invites/claim | 凭邀请码领取成员身份 |
| GET | /api/babies/{babyId}/members | 成员与权限列表回读（成员） |
| PUT | /api/babies/{babyId}/members/{userId} | 调整成员角色（管理） |
| DELETE | /api/babies/{babyId}/members/{userId} | 移除成员（管理） |
| POST | /api/babies/{babyId}/leave | 退出家庭（创建者除外） |
| GET · POST | /api/babies/{babyId}/growth | 生长记录（查看 / 记录） |
| GET · POST | /api/babies/{babyId}/vaccines | 疫苗计划（查看 / 记录） |
| GET | /api/babies/{babyId}/foods/recommend | 辅食推荐（查看） |

除注册与登录外，所有接口都需要请求头 `Authorization: Bearer <token>`。

## 本地开发方式

```bash
cd backend
mvn spring-boot:run
```

```bash
cd frontend
npm install
npm run dev
```

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vant、Vite、ECharts |
| 后端 | Spring Boot、Java 17、MyBatis-Plus、JWT、SLF4J、Logback |
| 数据库 | MySQL 8.0 |
| 部署 | Docker Compose、Nginx |

## 项目目录结构

```text
.
├── backend
│   └── src/main
│       ├── java/com/babytracker
│       │   ├── config        # 登录拦截器与 Web 配置
│       │   ├── constants     # 角色/邀请码枚举、错误码
│       │   ├── controller    # 用户/宝宝/家庭协作/生长/疫苗/辅食
│       │   ├── dto
│       │   ├── entity
│       │   ├── exception     # 统一异常与全局处理器
│       │   ├── mapper
│       │   ├── service       # 家庭协作核心逻辑在 FamilyService
│       │   └── utils         # JWT、登录上下文
│       └── resources
├── database
│   └── init.sql              # 含 app_user / baby_member / baby_invite 表
├── frontend
│   └── src                   # api.ts 封装协作接口，App.vue 含家庭协作面板
└── docker-compose.yml
```

## 环境变量说明

| 变量 | 说明 |
| --- | --- |
| COMPOSE_PROJECT_NAME | Compose 项目名，默认 babytracker |
| SPRING_DATASOURCE_URL | Spring Boot MySQL 地址 |
| SPRING_DATASOURCE_USERNAME | 数据库用户名 |
| SPRING_DATASOURCE_PASSWORD | 数据库密码 |
| JWT_SECRET | JWT 签名密钥 |

## Docker 部署说明

- 前端端口：`18405:80`
- 后端端口：`19405:8080`
- MySQL 数据使用命名卷 `babytracker-db-data`。
- Nginx 将 `/api` 代理到 `backend:8080`。

## License

MIT
