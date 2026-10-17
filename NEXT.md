---
handoff_schema: tavernweave/next/v1
project_id: kungal-android-client
status: p4-driver-acceptance-pending
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
- 2026-10-17 **P3 代码完成**（提交 0b8456f）：core 写通道（反应/发回复/浏览计数，27 项测试全绿）；
  app 首版界面（列表/详情/WebView 登录/正文树渲染/点赞与回复）——**匿名读闭环真机实测通过**
  （截图 `research/probe/2026-10-17/smoke_*.png`）；详见 `docs/devlog/2026-10-17-P3首版界面.md`
- 日志三份：`docs/devlog/2026-10-17-P1工程骨架.md` / `-P2接口与会话层.md` / `-P3首版界面.md`

## 开放风险

- WebView 登录捕获 cookie 的稳定性（`kungal_session` 为 HttpOnly+Secure，需在 WebView CookieManager 内捕获；
  若站点改机制需带证据回门）
- Centrifugo 实时聊天（私信）是最后一块硬骨头，排在 BP-04
- Markdown 渲染（KaTeX/代码高亮/spoiler/表情包）方案在 P2/P3 定选型
- AGPL 边界纪律：参考源码只读，不复制代码（红线 C6）

## 下一道门

1. **P4 驾驶员真机验收门（当前）**：debug APK 已装到驾驶员手机（adb 实测时的设备）。
   走查清单（按 First Playable 合同）：
   - 话题列表滚动/触底加载/下拉刷新
   - 点开话题 → 正文图片/排版 → 回复列表
   - **登录**：点「点赞」或底部回复框 → 弹出网页登录 → 用驾驶员账号登录 → 自动返回
   - **点赞**：点一下变红并 +1 → 再点取消 → 数字回落（真实生效）
   - **发回复**：输入内容 → 发送 → 出现在回复列表（真实生效；请用一句无营养的测试回复即可）
   - 失败路径：飞行模式下打开列表 → 应显示"网络连接失败"与重试按钮
2. 走查通过 → P4 关闭 → 按 BP-01（话题与讨论域全量）开始 Growth Tracks
3. 若有任何观感/交互问题，直接说，属 P4 修复范围（不得扩范围）

## 一句续接

读取总设计案、蓝图集索引和本文件，先向驾驶员要 Git 授权，然后完成 P1 构建验证、进入 P2。
