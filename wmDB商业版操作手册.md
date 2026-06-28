# wmDB 商业版操作手册

## 一、 系统介绍
wmDB (完美数据库) 是一款高可用、高安全性的企业级变更管控与自动化审批平台。商业版在一期框架的基础上，全面引入了国密算法（SM2/SM3/SM4）、多租户体系、OpenAPI 扩展、以及商业大盘监控能力。

## 二、 基础软件安装要求与下载地址

为确保 wmDB 商业版稳定运行，请在部署前准备以下基础环境（附官方下载地址）：

| 软件名称 | 版本要求 | 下载地址 |
| --- | --- | --- |
| **JDK** | 17+ | [Adoptium Temurin 17](https://adoptium.net/zh-CN/temurin/releases/?version=17) |
| **Maven** | 3.6+ | [Apache Maven](https://maven.apache.org/download.cgi) |
| **Node.js** | 20.x+ | [Node.js Official](https://nodejs.org/zh-cn/download/) |
| **MySQL** | 8.0+ | [MySQL Community Server](https://dev.mysql.com/downloads/mysql/) |
| **MinIO** | 最新版 | [MinIO Download](https://min.io/download) |
| **Redis** | 6.0+ | [Redis Download](https://redis.io/download/) |

## 三、 部署与启动步骤

### 1. 启动依赖组件
请确保 MySQL, Redis 和 MinIO 已启动并正常运行。并在后端的 `application.yml` 或环境变量中配置正确的连接信息，必须配置的变量如下：
- `WMDB_JWT_SECRET`：JWT 签名密钥
- `WMDB_MINIO_ENDPOINT`：MinIO 服务地址 (例如 `http://localhost:9000`)
- `WMDB_MINIO_ACCESS_KEY`：MinIO Access Key
- `WMDB_MINIO_SECRET_KEY`：MinIO Secret Key
- `WMDB_MINIO_BUCKET`：Bucket 名称 (默认 `wmdb`)

### 2. 后端服务启动
1. 进入后端目录：`cd backend`
2. 编译并打包：`mvn clean install -DskipTests`
3. 启动应用：`mvn spring-boot:run`

### 3. 前端服务启动
1. 进入前端目录：`cd frontend`
2. 安装依赖 (支持国密 sm-crypto)：`npm install`
3. 启动开发服务器：`npm run dev`

---

## 四、 核心功能与页面展示

### 1. 登录页面 (国密 SM2 保护)

![登录页面-实名制身份证登录功能截图](补充图片路径)

- **功能描述**：支持用户使用实名制身份证号进行身份认证，系统自动通过国密算法（SM2/SM3）保障数据传输与存储的安全。
- **操作步骤**：
  1. 在登录界面输入 18 位实名制身份证号码。
  2. 输入对应的账户密码。
  3. 点击“登录”按钮即可进入系统。

### 2. 商业化大盘总览 (Dashboard)

![商业化大盘总览-平台全局监控与数据统计截图](补充图片路径)

- **功能描述**：登录成功后，系统自动跳转至该总览面板。它提供全面的数据库运行状态，包括健康评分、SQL 拦截统计与当前工单概况，辅助运维决策。
- **操作步骤**：
  1. 成功登录系统，默认进入大盘总览页面。
  2. 浏览核心数据指标（如：数据库健康评分、历史 SQL 数量、拦截统计及 DBA 工作负载等）。

### 3. 工单中心与多租户隔离

![工单中心-个人SQL工单管理与状态追踪截图](补充图片路径)

- **功能描述**：用于集中管理个人的 SQL 审核工单。通过集成 SaaS 化多租户技术，确保各个租户间数据的绝对隔离。
- **操作步骤**：
  1. 在系统左侧导航栏找到并点击“工单中心”。
  2. 在工单列表中查看申请中的工单、待审核的工单以及已审批完成的工单。
  3. 点击特定工单查看其详细状态流转与审核日志。

## 五、 OpenAPI 接入指南
系统现提供标准化的 `RESTful OpenAPI`。
企业内部 DevOps 或 CI/CD 平台可通过如下接口查询工单流转状态：
```http
GET /api/v1/openapi/ticket/{id}/status?applicantId=身份证号
```
返回结果遵循阿里巴巴 Java 规范 `00000` 状态码。

---
*版权所有 © wmDB 完美数据库商业版*
