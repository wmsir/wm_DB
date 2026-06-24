# wmDB (完美数据库) V2.0 商业版

wmDB 是一款高可用、高安全性的企业级 SQL 审核与自动化流转平台。

## 核心特性
1. **彻底解耦**：审批状态机（Flowable）与业务执行引擎（SQL Domain）物理拆分，通过 Webhook 回调事件驱动流转。
2. **绝对安全**：实名制强制认证（身份证号），DB 凭证防泄露（无状态存储），长短文分流防 OOM（超大附件流式处理），前端防截图（动态实名水印防篡改）。
3. **架构防腐**：通过 SPI 策略模式支持异构数据库引擎。

---

## 一、本地开发与启动指南 (联调说明)

### 1. 启动依赖环境
后端服务依赖于 MySQL（存储业务数据）以及 MinIO（存储大文件 SQL 附件）。
请确保本地已通过原生安装或 Docker 启动了这两个基础服务，并在 `backend/src/main/resources/application.yml` 中修改为正确的连接配置。

### 2. 后端启动 (Spring Boot)
1. 确保已安装 JDK 17 和 Maven 3.6+。
2. 进入后端目录并编译：
   ```bash
   cd backend
   mvn clean install
   ```
3. 运行项目：
   ```bash
   mvn spring-boot:run
   ```
   *后端服务默认监听 8080 端口（根据 yml 自动加载），提供认证、上传、和对接工作流的 API。*

### 3. 前端启动 (Vue 3 + Vite)
1. 确保已安装 Node.js 18+ 和 NPM。
2. 进入前端目录，安装依赖：
   ```bash
   cd frontend
   npm install
   ```
3. 启动本地开发服务器：
   ```bash
   npm run dev
   ```
   *Vite 会启动开发服务器，通常在 `http://localhost:5173`。前端页面将自动与后台联调交互。*

---

## 二、工作流 (Flowable) 开发与部署说明

wmDB 的审批流转机制采用 **Flowable 7.x** 驱动：

1. **流程定义**：使用前端集成的 `bpmn.js` (白屏化流转配置) 绘制 `.bpmn20.xml` 流程文件。
2. **流程部署**：将生成的 XML 提交给 Spring Boot 后端的 Flowable RepositoryService 进行部署。
3. **安全隔离机制**：
   - 工作流引擎上下文中**严禁**存放数据库实例密码以及大文本 SQL。
   - 引擎中流转的 `Variables` 仅保存 `ticketId`、`businessKey` 和申请人 `idCard` 等轻量级元数据。
4. **状态回调驱动**：
   - OA 等外部审批系统完成节点审批后，调用系统的 Webhook `/api/v1/workflow/callback` 接口。
   - 该接口接收到状态变更信号后，将触发 `TicketService.approveTicket` 动作。状态流转为 `APPROVED` 后，后端自动发起内存解密，并启动 JDBC Stream 异步安全执行目标库。

---

## 三、Docker 部署指南

当开发完毕，可以通过 Docker 进行一键式发布部署。以下是使用 Docker Compose 部署的步骤指导（需在生产服务器执行）：

### 1. 编写 Dockerfile
为前后端分别编写 Dockerfile。
* **后端 (Backend)**：使用基于 `eclipse-temurin:17-jre` 的镜像，将 Maven 打包出的 `wmdb-backend-*.jar` 拷贝至镜像并指定 `java -jar` 作为 `ENTRYPOINT`。
* **前端 (Frontend)**：使用 `nginx:alpine` 镜像。先运行 `npm run build` 生成 `dist/`，再将其复制到 Nginx 的静态挂载目录 `/usr/share/nginx/html`，并配置好 `/api` 到后端的转发。

### 2. 构建与运行 Docker Compose
创建一个 `docker-compose.yml` 编排文件，包含四个核心容器：
- `wmdb-mysql` (存储系统数据)
- `wmdb-minio` (大文件对象存储)
- `wmdb-backend` (Spring Boot 业务服务，依赖 mysql 和 minio)
- `wmdb-frontend` (Nginx 前端资源，依赖 backend)

**部署指令：**
```bash
# 构建所有的 Docker 镜像
docker-compose build

# 后台拉起全套组件
docker-compose up -d

# 查看运行日志确认是否成功
docker-compose logs -f
```

部署完成后，通过浏览器访问 Nginx 映射的 80/443 端口即可开始使用 wmDB。