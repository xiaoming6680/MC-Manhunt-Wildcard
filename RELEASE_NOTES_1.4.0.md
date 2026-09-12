# Manhunt Wildcard 1.4.0

Minecraft 1.21.11 · Fabric

## 外卡重做

**新增 12 张**：回头杀、吸血鬼、闪电侠、移形换影、受伤瞬移、空间波动、传送门、静止发光、潜行冻结、地动山摇、掉落物炸弹、连锁挖矿。

**删除**：疾速追猎、轻盈之身、全员发光、暗夜追猎、指南针干扰、暂时停用。

**重做**：补给空投（双方中点、每名逃亡者一只、宣布即竖信标柱、20 秒后落地）、猎人雷达（逃亡者全程发光 + 猎人接近警告）、血怒时刻（不到 3 颗心一击必杀）、饥饿追逐（进食 10 秒速度 VI）、武器过热热量条放大、后室双方每 20 秒同亮 5 秒并提示。

## 对局

- 选队时按阵营发光（猎人红、逃亡者蓝），所有发光效果都按阵营着色。
- 逃亡者复活后，击杀他的猎人发光 10 秒并提示名字。
- 外卡间隔与时长可选固定或随机区间；本局抽过的外卡权重衰减。

## 菜单

- 外卡按战斗 / 机动 / 视野 / 环境与道具分类，每类可一键全开全关。
- 首页选队和开始按钮放到第一屏；抽卡动画期间隐藏状态卡。
- 滚动不再重建控件，修复偶尔点不动输入框和下拉框；返回外卡列表时保留滚动位置；设置页标题不再报格式错误。

## 配置

外卡开关改为 `enabledWildcards` 表（旧字段自动迁移）；雷达间隔改为 `hunterRadarWarningDistance`；新增 `spaceShiftIntervalSeconds` 与时长/间隔模式字段。

---

## Wildcard rework (English)

**12 new**: Backstab, Vampire, Flash, Shadow Step, Hurt Teleport, Space Shift, Portals, Still Glow, Sneak Freeze, World Tilt, Drop Bomb, Chain Mining. **Removed**: Speed Rush, Featherweight, Glowing, Night Hunt, Compass Chaos, Disabled. **Reworked**: Supply Drop, Hunter Radar, Blood Rage, Hunger Chase, Weapon Overheat HUD, Backrooms glow.

Lobby team glow, killer highlight after a runner respawns, fixed/random wildcard timing with draw-weight decay, categorised wildcard menu, home page with team picking first, and menu input fixes. Config toggles moved to an `enabledWildcards` map (old keys migrate automatically).
