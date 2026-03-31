# SecureMemo

Android 安全备忘录应用 | Android Secure Memo App

## 功能特点

- **图案密码锁屏**：3x3 网格图案解锁，数据安全无忧
- **AES-256-GCM 加密**：所有数据本地加密存储
- **四种记事类型**：普通笔记、Todo 清单、联系人、账号密码
- **WebDAV 同步**：支持坚果云、Nextcloud 等 WebDAV 服务
- **鸿蒙兼容**：minSdk 26，targetSdk 31，仅 arm64-v8a

## 技术栈

| 组件 | 技术 |
|------|------|
| Android | Java, minSdk 26, targetSdk 31 |
| 加密 | AES-256-GCM |
| 同步 | WebDAV |
| PC 端 | Aardio |

## 项目结构

```
SecureMemo/
├── app/                    # Android 端
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/securememo/app/
│       │   ├── crypto/     # AES-256 加密模块
│       │   ├── data/       # 数据模型、WebDAV
│       │   └── ui/         # 界面 Activity
│       └── res/            # 布局、资源文件
├── aardio/                 # PC 端（Aardio）
└── .github/workflows/      # 自动打包
```

## 使用说明

### Android 端
1. 在 Android Studio 中打开 `SecureMemo` 文件夹
2. Run → Run 'app' 或生成 APK
3. 首次使用：绘制图案密码（至少连接 4 个点）
4. 配置 WebDAV：设置 → 同步

### PC 端
1. 用 Aardio IDE 打开 `aardio/SecureMemo.aardio`
2. 按 F5 运行
3. 打开 Android 端生成的 vault.dat 文件

## 下载 APK

从 Actions 页面的 Artifacts 下载自动构建的 APK：
https://github.com/key996/SecureMemo/actions

## License

MIT
