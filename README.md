# LynxTrip-a（前后端脚手架）

本仓库已按 `项目介绍.md` 技术栈搭好**最小可运行**的前后端工程，用于后续逐步填充业务模块。

## 目录结构

- `backend/`: Java 21 + Spring Boot 3 + Spring Security + JWT + MyBatis-Plus + Flyway（默认 H2 内存库，可切换 MySQL）
- `frontend/`: Vue 3 + Vite + Vue Router + Pinia + Element Plus + Axios + Sass（已配置 `/api` 代理到后端）

## 本地启动

### 后端

后端默认使用 **H2 内存数据库**，保证开箱即跑。

在项目根目录执行：

```bash
mvn -Dmaven.repo.local=backend/.m2 -f backend/pom.xml spring-boot:run
```

验证：

```bash
curl http://127.0.0.1:8080/api/health
```

### 前端

在项目根目录执行：

```bash
npm --prefix frontend install
npm --prefix frontend run dev -- --host 127.0.0.1 --port 5173
```

打开浏览器访问 `http://127.0.0.1:5173`，首页会自动请求 `/api/health` 检测后端联通性（通过 Vite 代理转发到 `http://127.0.0.1:8080`）。

## 切换到 MySQL（需要你操作数据库）

当你准备好 MySQL 8 后：

- **你需要做**：创建数据库（例如 `lynxtrip`），并准备账号/密码。
- **然后后端启动时**：带上 profile `mysql`，并配置环境变量：

```bash
export SPRING_PROFILES_ACTIVE=mysql
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306
export MYSQL_DB=lynxtrip
export MYSQL_USER=root
export MYSQL_PASSWORD=你的密码

mvn -Dmaven.repo.local=backend/.m2 -f backend/pom.xml spring-boot:run
```

（Flyway 会自动执行 `backend/src/main/resources/db/migration/` 下的迁移脚本。）

