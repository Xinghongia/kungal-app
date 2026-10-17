---
blueprint_schema: tavernweave/blueprint-index/v1
project_id: kungal-android-client
blueprint_id: BP-ROOT
status: candidate
runtime_persistent_blueprint_budget: 0
updated: 2026-10-17
---

# kungal 第三方安卓客户端 · 蓝图集索引

## 总体阶段与依赖

阶段只能顺序进入；每个阶段进入时冻结其合同，退出时只更新证据与结果；不得跳阶段。

| 阶段 | 输出 | 允许修改范围 | 退出条件 | 证据 |
|---|---|---|---|---|
| P1 工程骨架 | Gradle 工程（:core + :app）、可安装 debug APK、AGENTS.md 与权威链文件 | 项目根、`core/`、`app/`、`.tavernweave/` | `assembleDebug` 成功；权威文件验证器通过 | automated |
| P2 接口与会话层 | core：API 客户端（分页/错误映射/幂等键）、WebView 登录会话捕获与持久化、话题/回复读取模型 + 离线单测 | `core/` | 单测全绿（样本来自真实响应，脱敏） | automated |
| P3 首版界面 | app：话题列表/详情/登录页/Markdown 渲染/点赞或回复写通道 | `app/`（+ core 必要写接口） | 编译通过；静态产物；真机主路径 | static-preview + real-host |
| P4 首版真机验收 | 驾驶员手机完整主路径体验 | 只许修复，不得扩范围 | 驾驶员亲手跑通并确认 | real-host + driver |

P4 通过后 → 按领域蓝图候选（下方）逐个提取并交付；不得跳过 P4 直接开工。

## 已声明蓝图

| ID | 文件 | 状态 | 说明 |
|---|---|---|---|
| BP-ROOT | 本文件 | candidate | 全局阶段、跨域接口、停止条件 |

## 领域蓝图候选（不预建；驾驶员在对应门确认后提取）

| 候选 ID | 领域 | 触发条件 | 主要输入 |
|---|---|---|---|
| BP-01 | 话题与讨论域（分类/版块/发帖/嵌套评论/reactions/投票/抽奖/草稿/关注动态） | P4 通过后 | OpenAPI topics/replies/comments/polls/lotteries 接口组 + 前端页面清单 |
| BP-02 | Galgame 资料库域（词条/评分/编辑提案与历史/投稿） | BP-01 交付后 | OpenAPI works/tags/companies/engines/series/characters + edit-proposals 组 |
| BP-03 | 资源与工具集域（资源/下载/过期报告/工具集） | BP-02 交付后 | OpenAPI galgame-resources/toolsets 组 |
| BP-04 | 用户中心与消息域（用户主页/积分/商城/通知/私信实时聊天） | BP-03 交付后 | OpenAPI me/users/messages/chat relay + Centrifugo |
| BP-05 | 搜索、排行、月历、题库、情报、设置域 | BP-04 交付后 | OpenAPI search/rankings/release-calendar/quizzes/news/docs + /me/preferences |

## 活动阶段合同

当前活动阶段：**无（等待驾驶员设计确认门）**。

任意活动步骤只允许由真实问题触发的一层临时问题支线（绑定父步骤、保存触发证据、
`persistent: false`、不扩大产品范围），解决或确认阻塞后关闭并回到父步骤。

## 跨域接口（各领域蓝图共享的稳定边界）

- **会话通道**（core session）：唯一鉴权源 = `kungal_session` cookie；任何域不得自建登录方式
- **API 客户端**（core api）：统一 base url、分页形状、problem+json 错误映射、幂等键、限流退避
- **内容渲染**（core markdown）：Markdown 源文 → 界面块的唯一渲染管线（正文/回复/评论/题库共用）
- **图片管线**：内容寻址 CDN 拼址、缩略/原图、加载占位与失败兜底
- **分页模型**：复用 saku `PageWindow` 窗口模型（已加载→滚动 / 相邻→追加 / 跳远→重开）
- **设计系统**：Material 3 主题、深浅色、设置存储（复用 saku 经验）

## 失败回退

- 真实问题（编译错误、接口变化、依赖不可达等）允许建立一层临时问题支线；解决后回归父步骤
- 同一问题修复 2–3 轮仍不稳 → 停下复盘，考虑换实现路线而非继续打补丁
- 接口与实测样本冲突（站点改版）→ 重新抓样本、更新测试夹具，不改产品范围
- WebView 登录链路若被站点阻断（如风控）→ 停下，带证据回驾驶员门重新讨论登录形态

## 停止条件

出现以下情况必须停下、更新 `NEXT.md` 并回到驾驶员门：

- 阶段退出条件不清或需要变更首版合同
- 新事实推翻已确认项目类型或红线（例如"必须绕过验证才能继续"）
- 需要安装/卸载软件、密钥、付费调用、真实外部 POST、Git 动作或破坏性操作
- 自动化证据与真机证据互相矛盾
- 站点明确反制第三方客户端

## 下一道门

驾驶员设计确认门（见 `总设计案.md` 末尾）。确认后从 P1 工程骨架开始。
