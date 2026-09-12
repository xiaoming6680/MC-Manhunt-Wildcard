# Releasing / 发布

## English

1. Follow [Building](BUILDING.md) to build and test all fifteen stable Minecraft targets. Set the mod version in the root `gradle.properties`; the version projects inherit it.
2. Update the English-first README, changelog, release notes and [Modrinth description](MODRINTH_DESCRIPTION.md). Keep the separate English and Chinese guides aligned.
3. Run `powershell -File scripts/package-release.ps1`. This verifies each JAR's mod ID, mod version and exact Minecraft dependency before copying the fifteen installable files and SHA-256 checksums to `dist/<version>/`.
4. Commit the reviewed source, documentation and curated screenshots. Push the commit and annotated `v<version>` tag, then create one GitHub release with the bilingual notes, fifteen JARs and `SHA256SUMS.txt`.
5. On Modrinth, create **one version entry per Minecraft target**. Drop only its matching JAR into the upload dialog. Use `1.4.6+mc26.2` as the version-number pattern and `Manhunt Wildcard 1.4.6 (Minecraft 26.2)` as the title pattern. Select Release, Fabric, the exact game version, and Fabric API as a required dependency. Paste the English-first bilingual [Modrinth changelog](MODRINTH_CHANGELOG.md).
6. Replace the project description with `docs/MODRINTH_DESCRIPTION.md`. Verify the public version list, download metadata and description after publishing.

Do not upload a ZIP or sources JAR as the primary file. A single version entry with multiple incompatible game JARs cannot reliably select the correct primary download. Files in `dist/` are generated release assets and are intentionally ignored by Git.

## 简体中文

1. 按[构建文档](BUILDING.md)构建、测试全部十五个正式版本。模组版本只在根目录 `gradle.properties` 中修改。
2. 同步更新英文在前的 README、更新日志、发布说明与 [Modrinth 介绍](MODRINTH_DESCRIPTION.md)，保持独立中英文文档一致。
3. 运行 `powershell -File scripts/package-release.ps1`。脚本核对每个 JAR 的模组 ID、模组版本和精确游戏版本，将十五个安装包与 SHA-256 校验文件整理到 `dist/<版本>/`。
4. 提交并推送审阅后的代码、文档与展示截图，创建带说明的 `v<版本>` 标签。一个 GitHub Release 附带双语说明、十五个 JAR 和 `SHA256SUMS.txt`。
5. Modrinth **每个游戏版本单独创建一个发布条目**，只拖入对应 JAR。版本号格式为 `1.4.6+mc26.2`，标题格式为 `Manhunt Wildcard 1.4.6 (Minecraft 26.2)`。选择 Release、Fabric、精确游戏版本，并将 Fabric API 设为必需依赖，粘贴[英文在前的双语更新日志](MODRINTH_CHANGELOG.md)。
6. 用 `docs/MODRINTH_DESCRIPTION.md` 替换项目介绍，发布后核对公开版本列表、下载信息和介绍。

不要把 ZIP 或源码 JAR 当作主文件；不要在同一条目中堆放多个不兼容游戏版本的 JAR。`dist/` 是生成的发布附件目录，已被 Git 忽略。
