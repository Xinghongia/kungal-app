---
handoff_schema: tavernweave/next/v1
project_id: kungal-android-client
status: bp01-delivered-driver-testing
updated: 2026-10-17
---

# NEXT · kungal 第三方安卓客户端

## 当前权威

- 总设计案：`总设计案.md`（driver-approved 2026-10-17）
- 活动蓝图：`蓝图集/BLUEPRINT_INDEX.md`（BP-ROOT）
- 执行期持久权威蓝图预算：`0`
- 临时问题支线：仅真实问题触发，最多一层/同时一条，关闭后回归父步骤

## 已确认事实（2026-10-17）

- 站点 = 鲲 Galgame 论坛（kungal.com）；Go Fiber v3 API（同域 `https://www.kungal.com/api/v1`，
  官方 OpenAPI 368 个 operation）+ Nuxt4/Vue3 SSR 前端；仓库 KunMoe/kun-galgame-forum（PUBLIC，AGPL-3.0）
- 登录 = OAuth（账号统一在 account.nextmoe.com）；会话 = `kungal_session` cookie（90 天滑动续期）；
  Bearer 白名单 client_id 属官方 App，第三方不得冒用
- 官方 App 未发布（全平台「即将推出」、仓库私有）→ 第三方客户端定位成立（驾驶员已确认照做）
- 匿名可读实测（`GET /api/v1/topics` 200）；API 无验证码/CSRF/UA 校验；Cloudflare 在最外层
- **命名 = kungal-app**（包名 `com.kungal.app`，显示名 "kungal"）——驾驶员 2026-10-17 定案
- **官方化方向（C9）**：驾驶员日后联系站长争取本项目成为官方客户端（站长欢迎开源项目）；
  达成前保持非官方声明、不冒用身份；达成后可申请官方 client_id + 原生 OAuth 登录
- 设计提案 P1–P4 已随设计确认生效（blueprint-set / First Playable 合同 / 技术选型 / 参考源码位置）

## 最近证据

- 2026-10-17 设计确认门通过：驾驶员「不错不错」+ 定名 kungal-app
- 2026-10-17 官方源码浅克隆在 `research/reference/kun-galgame-forum/`（gitignored，仅作契约参考）
- 2026-10-17 **P1 关闭**：构建通过 + Git 授权（私有仓库 https://github.com/Xinghongia/kungal-app）+ 首提交 bb8ae3f
- 2026-10-17 **P2 关闭**：core API 客户端/会话/模型 + **21 项单测全绿**；提交 62885b7 已推送
  （真实样本夹具 `core/src/test/resources/fixtures/`；样本原件 `research/probe/2026-10-17/`）
- 2026-10-17 **P4 驾驶员验收通过**（驾驶员实测登录/点赞/回复/界面，「实测还不错」）
- 2026-10-17 **整体架构完成（v0.2.0，845bdc3）**：底部五栏导航（话题/Galgame/发现/消息/我的，
  对应官方侧边栏全部条目）、发布 FAB、设置页、主题体系（6 套预设+鲲蓝默认/动态取色/深浅/纯黑）、
  我的页账号数据（/me、/me/account）；未交付功能为如实占位页；core 32 项单测全绿；
  详见 `docs/devlog/2026-10-17-架构导航主题设置.md`
- 2026-10-17 **内容分级（NSFW）交付**：根因 = v1 列表只有显式 `include_nsfw=true` 才返回
  NSFW 话题（服务端不读 cookie/账号偏好）；新增设置页「内容」隐藏/模糊/显示三态：
  - 登录：读 `/me/account` 的 `content_stance`，写 `PUT /me/nsfw-display`（跟随 NextMoe 账号）
  - 未登录：本机 DataStore（默认隐藏，与网页匿名默认一致）
  - 话题列表按姿态传 `include_nsfw` 并自动重载；模糊模式打码 NSFW 封面、点击揭示；
    隐藏模式下直接打开 NSFW 话题先过门禁
  - 证据：core 38 + app 6 = **44 项单测全绿**；`assembleDebug` 通过；真机实测把本地内容设置
    切到「显示」后，话题列表出现带「NSFW」标签的话题（未登录状态）
  - 详见 `docs/devlog/2026-10-17-内容分级NSFW.md`
- **驾驶员新约定（2026-10-17）**：不要每步都真机测试截图，驾驶员自己会测；只对重要功能做最小真机验证
- 2026-10-17 **BP-01 话题与讨论域交付**（代码完成，驾驶员已测「基本没啥问题」）：
  发帖/编辑/草稿、图片上传、楼中楼评论、33 种反应、引用/编辑/删除/置顶/最佳答案、
  收藏/订阅/推/隐藏/已读、投票、抽奖、关注动态、话题搜索、我的订阅；
  core 53 + app 6 = **59 项单测全绿**，`assembleDebug` 通过；
  详见 `蓝图集/BP-01-话题与讨论域.md` 与 `docs/devlog/2026-10-17-BP01话题域全量.md`
- 2026-10-17 驾驶员反馈已修：详情页回复框默认收起、搜索框椭圆并与搜索按钮同行
- **教训（写入记忆）**：不得擅自在驾驶员账号上做真实发布/回复等写操作；写验证先问或由驾驶员操作
- **线上残留**：测试话题 4482「不怕——smoke——」已隐藏（普通账号无法彻底删除）

## 开放风险

- WebView 登录捕获 cookie 的稳定性（`kungal_session` 为 HttpOnly+Secure，需在 WebView CookieManager 内捕获；
  若站点改机制需带证据回门）
- Centrifugo 实时聊天（私信）是最后一块硬骨头，排在 BP-04
- Markdown 渲染（KaTeX/代码高亮/spoiler/表情包）方案在 P2/P3 定选型
- AGPL 边界纪律：参考源码只读，不复制代码（红线 C6）

## 下一道门

1. BP-01 已交付（代码 + 单测 + 真机主路径）；驾驶员的体验反馈已修。
2. **待驾驶员确认本批 UI 修正后提交 GitHub**。
3. **下一个工作项 = BP-02 Galgame 资料库域**（按蓝图集顺序）：
   词条浏览、标签/会社/引擎/系列/角色、八维评分、编辑提案与历史、投稿。
   进入时先提取 `蓝图集/BP-02-Galgame资料库域.md`。
4. 设置面板其余项目候选（未排期）：屏蔽标签（依赖 BP-02）、全局圆角/背景图/页面透明度、
   主页 feed 偏好、云端偏好 `/me/preferences`（If-Match）。
5. 每批证据：core 单测 + assembleDebug；重要新协议（首次打通）做一次最小真机验证，
   其余由驾驶员自行走查。

## 一句续接

读取总设计案、蓝图集索引和本文件；BP-01 已完成，下一步提取 BP-02 领域蓝图，
或先处理驾驶员对本批 UI 的反馈。
