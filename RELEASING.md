# Maple Nilset Release Guide

## 本地签名

从仓库根目录创建 keystore。`keytool` 由当前 JDK 提供：

```powershell
New-Item -ItemType Directory -Force -Path .\keystores
keytool -genkeypair -v `
  -alias maple-nilset-release `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10950 `
  -keystore .\keystores\maple-nilset-release.jks
```

不要在命令行或配置库中写入真实密码。复制模板并填写本地值：

```powershell
Copy-Item .\keystore.properties.example .\keystore.properties
```

可用属性：

```properties
MAPLE_NILSET_RELEASE_STORE_FILE=keystores/maple-nilset-release.jks
MAPLE_NILSET_RELEASE_STORE_PASSWORD=your-real-keystore-password
MAPLE_NILSET_RELEASE_KEY_ALIAS=maple-nilset-release
MAPLE_NILSET_RELEASE_KEY_PASSWORD=your-real-key-password
```

环境变量优先级高于属性文件：

- `MAPLE_NILSET_RELEASE_STORE_FILE`
- `MAPLE_NILSET_RELEASE_STORE_PASSWORD`
- `MAPLE_NILSET_RELEASE_KEY_ALIAS`
- `MAPLE_NILSET_RELEASE_KEY_PASSWORD`

任一签名配置缺失时，release 构建回退到 debug 签名，只适合本地验收，不适合正式分发。

## 构建验证

```powershell
.\gradlew.bat clean testDebugUnitTest lintRelease assembleRelease
```

检查产物和哈希：

```powershell
Get-ChildItem .\app\build\outputs\apk\release\*.apk
Get-FileHash .\app\build\outputs\apk\release\*.apk -Algorithm SHA256
```

## CI 发布

推送 annotated tag 后，`Android Pre-release` workflow 会构建 release APK 并创建 GitHub release。

建议正式版本格式：

```powershell
git tag -a v1.0.0 -m "release: Maple Nilset 1.0.0"
git push origin v1.0.0
```

CI 需要配置以下 GitHub secrets：

- `MAPLE_NILSET_RELEASE_KEYSTORE_BASE64`
- `MAPLE_NILSET_RELEASE_STORE_PASSWORD`
- `MAPLE_NILSET_RELEASE_KEY_ALIAS`
- `MAPLE_NILSET_RELEASE_KEY_PASSWORD`

缺少 keystore base64 或密码时，workflow 产物会回退为 debug 签名。
