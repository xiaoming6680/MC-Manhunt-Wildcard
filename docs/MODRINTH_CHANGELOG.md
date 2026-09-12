# Manhunt Wildcard 1.4.5

## English

Upgrading from Modrinth 1.2.0 brings 28 wildcards, a redesigned menu and major gameplay fixes.

- Add separate Fabric downloads for Minecraft **1.21.11, 26.1, 26.1.1, 26.1.2 and 26.2**. Download the file matching your game version.
- Add Backrooms!, World Tilt, Key Scramble, Portals, Space Shift and more; retain Classic Manhunt without wildcards. Rename Blood Rage to **Last Stand**.
- Fix World Tilt gravity, collision, camera, jumping, swimming and diving. Preserve vanilla movement effects.
- Keep Nether portal exits below the bedrock roof and validate the exit again before teleporting.
- Reclaim only Pearl Frenzy's temporary pearls; preserve ordinary player pearls. Add an optional **0–100% blaze rod drop chance** alongside piglin trade settings.
- Follow living teammates while waiting to respawn; **Z / X** switches teammates. Fix stalls during rapid switches, long-distance teleports and dimension changes.
- Eliminated players use free spectator mode; **Z / X** visits any other online player's location across teams and dimensions while retaining free flight.
- Return to the Overworld after Nether/End deaths. Preserve respawn timers and life rules. Apply death-inventory settings during preparation and reset all online players' advancement progress on each successful round start.
- Redesign match/rules/wildcard menus with presets, a searchable matrix, item selection, editable drafts, save-conflict handling and local display preferences.
- Fix time input, minimum/maximum editing and untranslated respawn strings. Place objectives at the left center; reduce the information card's height while keeping its width and prevent overlap with the draw animation.
- Improve Backrooms exits, team glow, ambience and team departure handling; prevent fused Drop Bomb items from merging away.

**Update every client and the server together.** Remove old mod JARs and install the corresponding Fabric API. Use Java 21 for 1.21.11, Java 25 for 26.x, and Fabric Loader 0.19.3 or newer. Protocol v3 cannot be mixed with older builds. Existing rule values are preserved; new settings receive defaults.

[Full release notes](https://github.com/xiaoming6680/MC-Manhunt-Wildcard/releases/tag/v1.4.5)

---

## 简体中文

从 Modrinth 初版 1.2.0 升级，将获得 28 张外卡、重做的菜单与多项玩法修复。

- 为 Minecraft **1.21.11、26.1、26.1.1、26.1.2、26.2** 提供独立 Fabric 安装包，请选择匹配的游戏版本。
- 新增后室、地动山摇、按键错乱、传送门、空间错位等外卡，保留关闭全部外卡的经典追逃预设。“血怒时刻”更名为**背水一战**。
- 修复地动山摇的重力、碰撞、镜头、跳跃、游泳与下潜，保留原版相关移动效果。
- 传送门的地狱出口限制在基岩顶层以下，传送前再次检查安全位置。
- 珍珠狂潮仅回收自身发放的临时珍珠，保留玩家普通珍珠；新增可选的 **0–100% 烈焰棒掉落概率**。
- 等待复活时跟随存活队友，**Z / X** 切换；修复连续切人、远距离传送及跨维度导致的观战卡死。
- 已出局玩家默认自由观察，**Z / X** 可前往全场其他在线玩家的位置，包含敌方与其他维度，跳转后仍可自由飞行。
- 地狱/末地死亡后回主世界复活，保留原有计时与生命规则；准备阶段也遵守死亡物品设置，每次成功开局重置全部在线玩家的成就进度。
- 重做对局、规则与外卡界面，加入预设、可搜索矩阵、物品选择、编辑草稿、保存冲突处理及本地显示设置。
- 修复时间输入、最小/最大值编辑与复活翻译字串；目标置于左中，信息卡降低高度并保留宽度，避免遮挡抽卡动画。
- 改进后室出口、阵营发光、环境音和整队退出处理；点燃的掉落物炸弹不再合并消失。

**客户端与服务端一起更新。** 移除旧包，安装对应 Fabric API；1.21.11 使用 Java 21，26.x 使用 Java 25，建议 Fabric Loader 0.19.3 或更新版本。v3 协议不能与旧构建混用。已有规则值保留，新配置项使用默认值。

[完整双语发布说明](https://github.com/xiaoming6680/MC-Manhunt-Wildcard/releases/tag/v1.4.5)
