---
mode: agent
description: ビルドエラーの自動解析・修復（Minecraft 26.1.2 への移行起因エラー対応）
---

# ビルドエラー修復指示

## 前提

Minecraft 26.1.2 は 1.21.11 から大きく API が変更されている。
ビルドエラーの多くは旧バージョンの API がそのまま残っていることに起因する。
**焦って一括修正せず、エラーを 1〜2 個ずつ段階的に解消すること。**

## 基本フロー

```
1. ./gradlew build でエラーを確認
       ↓
2. エラーメッセージを日本語で解説する
       ↓
3. エラーの種別を分類する（下記の分類表を参照）
       ↓
4. server.jar の逆コンパイルソースで代替 API を検索する
       ↓
5. 修正を適用する
       ↓
6. ./gradlew build を再実行して確認する
       ↓
7. 成功するまで 2〜6 を繰り返す
```

## エラー種別と対処方針

### ① シンボルが見つからない（クラス・メソッド・フィールド）

**症状例：**
```
error: cannot find symbol
  symbol:   class ServerPlayerEntity
```

**対処：**
`genSources` で生成した逆コンパイルソース（通常 `.gradle/loom-cache/` 配下）を
以下のキーワードで検索し、移動・改名された新しいクラスを特定する。

```bash
grep -r "ServerPlayerEntity" .gradle/loom-cache/ --include="*.java" -l
# または IDE の「全体検索」で同等の機能を使う
```

見つかった場合は import 文と使用箇所を新しいクラス名に置換する。

---

### ② レジストリ API の不一致

**症状例：**
```
error: method register in Registry<T> cannot be applied to given types
```

**対処：**
`Registry` クラスのシグネチャが変更されている。
逆コンパイルソースで `Registry.java` を開き、`register` メソッドの
新しいシグネチャを確認して呼び出し方を修正する。

---

### ③ Fabric イベントコールバックの型不一致

**症状例：**
```
error: incompatible types: lambda ...
```

**対処：**
GitHub の `FabricMC/fabric` リポジトリで該当イベントのソースを確認する。
`callback` の関数型インターフェースが変更されていないか確認し、
引数の型・順序を修正する。

---

### ④ DataComponent 関連エラー

**症状例：**
```
error: cannot find symbol
  symbol:   method getNbt()
```

**対処：**
1.21.x 以降、アイテムのデータは `NbtCompound` から `DataComponentType` へ移行している。
対象アイテムについて、新しい `ComponentType` を特定し、
`get(DataComponentTypes.XXX)` 形式で書き直す。

---

### ⑤ Gradle ビルドスクリプトのエラー

**症状例：**
```
Could not find method compile() for arguments ...
```

**対処：**
Gradle 9.x では古い `compile`・`runtime` configuration が廃止されている。
`implementation`・`runtimeOnly` 等に置き換えること。

---

## 解決困難なエラーの報告フォーマット

どうしても解決できないエラーは、以下の形式で報告すること。

```
【未解決エラー報告】
- エラー発生箇所：<ファイルパス>:<行番号>
- エラーメッセージ：<原文>
- 推定原因：<日本語での分析>
- 試みた対処：<実施した内容>
- 調査コマンド例：
    grep -r "<キーワード>" .gradle/loom-cache/ --include="*.java"
```
