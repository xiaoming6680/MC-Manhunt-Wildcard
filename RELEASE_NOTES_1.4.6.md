# Manhunt Wildcard 1.4.6

## English

Manhunt Wildcard now supports every stable Minecraft release from **1.21.1 through 26.2** on **Fabric**: 1.21.1–1.21.11, 26.1, 26.1.1, 26.1.2 and 26.2. Each Minecraft version has its own download.

- Add ten backports for Minecraft **1.21.1–1.21.10**, retaining the 28 wildcards, team objectives, preparation rules, configurable loot, bilingual menu and HUD.
- Adapt networking, inventory components, advancement resets, respawning, spectator cameras, world generation, rendering and input to each supported API family.
- Preserve safe Nether portal destinations, Overworld respawns after Nether/End deaths, temporary-pearl cleanup and configurable blaze rod drops.
- Preserve teammate spectating while waiting to respawn and free spectator flight after elimination, with **Z / X** target switching.
- Backport rotated gravity, collision, jumping, swimming and diving for World Tilt, alongside the editable time fields and menu fixes.
- Correct background blur ordering on 1.21.1–1.21.5 so menu text and panels remain sharp.
- Fix legacy keybinding conflicts so spectator switching still works when Minecraft's default creative-toolbar shortcuts share the same key.
- Hide the vanilla locator-bar setting before Minecraft 1.21.6, where that feature does not exist. Supply drops use a trident instead of the diamond spear on versions before 1.21.11; copper equipment is only considered where Minecraft includes it.
- Expand the build matrix, release packaging and English-first documentation to all fifteen targets.

**Installation:** use the file for your exact Minecraft version on both the client and server, plus matching **Fabric API** and **Fabric Loader 0.19.3+**. Minecraft 1.21.x requires **Java 21**; 26.x requires **Java 25**. Remove older copies of this mod. These downloads do not allow clients and servers on different Minecraft versions to join each other.

## 简体中文

Manhunt Wildcard 现已支持 **1.21.1 至 26.2 的全部正式版本**，加载器为 **Fabric**：1.21.1–1.21.11、26.1、26.1.1、26.1.2、26.2。每个游戏版本提供独立下载包。

- 新增 **1.21.1–1.21.10** 十个向下兼容构建，保留 28 张外卡、阵营目标、准备阶段规则、可配置掉落、双语菜单与 HUD。
- 分别适配各版本的网络、物品组件、成就重置、复活、观战相机、世界生成、渲染和输入 API。
- 保留地狱传送门安全落点、地狱/末地死亡回主世界、临时珍珠回收和烈焰棒掉落概率设置。
- 保留等待复活时跟随队友、出局后自由观察，以及 **Z / X** 切换目标。
- 将地动山摇的旋转重力、碰撞、跳跃、游泳和下潜，以及时间输入和菜单修复移植到旧版。
- 修正 1.21.1–1.21.5 的背景模糊顺序，保持菜单文字和面板清晰。
- 修复旧版按键冲突：观战切换与原版创造模式工具栏快捷键使用相同按键时仍可触发。
- 1.21.6 之前没有原版定位栏，因此隐藏对应设置；1.21.11 之前补给中的钻石矛改用三叉戟，仅在原版存在铜装备的版本计算铜装备。
- 构建矩阵、发布打包、英文在前的文档同步扩展至十五个目标。

**安装**：客户端与服务器都安装对应精确 Minecraft 版本的同一份模组包，同时安装匹配的 **Fabric API** 和 **Fabric Loader 0.19.3+**。1.21.x 使用 **Java 21**，26.x 使用 **Java 25**，并移除旧模组包。向下兼容构建不代表不同 Minecraft 版本的客户端和服务器可以互相连接。
