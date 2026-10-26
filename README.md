# 枫糖空集

枫糖空集（Maple Nilset）是一个完全本地化的 Android 客户端。它不包含 Nilset 服务端登录、账号系统或远程课表能力；应用数据保存在应用私有存储中。BiliNil 仍按其自身需要访问 B 站公开接口与资源，但这不属于 Nilset 服务端交互。

## 功能

- **课表**：本地课程创建、编辑、删除，按星期筛选，问候栏显示下一节课。
- **日历**：独立月历视图，支持月份切换和今天高亮。
- **控制台**：内部指令、输入补全、进程内历史与会话日志。
- **随心记基础**：Markdown 文件读写能力已接入，当前主页文本为占位展示。
- **主题**：Material 3 动态界面、自定义配色、自定义字体、自定义背景、卡片描边与透明度。
- **BiliNil**：封面下载、视频下载、清晰度解析、下载任务管理和 B 站 WebView Cookie 登录。
- **关于**：显示版本、版权、源码地址和 GPL-3.0 授权说明。

## 构建

环境要求：

- JDK 17 或更新版本
- Android SDK（compileSdk 37）

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Release 检查：

```powershell
.\gradlew.bat clean testDebugUnitTest lintRelease assembleRelease
```

应用包名为 `com.mapherdf.maplenilset`，当前版本为 `1.0.0`。

## 源码与许可

- 源码：<https://github.com/Mapher_df/maple-nilset>
- 许可证：GPL-3.0-only
- 版权：Copyright © 2026 Mapher_df
- 图标来源：<https://yesicon.app/zh-Hans/ic?k=tv>

完整许可证文本见 [LICENSE](LICENSE)。
