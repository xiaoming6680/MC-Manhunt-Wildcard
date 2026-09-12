# 1.4.6 validation / 验证记录

## English

All ten new backports completed the four client regression entrypoints in real Minecraft clients with integrated servers. Additional participants use server-side test players. The table records the completed scenarios per target; graphical regressions were run locally on Windows with Java 21.

| Minecraft | Validation |
| --- | --- |
| 1.21.1 | Complete client suite; water/time-input suite; visual review after correcting background blur |
| 1.21.2 | Complete client suite; water/time-input suite |
| 1.21.3 | Complete client suite |
| 1.21.4 | Complete client suite; water/time-input suite; visual review after correcting background blur |
| 1.21.5 | Complete client suite; water/time-input suite; menu screenshot review |
| 1.21.6 | Complete client suite; water/time-input suite |
| 1.21.7 | Complete client suite |
| 1.21.8 | Complete client suite; water/time-input suite; menu screenshot review |
| 1.21.9 | Complete client suite; water/time-input suite |
| 1.21.10 | Complete client suite; water/time-input suite |
| 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 | Rebuilt as 1.4.6; gameplay source unchanged from the verified 1.4.5 builds. See the [previous validation record](VALIDATION_1.4.5.md). |

The complete suite covers:

- Spectator target switching, rapid repeated requests, pending teleport acknowledgments, cross-dimension following, detachment, eliminated players visiting opponents with free flight, and Overworld respawns after Nether/End deaths.
- Preparation inventory rules, advancement resets, draw-overlay transitions, network/configuration synchronization, blaze drop probabilities and unsafe portal destinations.
- Gravity, collision, jumping, fall damage and camera alignment; menu edits and save conflicts; wildcard activation and cleanup, including Backrooms.

The focused water suite compares native movement with four rotated gravity directions across sprint, pitch and status-effect combinations, checks diving/jumping, and uses real mouse/keyboard input to save values above 1:30. The fixture explicitly teleports into its water volume and waits for client chunk data, avoiding dependence on random world spawn. The 1.21.5 damage fixture uses a dry platform and verifies the test hit was accepted, so incidental drowning cannot invalidate the key-scramble assertion.

Minecraft 1.21.1–1.21.8 uses the repository's test-only client harness; 1.21.9–1.21.10 uses Fabric's client test API. For the latter, the framework's network synchronizer is disabled because the suite deliberately exercises low-level packet handling; it still runs the actual Minecraft networking and client/server assertions. No test harness or test classes are included in installable JARs.

Every release JAR is checked for the correct mod ID, version, exact Minecraft dependency and absence of test classes. Release packaging generates SHA-256 checksums. The build workflow covers all fifteen exact targets; graphical tests require a local display. Commands and dependency versions are in [Building](BUILDING.md).

These checks cover the included automated scenarios, not a long-running public server or arbitrary third-party modpack combination. Logs, test worlds, reports and generated screenshots remain under ignored `build/` directories.

## 简体中文

新增的十个向下兼容版本均已在真实 Minecraft 客户端和内置服务器中完成四个客户端回归入口，额外参与者使用服务端测试玩家。上表记录各目标实际完成的范围；图形回归环境为 Windows 和 Java 21。

完整套件覆盖连续切换观战目标、传送确认、跨维度跟随、解除视角、出局后的跨阵营自由观察，以及地狱/末地死亡后回主世界；同时验证准备阶段物品、成就重置、抽卡界面、网络与配置、烈焰棒概率、传送门落点、旋转重力/碰撞/跳跃/摔伤/镜头、菜单编辑和保存冲突，以及包括后室在内的外卡启停。

水中专项对比原版和四个旋转方向的移动，组合疾跑、视角和状态效果，检查下潜/跳跃，并通过真实输入保存超过 1:30 的时间。测试会先传送到水池并等待客户端区块数据，避免随机出生点造成干扰；1.21.5 的受伤测试使用干燥平台并确认伤害实际被接受，避免意外溺水影响按键错乱断言。

1.21.1–1.21.8 使用仓库中的测试专用运行器，1.21.9–1.21.10 使用 Fabric 客户端测试 API。后者按框架要求关闭其网络同步器，以允许直接验证底层数据包；实际 Minecraft 网络和客户端/服务端断言仍正常执行。安装包不包含测试程序和测试类。

原有五个目标的玩法源码未改动，已重新构建为 1.4.6；实测范围见[1.4.5 验证记录](VALIDATION_1.4.5.md)。发布前检查各 JAR 的模组 ID、版本号、精确游戏依赖及测试类排除情况，并生成 SHA-256 校验文件。自动构建覆盖十五个目标，具体命令见[构建文档](BUILDING.md)。

这些结果对应已包含的自动化场景，不等同于长期公网服务器或任意第三方整合包兼容性测试。日志、测试世界、报告和生成截图保存在被 Git 忽略的 `build/` 目录中。
