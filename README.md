# NovaMusic

一款优雅的 Android 本地音乐播放器，基于 **Jetpack Compose** + **Material 3** 构建。

## 功能特性

- **音乐库管理** — 自动扫描本地音频文件，提取元数据（封面、艺术家、专辑）
- **多种视图** — 列表 / 网格视图切换，支持按名称/艺术家/时长排序
- **播放控制** — 顺序播放/随机播放/单曲循环，滑动切歌
- **播放列表** — 创建、编辑、删除播放列表
- **收藏与历史** — 一键收藏歌曲，查看最近播放记录
- **动态主题** — Material You 动态配色，浅色/深色/纯黑模式
- **桌面小部件** — 2×2 和 4×2 两种尺寸，控制播放
- **睡眠定时器** — 自动停止播放
- **通知栏控制** — MediaStyle 通知，前台服务

## 技术栈

| 层级 | 技术 |
|------|------|
| UI | Jetpack Compose, Material 3 |
| 架构 | MVVM + Clean Architecture |
| DI | Hilt |
| 数据库 | Room |
| 播放器 | Media3 / ExoPlayer |
| 图片 | Coil, Palette API |
| 状态 | DataStore Preferences |
| 异步 | Kotlin Coroutines + Flow |

## 构建

```bash
# 安装 JDK 17 并设置 JAVA_HOME
export JAVA_HOME=/path/to/jdk-17

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

Debug APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 项目结构

```
app/src/main/java/com/novamusic/
├── data/
│   ├── local/         # Room 数据库、DAO、Entity
│   ├── repository/    # Repository 实现
│   └── scanner/       # 音频扫描与元数据提取
├── di/                # Hilt 依赖注入模块
├── domain/
│   ├── model/         # 领域模型
│   ├── repository/    # Repository 接口
│   └── usecase/       # UseCase
├── presentation/
│   ├── common/        # 通用组件（迷你播放器）
│   ├── favorites/     # 收藏页
│   ├── history/       # 历史记录页
│   ├── library/       # 音乐库主页
│   ├── navigation/    # 导航
│   ├── player/        # 播放器页面
│   ├── playlist/      # 播放列表
│   ├── settings/      # 设置页
│   ├── theme/         # 主题系统
│   └── widget/        # 桌面小部件
└── service/           # MusicService、播放状态管理
```
