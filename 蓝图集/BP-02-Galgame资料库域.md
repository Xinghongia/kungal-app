---
blueprint_schema: tavernweave/blueprint/v1
project_id: kungal-android-client
blueprint_id: BP-02
status: active
updated: 2026-10-17
parent: BP-ROOT
persistent_blueprint: true
---

# BP-02 · Galgame 资料库域

## 合同

把 App 的「Galgame」栏从入口占位补成网站同款的资料库：作品列表与检索、词条详情、
标签/会社/引擎/系列/角色/Staff/特质目录、收藏夹、封面投票、评分与游玩记录、
词条评论区、编辑提案与修订历史、新词条投稿。全部走站点官方接口，可见性与权限跟网页一致。

- 范围来源：`apps/web/app/pages/galgame/`、`pages/gallib.vue`、`pages/galgame-rating/`、
  `pages/collection/`、`pages/edit/galgame/`、`components/galgame/`；
  OpenAPI `works / library-works / tags / companies / engines / series / characters /
  credit-names / traits / ratings / wall-comments / collections / edit-proposals /
  work-submissions` 组。
- 依赖排序：BP-03（资源下载/工具集）与 BP-05（题库/月历/全站搜索）尚未交付。
  词条详情的「本体资源下载」「补丁资源下载」「题库」暂为如实占位并给浏览器兜底，
  对应域交付后接管；不删入口、不伪造空数据。
- 不属于本蓝图：资源详情/下载/过期报告（BP-03）、题库作答与收藏（BP-05）、
  发售月历（BP-05）、全站搜索（BP-05；资料库范围内的 `q` 检索属于本域）、
  站务/管理员审核队列（非普通用户面；普通用户能看到的「我的提案/我的投稿」在范围内）。
- 红线不变：不批量抓取；写操作带 `Idempotency-Key`；评分/投稿/编辑等写操作
  必须由驾驶员明确同意后线上验证；不绕过 NSFW 门禁；不冒用官方身份。

## 已核实事实（2026-10-17 OpenAPI + 官方源码只读核对）

### 作品列表

- `GET /works`：page-number；默认 `sort=resource_updated_desc`、`limit=24`。
  默认只列有本站资源的已发布作品；`include_resourceless=true` 列全部已发布作品。
  支持 `sort`（16 种）、`resource_type`、`resource_platforms[]`、`resource_languages[]`、
  `resource_runtimes[]`、`game_type`、`resource_providers[]`、`excluded_sole_providers[]`、
  `released_from/to`、`released_months[]`、`collected_from/to`、`collected_months[]`、
  `min_rating`、`min_rating_count`、`include_nsfw`、`include_all_original_languages`、
  `exclude_tag_ids[]`、`exclude_tag_trees[]`。
- `GET /library-works`：全量资料库（含无资源作品）；`sort=popularity_desc|released_desc|
  released_asc|updated_desc|relevance_desc`；支持 `q`、`released_from/to`、
  `include_nsfw`、`include_all_original_languages`、`exclude_tag_ids/trees`。
- `GET /works/collected-months`：收录月份列表（年降序、月升序），用于收录筛选。
- 语言门槛：原始语言非日语/中文的作品默认不出现，需 `include_all_original_languages=true`
  （网页不默认打开；客户端保持一致，筛选面板可提供开关）。
- 列表响应 `items[] WorkSummary + total + total_relation`；`WorkSummary` 含封面/横幅、
  maker、发售日期与精度、评分、like/view、资源轴、`is_nsfw`、`is_published`。
- 标签屏蔽：`GET /me/blocked-tags` 返回 `{id, mode: tree|exact}`；列表请求时网页把
  tree 的 id 放进 `exclude_tag_trees`、exact 放进 `exclude_tag_ids`，服务端不会自动应用。

### 词条详情

- `GET /works/{work_id}?include_nsfw=`：`Work` 含别名、多语言标题、简介 `intros[]`、链接、
  `external_refs`、`covers[]`（cover_slot + vote_count）、`screenshots[]`、`companies[]`
  （attribution_roles）、`engines[]`、`series[]`、`related_works[]`、`_tags[]`（含剧透档）、
  `credits[]` 分组、`roster[]`（角色+声优）、`resource_types/...`、`favorite_count`、
  `is_resource_publish_banned`、`is_resource_hosted_elsewhere`、`creator`/`contributors`、
  `external_ratings[]`、`playtimes[]`、`dlsite`、`viewer.has_liked`。
- 合并条目返回 409 `ENTITY_MERGED` + `current_id`；网页 301 到新 id，App 应自动跳转。
- NSFW 门禁：`is_nsfw` + 内容设置；模糊模式可进详情但打码封面/画廊，点击揭示；
  隐藏模式先过门禁。`include_nsfw=false` 会剔除成人标签。
- 封面投票：`GET /works/{work_id}/covers/{cover_id}`；`PUT/DELETE /works/{work_id}/
  covers/{cover_id}/vote`；一人一票，投别的封面会移动选票；自己的票用
  `GET /me/cover-votes?work_ids[]=` 批量读。网页在 `include_nsfw=false` 时隐藏
  `image.sexual=explicit` 的封面。
- 点赞：`PUT/DELETE /works/{work_id}/like`（`SELF_LIKE_FORBIDDEN`）。
- 收藏：作品可加入任意收藏夹；`GET /me/works/{work_id}/collections` 返回带 `has_work` 的选择器
  数据；`PUT/DELETE /collections/{collection_id}/works/{work_id}`；
  收藏夹 CRUD：`POST /collections`、`PATCH/DELETE /collections/{id}`（幂等键）。
- 作品页标签按 `spoiler` 分档显示；网页 SEO 只取 `spoiler=none`，App 可提供剧透显示开关。
- 资源发布禁止（版主）：`PUT/DELETE /works/{work_id}/resource-publish-ban`；
  `viewer.can_ban_resource_publish` 决定是否显示。

### 目录实体

- 标签：`GET /tags`（q/ids/分页）、`GET /tags/{id}`（父/子标签、祖先）、
  `GET /tags/{id}/descendants`、`GET /tags/{id}/works`（资源轴筛选与作品列表参数同 `/works`）。
  成人标签在 `include_nsfw=false` 时为 NOT_FOUND。标签屏蔽 `PUT/DELETE/GET
  /me/blocked-tags/{tag_id}`（mode 默认 tree，最多 100）。
- 会社：`GET /companies`（q/ids/company_kind）、`GET /companies/{id}`、
  `GET /companies/{id}/works`（via=own|imprint + 资源轴筛选）、
  `GET /companies/{id}/graph`（母子公司/品牌/更名关系节点与边）。
- 引擎：`GET /engines`、`GET /engines/{id}`、`GET /engines/{id}/works`。
- 系列：`GET /series`、`GET /series/{id}`（sample_works）、`GET /series/{id}/works`。
- 角色：`GET /characters`（q/trait_ids/trait_match/genders/sort）、`GET /characters/{id}`
  （立绘、画像、特质、简介、链接）、`GET /characters/{id}/appearances`（作品+声优，游标）。
- Staff：`GET /credit-names?q=`（搜索）、`GET /credit-names/{id}`（照片、生日、性别、
  同人名义）、`GET /credit-names/{id}/credits`（参与作品+职务+角色，游标）。
- 特质：`GET /traits`（q/ids/parent_id；无参数列根组）、`GET /traits/{id}`（父/子特质）。
- 收藏夹：`GET /users/{id}/collections`（公开）、`GET /me/collections`（含私有）、
  `GET /collections/{id}`、`GET /collections/{id}/works`（sort favorited/released）。
- 会社作品列表的 `via` 与资源轴筛选单向可用；实体页作品列表为 page-number。

### 评分与游玩记录

- `GET /ratings`：page-number，`sort=created|view|overall × asc/desc`；可按 `work_id`、
  `author_id`、`spoiler_level`、`play_status`、`game_type` 筛选；默认排除 NSFW 作品。
- `GET /ratings/{id}`（每次阅读计一次浏览）；`PATCH/DELETE /ratings/{id}` 仅作者；
  `PUT/DELETE /ratings/{id}/like`；`POST /ratings` 幂等键；一个用户对一部作品只有一条评分。
- `GET/PUT /works/{work_id}/record`：评分、游玩状态、本站游玩时长统一表单；
  PUT 整体替换，漏发的部分会被删除；`play_state=wish` 时网页禁止评分；
  新评分按短评长度给作者萌点；删除评分会收回萌点（本站首次评分的）。
- 评分详情带 `likers[]`（最近 50）与 `comment_count`。
- 页面：`/galgame-rating` 列表（筛选/排序/分页）、`/galgame-rating/{id}` 详情
  （评分内容、八维雷达、点赞、评论墙）。
- 评论墙（通用）：`GET/POST /wall-comments`（subject_type=galgame|galgame_rating|…），
  游标分页；`GET/PATCH/DELETE /wall-comments/{id}`、`PUT/DELETE .../like`、
  `POST .../flags`、`GET .../source`；回复用 `parent_comment_id`，两层展示按
  `root_comment_id` 分组；被审核中的评论仅作者可见；删除是墓碑。
  字数上限：galgame 5000、rating 1314、其他 1007。写操作幂等键。
- 评分编辑需先读 `GET /works/{work_id}/record`；保存 PUT 后返回最新记录。

### 编辑提案与修订历史

- `GET /works/{work_id}/edit-form`：`field_values`（按 `catalog.work.*` 键）+ `fields[]`
  （field_type/diff_hint/vocabulary/encoding/base/is_nullable/max_elements/element）+
  `vocabularies[]`。字段类型：text / i18nmap / enum / int / date / list / ref / imagehash。
- `POST /works/{work_id}/edit-proposals`（幂等键，patch + note）：可信账号或创建者可能立即
  `merged`，否则进审核；`GET /works/{work_id}/edit-proposals?state=open` 读待审；
  `GET/PATCH /edit-proposals/{id}`（withdrawn/merged/declined，If-Match）；
  `POST /edit-proposals/{id}/amendments`（修正，幂等键）；`GET /me/edit-proposals`。
- 修订：`GET /works/{work_id}/edit-revisions`（page）、`GET .../edit-revisions/diff?
  from_seq=&to_seq=`、`POST /works/{work_id}/edit-reverts`（幂等键，恢复到某 seq）。
- 表单分组（网页）：标题 / 介绍 / 基本信息 / 关联条目（系列/标签/会社/引擎）/
  出演与署名（roster/credits/隐藏项）/ 链接 / 图片（封面/画廊）。复杂控件：
  多语言标题、多语言简介、出演名单、本站署名、图片上传（`POST /work-edit-images`，
  multipart：`file` + `preset=cover|screenshot`，≤10MiB，返回 `Image` hash）。
- 编辑权限提示：`viewer.can_decide` 决定审阅按钮；审核队列需要权限
  `galgame.edit_proposal.review`（`GET /me/permissions`）；普通用户只见「我的提案」。

### 投稿（新词条）

- `GET /work-submission-candidates?q=`：投稿前查重（游标，无 total）。
- `POST /work-submissions`（幂等键）：titles[]（locale+title）、aliases[]、
  introductions[]、original_language、content_rating、is_nsfw、release_date、
  release_date_precision、banner_hash、is_duplicate_confirmed。
  同名已发布作品返回 409 `DUPLICATE_SUSPECTS`；用户确认后重发 `true`。
- `GET /me/work-submissions`（状态过滤）、`GET/PATCH/DELETE /work-submissions/{work_id}`
  （submitter 可 pending/draft；撤回/删除草稿；If-Match）。
- 投稿状态 `live|draft|pending|declined|hidden`；审核队列（staff）不在普通用户范围。

### 通用事实

- 图片 URL 已由服务端给绝对地址（内容寻址 CDN）；客户端仍走现有图片组件。
- 错误为 problem+json；429 有 Retry-After；401/205 = 会话失效。
- 所有 page-number 集合 `total_relation` 可能是 `gte`（达到统计深度上限）。

## 交付批次与退出条件

### U1 资料库浏览（`/works` + `/library-works`）

**状态：已交付（代码 + 单测 + 构建 + 装机，2026-10-17）**
见 `docs/devlog/2026-10-17-BP02-U1资料库浏览.md`。

- core：`GalgameApi` 列表两套 + 收录月份 + 标签屏蔽读取；模型 `WorkSummary` 补齐字段；
  查询参数建模（多值轴、排序、NSFW、语言门槛、排除标签）。
- app：「Galgame」tab 入口接真实页面：资源资料库（默认有资源）、信息资料库（全量）、
  排序/方向、平台/模拟器/游戏类型/资源类型/语言筛选、更多筛选（评分/年份/月份/收录/提供商）、
  筛选 chips 与清空、分页、作品卡片（封面/NSFW 模糊/评分/会社/发售）、空态、加载态。
- 标签屏蔽在列表请求中生效（已有屏蔽从网页同步）。
- 退出条件：匿名与登录下列表数据与网页一致（抽样对照）；筛选组合生效；翻页正确；
  NSFW 姿态切换后列表按设置重载。

### U2 词条详情与封面投票

- core：`getWork`、点赞、封面读/投票/我的投票、收藏夹选择/加入/移除、封面投票批量读。
- app：详情页全部只读区块（头部、别名、资源轴、评分条、游玩时长聚合、简介、链接、
  Staff 分组、角色名单（剧透/NSFW 处理）、画廊、系列、关联作品、会社/引擎/信息栏、标签、
  贡献者）；Like；收藏夹选择弹窗（可新建）；封面画廊（按 slot 分组、投票、NSFW 过滤/模糊）；
  NSFW 门禁与模糊揭示；`ENTITY_MERGED` 自动跳转。
- 占位：本体资源/补丁/题库 tab 显示「BP-03/BP-05 接入」与浏览器入口，不伪造数据。
- 退出条件：真实词条各区块与网页一致（抽样 3 部，含 1 部 NSFW、1 部多会社/系列）；
  点赞/收藏/投票（驾驶员同意后）线上生效；合并跳转正确。

### U3 目录实体与收藏夹

- core：标签/会社/引擎/系列/角色/Staff/特质 API 与模型；标签屏蔽写；收藏夹 CRUD 与作品成员。
- app：8 个目录列表与详情（含会社关系图、标签树/屏蔽、角色特质筛选、Staff 参与作品、
  收藏夹详情与作品列表、我的收藏夹管理）；目录页的作品列表接入 U1 的筛选/分页组件。
- 退出条件：每类目录列表与详情抽样对照网页；收藏夹创建/改名/可见性/删除/移出作品
  （驾驶员同意后）生效；标签屏蔽后列表排除生效。

### U4 评分、游玩记录与评论墙

- core：`RatingApi`（列表/详情/创建/修改/删除/点赞）、`WorkRecord` 读写、
  `WallCommentApi`（通用墙：列表/发帖/编辑/删除/点赞/举报/源文）；模型完整。
- app：评分列表（筛选/排序/分页）、评分详情（雷达图/推荐度/短评/剧透/点赞/作者）；
  评分表单（总体分、推荐度、游戏类型、剧透档、短评、八维高级评分、游玩状态/时长，
  想玩不可评分、删除评分确认）；作品页评论墙（两级回复、点赞、编辑、删除、举报、
  剧透/NSFW 处理、分页加载、作者本人可编辑）；我的评分入口（用户主页已有列表）。
- 退出条件：评分列表与详情抽样一致；评分创建/修改/删除（驾驶员同意后）生效且与网页同步；
  评论墙发帖/回复/点赞/删除（驾驶员同意后）生效；被审核评论仅作者可见。

### U5 编辑提案与修订历史

- core：`EditForm` 读取、提案提交/撤回/修正/审阅、我的提案、修订列表/diff/回滚、
  编辑图片上传（multipart）。
- app：动态编辑表单（覆盖全部字段类型与分组；多语言标题/简介、出演名单、署名、
  实体选择器、图片上传与排序）；待审提案卡片（我的/可审）；提案详情与审阅
  （合并/拒绝需说明/修正）；修订时间线、版本对比、回滚；权限控制（`/me/permissions`）。
- 退出条件：表单能完整回显真实 `edit-form` 并提交一个最小字段修改提案（驾驶员同意后）；
  提案撤回/审阅与网页一致；修订 diff 与回滚（驾驶员同意后）正确。

### U6 投稿与我的提交

- core：候选搜索、创建投稿、我的投稿列表、投稿详情/状态变更/删除。
- app：投稿向导（搜索查重 → 已有作品跳详情 → 新建申请：多语言标题/别名/简介、
  原始语言、年龄分级、NSFW 展示、发售日/待定、横幅上传、重复确认）；我的提交
  （状态、拒绝原因、继续编辑/撤回/删除）。
- 退出条件：候选搜索与网页一致；新建投稿的重复拦截与确认流程正确；
  一次真实投稿由驾驶员操作或明确同意后验证。

## 技术与证据要求

- core 全部新接口离线单测（MockWebServer + 脱敏真实夹具）；分页模型复用 `PagedList`；
  游标集合新增通用 `CursorList`（若现有模型不足）。
- NSFW：所有列表/详情/实体/评分接口按当前内容姿态传 `include_nsfw`；
  模糊模式对封面/截图/角色图打码并可揭示。
- 写操作：幂等键统一 UUID；失败如实展示 problem+json；不做自动重试轰炸。
- app 每批 `assembleDebug` 通过；core + app 单测全绿；每批装机。
- 证据四道门独立：automated / static-preview / real-host / driver；自动化不标 `driver-accepted`。
- 文档：每批更新 `NEXT.md` 与 `docs/devlog/`；线上残留（如有）必须列明。

## 失败回退

- 作品 404 / `ENTITY_MERGED` / `PROFILE_SIGN_IN_REQUIRED` 等按网页文案如实展示。
- 编辑表单词汇表读取失败（`vocabularies=[]`）→ 对应字段只读，照常展示提示。
- 投稿重复（`DUPLICATE_SUSPECTS`）→ 展示候选并二次确认，绝不静默重发。
- 评分/评论被限流（429）→ 提示稍后再试，保留草稿。
- 实体图/关系图渲染失败 → 降级为列表，不丢数据。

## 停止条件

- 站点把资料库读取改为需要新身份或非公开接口 → 带证据回驾驶员门。
- 出现需要绕过权限、批量抓取或冒用身份才能继续的情况 → 立即停止。
- 写操作造成线上可见内容（评分/评论/提案/投稿）前，必须先取得驾驶员明确同意。
