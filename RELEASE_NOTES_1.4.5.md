# Manhunt Wildcard 1.4.5

## English

**Fabric builds:** Minecraft 1.21.11, 26.1, 26.1.1, 26.1.2 and 26.2. Each game version has a separate download.

This update brings the current Manhunt Wildcard experience to players upgrading from the Modrinth 1.2.0 release: 28 wildcards, a redesigned in-game menu, configurable match rules, and major fixes to movement, portals, respawning and spectator cameras.

### Gameplay and wildcards

- Expand the lineup to 28 events, including Backrooms!, World Tilt, Key Scramble, Portals, Space Shift, Backstab, Vampire and Chain Mining. The Classic Manhunt preset remains available without wildcards.
- World Tilt rotates gravity, player collision, movement, camera and jumping. Fix swimming and diving to follow the rotated frame while preserving vanilla movement effects.
- Rename Blood Rage to **Last Stand** while preserving its configuration ID and effect.
- Validate portal exits and keep Nether destinations below the bedrock roof. Recheck the destination immediately before teleporting.
- Pearl Frenzy reclaims only its own temporary pearl grants. Ordinary pearls are preserved, including when granted pearls are dropped, transferred or retrieved from storage after the event.
- Add an optional **0–100% blaze rod drop chance**, alongside the piglin pearl trade settings. The blaze override is disabled by default.
- Improve Backrooms exits, team glow and ambience; fix a crash when an entire team leaves the maze. Fused Drop Bomb items no longer merge away.

### Death, respawn and spectating

- Automatically spectate living teammates while waiting to respawn; **Z / X** switches targets. With no living teammates, use free spectator flight.
- Eliminated players start in **free spectator mode**. **Z / X** visits any other online player's location, including opponents and players in other dimensions. Free flight remains available after each jump.
- Fix spectator stalls during rapid switches, long-distance teleports and dimension changes, including pending teleport confirmations and target tracking.
- Respawn in the **Overworld** after Nether or End deaths. Spectator travel never changes the saved respawn destination or the existing timer and life rules.
- Apply each team's death-inventory setting during preparation too. Reset complete and partial advancement progress for all online players whenever a round successfully starts.
- Improve kill credit, environment-death settings, random respawn spacing and team-colored feedback.

### Menu and HUD

- Redesign the match, rules and wildcard menus with team cards, presets, categorized settings, a searchable wildcard matrix, item selection and configurable local display preferences.
- Keep edits in a draft until Apply. Preserve drafts on save conflicts and report validation and persistence errors clearly.
- Fix time-field focus and editing; accept seconds, `m:ss`, full-width digits/colons and the Chinese seconds suffix. Raising only the minimum can extend the previous maximum; explicitly reversed bounds remain invalid.
- Place the objective panel at the left center and hide overlapping panels during the card draw. Reduce the top-right information card's height while retaining its width.
- Replace the black death cover with a compact transparent status panel and fix untranslated respawn-duration strings.

### Updating

Install the download for your Minecraft version with the corresponding Fabric API. **Update every client and the server together and remove old mod JARs.** Configuration traffic uses protocol v3; older builds cannot be mixed with this release. Existing rule values are retained and new fields receive defaults.

Minecraft 1.21.11 requires Java 21; 26.x requires Java 25. Use the supported game versions on the individual download entry.

---

## 简体中文

**Fabric 适配：**Minecraft 1.21.11、26.1、26.1.1、26.1.2、26.2，每个游戏版本提供独立安装包。

本次更新面向从 Modrinth 初版 1.2.0 升级的玩家，带来当前完整玩法：28 张外卡、重做的游戏内菜单、可配置的对局规则，以及移动、传送门、复活和观战视角的重要修复。

### 玩法与外卡

- 外卡扩充至 28 张，包含后室、地动山摇、按键错乱、传送门、空间错位、背刺、吸血鬼和连锁挖掘等；经典追逃预设仍可关闭所有外卡。
- 地动山摇旋转重力、人物碰撞、移动、镜头和跳跃；修复水中游泳与下潜方向，保留原版相关移动效果。
- “血怒时刻”更名为**背水一战**，保留效果及配置 ID。
- 传送门检查出口安全，将地狱落点限制在基岩顶层以下，并在传送前再次验证落点。
- 珍珠狂潮仅回收自身发放的临时珍珠，保留玩家原有珍珠；支持丢弃、转交和外卡结束后从容器取回时的过期处理。
- 在猪灵珍珠交易设置之外，新增可选的 **0–100% 烈焰棒掉落概率**，默认关闭覆盖，沿用原版。
- 改进后室出口、阵营发光和环境音，修复整队离开迷宫时的崩溃；掉落物炸弹点燃后不再合并消失。

### 死亡、复活与观战

- 等待复活时自动观察存活队友，**Z / X** 切换目标；没有存活队友时自由观察。
- 已出局玩家默认进入**自由观察者模式**，**Z / X** 可前往全场其他在线玩家的位置，包括敌方与其他维度的玩家，跳转后仍可自由飞行。
- 修复连续切换、远距离传送和跨维度时观战卡死，正确处理传送确认与目标追踪。
- 地狱或末地死亡后回**主世界**复活；观战移动不会改变保存的复活位置、计时和生命规则。
- 准备阶段也遵守各阵营死亡物品保留设置；成功开局时清空所有在线玩家完整与部分成就进度。
- 改进击杀归属、环境死亡设置、随机复活间距及阵营颜色反馈。

### 菜单与 HUD

- 重做对局、规则与外卡菜单，加入阵营卡片、预设、分类规则、可搜索外卡矩阵、物品选择及本地显示设置。
- 修改先保留为草稿，点击应用后保存；冲突时保留修改，明确提示校验及保存错误。
- 修复时间输入焦点和编辑问题，支持秒数、`分:秒`、全角数字/冒号和“秒”后缀；单独提高最小值时可同步扩展原最大值，明确输入反向区间仍会提示错误。
- 目标面板置于屏幕左中，抽卡时隐藏重叠信息；右上信息卡降低高度、保留横向长度。
- 死亡黑屏改为紧凑透明状态面板，修复复活时间显示内部翻译键的问题。

### 升级方式

选择与游戏版本匹配的文件，并安装对应 Fabric API。**客户端与服务端必须一起更新，并移除旧 JAR。** 配置通信使用 v3 协议，不能与旧构建混用。已有规则值保留，新字段采用默认值。

Minecraft 1.21.11 使用 Java 21，26.x 使用 Java 25。具体支持的游戏版本以下载条目为准。
