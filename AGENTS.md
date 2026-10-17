# AGENTS.md · kungal 第三方安卓客户端项目规则

## 项目是什么

kungal.com（鲲 Galgame 论坛）的安卓第三方客户端（非官方）。
Kotlin + Jetpack Compose；`core/`（纯 Kotlin 接口层，可离线单测）+ `app/`（安卓界面）。
目标先自用跑通，功能完整复刻网站；长期以开源第三方客户端为目标。

## 红线（不许碰）

- 不绕过登录、权限或任何门禁。客户端可见性与可操作性跟网页完全一致，一切如实呈现。
- 不做全站批量抓取 / 批量下载 / 离线镜像；请求保持克制。
- 写操作只复刻站点官方 API 请求（含 `Idempotency-Key`）；失败如实报错，不重试轰炸。
- 不冒用官方 App 身份（client_id、包名、名称、图标）；README 必须写明"非官方第三方客户端"。
- 官方源码（`research/reference/kun-galgame-forum`，AGPL-3.0）只作契约参考，**不复制任何代码**。

## 验收与证据

- `automated`（测试/构建）、`static-preview`、`real-host`（真机）、`driver`（驾驶员亲验）是四道独立的门，
  一道通过不能替代另一道；自动化不许标记 `driver-accepted`。
- 说"做完了"必须带证据：跑起来、测试通过或真机现象，不接受"应该没问题"。
- 涉及真机行为的功能，离线检查通过 ≠ 交付。

## 权威链（按此顺序读）

1. `总设计案.md` — 做什么、为什么、红线
2. `蓝图集/BLUEPRINT_INDEX.md` — 阶段合同、领域蓝图候选（BP-ROOT，candidate）
3. `NEXT.md` — 现在从哪里继续（唯一进度文档，只写这一份）

执行期不新增持久蓝图；真实问题只允许一层临时问题支线，解决后回归父步骤。

## 站点接口备忘（实现前先看）

- API base：`https://www.kungal.com/api/v1`；官方 OpenAPI：`research/reference/kun-galgame-forum/apps/api/openapi/kungal-v1.json`
- 分页：`{object:"list", items[], total, total_relation}`（cursor + limit≤100）；错误：RFC 9457 problem+json
- 会话：`kungal_session` cookie（WebView 官方登录后捕获）；401/205 = 会话失效 → 引导重登
- 写操作带 `Idempotency-Key`（UUID）；偏好类带 `If-Match`
- 图片：内容寻址 CDN `https://image.kungal.iloveren.link/<h0:2>/<h2:4>/<hash>.webp`
- 私信实时通道 = Centrifugo WebSocket（`POST /api/v1/chat/realtime-token`）

## 本机构建环境

- JDK 21：`D:\Java`；Android SDK：`D:\AndroidStudiozy`（见 `local.properties`）
- Gradle 用仓库自带 wrapper；依赖下载走本机代理 `127.0.0.1:7897`（用户级 gradle.properties，不进仓库）
- 常用命令：`./gradlew :core:test`、`./gradlew :app:assembleDebug`

## 沟通

- 面向用户一律大白话中文，先结论后依据；术语第一次出现解释其在本项目中的含义。
- 实测证据存放在 `research/probe/`（gitignored），引用时说明日期。
