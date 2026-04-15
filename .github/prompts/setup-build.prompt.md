---
mode: agent
description: ビルド環境の初期セットアップ（Gradle 9.4.1 / Java 25 / Fabric / Minecraft 26.1.2）
---

# ビルド環境セットアップ指示

## 目的
このプロジェクトを Minecraft 26.1.2 向け Fabric MOD として
Java 25 / Gradle 9.4.1 でビルドできる状態にする。

## 実行手順

### 1. server.jar の取得

以下の手順で Minecraft 26.1.2 の `server.jar` を取得し、`libs/` に配置すること。

```bash
# バージョンマニフェストから 26.1.2 のメタデータ URL を取得
curl -s https://launchermeta.mojang.com/mc/game/version_manifest_v2.json \
  | python3 -c "
import json, sys
data = json.load(sys.stdin)
v = next(v for v in data['versions'] if v['id'] == '26.1.2')
print(v['url'])
"
# 出力された URL から server の download URL を取得してダウンロード
# 取得した URL をもとに libs/minecraft-server-26.1.2.jar として保存する
```

取得できない場合は、Fabric の公式メタ API も試みること。
```
https://meta.fabricmc.net/v2/versions/game
```

### 2. Gradle ラッパーの確認・更新

`gradle/wrapper/gradle-wrapper.properties` の `distributionUrl` が
Gradle **9.4.1** を指していることを確認する。
異なる場合は以下のコマンドで更新する。

```bash
./gradlew wrapper --gradle-version=9.4.1 --distribution-type=bin
```

### 3. build.gradle.kts の検証と修正

`build.gradle.kts` を開き、以下の項目を確認・修正すること。

- Java ツールチェーンが **Java 25** を指定しているか
- Fabric Loom のバージョンが Gradle 9.x に対応しているか
- Minecraft バージョンが **26.1.2** になっているか
- Fabric API のバージョンが 26.1.2 対応の最新版か

### 4. 逆コンパイルソースの生成

```bash
./gradlew genSources
```

エラーが出た場合は、Loom のバージョン不一致が原因である可能性が高い。
エラーメッセージを日本語で解説した上で修正案を提示すること。

### 5. 試験ビルド

```bash
./gradlew build
```

ビルドが通れば完了。失敗した場合は `.github/prompts/fix-build-error.prompt.md` の手順に従うこと。

## 完了条件

- `./gradlew build` が成功する
- `build/libs/` 配下に `.jar` ファイルが生成される
- コンパイル警告が最小化されている（非推奨 API の使用がない）
