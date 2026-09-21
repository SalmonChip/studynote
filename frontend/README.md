# 拾题社区前端

Vue 3 + TypeScript + Vue Router + Pinia + Ant Design Vue。后端仍使用现有 Spring Boot 服务。

## 启动

使用 Node 22，在 frontend 目录执行：

```sh
npm ci
npm run dev
```

开发默认地址为 http://127.0.0.1:5173，`/api` 代理到 http://127.0.0.1:8080。后端未启动时会显示请求失败；页面有重试入口。

`VITE_API_BASE_URL` 默认为空，同源调用。独立后端域名可在 `.env.local` 中配置，需后端允许对应跨域请求。生产服务器必须配置 SPA 路由回退和 `/api` 代理。

## 检查

```sh
npm run typecheck
npm run lint
npm test
npm run build
npm run format:check
```

Windows 沙箱中的 Node 若出现 `EPERM ... lstat C:\Users\fish`，通过宿主的权限批准流程运行，不要修改业务代码绕过。

## 隔离交互验收

```sh
npm run dev:smoke
```

地址 http://127.0.0.1:5174，测试登录账号 `demo_admin`，密码 `demo_pass`。测试模式页面带明确提示；只使用 `tests/mock-api.ts` 的内存数据，重启即重置，不连接真实后端。该模式是前端交互验收工具，不是完整后端模拟，也不能证明真实接口、鉴权、验证码、上传或批量导入已联调成功。普通 dev 和生产 build 均不启用 mock 插件。

## 目录与数据流

- `src/router`：原用户和后台路由，登录及管理员守卫。
- `src/stores/session.ts`：当前用户、令牌、登录弹窗、未读消息；退出阻止未完成的恢复请求重新登录。
- `src/request`：单一 fetch 请求层，附带 Bearer token，处理超时、错误和 401；保留顶层 token/pagination。
- `src/domain`：按业务划分的 API 路径、服务和 TypeScript 类型，与后端 DTO/VO 对应。
- `src/composables`：读取资源状态、旧请求失效处理、异步写操作的 busy 与错误提示。
- `src/components`：笔记流、评论树、收藏夹、编辑器、个人资料、公共状态组件。
- `src/pages`：用户页面；`src/pages/admin`：管理页面。
- `src/style.css`：共享样式及响应式布局。
- `tests/mock-api.ts`：仅显式 smoke 模式启动的内存测试服务。

页面通过服务调用请求层；列表筛选通过响应式依赖触发读取，失效的旧结果不覆盖新页面；修改先等待服务成功再刷新状态。社区内容的 author/authorId 是业务字段，保留。原项目作者 meta、统计账号、旧部署地址和前端代码注释已清理；第三方依赖许可证保留。

完整接口映射、已知限制、每批改动和真实验证记录见根目录 `MIGRATION_HANDOFF.md`。新接手者先读根目录 `AGENTS.md`。
