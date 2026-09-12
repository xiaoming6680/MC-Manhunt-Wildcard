# 1.4.5 validation / 验证记录

## English

All five installable Fabric builds were compiled and exercised in real Minecraft clients with integrated servers. The regression tests use server-side test players as additional participants.

| Minecraft | Completed validation |
| --- | --- |
| 1.21.11 | Build; preparation inventory, advancement reset, spectator/respawn, water/time-input and network regressions |
| 26.1 | Build; complete client game-test entrypoint suite, plus focused water/time-input regressions |
| 26.1.1 | Build; spectator and cross-dimension respawn regressions |
| 26.1.2 | Build; spectator/respawn and water/time-input regressions |
| 26.2 | Build; complete client game-test entrypoint suite, plus focused water/time-input regressions |

The spectator suite covers rapid target switches, pending teleport acknowledgments, cross-dimension following, manual detachment, eliminated players visiting opponents while retaining free flight, and Overworld respawns after Nether/End deaths. The network suite covers protocol handling, configuration synchronization, blaze probability overrides and unsafe portal exits. The water suite compares native movement with the rotated gravity frame and exercises real input widgets, including values above 1:30.

The complete client suite also exercises preparation inventory and advancement reset, gravity/camera/collision/jumping, menu editing and save conflicts, and wildcard activation/cleanup including Backrooms. Local image review confirmed that the 26.2 menu renders correctly. Test reports, screenshots and logs are generated under each project's `build/` and are intentionally excluded from Git. See [Building](BUILDING.md) for commands.

These results cover the included automated scenarios on Windows with Java 21/25 and the Fabric versions listed in the build matrix. They do not represent a long-running public-server or arbitrary third-party modpack compatibility test.

## 简体中文

五个 Fabric 安装包均已编译，并在真实 Minecraft 客户端及内置服务器中运行验证；额外参与者使用服务端测试玩家。

上表列出每个版本实际完成的范围。观战回归覆盖连续切人、传送确认、跨维度跟随、主动解除视角、出局后跨阵营跳转并保留自由飞行，以及地狱/末地死亡后回主世界。网络回归覆盖协议、配置同步、烈焰棒概率与不安全传送门出口；水中回归比较原版与旋转重力下的移动，并实际操作时间输入框，包含超过 1:30 的输入。

完整客户端套件还覆盖准备阶段物品、成就重置、重力/镜头/碰撞/跳跃、菜单编辑与保存冲突，以及包括后室在内的外卡启停。已查看 26.2 菜单截图确认显示正常。日志、报告与截图位于各项目的 `build/`，不提交到 Git；命令见[构建文档](BUILDING.md)。

验证环境为 Windows、Java 21/25 和构建矩阵列出的 Fabric 版本；测试范围不等同于长期公网服务器或任意第三方整合包兼容性测试。
