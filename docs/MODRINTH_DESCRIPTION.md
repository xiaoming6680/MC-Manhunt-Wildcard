# Manhunt Wildcard

**Hunt, escape, and adapt when the rules change.**

[English](#english) · [简体中文](#简体中文)

![Manhunt Wildcard](https://raw.githubusercontent.com/xiaoming6680/MC-Manhunt-Wildcard/main/Manhunt-Wildcard.jpg)

## English

Manhunt Wildcard turns Minecraft Manhunt into a configurable multiplayer game with **28 rotating wildcard events**. Hunters track the runners. Runners race to defeat the Ender Dragon, survive a time limit, or complete a collection objective. Then a wildcard changes what everyone can do.

### A chase that keeps changing

- **World Tilt:** gravity turns sideways, with the camera, collision and controls following it. Walk and jump on walls.
- **Backrooms!:** both teams enter a yellow maze. Find a false-floor exit before the timer runs out.
- **Portals and Space Shift:** paired portals and shuffled positions change who is chasing whom.
- **Key Scramble:** getting hit rearranges your movement keys.
- **Last Stand:** at three hearts or less, your hits become lethal.
- **Pearl Frenzy, Supply Drop, Chain Mining, Tiny Players** and more add new opportunities throughout the round.

Configure fixed or random event timing, enable individual cards, and adjust their parameters. Prefer the original format? Choose **Classic Manhunt** to turn wildcards off.

![Wildcard menu](https://raw.githubusercontent.com/xiaoming6680/MC-Manhunt-Wildcard/main/screenshot/UI_matrix.png)

### Run the game from one menu

Press **M** to join a team, see the match status, or browse the rules. Operators can choose a preset and edit settings in-game. Changes stay in a draft until **Apply**, and the menu reports save conflicts instead of discarding your edits.

- Red hunters and blue runners, with a target-selecting tracking compass.
- Configurable objectives, preparation time, lives, respawn waits, death drops, combat balance and piglin/blaze probabilities.
- Automatic teammate spectating while waiting to respawn; **Z / X** switches teammates. With no living teammate, fly freely.
- Eliminated players use free spectator mode; **Z / X** visits any other online player, including opponents and players in other dimensions.
- Nether and End deaths return to the Overworld. Watching another player does not change your respawn destination.
- Local HUD scale, opacity, margins and animation preferences; English and Simplified Chinese follow Minecraft's language setting.

![Match menu](https://raw.githubusercontent.com/xiaoming6680/MC-Manhunt-Wildcard/main/screenshot/UI_lobby.png)

### Installation and first round

1. Select a download matching your **Minecraft version** and install **Fabric Loader** and **Fabric API** for that version.
2. Install the **same Manhunt Wildcard build on every client and the server**. Remove old copies before updating. Single-player with an integrated server is supported too.
3. Enter the world, press **M**, and join Hunters or Runners.
4. An operator chooses rules, clicks **Apply**, and starts the round when both sides have players.

**Supported stable versions:** 1.21.11, 26.1, 26.1.1, 26.1.2 and 26.2 (Fabric). Use Fabric Loader 0.19.3 or newer.

Use **Java 21** for Minecraft 1.21.11 and **Java 25** for 26.x. Check the version entry for supported Minecraft versions; one file is not automatically compatible with every version.

**Controls:** M — menu · H — status HUD · Z / X — spectator target · compass right-click — target selection · sneak + compass right-click — cycle target. Keys can be rebound.

**Configuration:** server rules live in `config/hunterwildcard.json`; local display preferences live in `config/hunterwildcard-ui.json`. Operators can also use `/hw start`, `/hw stop`, `/hw config reload`, and `/hw wildcard test <id>`.

Each successful round start resets advancement progress for **all online players**, including spectators. Existing saved rule values are preserved when upgrading; new fields receive defaults.

[Full guide and wildcard list](https://github.com/xiaoming6680/MC-Manhunt-Wildcard#readme) · [Report an issue](https://github.com/xiaoming6680/MC-Manhunt-Wildcard/issues)

---

## 简体中文

**追击、逃亡，随机外卡随时改变战局。**

Manhunt Wildcard 将 Minecraft 猎人追逃变成可配置的多人游戏，加入 **28 张轮换触发的外卡**。猎人追踪逃亡者，逃亡者争取击败末影龙、坚持指定时间或完成物品收集；而随机外卡会不断改变双方的行动方式。

### 每一轮追逐都有变数

- **地动山摇：**重力转向侧面，镜头、碰撞与操作随之变化，可以在墙上行走和跳跃。
- **后室！：**双方进入黄色迷宫，在时限内寻找假地板出口。
- **传送门、空间错位：**成对传送或随机交换位置，改变追逐局势。
- **按键错乱：**受击后移动按键重新排列。
- **背水一战：**三颗心及以下时，命中的攻击致命。
- 还有**珍珠狂潮、补给空投、连锁挖掘、迷你玩家**等外卡。

可以设置固定或随机抽取时间、单独开关每张外卡并调整参数。想玩传统规则？选择**经典追逃**预设，关闭全部外卡。

### 在游戏内管理整场对局

按 **M** 加入阵营、查看状态和规则。管理员可直接选择预设、调整设置；修改保留为草稿，点击**应用**后保存。发生保存冲突时会提示并保留修改。

- 红色猎人与蓝色逃亡者，指南针可选择追踪目标。
- 自定义胜利条件、准备时间、生命次数、复活等待、死亡掉落、战斗平衡，以及猪灵交易和烈焰棒掉落概率。
- 等待复活时自动观察存活队友，**Z / X** 切换；没有队友时自由观察。
- 已出局玩家默认自由观察，**Z / X** 可前往全场其他在线玩家的位置，包含敌方和其他维度的玩家。
- 地狱或末地死亡后回主世界复活，观战位置不会改变复活地点。
- 独立设置 HUD 缩放、透明度、边距与动画；中英文界面跟随游戏语言。

### 安装与开局

1. 下载与 **Minecraft 版本匹配**的文件，并安装对应版本的 **Fabric Loader** 和 **Fabric API**。
2. **所有客户端与服务端安装同一构建的 Manhunt Wildcard**，更新时移除旧包。也支持带内置服务器的单人游戏环境。
3. 进入世界按 **M**，加入猎人或逃亡者。
4. 管理员调整规则、点击**应用**，双方有人后开始游戏。

**支持的正式版本：**1.21.11、26.1、26.1.1、26.1.2、26.2（Fabric），建议使用 Fabric Loader 0.19.3 或更新版本。

Minecraft 1.21.11 使用 **Java 21**，26.x 使用 **Java 25**。具体兼容版本以下载条目为准，不能将一个文件直接用于全部游戏版本。

**按键：**M 打开菜单 · H 切换状态 HUD · Z / X 切换观战目标 · 指南针右键选择目标 · 潜行右键循环目标。按键均可重新绑定。

**配置：**服务端规则为 `config/hunterwildcard.json`，本地显示偏好为 `config/hunterwildcard-ui.json`。管理员还可使用 `/hw start`、`/hw stop`、`/hw config reload` 和 `/hw wildcard test <id>`。

每次成功开局都会重置**全部在线玩家**的成就进度，包括旁观者。更新保留已有规则值，新配置项使用默认值。

[完整说明与外卡列表](https://github.com/xiaoming6680/MC-Manhunt-Wildcard/blob/main/README.zh-CN.md) · [反馈问题](https://github.com/xiaoming6680/MC-Manhunt-Wildcard/issues)

---

Inspired by Minecraft Manhunt and temporary wildcard rules. / 灵感来自 Minecraft 猎人追逃与临时外卡规则。

MIT License. Not an official Minecraft product; not approved by or associated with Mojang or Microsoft.
