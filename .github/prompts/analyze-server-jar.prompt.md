---
mode: agent
description: server.jar の構造解析 — 実装前に現バージョンの API を確認する
---

# server.jar 構造解析指示

## 目的

Minecraft 26.1.2 の内部 API は公開ソースがない。
実装を始める **前に必ずこの手順で API を確認** し、
存在しないクラス・メソッドへの依存を防ぐこと。

## 前提条件

- `libs/minecraft-server-26.1.2.jar` が存在すること
  （なければ `setup-build.prompt.md` の手順で取得する）
- `./gradlew genSources` が完了していること

## 解析手順

### Step 1. 逆コンパイルソースの場所を確認する

```bash
find .gradle/loom-cache -name "*.java" | head -5
```

通常は `.gradle/loom-cache/minecraftMaven/` または
`.gradle/loom-cache/remapped/` 配下にソースが展開される。

### Step 2. 旧バージョンのクラスが存在するか確認する

実装に必要なクラスが 26.1.2 でも存在するか確認する。

```bash
# 例：ServerPlayerEntity の現在のパッケージを調べる
grep -r "class ServerPlayerEntity" .gradle/loom-cache/ --include="*.java"

# 例：Registry クラスの register メソッドシグネチャを確認する
grep -A5 "register" .gradle/loom-cache/net/minecraft/registry/Registry.java 2>/dev/null \
  || find .gradle/loom-cache -name "Registry.java" -exec grep -l "register" {} \;
```

### Step 3. パッケージ変更の全体像を把握する

旧バージョンで使用していたクラス群のパッケージ変更を一括確認する。

```bash
# net.minecraft 配下のパッケージ一覧を表示
find .gradle/loom-cache -type d -name "minecraft" | head -3 | xargs -I{} find {} -type d | sort
```

### Step 4. Fabric API の変更点を確認する

```bash
# fabric モジュールの公開 API を一覧表示
find .gradle/loom-cache -path "*/fabric*" -name "*.java" | grep -v test | head -20
```

### Step 5. 解析結果を記録する

解析で判明したクラス移動・メソッド変更は、
`docs/api-migration-notes.md` に以下の形式で記録すること。

```markdown
## API 移行メモ（26.1.2 対応）

| 旧クラス / メソッド | 新クラス / メソッド | 確認日 |
|---|---|---|
| ServerPlayerEntity | （調査結果を記入） | YYYY-MM-DD |
| Registry.register() | （調査結果を記入） | YYYY-MM-DD |
```

## 解析が必要な主要クラス一覧

以下のクラスは本プロジェクトで使用頻度が高い。
実装開始前に必ず現バージョンでの存在とシグネチャを確認すること。

- `ServerPlayerEntity`（プレイヤー操作全般）
- `ServerWorld`（ワールド操作）
- `Registry` / `RegistryKey`（アイテム・エンティティ登録）
- `ServerBossBar`（BossBar 表示）
- `Text` / `MutableText`（チャット・タイトル表示）
- `ServerCommandSource`（コマンド処理）
- `ActionBar` に相当する Title/Subtitle 送信 API
- カスタムパケット送受信クラス
