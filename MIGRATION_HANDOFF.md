# React → Vue 迁移交接

## 最新交接：收尾验证完成 + 真实后端联调已跑通（2026-09-16）

本节优先于下方历史进度，下方出现的 20%/80%、缺失页面、未运行测试等为旧阶段记录，不代表当前状态。

### 本轮实际完成（前端收尾验证）

- 5 项校验全部通过：`npm run format`、`npm run typecheck`、`npm run lint`（0 错误）、`npm test`（13/13，4 个文件）、`npm run build`（vue-tsc + vite 均退出 0）。
- 具名组件注册已核对完整：模板中所有 `a-*`（含 checkbox-group / input-password / input-search / textarea / list-item / menu-item 等子组件）均在 `main.ts` 注册范围内，无遗漏。
- 前端 src 无残留注释、React 依赖或作者信息：package.json 无 react/redux/tailwind；grep 仅命中业务字段 author/authorId（用户昵称/头像，属保留范围）。
- 构建产物：主包 index 1030KB（gzip 325.7KB），较上轮 1.56MB（gzip 488KB）明显下降；仍超 500KB 警告，未调高阈值。
- `vite.config.ts` smoke 条件加载、`App.vue` smoke 提示、`style.css` min-width:0 + 表格 620px 均已在代码中并随 build 通过。

### 真实接口联调（已跑通，非 mock）

- 后端以 Java 17（E:\programtools\JavaInterpreter\JAVA17）打包并启动：`mvn -DskipTests package` → `java -jar target/notes-0.0.1.jar`，端口 8080，MySQL（kamanote_tech）+ Redis 连接正常，`Started in 4.4s`。
- Redis 原本 STOPPED，`net start Redis` 因权限不足失败；改为直接运行 `E:\Programtools\redis\redis-server.exe --port 6379`（后台）后 `PONG` 正常。
- 通过前端 dev 代理 5173 → 8080 验证真实数据：分类树、题目列表（共 133）、题目详情（`userNote.finished/noteId/content` 契约一致）、用户信息、笔记列表（共 457）、题目搜索、排行榜（空）。前端 dev index.html 正常输出 `拾题社区`。
- 登录错误契约正确：错密码返回 `{code:400,"密码错误"}`；whoami 无 token 返回 `{code:400,"用户 ID 异常"}`。

### 观察与未完成（非本轮修复范围）

- 注册接口 email 选填：不带邮箱无需验证码；带邮箱需验证码但 SMTP 为占位配置（your@qq.com），无法真实发信。本轮未做写入型注册/登录全流程，避免污染库；已确认库中无 `it_*` 测试用户残留。
- SecurityConfig 对 `/api/**` 全部 permitAll，`/api/statistic` 等管理接口未带 token 也返回数据，服务端未强制鉴权，仅靠前端路由守卫兜底；属后端既有行为，本轮未改后端。
- `frontend/.husky/` 已删除（死文件，lint-staged 依赖已移除、根目录无 Git 仓库）。
- `public/favicon.ico` 存在但 HTML 未引用（旧 React 项目遗留，可选删除）。
- 后端作者/git/kama 信息已清理（2026-09-17）：删除 13 个文件的 `@Author Tong`/`@author kama` 标签；删除 `backend/.idea/`、`backend/.gitignore`；数据库 `kamanote_tech` → `studynote_tech`（16 张表数据完整复制，user/note/question=106/217/133 一致），`application-dev.yaml` 已改，`mvn clean package` 重建通过、target 无 kama 残留。旧库 `kamanote_tech` 未删（待用户确认）；`backend/logs/`、`tmp/uploads/` 仍为旧运行产物。
- 手机 390px 横向溢出修复（style.css）已在代码中，本轮未再截图复验视觉。

### 当前进程状态（2026-09-17 复核）

- 上一轮会话结束后全部进程已停止：后端(8080)、Redis(6379)、前端 dev(5173)、smoke(5174) 均未监听。
- 重启步骤（已实测跑通）：
  1. Redis：`E:\Programtools\redis\redis-server.exe --port 6379 --save "" --appendonly no`（后台）；或管理员 `net start Redis`（服务版默认 6379，之前普通权限报“系统错误 5”）。
  2. 后端：`cd backend` → `mvn -DskipTests package` → `E:\programtools\JavaInterpreter\JAVA17\bin\java -jar target/notes-0.0.1.jar`（端口 8080，连 MySQL kamanote_tech + Redis）。
  3. 前端：`cd frontend` → `npm run dev`（5173，`/api` 代理到 8080）；mock 验收用 `npm run dev:smoke`（5174）。
- 注意：PATH 里的 `java` 是 1.8，Maven 实际用 Java 17；跑后端必须显式用 `E:\programtools\JavaInterpreter\JAVA17\bin\java`。

### 下一位 AI 的收尾

1. `frontend/.husky/` 已删除；后端作者/git/kama 信息已清理、数据库已改名 `studynote_tech`。剩 `public/favicon.ico`、旧库 `kamanote_tech`（确认后删）、`backend/logs/`、`tmp/uploads/` 待清理。
2. 如需完整注册/登录联调：用不带邮箱的账号注册即可（无需验证码）；带邮箱注册需先配置真实 SMTP。
3. 如需服务端强制鉴权属后端改动，需用户另行授权，不在本次迁移范围。
4. 更新下方历史摘要后交付。不要重写现有页面。

---

更新时间：2026-09-16（本轮完成收尾验证与真实后端联调）

## 30 秒接手摘要

- 目标：将 `frontend` 从 React 改为 Vue，保持现有业务功能、路由和后端接口，删除项目原作者信息及代码注释。
- 用户最新要求：边改边记录，准备换另一个 AI 接手；每完成一批工作都更新本文件。
- **收尾验证与真实后端联调已完成（2026-09-16）：format/typecheck/lint/test/build 全通过，后端 8080 已启动并接通真实 MySQL/Redis，经 dev 代理验证分类/题目/详情/用户/笔记/搜索等真实数据与登录错误契约。**
- Vue 基础代码已写入；旧 React 页面、Hooks、Redux 等已移除，保留了 35 个接口、服务及类型文件并去掉注释。
- **根目录没有 Git 仓库，未创建旧源码备份，不要声称可从 Git 恢复。** 目前磁盘的保留服务、类型以及本文件是继续实现依据；后端源码完整，未修改。
- 下一步：完成自动测试、lint 和浏览器验收，复查接口交互及异常恢复。代码已覆盖所有原路由，不是占位页。
- 当前没有启动的开发服务，也没有部署。上一轮 npm 安装调用因对话中断丢失最终返回，但依赖文件和锁文件已落盘，`vue-tsc` 已可执行。

## 用户意图、默认方案及待澄清项

原要求：先梳理前端接口、功能与代码逻辑，将 React 重构为 Vue，并删除所有作者相关信息和注释。

已采用方案：Vue 3 + TypeScript + Vue Router + Pinia + Ant Design Vue，Vite 构建。尚未收到用户对具体技术选型的逐项确认；该方案是执行重构时的合理默认选择。

上一轮曾询问，用户尚未回答：

1. UI 尽量保持原样，还是重新设计？默认保持原功能与布局，不擅自重做产品设计。
2. 作者信息是否包含笔记/评论的发布者？默认仅清理项目作者署名、个人链接、统计账号和原部署信息；保留用户昵称、头像、`authorId`、`author` 等业务字段。
3. 注释仅前端还是包括后端？当前仅处理前端，后端未清理或更改。

用户最新指示关注持续记录和可接手性，不必为了上述可使用默认值的问题重复阻塞记录工作。若用户明确扩大范围，再调整。

## 工作环境

- 根目录：`C:\Users\fish\Desktop\拾题社区`
- 前端：`frontend`；后端：`backend`（Spring Boot / Java）。
- PowerShell，Node v22.10.0，npm 10.9.0。
- 沙箱中 Node 加载本地模块曾报 `EPERM: operation not permitted, lstat 'C:\Users\fish'`。这是环境读取权限问题，使用正式提权批准后可运行，不要通过改变代码绕过。
- npm 安装首次在沙箱内无进展，已中止并申请提权重试。磁盘显示 Vue 3.5.42、Ant Design Vue 4.2.6；`package-lock.json` 根包与新 `package.json` 一致。未执行 `npm ci` 完整性验证。
- 接手后按需运行：`cd frontend`，`npm run typecheck`、`npm run build`、`npm run lint`、`npm test`。测试尚未编写，不能把脚本存在当成测试通过。

## 已写入的代码（不等于已通过浏览器验证）

| 文件/范围 | 当前实现 |
| --- | --- |
| `frontend/package.json`、`package-lock.json` | 替换 React 依赖，添加 Vue、Pinia、Ant Design Vue、Markdown、Vitest、vue-tsc 等 |
| `frontend/vite.config.ts` | Vue 插件、`@` 别名、开发 `/api` 代理至 `http://127.0.0.1:8080`、Vitest jsdom |
| `frontend/tsconfig.json` | Vue/TS Bundler 严格配置 |
| `frontend/eslint.config.js`、`.prettierrc` | Vue + TypeScript lint，禁止显式 any；格式尚未统一 |
| `frontend/index.html` | Vue 入口 `#app`，去掉作者 meta、原百度统计脚本，站名使用拾题社区 |
| `.env`、`.env.development`、`.env.production` | `VITE_API_BASE_URL=`，使用同源 `/api`；不再携带原部署 IP |
| `frontend/Dockerfile`、`nginx/default.conf`、`.dockerignore` | 多阶段 `RUN npm run build`、SPA fallback、代理 `backend:8080`；未实际构建镜像，部署须核实服务名 |
| `src/request/types.ts`、`fetchClient.ts`、`index.ts` | 统一 fetch，路径参数编码、查询参数过滤、Bearer token、FormData、15 秒超时、401 事件、兼容 message/msg、保留 pagination/token |
| `src/stores/session.ts` | Pinia 用户状态、自动登录、注册/登录、退出清理令牌、登录弹窗、未读计数 |
| `src/composables/useResource.ts` | 异步资源 loading/error/重试，版本号防止旧请求覆盖新结果，卸载失效 |
| `src/composables/useTask.ts` | 操作 busy、防重复提交、失败提示、成功提示 |
| `src/router/index.ts` | 原有用户/管理路由、懒加载、等待恢复会话、登录和管理员守卫 |
| `src/main.ts`、`src/App.vue` | 注册插件、401 通知、全局异常提示、顶栏、后台菜单、登录/搜索入口、每分钟未读刷新 |
| `src/components/LoginModal.vue` | 登录/注册、邮箱验证码倒计时、表单基础校验、登录后返回目标路由 |
| `src/components/SearchModal.vue` | 关键词搜索题目并跳转 |
| `src/components/ResourceState.vue` | 骨架屏、错误重试、空态 |
| `src/components/MarkdownView.vue` | MarkdownIt + DOMPurify + highlight.js，禁用原始 HTML，双击代码块复制 |
| `src/components/MarkdownEditor.vue` | textarea 编辑、Markdown 工具栏、图片上传、预览；不是原 Cherry 编辑器等价实现，需验证并补齐必要体验 |
| `src/pages/StatusPage.vue` | 403/404 页面，本轮修复 script 标签同行引发的 TS 解析错误 |
| `src/utils/categories.ts`、`download.ts` | 分类展平、Markdown 下载 |
| `src/domain/**/{api,service,types}` | 保留旧有接口、服务、类型；跨模块导入改为直接引用类型文件，去掉注释 |

已删除：旧 React 页面、组件、Hooks、Redux、旧 CSS、两套旧请求封装中的冗余部分、旧 Tailwind/PostCSS 配置、旧 TS app/node 配置、含原 IP 的 `upload.sh`。

清理脚本最初名为 `frontend/migrate-source.cjs`，已执行一次（保留 35，移除 170 个源码文件），**本轮已删除脚本以防重复执行误删新 Vue 文件**。

## 当前验证结果与明确阻塞

2026-09-16 实际执行 `npm run typecheck`：

- 沙箱内失败：EPERM 目录访问；提权后能够运行。
- 首次报告 `StatusPage.vue` 语法问题，已修复。
- 全部 13 个页面已实现。第一轮 `npm run build`（包括 vue-tsc）退出码 0，Vite 生成 dist；主包偏大（Ant Design Vue 全量注册），待评估分包。
- `src/style.css` 已补齐，尚未进行浏览器视觉检查。
- 首轮 lint 发现 LoginModal 正则无用转义（已修复）与 Markdown v-html 警告（仅对已 DOMPurify 消毒的组件定点关闭规则）。正在格式化源码并跑测试；未浏览器/后端联调验证。

接手时还应复查现有基础代码：

- 已修复 `App.vue` dropdown trigger 数组、`SearchModal.vue` locale.emptyText。
- 检查 session 自动恢复/注册后恢复失败时的状态、401 弹窗及受保护页面是否及时退出；当前守卫只在导航时执行。
- 暂未迁移旧 `studynote:userToken` 到 `token`，仅退出时清理旧键；不要意外保留敏感登录信息。
- 请求层无需 UI 库依赖；接口 400/401/500、网络错误、超时都应有测试。业务 code 已兼容 `message` 与 `msg`。

## 路由与应恢复的页面功能

| 路由 | 应保留功能 |
| --- | --- |
| `/`、`/home` | 最新笔记，分页/排序；右侧笔记排行榜；登录后个人热力图与本月/上月前三次数 |
| `/question-set?categoryId=...` | 左分类树，右题目表；完成状态、ID、题目、考点、难度、浏览量；分页与难度/浏览量排序；切分类重置页码、同步 URL |
| `/question-list?questionListId=...` | 按普通题单/训练营题单分组选择；题单题目、完成状态、分页；空选择态；不要擅自绕过训练营权限 |
| `/questions/:questionId` | 题目标题/考点/难度/浏览量；我的笔记新建或修改、Markdown 图片上传与预览；关联笔记列表及互动 |
| `/user/:userId` | 用户信息、该用户笔记和收藏夹 |
| `/user-center/info` | 昵称、性别、生日、邮箱、学校、签名修改；头像上传并 PATCH 保存头像 URL |
| `/user-center/note` | 我的笔记分页、修改/删除、单条下载和全部导出 |
| `/user-center/collect` | 收藏夹创建/删除/列表，查看收藏夹内笔记 |
| `/messages` | 全部/点赞/评论/系统筛选、未读数、单条/批量/全部已读、删除；点击目标跳转题目（旧版只有 console.log，需实装） |
| `/admin` | 按日统计表、分页 |
| `/admin/user` | 用户 ID、账号、昵称、管理员、封禁状态筛选和分页；后端只有查询接口，不虚构编辑/封禁接口 |
| `/admin/category` | 分类树展示、创建子分类、改名、删除 |
| `/admin/question` | 分类筛选和题目列表、创建/编辑/删除、Markdown 批量导入 |
| `/admin/question-list` | 题单创建/修改/删除、类型和描述 |
| `/admin/question-list/:questionListId` | 题单详情、搜索题目并添加、移除、排序并保存 |

通用笔记组件须覆盖：发布者/题目链接、时间、Markdown 展示/展开收起、点赞取消点赞、选择多个收藏夹、评论抽屉、回复与评论点赞、本人笔记编辑/删除。请求成功后再更新状态，或失败回滚；更新计数并防重复提交。

## 后端接口核对（源码是真实依据）

控制器路径：`backend/src/main/java/com/studynote/notes/controller/`。
DTO/VO 路径：`backend/src/main/java/com/studynote/notes/model/`。
服务实现：`backend/src/main/java/com/studynote/notes/service/impl/`。

通用响应：`{ code: 200, message, data, pagination?, token? }`。登录 token 在顶层；分页在顶层；旧前端使用的 `msg` 并不是当前后端主字段。用户 ID 前端类型是 string，Java 是 Long；比较身份建议统一 `String(id)`，勿盲目转换为 number。

| 模块 | 已核实接口与载荷 |
| --- | --- |
| 登录 | `POST /api/users/login`，`{account?, email?, password}`，返回完整用户和顶层 token；不是 `/api/auth/login` |
| 注册 | `POST /api/users`，`{account, username, password, email, verifyCode}`；data 只有 userId，顶层 token；随后 whoami 获取完整用户，不能把密码混入 store |
| 自动登录 | `POST /api/users/whoami`，返回用户和新 token |
| 退出 | 后端无 logout 控制器；清本地状态。保留的 userApi 中 `/users/logout` 是旧死接口，不调用 |
| 用户 | `GET /api/users/{userId}`；`PATCH /api/users/me`；`GET /api/admin/users` 带分页及筛选 |
| 验证码 | `GET /api/email/verify-code?email=...`；旧服务还带 type，后端只接收 email |
| 图片 | `POST /api/upload/image`；头像 `POST /api/users/avatar`；FormData 字段 file，data.url；头像上传后还需 PATCH me |
| 分类 | 用户 `GET /api/categories`，管理 `GET/POST /api/admin/categories`，`PATCH/DELETE /api/admin/categories/{categoryId}`；创建 `{parentCategoryId,name}` |
| 题目 | `GET /api/questions`，`GET /api/questions/{questionId}`，`POST /api/questions/search` body `{keyword}`；列表 query `categoryId,sort:view/difficulty,order:asc/desc,page,pageSize` |
| 管理题目 | `GET/POST /api/admin/questions`，`PATCH/DELETE /api/admin/questions/{questionId}`；创建/更新 `title,difficulty,examPoint,categoryId`；`POST /api/admin/questions/batch` body `{markdown}` |
| 题单 | 现有只有 `GET /api/admin/questionlists` 与 `GET /api/admin/questionlists/{id}` 读取题单；旧用户页也使用该列表服务；不能杜撰 `/api/questionlists`，须验证实际权限 |
| 管理题单 | `POST /api/admin/questionlists`，`PATCH/DELETE /api/admin/questionlists/{id}`，body `{name,description,type}`，类型 1 普通、2 训练营 |
| 题单项 | 用户 `GET /api/questionlist-items?questionListId=&page=&pageSize=`；管理 `GET /api/admin/questionlist-items/{questionListId}` |
| 编辑题单项 | `POST /api/admin/questionlist-items` body `{questionListId,questionId}`；`DELETE /api/admin/questionlist-items/{questionListId}/{questionId}`；`PATCH /api/admin/questionlist-items/sort` body `{questionListId,questionIds:[...]}` |
| 笔记 | `GET/POST /api/notes`，`PATCH/DELETE /api/notes/{noteId}`；列表 query `questionId,authorId,collectionId,sort:create,order,recentDays,page,pageSize`；创建/更新 `{content,questionId}` |
| 笔记附加 | `GET /api/notes/ranklist`、`/heatmap`、`/top3count`、`/download`；下载返回 data.markdown，需要前端生成文件 |
| 点赞笔记 | `POST/DELETE /api/like/note/{noteId}` |
| 收藏夹 | `GET /api/collections` query `{creatorId?,noteId?}`；`POST /api/collections` body `{name,description?}`；`DELETE /api/collections/{collectionId}` |
| 收藏笔记 | `POST /api/collections/batch` body `{noteId,collections:[{collectionId,action:'create'|'delete'}]}`；列表中的 `noteStatus.isCollected` 表示该笔记是否在夹中 |
| 评论 | `GET /api/comments` query `{noteId,page,pageSize}`，`POST /api/comments` body `{noteId,parentId?,content}`；`PATCH/DELETE /api/comments/{commentId}`；点赞 `POST/DELETE /api/comments/{commentId}/like` |
| 消息 | `GET /api/messages`、`GET /api/messages/unread/count`；`PATCH /api/messages/{messageId}/read`、`PATCH /api/messages/batch/read` body `{messageIds}`、`PATCH /api/messages/all/read`；`DELETE /api/messages/{messageId}`；不是旧请求封装中的 PUT 路径 |
| 统计 | `GET /api/statistic?page=&pageSize=` |

已新增 categoryService.list 使用 `/api/categories`；已补 commentService.updateComment/deleteComment。messageService 有批量已读，单条也可用一项数组。

关键数据：

- 题目详情：`userNote: {finished,noteId,content}`；创建/更新后重取详情，不能只改展示文本。
- 笔记：`noteId,content,displayContent,needCollapsed,likeCount,commentCount,collectCount,createdAt,author,question,userActions`。
- 评论：`commentId,noteId,content,likeCount,replyCount,createdAt,author,userActions,replies[]`，回复采用 parentId。
- 消息：type 1 点赞、2 评论、3 系统；sender 含 userId/username/avatar；target 可空，含 question.questionId/title。
- 热力图：`date,count,rank`；前三次数：`lastMonthTop3Count,thisMonthTop3Count`。
- 分类树：categoryId/name/parentCategoryId/children；普通题单 type=1，训练营 type=2。
- 注册后端允许账号和密码 6–32 位；旧 React 校验更严格，新登录组件按后端改。个人信息 username 1–16，school≤64，signature≤128，生日需过去日期。

## 建议接手顺序

1. 阅读本文件、`src/request`、`src/stores/session.ts`、保留服务和类型。不要再次进行整目录删除。
2. 补用户分类 API，完成 `style.css`；保持顶栏、白色卡片、左右分栏、后台侧栏等原布局思路，适配手机。
3. 实现笔记流/卡片、评论/回复、收藏选择与收藏夹组件，使用已存在 composables。
4. 按路由表完成用户页面，再完成后台 CRUD、批量导入与题单排序。
5. 每批执行 typecheck；全部模块落地后执行 build/lint；补有意义的请求契约、鉴权、分页/筛选、失败恢复测试，不用空壳掩盖缺功能。
6. 浏览器检查登录、题库筛选、笔记编辑、互动、个人资料、管理权限；有可用后端再联调，否则明确标明仅 mock 验证。
7. 扫描前端 React 残留、项目作者信息、代码注释、旧 IP、旧统计脚本。`node_modules`、第三方许可证不作作者清理对象；public/favicon.ico 未核验，当前 HTML 已不引用。
8. 更新本文件的状态和验证结果后交付。不能以“路由可打开”代替全部业务迁移完成。

## 变更记录

| 日期/批次 | 实际变更 | 验证 | 下一步 |
| --- | --- | --- | --- |
| 2026-09-16 / 基础迁移 | 梳理旧路由、接口、后端 DTO；替换工程配置、请求、状态、框架和基础组件；移除旧 React 源码 | 未构建，业务页面缺失 | 补齐业务组件与页面 |
| 2026-09-16 / 交接固化 | 新增根 AGENTS.md 与本文件；核实依赖落盘；删除不可重复执行的迁移脚本；修复 StatusPage script 语法 | typecheck 失败：13 个页面模块不存在；CSS 尚缺；无浏览器/联调验证 | 从共用笔记组件、用户分类接口和 CSS 开始 |
| 2026-09-16 / 共用业务组件 | 新增 NoteCard/NoteFeed、CommentNode/CommentsPanel、CollectionPicker、style.css；用户分类 API、评论编辑删除；修 dropdown/搜索空态与退出受限页 | typecheck 仅报原 13 个业务页面缺失，新增组件无类型错误；未运行浏览器 | 补用户页面再补后台 |
| 2026-09-16 / 用户页面 | 新增首页/题库/题单/题目详情/用户主页/个人中心/消息中心；ActivityPanel、QuestionRows、DifficultyTag、ProfileForm、CollectionsPanel；个人资料、头像、导出与消息操作 | typecheck 仅报 6 个管理页面缺失；尚未浏览器验证 | 实现管理端并统一验证 |
| 2026-09-16 / 管理页面与首轮构建 | 完成后台统计、用户筛选、分类 CRUD、题目 CRUD/Markdown 导入、题单 CRUD 和题目排序；补请求/会话/资源测试；修退出期间异步登录恢复竞态、笔记刷新保持评论抽屉 | 首轮 build 通过，lint 单个错误已修；测试与格式化进行中 | 完成测试/lint/浏览器验收 |
| 2026-09-16 / 收尾验证与真实联调 | format/typecheck/lint/test(13)/build 全部通过；核对具名组件注册完整；启动 Redis+后端(Java17, 8080)，经 dev 代理 5173→8080 验证分类/题目/详情/用户/笔记/搜索等真实数据与登录错误契约；确认前端无残留注释/React 依赖 | 后端 Started in 4.4s，MySQL+Redis 连接正常；主包 1030KB(gzip 325.7KB) | 删除死文件 .husky、可选 favicon；带邮箱注册需真实 SMTP；服务端强制鉴权属后端改动需另行授权 |
| 2026-09-17 / 后端作者与 kama 清理 | 删除 13 个文件 `@Author Tong`/`@author kama` 标签；删除 `backend/.idea/`、`backend/.gitignore`；数据库 `kamanote_tech`→`studynote_tech`（mysqldump 复制 16 表，行数一致），`application-dev.yaml` 改连接串 | `mvn clean package` 重建通过，target 无 kama 残留 | 待确认删旧库 kamanote_tech；包名/应用名/品牌名/硬编码域名/弱凭据待下一批改 |

后续 AI 每批追加记录，并同步更新顶部摘要、当前阻塞及剩余任务；已解决问题移入记录，避免让过时信息继续误导接手者。
