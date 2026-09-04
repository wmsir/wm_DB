# WMDB 完美数据库平台 - 生产服务器部署与运维操作手册

> **部署目标服务器**：`101.35.100.169` (CentOS 7 Linux)  
> **访问协议与入口**：`http://101.35.100.169` (直接通过公网 IP 访问，默认 80 端口)  
> **更新时间**：2026-09-04  

---

## 一、 快速使用与访问入口

### 1. 系统在线访问地址
- **前端 Web 界面**：[http://101.35.100.169](http://101.35.100.169)
- **后端 API 代理**：[http://101.35.100.169/api/](http://101.35.100.169/api/)

### 2. 内置角色测试账号与密码
| 登录账号 (Account) | 真实姓名 | 所属角色 | 默认密码 | 角色权限与使用场景 |
| :--- | :--- | :--- | :--- | :--- |
| **`testadmin1`** | **王总** | **ADMIN (系统超级管理员)** | `123456` | 具备全功能特权：全局参数/安全策略/审批流设计/多源实例/直接终审 |
| **`testadmin2`** | **张伟** | **DEV_LEAD (业务开发组长)** | `123456` | 具备初审权限：业务变更审核、所属资源组工单管理 |
| **`testadmin3`** | **赵工** | **DBA (核心安全运维)** | `123456` | 具备复核/终审权限：SQL执行窗口控制、灰度分批/线下执行确认 |
| **`admin`** | **管理员** | **ADMIN** | `123456` | 系统内置通用管理特权账号 |

---

## 二、 部署架构与容器矩阵

系统采用高性能的 **Docker + Nginx 反向代理 + Spring Boot 3 容器化** 架构，与服务器原有业务相互隔离、互不干扰：

```
                              [ 用户浏览器 / 终端 ]
                                        │
                                        │ (HTTP 80 端口)
                                        ▼
                 ┌──────────────────────────────────────────────┐
                 │          Nginx 容器 (nginxtest)              │
                 │      监听 80 端口 (支持 IP 访问与域名隔离)        │
                 └──────────────┬────────────────────────┬──────┘
                                │                        │
                  (静态页面 /)  │                        │ (/api/ 代理)
                                ▼                        ▼
                 ┌────────────────────────┐    ┌────────────────────────┐
                 │   前端静态资源 (Vue3)   │    │  后端容器 (wmdb-backend)│
                 │ /usr/share/nginx/html/ │    │      (端口 8088)       │
                 │          wmdb          │    │   JDK 17 + SpringBoot  │
                 └────────────────────────┘    └────────────┬───────────┘
                                                            │
                                    ┌───────────────────────┴───────────────────────┐
                                    ▼                                               ▼
                         ┌───────────────────────┐                     ┌────────────────────────┐
                         │  Redis 缓存容器        │                     │   生产 RDS MySQL 8     │
                         │ (wmdb-redis: 6379)    │                     │ (rm-uf6ab...aliyuncs)  │
                         └───────────────────────┘                     └────────────────────────┘
```

### 运行容器一览
| 容器名称 | 基础镜像 | 映射端口 | 容器说明 |
| :--- | :--- | :--- | :--- |
| **`nginxtest`** | `nginx:latest` | `0.0.0.0:80->80`<br>`0.0.0.0:443->443` | 负责静态前端托管（单页路由 History 支持）与 `/api/` 转发 |
| **`wmdb-backend`** | `wmdb-backend:latest`<br>*(基于 eclipse-temurin:17-jre-alpine)* | `0.0.0.0:8088->8088` | Spring Boot 3 + Flowable 审批引擎核心服务 |
| **`wmdb-redis`** | `redis:7-alpine` | `0.0.0.0:6379->6379` | 会话缓存与频次限制 |

---

## 三、 从零开始部署全流程

### 步骤 1：本地构建与打包

#### 1.1 前端项目打包 (Vue3 + Vite + Element Plus)
在本地 `frontend/` 目录下执行：
```bash
cd frontend
npm run build
```
打包生成后的文件位于 `frontend/dist/`，将其压缩打包为 `frontend_dist.zip` 便于远端快速传输：
```powershell
Compress-Archive -Path frontend\dist\* -DestinationPath frontend_dist.zip -Force
```

#### 1.2 后端项目打包 (Spring Boot 3 + Java 17)
在本地 `backend/` 目录下执行 Maven 编译打包：
```bash
cd backend
mvn clean package -DskipTests=true
```
编译成功后生成：`backend/target/wmdb-backend-1.0.0-SNAPSHOT.jar`。

---

### 步骤 2：上传发布包至远程服务器
通过 SFTP / SCP 将文件传送到服务器的 `/opt/wmdb/` 目录：
```bash
# 在本地终端执行（也可以使用 Xftp / FinalShell 拖拽上传）
scp frontend_dist.zip root@101.35.100.169:/opt/wmdb/
scp backend/target/wmdb-backend-1.0.0-SNAPSHOT.jar root@101.35.100.169:/opt/wmdb/wmdb-backend.jar
```

---

### 步骤 3：服务器端解压前端静态资源
登录服务器执行：
```bash
# 确保 Nginx 挂载目录存在并解压
mkdir -p /opt/docker/nginx/html/wmdb
unzip -o /opt/wmdb/frontend_dist.zip -d /opt/docker/nginx/html/wmdb/
```

---

### 步骤 4：启动与构建后端容器

#### 4.1 启动 Redis 依赖容器
```bash
docker rm -f wmdb-redis || true
docker run -d --name wmdb-redis --restart always -p 6379:6379 redis:7-alpine
```

#### 4.2 编写后端 Dockerfile (`/opt/wmdb/Dockerfile`)
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY wmdb-backend.jar app.jar
EXPOSE 8088
ENV JAVA_OPTS="-Xms256m -Xmx600m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=8088 --spring.redis.host=172.17.0.1 --spring.redis.port=6379"]
```

#### 4.3 构建后端镜像并启动
```bash
docker rm -f wmdb-backend || true
docker build -t wmdb-backend:latest /opt/wmdb
docker run -d --name wmdb-backend --restart always -p 8088:8088 wmdb-backend:latest
```

---

### 步骤 5：配置 Nginx 代理与重载
编辑 `/opt/docker/nginx/conf/nginx.conf`，在 `http` 块内加入默认 IP 访问配置：

```nginx
# 🟢 WMDB 完美数据库管理系统 (直接通过 IP:80 访问)
server {
    listen       80 default_server;
    server_name  101.35.100.169 _;

    root   /usr/share/nginx/html/wmdb;
    index  index.html index.htm;

    # 前端单页面应用 HTML5 History 模式支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 反向代理至 Spring Boot 容器
    location /api/ {
        proxy_pass http://172.17.0.1:8088/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 100m;
        proxy_connect_timeout 60s;
        proxy_send_timeout 180s;
        proxy_read_timeout 180s;
    }

    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   /usr/share/nginx/html;
    }
}
```

配置保存后测试并重载 Nginx：
```bash
docker exec nginxtest nginx -t
docker exec nginxtest nginx -s reload
```

---

## 四、 常见运维与排障命令

### 1. 查看后端运行状态与日志
```bash
# 实时跟踪后端最新日志
docker logs -f --tail 100 wmdb-backend

# 检查后端端口连通性
curl -I http://127.0.0.1:8088/api/v1/auth/login
```

### 2. 重启服务容器
```bash
# 重启后端服务
docker restart wmdb-backend

# 重启 Redis
docker restart wmdb-redis

# 重载 Nginx 配置
docker exec nginxtest nginx -s reload
```

### 3. 一键热更新发版脚本 (`/opt/wmdb/deploy.sh`)
服务器已放置自动化热更新脚本，后续代码更新后只需执行：
```bash
cd /opt/wmdb
bash deploy.sh
```

---

## 五、 验证检查清单

- [x] **页面直访验证**：浏览器打开 `http://101.35.100.169`，页面正常渲染 Vue 3 界面。
- [x] **用户登录验证**：输入 `testadmin1` / `123456`，成功获取 JWT Token 并跳转控制台首页。
- [x] **数据连通验证**：工单列表拉取阿里云 RDS 数据库真实历史工单（60+条）。
- [x] **接口代理验证**：前端发起 `/api/v1/...` 请求返回 HTTP 200，无跨域报错。
- [x] **原有服务不受影响**：服务器原有的 `toolso.cn` HTTPS 站点正常运行。
