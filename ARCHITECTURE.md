# 枫糖空集架构

## 总览

枫糖空集是单 Activity、Jetpack Compose 和 Material 3 的本地 Android 客户端。导航使用 Navigation Compose；状态层使用 ViewModel + `StateFlow`；持久化使用 DataStore 与应用私有文件。

本项目没有 Nilset 服务端客户端、会话系统、远程课表、用户身份或多用户课表查看。本地课表不属于任何账号，应用内也没有用户卡或登录入口。

## 分层

- `ui/`：Compose 页面、页面组件和 ViewModel。
- `core/`：不依赖 Android UI 的业务模型、指令系统、主题模型和日志模型。
- `data/`：仓库实现、DataStore、本地课表、Markdown、BiliNil 数据源和下载适配。
- `const/`：跨层共享的路线、存储键和表达式常量。
- `di/`：Hilt 模块和仓库绑定。
- `lib-bilidownload/`：BiliNil 的独立下载库，承载 API、WBI、Cookie、分流、任务引擎、合并和导出。

## 主要模块

### 课表

- `LocalCalendarRepository` 是唯一 `CalendarRepository` 实现。
- 课程记录序列化到 DataStore；没有本地文件时表现为空课表，不触发网络请求。
- `ScheduleViewModel` 负责校验课程草稿、保存/编辑/删除、星期筛选、排序和计算下一节课。
- `ScheduleCourseSelector` 提供问候语、当前星期课程、下一节课和时间格式化等纯函数。

### 日历

- 日历视图与课表数据完全分离，只用于月历浏览。
- 月份状态是纯 Kotlin 状态，便于单元测试；UI 只负责 7 列网格、切换和标记。

### 控制台

- `NilSetCommand` 定义命令，`CommandRegistry` 收集命令，`NilSetCommandCenter` 负责执行和补全。
- 当前本地指令包括清空控制台和清空本地课表；`clear:data` 仅在 debug 构建可见。
- 控制台历史保存在进程内，日志文件写入应用私有目录。

### 主题与设置

- `ThemeRepository` 保存主题模式、配色、文字缩放、界面缩放、卡片描边、卡片透明度、自定义字体和背景。
- 设置页只包含本地设置、附属设置页和关于页入口。
- 关于页读取 `BuildConfig.VERSION_NAME`，并展示 GPL-3.0 源码可用性说明。

### Markdown

- `MarkdownRepository` 支持应用私有 Markdown 文本读写。
- 当前主页 End Poem 是占位展示；后续随心记编辑器会复用该仓库。

## BiliNil

BiliNil 保留独立网络能力：

- 封面和视频信息来自 B 站公开 API；媒体下载使用 B 站 CDN。
- Cookie 按域名隔离，凭据保存在加密存储中，不进入日志或普通 UI 状态。
- 视频下载优先 AVC/HEVC；仅有 AV1 时按清晰度降级，最终仍只有 AV1 时保存音视频分离文件。
- 导出使用 MediaStore，封面和视频分别进入 `Download/Nilset/Cover` 与 `Download/Nilset/Video`。

BiliNil 的登录是 B 站账号登录，不是 Nilset 用户登录；移除 Nilset 账号体系不改变这部分功能。

## 当前状态

- Phase 0-6：本地架构、Compose、控制台、主题、日历、课表和 BiliNil 基础能力已落地。
- Phase 7：发布工程化仍在完善，当前保留 debug 回退签名和基础 CI/Release workflow。
