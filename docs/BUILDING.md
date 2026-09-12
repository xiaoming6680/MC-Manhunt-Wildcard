# Building / 构建

## English

The mod version is defined once in the root `gradle.properties`. Minecraft 1.21.11 uses Yarn mappings and Java 21. Minecraft 26.x uses the unobfuscated game and Java 25. Gradle 9.5.1 is pinned and checksum-verified by the wrapper.

| Minecraft | Project | Java | Fabric API |
| --- | --- | --- | --- |
| 1.21.11 | repository root | 21 | 0.141.4+1.21.11 |
| 26.1 | versions/26.1 | 25 | 0.145.1+26.1 |
| 26.1.1 | versions/26.1 | 25 | 0.145.4+26.1.1 |
| 26.1.2 | versions/26.1 | 25 | 0.155.3+26.1.2 |
| 26.2 | versions/26.2 | 25 | 0.160.0+26.2 |

Set `JAVA_HOME` to the appropriate JDK, then run:

```sh
./gradlew build
./gradlew -p versions/26.1 '-Pminecraft_version=26.1' build
./gradlew -p versions/26.1 '-Pminecraft_version=26.1.1' build
./gradlew -p versions/26.1 '-Pminecraft_version=26.1.2' build
./gradlew -p versions/26.2 build
```

On Windows use `gradlew.bat`. JARs appear in each project's `build/libs/`. Only the regular mod JAR is installed; `-sources.jar` is for developers.

The 26.1 project owns the modern source tree. The 26.2 build applies explicit API renames during source preparation and supplies a separate collision mixin for the changed bounce system. Generated sources are ignored; edit the source tree or the transformation in `versions/26.2/build.gradle`. Language files, textures, sounds and most data are shared from the root; modern dimension data overrides the legacy format.

The GitHub Actions workflow compiles all five targets. Client regression tests require a graphical environment. To run the spectator suite, set `HW_SPECTATE_TEST_ONLY=1` and execute `runClientGameTest` for the target. Other focused suites use `HW_WATER_INPUT_TEST_ONLY=1` and `HW_NETWORK_TEST_ONLY=1`. Set only one selection flag at a time. Test instances and logs stay under `build/`.

## 简体中文

模组版本统一定义在根目录 `gradle.properties`。Minecraft 1.21.11 使用 Yarn 映射和 Java 21；26.x 使用原版非混淆命名和 Java 25。Gradle Wrapper 固定为 9.5.1，并验证下载校验值。

设置对应 JDK 的 `JAVA_HOME` 后，运行上方构建命令；Windows 使用 `gradlew.bat`。文件生成在各项目的 `build/libs/`，安装普通模组 JAR，`-sources.jar` 仅用于开发。

26.1 目录维护现代版本源码；26.2 在构建时应用明确的 API 改名，并单独适配碰撞反弹逻辑。请修改源文件或 `versions/26.2/build.gradle` 中的转换规则，不要编辑忽略的生成目录。中英文语言、纹理、声音及多数数据与根目录共用；现代版本覆盖旧版维度数据格式。

GitHub Actions 编译全部五个目标。客户端回归需要图形环境：设置 `HW_SPECTATE_TEST_ONLY=1` 后运行目标的 `runClientGameTest` 可验证观战；水中移动与时间输入使用 `HW_WATER_INPUT_TEST_ONLY=1`，网络与掉落验证使用 `HW_NETWORK_TEST_ONLY=1`。一次只设置一个筛选标记，测试实例与日志保存在 `build/` 内。
