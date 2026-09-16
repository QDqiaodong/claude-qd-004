# 奶牛场 · 牛舍挤奶位与饲料管理系统

牧场的日常作业台账：**牛舍与挤奶位**、**奶牛档案**、**挤奶班次**、**饲料领用**。

## 技术栈

Spring Boot 3.3（Java 17）+ MySQL 8.0 + Redis 7 + Vue 3 + Element Plus + Vite + nginx，全栈 `docker compose` 一键启动。

## 启动

```bash
./start.sh              # 等价于 docker compose up -d --build
```

| 入口 | 地址 |
| --- | --- |
| 前端页面 | http://127.0.0.1:8204/ |
| 后端接口 | http://127.0.0.1:8304/api/ |
| MySQL | 127.0.0.1:3504（库 `dairy_farm`） |
| Redis | 127.0.0.1:6504 |

## 停止

```bash
docker compose down       # 保留数据卷
docker compose down -v    # 连数据卷一起删，下次启动重新灌种子数据
```

## 端口与库名

都在 `.env` 里改，`.env.example` 是同一份模板。容器名统一是
`claude-qd-004-{mysql,redis,backend,frontend}`。

## 业务模块

### 1. 牛舍与挤奶位台账（`barn` / `milking_stall`）

牛舍编号 `BN-xx` 唯一，类型分产奶舍 / 犊牛舍 / 干奶舍 / 隔离舍，带「可容纳头数」，状态 `在用 / 停用`。
要停用牛舍时，栏里还有在栏的牛、或者当天还有没结束的挤奶班次，都会被拦住。
挤奶位编号 `MS-xx` 唯一，归属到某个牛舍，型式分并列式 / 转盘式，状态 `可用 / 停用 / 维修`；
当天还有没结束的班次时，不许停用、维修、也不许改归属。

- 页面：牛舍与挤奶位（`/barns`）
- 接口：`GET/POST /api/barns`、`PUT /api/barns/{id}`、`GET/POST /api/stalls`、`PUT /api/stalls/{id}`

### 2. 奶牛档案（`cow`）

耳号 `DH-xxxx` 全库唯一，一头牛同时只在一个牛舍。泌乳状态 `泌乳中 / 干奶期 / 待产 / 已淘汰`，
在栏状态 `在栏 / 离栏`。转入牛舍时目标舍必须在用、而且没有满（可容纳头数）；改成 `离栏` 会自动摘掉牛舍；
已经 `已淘汰` 的牛不能改回泌乳状态。

- 页面：奶牛档案（`/cows`）
- 接口：`GET/POST /api/cows`、`PUT /api/cows/{id}`

### 3. 挤奶班次（`milking_shift`）

班次号 `MS-xxxx` 自动生成。一个班次 = 某天 + 某个班次（早/中/晚）+ 一个挤奶位 + 一个牛舍 + 一位挤奶员 + 一段时段。
排班时校验：挤奶位必须可用、牛舍必须在用、**该牛舍必须还有在栏的泌乳牛**、结束时间晚于开始时间、
**同一挤奶位同日同时段不能重叠**、**同一位挤奶员同日同时段也不能重叠**。
状态机 `待开挤 → 挤奶中 → 已完成`，`待开挤 / 挤奶中` 可以取消；**收班必须登记当班产奶公斤数**。

- 页面：挤奶班次（`/shifts`）
- 接口：`GET/POST /api/shifts`、`POST /api/shifts/{id}/advance?action=&milkKg=`

### 4. 饲料领用与库存（`feed` / `feed_issue`）

饲料编号 `FD-xxxx` 唯一，带库存、单位与预警线。领料只能对 `在用` 的牛舍做，库存不够要拦；
退料不能超过这个牛舍在这件饲料上净领的数量；停用的饲料不能再领。

- 页面：饲料领用（`/feeds`）
- 接口：`GET/POST /api/feeds`、`PUT /api/feeds/{id}`、`GET/POST /api/feed-issues`

## 目录

```
backend/src/main/java/com/dairy/farm/
├── config/       CORS 配置
├── controller/   REST 入口
├── dto/          BizException + 统一错误响应
├── entity/       6 张业务表
├── repository/   Spring Data JPA
└── service/      业务规则（编号唯一、时段占用、状态机、容量与库存）
backend/src/main/resources/schema.sql   建表 + 种子数据（挂进 MySQL initdb）
frontend/src/views/                     4 个业务页面
```
