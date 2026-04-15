# GitHub Copilot 共通指示書

> **最優先ルール**：このリポジトリに関するすべての応答・コード・コメント・ドキュメントは **日本語** で行うこと。
> 英語を使用するのは、識別子（クラス名・メソッド名・変数名）、Minecraft/Fabric の公式 API 名、
> および外部ライブラリのシンボル名に限定する。

---

## プロジェクト概要

本プロジェクトは **カオス・レイド・レース** と呼ばれる Minecraft 用ゲームイベントの
サーバーサイド Fabric MOD である。
参加者はバニラクライアントで参加でき、サーバー側のみ MOD を導入して動作させる。

---

## ターゲット環境

| 項目 | バージョン |
|---|---|
| Minecraft | **26.1.2** |
| Mod Loader | Fabric |
| Java | **25** |
| Gradle | **9.4.1** |
| Fabric API | Minecraft 26.1.2 対応の最新安定版 |
| Fabric Loom | Gradle 9.x 対応の最新版 |

### ⚠️ バージョン移行に関する重要注意事項

Minecraft **26.1.2** は **1.21.11 から大きくデータ構造・API 設計が変更** されている。
以下の点を常に意識してコードを記述すること。

- **パッケージ構成の変更**：`net.minecraft.*` 配下のパッケージが再編されている可能性がある。
  コードを書く前に必ず後述の「構造解析手順」を参照し、実際の API を確認すること。
- **レジストリ API の変更**：`Registry`・`RegistryKey` 等の取り扱いが変わっている可能性がある。
  古い 1.x 系の記法をそのまま転用しないこと。
- **イベント API の変更**：Fabric API のコールバック名・引数型が変わっている場合がある。
- **NBT / DataComponentType**：アイテム・エンティティのデータモデルが Component 方式へ移行している。
  旧来の `NbtCompound` 直接操作に頼らず、ComponentType を優先すること。
- **ネットワークパケット API**：カスタムパケットの送受信 API が刷新されている可能性がある。

---

## ローカルビルド環境のセットアップ

### ディレクトリ構成ルール

Java 実行環境および Gradle ビルド環境は、
**プロジェクトフォルダの 1 つ親ディレクトリ** に配置すること。

```
（親ディレクトリ）/
├── .java/          ← JDK をここに展開する
├── .gradle-home/   ← Gradle をここに展開する
└── chaos-raid-race-minecraft-j/   ← プロジェクトフォルダ
    ├── .github/
    ├── build.gradle.kts
    └── ...
```

> **⚠️ PATH には追加しないこと。**
> システム全体の環境変数 `PATH`・`JAVA_HOME`・`GRADLE_HOME` は変更しない。
> ビルド実行時は常に上記パスを明示的に指定して呼び出すこと。

### バージョンの調査と取得

> **必ず最新バージョンを調査してから取得すること。**
> 以下に記載のバージョンはあくまで「最低要件」であり、
> 作業開始時点の最新安定版が存在する場合はそちらを優先すること。

#### JDK の取得

1. 作業開始前に以下のいずれかで最新の Java 25+ LTS / 最新安定版を確認する。
   - [Adoptium Temurin リリース一覧](https://adoptium.net/temurin/releases/)
   - [Oracle JDK ダウンロードページ](https://www.oracle.com/java/technologies/downloads/)
2. 確認した最新版の **zip / tar.gz アーカイブ（インストーラーではないもの）** を取得する。
3. 親ディレクトリ直下の `.java/` フォルダに展開する。

```
（親ディレクトリ）/.java/jdk-XX.X.X/   ← 展開先
```

#### Gradle の取得

1. 作業開始前に [Gradle リリースページ](https://gradle.org/releases/) で
   最新安定版のバージョン番号を確認する。
2. **binary-only zip**（`gradle-X.X.X-bin.zip`）を取得する。
3. 親ディレクトリ直下の `.gradle-home/` フォルダに展開する。

```
（親ディレクトリ）/.gradle-home/gradle-X.X.X/   ← 展開先
```

### PATH を通さない実行方法

ビルドコマンドは以下のように **フルパスで指定** して実行すること。
（以下は相対パス記述例。実際のパスは環境に合わせて調整すること。）

```bash
# Windows（PowerShell）の例
$JAVA  = ".\..\  .java\jdk-XX.X.X\bin\java.exe"
$GRADLE = "..\.gradle-home\gradle-X.X.X\bin\gradle.bat"

& $GRADLE build -Dorg.gradle.java.home=".\.java\jdk-XX.X.X"

# macOS / Linux（bash）の例
JAVA_HOME="../.java/jdk-XX.X.X"
GRADLE="../.gradle-home/gradle-X.X.X/bin/gradle"

$GRADLE build -Dorg.gradle.java.home="$JAVA_HOME"
```

または `gradle/wrapper/gradle-wrapper.properties` の
`org.gradle.java.home` にフルパスを記載して固定する方法も可。

### セットアップ後の確認

```bash
# バージョン確認（フルパス指定で実行）
../.java/jdk-XX.X.X/bin/java -version
../.gradle-home/gradle-X.X.X/bin/gradle --version
```

両コマンドが正常に出力されれば環境構築完了。

---

## ビルドツールチェーン

### Gradle 9.4.1 + Java 25 での注意点

- `settings.gradle.kts` は Kotlin DSL を使用すること。
- Java 25 の言語機能（sealed class、pattern matching、record 等）を積極的に活用してよい。
- Gradle の `toolchains` ブロックで Java 25 を明示的に指定すること。

```kotlin
// build.gradle.kts 記述例（参考）
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
```

- Fabric Loom の設定は Gradle 9.x の新しい API に従うこと。
  非推奨になった `compile`・`runtime` configuration は使用しないこと。

---

## server.jar を用いた構造解析

Minecraft の内部 API はソースが公開されておらず、かつバージョンごとに変化する。
このプロジェクトでは **server.jar を取得して逆コンパイル・解析することで正確な API を確認** しながら実装する。

### server.jar の取得手順

1. Minecraft の公式ランチャーメタデータ、または Fabric のバージョンマニフェスト API から
   Minecraft 26.1.2 の `server.jar` ダウンロード URL を取得する。

   ```
   https://launchermeta.mojang.com/mc/game/version_manifest_v2.json
   ```

   → `versions` 配列から `id: "26.1.2"` のエントリを探し、
     `url` フィールドのバージョン詳細 JSON を取得する。
   → `downloads.server.url` から `server.jar` をダウンロードする。

2. ダウンロードした `server.jar` を `libs/minecraft-server-26.1.2.jar` に配置する。

3. Fabric Loom が提供する `genSources` タスクを使用して逆コンパイル済みソースを生成する。

   ```bash
   ./gradlew genSources
   ```

### 構造解析の進め方

コードを実装する前に、以下の方法で現バージョンの実際の API を確認すること。

1. **IDE での参照**：`genSources` 後に IDE（IntelliJ IDEA 推奨）でソースを参照する。
2. **Grep・検索**：旧バージョンのクラス名をキーワードにして、
   逆コンパイルソース内で同等のクラスを探す。
3. **Fabric API ソース**：GitHub 上の `FabricMC/fabric` リポジトリの
   `1.21.x` → `26.1.x` 差分を参照して API 変更点を把握する。

---

## 自動ビルド・修復の方針

### ビルドが失敗した場合の対処フロー

1. **エラーメッセージを必ず日本語で解説** してから修正案を提示する。
2. エラーの原因が「バージョン変更による API の廃止・移動」である場合は、
   server.jar 解析結果を根拠にして代替 API を特定し、置換案を提示する。
3. **段階的修復**：一度に多くの変更を加えず、コンパイルエラーを 1〜2 個ずつ解消する。
4. 修復後は必ず `./gradlew build` が通ることを確認してから次に進む。
5. 解決不能なエラーは「この問題は〇〇が原因と考えられますが、
   server.jar の解析が必要です。以下のコマンドで確認してください」と
   具体的な調査コマンドを添えて報告する。

### よくある移行エラーのパターン

| エラーパターン | 推奨対処 |
|---|---|
| シンボルが見つからない（クラス・メソッド） | server.jar の逆コンパイルソースで同名・類似名を検索する |
| `Registry.register()` のシグネチャ不一致 | 新しいレジストリ API のシグネチャを解析ソースで確認する |
| Fabric イベントコールバックの型不一致 | Fabric API の 26.1.x 対応ブランチのソースを参照する |
| NBT 操作の API 不一致 | DataComponentType への移行を検討する |
| Gradle タスクの非推奨警告 | Gradle 9.x の API に従い書き換える |

---

## 仕様変更・実現不能時の確認ルール

> **⚠️ 自己判断で実装を変更・省略しないこと。**

以下のいずれかの状況に該当した場合は、**作業を止めて必ず確認を求める**こと。
推測や代替案による「自己解決」は禁止する。

### 確認が必要な状況

1. **仕様変更が必要と判断した場合**
   `docs/chaos_raid_race_spec.md` に記載された仕様と、
   実装中に判明した制約（API・パフォーマンス・MC の仕様限界等）が矛盾する場合。

2. **現バージョン (26.1.2) で同等機能が実現できないと判断した場合**
   旧バージョン向けの設計が 26.1.2 の API では直接再現できず、
   かつ自明な代替手段が存在しない場合。

3. **複数の実装方針が考えられ、どちらを選ぶかで仕様の振る舞いが変わる場合**
   どちらの方針でもビルドは通るが、プレイヤー体験や動作が異なる場合。

4. **仕様書に記載のない挙動について判断が必要な場合**
   仕様書に明示されていないエッジケース（例：同時発動ルールの競合・異常系）に
   ついて実装の判断が求められる場合。

### 確認時の報告フォーマット

確認を求める際は、以下の形式で状況を日本語で整理して報告すること。

```
【確認が必要な事項】
- 該当箇所：<ファイルパス または 仕様書のセクション名>
- 状況：<何が問題になっているかの説明>
- 制約・原因：<技術的な制約や判明した事実>
- 選択肢 A：<案の概要と、仕様への影響>
- 選択肢 B：<案の概要と、仕様への影響>
（選択肢が 1 つしかない場合は「選択肢 A」のみ記載し、確認を求める）
- 質問：どちらの方針で進めるか、または仕様を変更するかを教えてください。
```

---

## コーディング規約

- **コメント**：すべて日本語で記述する。
- **Javadoc**：公開 API（`public`・`protected`）には必ず Javadoc コメントを付ける。内容は日本語。
- **エラーメッセージ**：プレイヤーへの通知文字列はすべて日本語。
- **ログ**：`LOGGER.info()`・`LOGGER.warn()` 等のメッセージも日本語で記述する。
- **パッケージ名**：`io.github.<作者名>.chaosraidrace` を基底パッケージとする。
- **クラス設計**：責任を単一に保つ（SRP）。1 クラスの行数は原則 300 行以内を目安とする。

---

## このファイルの更新ルール

- Minecraft または Fabric のバージョンを変更した場合は、このファイルの
  「ターゲット環境」テーブルを最初に更新すること。
- server.jar 解析で新たな API 変更パターンを発見した場合は、
  「よくある移行エラーのパターン」テーブルに追記すること。
