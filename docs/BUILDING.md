# Building / 构建

## English

The mod version is defined once in the root `gradle.properties`. Minecraft 1.21.x uses Yarn mappings and Java 21; Minecraft 26.x uses the unobfuscated game and Java 25. The wrapper pins Gradle 9.5.1 and verifies its download checksum.

| Minecraft | Project | Java | Fabric API |
| --- | --- | --- | --- |
| 1.21.1 | versions/1.21 | 21 | 0.116.17+1.21.1 |
| 1.21.2 | versions/1.21.2 | 21 | 0.106.1+1.21.2 |
| 1.21.3 | versions/1.21.2 | 21 | 0.114.1+1.21.3 |
| 1.21.4 | versions/1.21.4 | 21 | 0.119.4+1.21.4 |
| 1.21.5 | versions/1.21.5 | 21 | 0.128.2+1.21.5 |
| 1.21.6 | versions/1.21.6 | 21 | 0.128.2+1.21.6 |
| 1.21.7 | versions/1.21.6 | 21 | 0.129.0+1.21.7 |
| 1.21.8 | versions/1.21.6 | 21 | 0.136.1+1.21.8 |
| 1.21.9 | versions/1.21.9 | 21 | 0.134.1+1.21.9 |
| 1.21.10 | versions/1.21.9 | 21 | 0.138.4+1.21.10 |
| 1.21.11 | . | 21 | 0.141.4+1.21.11 |
| 26.1 | versions/26.1 | 25 | 0.145.1+26.1 |
| 26.1.1 | versions/26.1 | 25 | 0.145.4+26.1.1 |
| 26.1.2 | versions/26.1 | 25 | 0.155.3+26.1.2 |
| 26.2 | versions/26.2 | 25 | 0.160.0+26.2 |

Set `JAVA_HOME` to the matching JDK and select one exact target:

```sh
./gradlew -p versions/1.21 '-Pminecraft_version=1.21.1' build
./gradlew -p versions/1.21.4 '-Pminecraft_version=1.21.4' build
./gradlew -p versions/1.21.6 '-Pminecraft_version=1.21.8' build
./gradlew -p versions/1.21.9 '-Pminecraft_version=1.21.10' build
./gradlew build
./gradlew -p versions/26.1 '-Pminecraft_version=26.1.2' build
./gradlew -p versions/26.2 build
```

On Windows use `gradlew.bat`. The 1.21.2, 1.21.4, 1.21.5, 1.21.6 and 1.21.9 projects place JARs in `build/<minecraft>/libs/`; the other projects use `build/libs/`. Install the regular mod JAR; `-sources.jar` is for developers. All fifteen exact targets are listed in `scripts/minecraft-targets.json` and compiled by GitHub Actions.

The 1.21.1 source tree lives in `versions/1.21`. The 1.21.2 and 1.21.4 projects share that tree with explicit API transformations and overrides. Later API families have separate source trees in `versions/1.21.5`, `versions/1.21.6`, `versions/1.21.9` and `versions/26.1`. The 26.2 build adapts the 26.1 tree and overrides changed collision behavior. Edit checked-in source files and Gradle transformations, never generated sources. Language files, textures, sounds and most data are shared from the root.

Client regressions require a graphical environment. For 1.21.1–1.21.8 run `runLegacyTest`; these projects provide a test-only harness that starts a real Minecraft client and integrated server. For 1.21.9 and newer run `runClientGameTest`, using Fabric's client test API. Both run spectator, preparation/inventory, protocol/loot and HUD/wildcard suites. Set `HW_WATER_INPUT_TEST_ONLY=1` for the focused swimming and time-input suite. Other selection flags are `HW_SPECTATE_TEST_ONLY`, `HW_INVENTORY_TEST_ONLY` and `HW_NETWORK_TEST_ONLY`; set only one at a time. Tests, worlds and logs are excluded from release JARs and Git.

After building every target, run `powershell -File scripts/package-release.ps1`. The script validates embedded mod ID, version and exact Minecraft dependency before staging the fifteen JARs, SHA-256 checksums and bilingual release notes in `dist/<version>/`.

## 简体中文

模组版本统一定义在根目录 `gradle.properties`。Minecraft 1.21.x 使用 Yarn 映射和 Java 21；26.x 使用原版非混淆命名和 Java 25。Wrapper 固定 Gradle 9.5.1，并校验下载文件。

上表列出全部十五个正式目标及其项目目录。设置对应 JDK 的 `JAVA_HOME`，然后使用上方命令选择具体游戏版本；Windows 使用 `gradlew.bat`。1.21.2、1.21.4、1.21.5、1.21.6、1.21.9 项目的产物在 `build/<游戏版本>/libs/`，其余项目在 `build/libs/`。安装普通模组 JAR，`-sources.jar` 仅供开发。

`versions/1.21` 保存 1.21.1 源码，1.21.2 和 1.21.4 项目在此基础上转换 API 并覆盖差异类。1.21.5、1.21.6、1.21.9 和 26.1 分别维护对应 API 的源码；26.2 复用 26.1 并适配碰撞变化。请修改受 Git 管理的源文件或 Gradle 转换规则，不要编辑生成目录。中英语言文件、纹理、声音和多数数据与根目录共用。

客户端测试需要图形环境。1.21.1–1.21.8 使用 `runLegacyTest`，测试专用程序会启动真实客户端与内置服务器；1.21.9 及更新版本使用 Fabric 的 `runClientGameTest`。默认覆盖观战、准备阶段物品、协议与掉落、HUD 与外卡。水中移动和时间输入专项设置 `HW_WATER_INPUT_TEST_ONLY=1`；其他筛选标记见英文说明，一次只设置一个。测试程序、世界和日志不会进入发布包或 Git。

全部构建完成后，执行 `powershell -File scripts/package-release.ps1`。脚本先验证 JAR 内模组 ID、版本号和精确 Minecraft 依赖，再将十五个发布包、SHA-256 校验值和双语更新说明整理到 `dist/<版本>/`。
