# 拾题社区

前后端分离的编程学习社区。用户在题目下写刷题笔记，支持题目管理、评论互动、消息通知、全局搜索，还有一个基于 Redis 的课程秒杀模块。

## 技术栈

后端：Spring Boot 2.7、MyBatis、MySQL 8、Redis、JWT、WebSocket、JavaMail、Flexmark、Jieba、Log4j2。

前端：Vue 3、TypeScript、Vite、Ant Design Vue、Pinia、Vue Router。

## 代码结构

后端 `backend/src/main/java/com/studynote/notes/`：

```
controller/   # 接口层
service/      # 业务逻辑
mapper/       # MyBatis 数据访问
model/        # entity / dto / vo / enums
task/         # 定时任务（秒杀超时取消、对账、Stream 消费等）
config/       # Spring 配置
interceptor/  # 登录、权限拦截
aspect/       # AOP 切面
annotation/   # 自定义注解
filter/       # 过滤器
event/        # 事件
exception/    # 异常处理
utils/        # 工具类
```

前端 `frontend/src/`：

```
domain/       # 按功能模块划分（user / note / comment / seckill ...），每个模块含 api、service、types
pages/        # 页面
components/   # 公共组件
stores/       # Pinia 状态
request/      # 请求封装
router/       # 路由
utils/        # 工具
```

## 快速开始

环境要求：JDK 17、Maven、MySQL 8、Redis、Node.js 18+。

后端：

```bash
DB_PASSWORD=xxx JWT_SECRET=xxx mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```
