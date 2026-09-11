# Manhunt Wildcard

[English](README.en.md) | 简体中文

Manhunt Wildcard 是一个用于 Minecraft Manhunt / 猎人追逃玩法的 Fabric 模组。玩家分为猎人和逃亡者，在对局中周期性触发随机外卡事件，让追逃节奏持续变化。

![Manhunt Wildcard](Manhunt-Wildcard.jpg)

## 灵感来源

本 MOD 的玩法灵感部分来自近期赛季 APEX Legends 的外卡活动：在常规对抗规则之外加入临时规则变化，让每一局都出现新的风险、机会和战术选择。Manhunt Wildcard 将这种"局内变体规则"的思路放进 Minecraft Manhunt，让追逃过程不再只依赖固定路线和固定节奏。

## 核心功能

- 猎人 vs 逃亡者阵营玩法
- 管理员控制对局开始、停止和外卡测试
- 准备阶段，避免开局混战
- 猎人追踪指南针，按配置周期刷新目标
- 周期性随机外卡，通过 HUD、BossBar、聊天提示展示
- 多种逃亡者与猎人胜利条件
- 可配置复活模式、生命数、死亡掉落和准备边界
- 可配置猎人对逃亡者伤害倍率、猎人 / 逃亡者移速倍率
- 可配置猪灵交易末影珍珠概率（默认 40%）
- 等待阶段自动显示"本局设置"HUD，对局中普通玩家按 `M` 查看对局状态
- 中英文语言文件，支持 Minecraft 原生语言切换

## 一局是怎么进行的

### 1. 准备阶段

开始对局后进入准备倒计时。猎人被限制在出生点附近的边界内，逃亡者趁这段时间拉开距离。顶部 BossBar 显示剩余准备时间。

![准备阶段](screenshot/GAMEPLAY1.png)

### 2. 对局进行中

准备结束后猎人拿到追踪指南针开始追击。左侧目标面板显示逃亡者当前的胜利目标（例如存活时间、收集物品），底部 ActionBar 提示自己的身份。

![对局进行中](screenshot/GAMEPLAY2.png)

### 3. 外卡触发

每隔一段时间随机抽取一张外卡，先播放抽卡动画，然后在左上角显示规则说明，BossBar 显示剩余时间。下图为"轻盈之身"生效时的画面。

![外卡触发](screenshot/GAMEPLAY4.png)

### 4. 击杀与复活反馈

猎人击杀逃亡者时右上角弹出击杀反馈；在击杀计数模式下还会显示距离目标还差几次。逃亡者复活时也有对应提示。

![击杀反馈](screenshot/GAMEPLAY5.png)

![复活提示](screenshot/GAMEPLAY6.png)

### 5. 对局结算

任一方达成胜利条件后进入结算：右上角显示胜负，聊天栏输出完整结算信息，胜方头顶放烟花。

![对局结算](screenshot/GAMEPLAY3.png)

### 6. 状态 HUD

等待阶段只要有人加入队伍，左上角自动显示"本局设置"，所有玩家都能看到这局的胜利条件、复活规则和外卡设置；对局开始后它会自动消失。

![本局设置](screenshot/HUD_lobby.png)

对局中，非 OP 玩家按 `M` 可以随时切换"对局状态"面板，查看阶段倒计时、当前外卡和剩余时间。

![对局状态](screenshot/HUD_status.png)

## 新外卡一览

| 受伤乱键 | 全体变小 |
| --- | --- |
| ![受伤乱键](screenshot/WILDCARD_key_scramble.png) | ![全体变小](screenshot/WILDCARD_tiny_players.png) |

| 小心翼翼 | 你是谁？ |
| --- | --- |
| ![小心翼翼](screenshot/WILDCARD_fragile.png) | ![你是谁？](screenshot/WILDCARD_who_are_you.png) |

| 别过来！ | 后室！ |
| --- | --- |
| ![别过来！](screenshot/WILDCARD_stay_away.png) | ![后室！](screenshot/WILDCARD_backrooms.png) |

后室结束后从主世界进入点上空无伤落回：

![后室返回](screenshot/WILDCARD_backrooms_return.png)

## 外卡列表

| 外卡 | 效果 |
| --- | --- |
| 疾速追猎 | 全员速度提升 |
| 轻盈之身 | 跳跃提升 + 缓降 |
| 全员发光 | 所有玩家发光 |
| 暗夜追猎 | 强制夜晚 + 猎人夜视 |
| 死亡爆炸 | 死亡或击杀触发爆炸 |
| 补给空投 | 生成随机补给箱 |
| 猎人雷达 | 猎人获得最近逃亡者距离提示 |
| 指南针干扰 | 追踪方向偏移 |
| 饥饿追逐 | 饥饿加快，进食获得速度节奏变化 |
| 武器过热 | 连续攻击触发过热惩罚 |
| 轻装上阵 | 轻甲加速，重甲减速 |
| 方块腐化 | 新放置方块延迟消失 |
| 珍珠狂潮 | 定期补给末影珍珠，但使用可能有副作用 |
| 风弹乱斗 | 定期补给风弹 |
| 血怒时刻 | 低血量获得强化 |
| 受伤乱键 | 受伤即随机打乱移动键位，右上角 HUD 显示当前键位 |
| 全体变小 | 所有玩家缩小到约一格高 |
| 小心翼翼 | 所有玩家生命上限变为 3 颗心 |
| 你是谁？ | 全员史蒂夫皮肤，隐藏名牌，TAB 和聊天中名字都显示为"玩家" |
| 别过来！ | 逃亡者获得锋利 255 的无限耐久金剑，无法丢弃，猎人无法拾取 |
| 后室！ | 全员掉入黄色迷宫维度，猎人每 20 秒发红光 1 秒，踩到假地板掉出去回主世界，任一方全员离开则全体返回 |
| 暂时停用 | 本轮无额外效果，用作节奏占位 |

## 配置界面

默认按 `M` 打开配置界面（可在按键设置中修改）。OP 可在等待阶段修改规则；普通玩家在等待阶段用它加入队伍，对局开始后按 `M` 则切换一个精简的对局状态面板。

### 游戏页

查看当前状态、自己的队伍、外卡状态，并由 OP 开始对局。

![游戏页](screenshot/GUI_game.png)

### 队伍页

加入猎人或逃亡者，查看两边人数。

![队伍页](screenshot/GUI_teams.png)

### 基础页

准备时间、结算时间、刷新间隔、准备边界、死亡掉落、阵营平衡（猎人对逃亡者伤害倍率、双方移速倍率）、猪灵交易珍珠概率。

![基础页](screenshot/GUI_basic.png)

### 胜利页

逃亡者胜利方式四选一（击败末影龙、存活指定时间、到达指定位置、收集指定物品），猎人胜利方式二选一（逃亡者全部出局、击杀计数）。

![胜利页](screenshot/GUI_victory.png)

### 复活页

猎人 / 逃亡者复活模式、生命数和复活时间。

![复活页](screenshot/GUI_respawn.png)

### 外卡页

设置外卡间隔与持续时间，逐张开关外卡，部分外卡有单独参数。

![外卡页](screenshot/GUI_wildcards.png)

**调试页**需要先执行 `/hw debug true`，提供开始 / 停止对局、立即抽卡和单独测试任意外卡。

## 环境要求

- Minecraft `1.21.11`
- Fabric Loader `0.16.0+`
- Fabric API
- Java `21`

建议客户端与服务端同时安装。服务端负责对局逻辑，客户端用于配置界面、HUD、皮肤替换和本地语言显示。

## 安装方式

1. 安装 Fabric Loader。
2. 安装 Fabric API。
3. 下载 `MC-Manhunt-Wildcard-<version>.jar`。
4. 将 jar 放入客户端和服务端的 `mods/` 文件夹。
5. 启动游戏或服务端。

首次启动会生成配置文件：

```text
config/hunterwildcard.json
```

内部配置文件名和 MOD ID 仍保留 `hunterwildcard`，用于兼容已有配置、语言 key 和网络协议。

## 玩家命令

| 命令 | 说明 |
| --- | --- |
| `/hw join hunter` | 加入猎人 |
| `/hw join runner` | 加入逃亡者 |
| `/hw leave` | 离开队伍 |
| `/hw status` | 查看当前对局状态 |
| `/hw wildcard list` | 查看外卡启用状态 |

## 管理员命令

以下命令需要 OP 权限：

| 命令 | 说明 |
| --- | --- |
| `/hw start` | 开始对局 |
| `/hw stop` | 停止对局 |
| `/hw wildcard roll` | 立即抽取外卡 |
| `/hw wildcard stop` | 停止当前外卡 |
| `/hw wildcard test <id>` | 测试指定外卡 |
| `/hw config reload` | 重新加载配置 |
| `/hw config save` | 保存配置 |
| `/hw debug true` | 开启调试界面 |
| `/hw debug false` | 关闭调试界面 |

## 设计目标

Manhunt Wildcard 的目标是让 Manhunt 对局更具变化：

- 让追逃节奏不再完全固定
- 增加随机性、临场判断和反制空间
- 保留 Minecraft Manhunt 的核心目标感
- 给服主提供可配置、可测试、可本地化的玩法扩展

## License

本项目使用 MIT License，详见 [LICENSE](LICENSE)。
