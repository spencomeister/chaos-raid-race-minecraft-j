---
mode: agent
description: 新機能の実装テンプレート — 仕様書をもとに Fabric MOD の機能を実装する
---

# 機能実装指示テンプレート

## 実装前チェックリスト

機能を実装する前に、必ず以下を確認すること。

- [ ] `analyze-server-jar.prompt.md` に従い、使用する API が 26.1.2 に存在することを確認した
- [ ] `./gradlew build` が現時点で通っている（クリーンな状態から始める）
- [ ] 実装対象の仕様が `docs/chaos_raid_race_spec.md` に記載されている

## 実装時の原則

### 言語・コメント
- すべてのコメント、Javadoc、ログメッセージは **日本語** で記述する。
- プレイヤーへの通知文字列（チャット・タイトル・ActionBar・BossBar）は日本語。

### コード品質
- 1 クラス = 1 責任（単一責任の原則）
- `public`・`protected` なメソッドには Javadoc を必ず付ける。
- マジックナンバーは定数（`static final`）または config 値として切り出す。
- Null を返すメソッドは極力避け、`Optional` を使う。

### Fabric MOD 固有の注意点
- サーバーサイドの処理はすべてサーバースレッドで行う。
  クライアントサイドコードを混入させないこと（`@Environment(EnvType.CLIENT)` 注意）。
- イベントリスナーは `ModInitializer#onInitialize()` で登録する。
- ワールドへの変更は `ServerWorld` 経由で行う。`World` の直接キャストは避ける。

### エラーハンドリング
- 例外は握りつぶさず、必ずログに記録する。
- プレイヤーが操作する機能は、不正な入力に対してわかりやすい日本語エラーメッセージを返す。

## 実装後チェックリスト

- [ ] `./gradlew build` が成功する
- [ ] 実装した機能が `docs/chaos_raid_race_spec.md` の仕様を満たしている
- [ ] 新規追加した public メソッドに Javadoc がある
- [ ] コメント・ログが日本語になっている
- [ ] 不要な `TODO` コメントや未使用 import がない

## 仕様書の参照方法

実装対象の機能仕様は必ず `docs/chaos_raid_race_spec.md` を参照すること。
仕様が不明瞭な箇所は実装前に確認を求め、推測で実装しないこと。
