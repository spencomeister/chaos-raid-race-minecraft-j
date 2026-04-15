# カオス・レイド・レース 詳細仕様書 v3.5
Fabric サーバーサイド MOD 実装向け / 総プレイ時間：120分 / PvEメイン・PvP自然発生あり / オーバーワールド＋エンド（ネザーなし）

---

## 1. ゲーム全体の流れ・タイムライン

```
00:00  ── ゲーム開始・初期配布・PvP無効
00:00 〜 05:00  ── 準備フェーズ（カオスルールなし・PvP無効）
05:00  ── 第1カオスルール開始・PvP解禁
         ↓ 以降5分ごとにカオスルール交代
20:00  ── 【第1中ボス】巨大ゾンビ出現
40:00  ── 【第2中ボス】巨大ラヴェジャー出現
         後半戦突入：ルール2枚同時発動が解禁
60:00  ── 【中間スコア発表】最下位救済
80:00  ── 【第3中ボス】巨大ウォーデン出現
100:00 ── 全討伐Pt 1.5倍・ラストスパート宣言
110:00 ── カオスルール終了・エンド移動準備
110:00 ── 【最終フェーズ】エンド強制TP・PvP総力戦・エンドラ出現
120:00 ── ゲーム終了・最終スコア集計・表彰
```

### フェーズ詳細

| フェーズ | 時間 | 内容 |
|---|---|---|
| 準備 | 0〜5分 | PvP無効。装備確認・拠点配置・VC確認 |
| 序盤 | 5〜40分 | カオスルール1枚。中ボス×1。装備を整えながらPt稼ぎ |
| 中盤 | 40〜80分 | カオスルール**2枚同時**解禁。中ボス×2。PvPが自然発生しやすい |
| 終盤 | 80〜110分 | カオスルール2枚継続。中ボス×1。Pt1.5倍で逆転要素 |
| 最終決戦 | 110〜120分 | エンド・PvP総力戦・エンドラ討伐 |

---

## 2. カオスルール 完全詳細仕様

### 2-1. 抽選ロジック

```
【前半 5〜40分】ルールを1枚ずつ抽選（重複なし）
【後半 40〜110分】ルールを2枚同時抽選（組み合わせ重複なし）
全ルール消化後はプールをリセット・再シャッフル。連続同一ルール禁止。
「Normal（何も起きない）」は全体の約20%の確率で抽選プールに含まれる。
```

### 2-2. 予告・演出タイムライン（実装仕様）

```
T-30秒  ▶ 予告チラ見せ
           BossBar更新: 「次のルール：[ルール名] まで 0:30」
           チャット: §e§l【予告】30秒後のルール → [ルール名]
           Sound: BLOCK_NOTE_BLOCK_HAT（低音・1回）

T-10秒  ▶ カウントダウン開始
           BossBar: 「次のルールまで：00:10」
           毎秒 ActionBar に §c10, 9, 8...と表示
           Sound: BLOCK_NOTE_BLOCK_HAT（毎秒・音程を徐々に上げる）

T-0     ▶ ルール発動
           Title: §6§l[ルール名]
           Subtitle: §f[ルールの説明文（例：空から矢が降ってくる！屋根を探せ！）]
           Sound: ENTITY_LIGHTNING_BOLT_THUNDER（ダメージなし）
           BossBarリセット: 「現在のルール：[名前] ／ 次のルールまで：05:00」

T+4:30  ▶ 残り30秒警告
           ActionBar: §c残り30秒！
           Sound: BLOCK_NOTE_BLOCK_HAT（高音・連打）

T+5:00  ▶ ルール終了
           Sound: ENTITY_PLAYER_LEVELUP
           次の予告サイクルへ
```

### 2-3. ルール一覧（全13種）

**【制約系】**

| # | ルール名 | タイトル表示 | 説明文（Subtitle） | 内容詳細 |
|---|---|---|---|---|
| 1 | 止まると死ぬぜ？ | §c§l静止禁止 | 3秒以上止まると足元が爆発する！動き続けろ！ | 3秒静止でTNT相当の小爆発。エフェクト：足元に煙パーティクル |
| 2 | 空が気になるお年頃 | §e§l視点固定 | 視点が強制固定された！慣れない方向で戦え！ | Pitchを上向き（-80）or下向き（+80）にランダム固定 |
| 3 | 右の頬を出せ | §a§l不殺の誓い | 与えたダメージは自分にも返ってくる！慎重に！ | AttackEntityCallbackで与ダメと同値を自分に反映。PvP含む |
| 4 | 君の荷物は僕の物 | §d§lシャッフル予告 | ルール終了時に全員のインベントリが入れ替わる！ | 終了タイミングで全参加者のインベントリをランダム再配布 |
| 5 | お空からのお仕置き | §c§l弓矢の雨 | 空から矢が降ってくる！屋根を探せ！ | SkyLight届く屋外に1秒ごと矢スポーン（ダメージ通常） |

**【物理系】**

| # | ルール名 | タイトル表示 | 説明文（Subtitle） | 内容詳細 |
|---|---|---|---|---|
| 6 | 月面着陸 | §b§l低重力 | ジャンプ力5倍・落下ダメージ無効！空中を駆けろ！ | JumpBoost IV付与＋FallDamageキャンセル |
| 7 | お豆腐建築ハンター | §6§l連鎖破壊 | ブロックを1つ壊すと隣接する同種も連鎖崩壊！ | 隣接同種ブロック最大32個を同時破壊 |
| 8 | 中身はお楽しみ | §d§lランダムドロップ | ブロックを壊すと何が出るか分からない！ | 破壊時のドロップをAllItemsからランダム選出 |
| 9 | 透視能力に目覚めた | §f§l全員発光 | 全員が発光！隠れても無駄だ！ | 全プレイヤー・Mobに常時GLOWINGエフェクト |

**【ボーナス系】**

| # | ルール名 | タイトル表示 | 説明文（Subtitle） | 内容詳細 |
|---|---|---|---|---|
| 10 | 石ころ錬金術師 | §e§l錬金タイム | 石を掘ると5%の確率でレアドロップ！掘り進め！ | Stone破壊時5%でダイヤ/金/鉄原石を直ドロップ |
| 11 | 爆走経験値 | §a§lダッシュ稼ぎ | 走れば走るほど経験値が溢れ出す！止まるな！ | isSprinting()中のみ1秒ごとに経験値オーブ生成 |
| 12 | モブのボーナス期 | §6§lハッピーモブ | この5分間、全ての討伐ポイントが2倍！稼げ！ | 期間中のMob討伐Pt（累計・消費とも）を2倍 |

**【特殊】**

| # | ルール名 | タイトル表示 | 説明文（Subtitle） | 内容詳細 |
|---|---|---|---|---|
| 13 | Normal | §7§lつかの間の平和 | 特に何も起きない。今のうちに作業を片付けろ！ | 何もなし。20%の確率でプールに追加 |

### 2-4. 同時発動の組み合わせ例（後半）

| 組み合わせ | 相乗効果 |
|---|---|
| 月面着陸 ＋ お空からのお仕置き | 浮いている間に矢が降ってくる地獄 |
| 透視能力 ＋ 止まると死ぬぜ？ | 全員丸見えで動き続けなければならない修羅場 |
| 石ころ錬金術師 ＋ 連鎖破壊 | 採掘が爆発的に稼げる逆転タイム |
| 不殺の誓い ＋ モブのボーナス期 | 戦闘で稼ぎたいのに自爆する矛盾 |
| Normal ＋ 何か | Normalが来ても2枚目はあるので油断禁物 |

---

## 3. カオスルール連動ミッション（ルール・コレクター）

### 3-1. 概要
ルールが切り替わるたびに、**そのルールに紐づいたチームミッション**が提示される。クリアすると「欠片」を獲得。一定数集めると追加ボーナス。

### 3-2. ミッション一覧

| 対象ルール | ミッション内容 | 報酬（チーム） |
|---|---|---|
| 止まると死ぬぜ？ | 5分間、チーム全員が一度も爆発を受けずに生存 | 累計Pt +200 |
| 空が気になるお年頃 | この5分間にMobを10体討伐 | 消費Pt +300 |
| 不殺の誓い | 5分間、チーム全員がデスなし | 累計Pt +300 |
| 弓矢の雨 | 屋内で敵を5体倒す | 消費Pt +200 |
| 月面着陸 | 空中でMobを3体倒す | 累計Pt +150 |
| 連鎖破壊 | 石を合計200個採掘 | 消費Pt +150 |
| 発光 | 発光状態のMobを15体倒す | 累計Pt +250 |
| 錬金術師 | レアドロップを3個獲得 | 消費Pt +500 |
| ハッピーモブ | 中堅以上のMobを5体討伐 | 累計Pt +400 |
| Normal | 5分間で一番多くのMobを倒したチームが勝利 | 累計Pt +100 |

### 3-3. 欠片の使い道
- **3欠片ごと**にショップで「欠片パック」と交換可能（消費Pt 300相当）
- **6欠片**集めたチームは中ボス出現時にBossBarに自チーム名が表示される（最後の一撃演出）
- ミッション達成通知：チャットに `§a[ミッション達成！] チームA：[ミッション名] クリア！(欠片+1)` と全体通知

---

## 4. 最終フェーズ「カオス・エンド決戦」（110〜120分）

### 4-1. コンセプト
- **PvP総力戦 × エンドラ討伐** の同時進行。
- エンドラは「倒せればボーナス」の報酬対象。倒せなくてもゲームは成立。
- スコアの累計はサイドバーが**バグ演出で見えなくなる**。ポイント獲得通知だけが頼り。
- 「今自分たちが勝ってるのか負けてるのか分からない」緊張感の中で10分間駆け抜ける。

### 4-2. エンドマップ事前整備（GM作業・ゲーム前）

| 箇所 | 内容 |
|---|---|
| 地面の平坦化 | 中央島の凹凸をならし、全体的にフラットな足場を確保 |
| チーム別スタート地点 | 中央から等距離に3箇所、各チームのワープ到着点を設置（石レンガ等でマーク） |
| 遮蔽物・壁 | 黒曜石の柱を活かしつつ、低い石壁や段差を数カ所設けて射線を切れるようにする |
| 奈落ガード | 島の端に1〜2ブロックの縁石を設置。完全には防げないが初心者の事故を軽減 |
| エンドクリスタル | デフォルト配置のまま残す（破壊がゲームプレイの一部） |

> 縁石は「完全な転落防止」でなく「うっかり落ち」を減らす程度に留める。奈落への転落リスクは緊張感として残す。

### 4-3. 突入タイムライン

```
108:00  BossBar: §c§l【最終フェーズまで 2:00】装備を整えろ！
109:30  全チャット: §4§l【警告】30秒後、エンドへ強制転送されます！
110:00  /tp @a [エンド中央島・チーム別スタート地点] 強制転送
         エンダーマンを全頭一掃: world.getEntitiesByType(ENDERMAN).forEach(Entity::discard)
         以降のエンダーマンスポーンを完全無効化（ディメンション限定）
         keepInventory をエンドディメンション限定で ON（アイテムロストなし）
         サイドバーのスコア表示がバグ演出に切り替わる
         Title:   §4§l奪い合え。生き残れ。
         Subtitle: §fスコアは見えない。信じられるのは自分の手だけだ。
         Sound: WITHER_SPAWN（全員）
         エンドラ召喚（/summon ender_dragon）
110:30  チャットに初心者向けガイドを順次表示（下記参照）
120:00  ゲーム終了・バグ演出解除・最終スコア発表
```

### 4-4. 初心者向けガイド（チャットに順次表示）

```
§b[ガイド①] エンドクリスタル（柱の上の光る球）を先に全部壊そう！回復が止まる！
§b[ガイド②] 弓矢で遠くから水晶を狙うのが安全！
§b[ガイド③] エンドラが降りてきたら近接攻撃のチャンス！頭を狙え！
§b[ガイド④] 奈落（黒い虚空）に落ちると即死！端には近づくな！
§b[ガイド⑤] 敵チームのプレイヤーも倒せばポイントになる！
```

### 4-5. スコア「バグ演出」仕様

| 状態 | サイドバー表示 | ActionBar（獲得通知） |
|---|---|---|
| 通常フェーズ（〜109:59） | `チームA: 4,820 pt`（正常） | `+5 pt (Wallet)` など正常表示 |
| 最終フェーズ（110:00〜） | `チームA: #X?@ pt`（全チーム同一桁数のランダム文字） | 正常表示のまま（獲得は見える） |
| ゲーム終了（120:00） | バグ解除 → 最終スコアをドラマチックに表示 | ― |

**バグ演出の実装イメージ（Fabric）**
```java
// 20tickごとに全チームのスコアを同一桁数のランダム文字列で上書き
// 桁数は「全チーム中の最大スコアの桁数」に統一することで桁数からの差バレを防ぐ
String[] glitch = {"#", "?", "!", "X", "%", "@", "$", "&", "*"};
Random rng = new Random();

// 全チームの実スコアから最大桁数を取得
int maxDigits = teams.stream()
    .mapToInt(t -> String.valueOf(t.getScore()).length())
    .max()
    .orElse(4);

// 全チーム同じ桁数でランダム文字列を生成
teams.forEach(team -> {
    String fakeScore = IntStream.range(0, maxDigits)
        .mapToObj(i -> glitch[rng.nextInt(glitch.length)])
        .collect(Collectors.joining());
    String fake = team.getName() + ": " + fakeScore + " pt";
    scoreboard.getTeam(team.getId()).setDisplayName(Component.literal(fake));
});
```
> 全チームの表示桁数を「最大スコアの桁数」に揃えることで、桁数からスコア差が推測できない。全チームが `チームA: #X?@ pt` `チームB: $&%# pt` `チームC: !X$? pt` と同じ桁で表示される。

### 4-6. PvPキル報酬（最終フェーズ限定）

| 条件 | 累計Pt | 消費Pt | 備考 |
|---|---|---:|---|
| プレイヤーキル | +150 | +150 | キルした側に加算 |
| キルされた側 | ― | -30% 没収 | 通常デスペナより重い |
| 連続キル（2キル目以降） | +200→+250→… | 同倍率 | 連続キルで報酬が増加 |

### 4-7. エンドラ討伐ポイント（PvPと並行）

| 条件 | 累計Pt | 消費Pt |
|---|---|---|
| クリスタル破壊（1個あたり） | +30 | +50 |
| エンドラ討伐ラストヒット（チーム） | +1500 | +2000 |
| エンドラ討伐時に全員生存 | 全チーム +300 ボーナス | ― |
| タイムアップ時・最多ダメージチーム | +400 | ― |

### 4-8. デス時のリスポーン処理（Fabric）

```java
// エンドディメンション限定で keepInventory を適用
ServerPlayerEvents.ALLOW_DEATH.register((player, source, amount) -> {
    if (player.getWorld().getRegistryKey() == World.END) {
        return ActionResult.FAIL; // デフォルトのアイテムドロップをキャンセル
    }
    return ActionResult.PASS;
});

// リスポーン座標：敵チームのいないランダムな安全地点を選出
private BlockPos getSafeRespawnPos(ServerPlayerEntity player) {
    List<BlockPos> candidates = RESPAWN_POINTS; // 事前に10〜15点ほど定義
    return candidates.stream()
        .filter(pos -> !isEnemyNearby(pos, player.getTeam(), 20))
        .findAny()
        .orElse(RESPAWN_POINTS.get(0));
}
```

### 4-9. エンダーマン無効化コード（Fabric）

```java
// ① 既存エンダーマンを一掃
world.getEntitiesByType(EntityType.ENDERMAN, e -> true)
     .forEach(Entity::discard);

// ② 以降のスポーンをエンドディメンション限定で無効化
MobSpawnEvents.ALLOW_MOB_SPAWN.register((entity, world, reason) -> {
    if (world.getRegistryKey() == World.END
        && entity.getType() == EntityType.ENDERMAN) {
        return ActionResult.FAIL;
    }
    return ActionResult.PASS;
});
```

### 4-10. 終了演出

```
エンドラ討伐時
  → Title: §5§l【DRAGON SLAIN】/ Subtitle: §f討伐チーム名 + ボーナスPt獲得！
  → 花火を10秒間エンド全体にスポーン
  → バグ演出は継続（120:00まで累計は見えない）

120:00 ゲーム終了
  → Sound: UI_TOAST_CHALLENGE_COMPLETE（全員）
  → Title: §6§l【GAME SET】
  → サイドバーのバグ解除 → 3秒かけて正しいスコアが「復元」するアニメーション
  → チャットに最終順位・各チームのスコアを表示
```

---

## 5. ポイント・ショップ バランス

### 5-1. 収支シミュレーション（1プレイヤー・120分）

| プレイスタイル | 想定行動 | 獲得消費Pt（財布）目安 |
|---|---|---|
| 戦闘特化型 | 雑魚50体/中堅20体/特種2体 | 約 730 pt |
| バランス型 | 雑魚30体/納品100個 | 約 550 pt |
| 兵站型 | 戦闘なし/納品メイン | 約 300 pt |
| 中ボスラストヒット | 1回成功 | +1000 pt（チーム） |

### 5-2. デスペナルティ

- **通常デス（モブ死・PvP含む）:** 消費Pt 20% 没収
- **最終フェーズPvPデス:** 消費Pt 30% 没収
- **金庫預け入れ上限:** 1回あたり最大 500pt
- **金庫引き出し:** いつでも可能。引き出した分はデスペナ対象に戻る

### 5-3. ショップ価格（最終版）

| カテゴリ | 商品名 | 価格(pt) | 備考 |
|---|---|---:|---|
| 超目玉 | 不死のトーテム | 1500 | デスペナ回避の最強投資 |
| 強化 | 鋭さ IV（付与） | 600 | PvEに最適な序盤強化 |
| 強化 | ダメージ軽減 IV | 600 | エンドラ戦でも有効 |
| 強化 | 無限 I（弓用） | 1000 | クリスタル破壊に必須 |
| 強化 | 火炎耐性（付与） | 300 | カオス・エンドラ対策 |
| 強化 | ダイヤ剣（無エンチャ） | 500 | ネザーなし・ダイヤが最終装備のため最高峰 |
| 強化 | ダイヤ防具セット（無エンチャ） | 1800 | 同上。セット割引価格 |
| 補給 | 鉄装備セット | 150 | デス後の立て直し |
| 補給 | 矢（64本） | 50 | エンドラ戦の生命線 |
| 補給 | ステーキ（32個） | 30 | 腹が減っては戦えない |
| 特殊 | 金床（設置型） | 100 | ドロップ品を合成 |
| 特殊 | エンダーパール（16個） | 200 | エンド移動・逃走用 |
| 特殊 | スピードII（30分） | 200 | 逃走・追撃用 |
| 特殊 | 不可視（10分） | 500 | 奇襲・逃走用 |
| 状態異常 | 鈍化の矢（8本） | 80 | 逃げるプレイヤーの足止めに |
| 状態異常 | 弱体化の矢（8本） | 80 | 中ボス・強敵の攻撃力を下げる |
| 状態異常 | 毒の矢（8本） | 100 | 継続ダメージで削る |
| 状態異常 | 即ダメの矢（8本） | 150 | 瞬間火力の底上げ |
| 状態異常 | 鈍化スプラッシュ | 120 | 範囲足止め。集団戦で有効 |
| 状態異常 | 弱体化スプラッシュ | 120 | ボス戦前に先制で投げる |
| 状態異常 | 毒スプラッシュ | 150 | 範囲継続ダメージ |
| 状態異常 | 即ダメスプラッシュ | 200 | 範囲瞬間火力 |
| 状態異常 | 毒の残留ポーション | 250 | 通路・拠点前のトラップ運用 |
| 状態異常 | 即ダメ残留ポーション | 300 | 最高火力の置き型兵器 |

---

## 6. 中ボスレイド 詳細仕様

### 6-1. 出現スケジュール（120分版）

| 回 | 出現時刻 | ベースMob | 体力 | 特殊行動 | 備考 |
|---|---|---|---|---|---|
| 第1弾 | 20:00 | Zombie（Scale 3.0） | 300HP | なし | 序盤の装備で倒せる難易度 |
| 第2弾 | 40:00 | Ravager（Scale 3.0） | 450HP | 周囲プレイヤーを一定間隔でノックバック | 戦術的な距離管理が必要 |
| 第3弾 | 80:00 | Warden（Scale 2.5） | 600HP | 暗闇エフェクト（弱め・半径8m） | ゲーム最強。全チーム参加が前提 |

### 6-2. Wardenの暗闇エフェクト調整

```java
// Warden近傍のみ暗闇を付与（半径外は解除）
players.forEach(player -> {
    double dist = player.squaredDistanceTo(warden);
    int radius = config.getInt("mobs.warden.darkness_radius"); // 8
    int amp = config.getInt("mobs.warden.darkness_amplifier"); // 1（弱め）
    if (dist <= radius * radius) {
        player.addStatusEffect(new StatusEffectInstance(
            StatusEffects.DARKNESS, 60, amp, false, false));
    } else {
        player.removeStatusEffect(StatusEffects.DARKNESS);
    }
});
```

| darkness_amplifier値 | 見え方 |
|---|---|
| 0 | ほぼ真っ暗（通常Warden相当） |
| 1 | 薄暗い・視界5〜8ブロック程度（**推奨**） |
| 2 | うっすら暗い程度 |

### 6-3. ドロップ内容（ボスごとに差別化）

| 回 | ラストヒット報酬 | 飛散ドロップ（半径10m） |
|---|---|---|
| 第1弾 | 累計500pt・消費1000pt | トーテム×1、金リンゴ×8、エンチャ本（鋭さIV）×1 |
| 第2弾 | 累計800pt・消費1500pt | トーテム×2、ダイヤ剣（鋭さV）×1、金リンゴ×16、経験値ボトル×32 |
| 第3弾 | 累計1500pt・消費3000pt | トーテム×3、フルダイヤ防具セット×1、金リンゴ×32、経験値ボトル×64 |

### 6-4. 貢献度報酬（ラストヒット以外も報われる）

| 順位 | 累計Pt |
|---|---|
| ラストヒットチーム | +500〜1500pt（ボスによる） |
| 2位貢献チーム | +150pt |
| 3位貢献チーム | +75pt |

### 6-5. 第3弾ボス（Warden）の特別演出

```
T-5:00  全チャット: §4§l【最終警告】地の底から何かが来る...
T-3:00  地鳴りパーティクル（マップ全体）+ 低音SE（WARDEN_AMBIENT）
T-0     Title: §4§l【FINAL BOSS】覚醒
        全員に暗闇エフェクト3秒 → 晴れた瞬間にWardenがスポーン
        BossBar: §4THE WARDEN / 体力バー赤色表示
```

---

## 7. 中間スコア発表（60分）

```
=== 中間スコア発表（60分経過）===
1位 チームA: 4,520 pt
2位 チームB: 3,870 pt
3位 チームC: 2,110 pt  ← 最下位チームに救済発動
================================
[最下位救済] チームCにショップ半額券を付与！（次の5分ルール終了まで有効）
```

**最下位救済の内容**
- 半額ショップ：**2回まで**使用可能
- 対象商品：トーテム・武器強化系のみ（補給品・状態異常系は対象外）
- 有効期限：次のカオスルール切り替えまで（最大5分）

---

## 8. 環境設定（天候・時間・視界）

### 8-1. 天候・時間固定

```yaml
environment:
  time_fixed: 18000          # 固定時刻（18000=真夜中）
  weather_fixed: clear       # 天候固定
  do_daylight_cycle: false
  do_weather_cycle: false
```

```java
world.setTimeOfDay(18000);
world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false, server);
world.setWeather(0, 0, false, false);
```

> 真夜中固定でゾンビが燃えず、自然スポーンも最大化。天候clear固定で矢の視認性を確保。

### 8-2. 暗視エフェクト常時付与

```yaml
environment:
  night_vision_permanent: true
```

```java
// 5秒ごとに確認・再付与。デス後リスポーン時も自動再付与
scheduler.repeat(() -> {
    server.getPlayerManager().getPlayerList().forEach(player -> {
        if (!player.hasStatusEffect(StatusEffects.NIGHT_VISION)
            || player.getStatusEffect(StatusEffects.NIGHT_VISION).getDuration() < 100) {
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        }
    });
}, 100);
```

---

## 9. 拠点設定・保護

### 9-1. config登録

```yaml
bases:
  teamA:
    center: [125, 64, 0]
    radius: 20
    bed_pos: [125, 65, 5]
  teamB:
    center: [-125, 64, 0]
    radius: 20
    bed_pos: [-125, 65, 5]
  teamC:
    center: [0, 64, 125]
    radius: 20
    bed_pos: [0, 65, 125]

  protection:
    block_break: true        # ブロック破壊を禁止
    explosion_cancel: true   # 爆発によるブロック破壊を無効化
    mob_grief: true          # クリーパー等のブロック破壊を無効化
```

### 9-2. 拠点保護・ベッド実装（Fabric）

```java
// ① ブロック破壊キャンセル
PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
    if (isInsideAnyBase(pos) && !player.hasPermissionLevel(2)) {
        player.sendMessage(Text.literal("§c拠点内のブロックは破壊できません"));
        return false;
    }
    return true;
});

// ② 爆発によるブロック破壊キャンセル（エフェクトは出るがブロックは残る）

// ③ ベッドによるリスポーン地点設定
UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
    BlockPos pos = hitResult.getBlockPos();
    if (world.getBlockState(pos).getBlock() instanceof BedBlock
        && isInsideOwnBase(pos, player)) {
        ((ServerPlayerEntity) player).setSpawnPoint(
            world.getRegistryKey(), pos, 0, true, false);
        player.sendMessage(Text.literal("§aリスポーン地点をセットしました"));
    }
    return ActionResult.PASS;
});

// ④ 夜スキップ無効（時間固定済みのため）
```

> ベッドは1チームに1つのみ。全員同時使用不可のため「誰がリスポーン地点をセットするか」の優先順位が自然に生まれる。デス後は拠点に戻るため、拠点が安全地帯かつ前線復帰の起点になる。

---

## 10. Mobスポーン管理

### 10-1. 自動スポーン設定

```yaml
mob_spawn:
  enabled: true
  interval_seconds: 30
  max_mobs_per_team: 40
  max_mobs_total: 120
  spawn_radius_min: 20
  spawn_radius_max: 60
  spawn_min_light_level: 8      # 光レベル8以下のみスポーン（拠点内の明るい場所には湧かない）
  boss_mob_natural_spawn: false

  weights:
    common: 55
    uncommon: 25
    rare: 15
    boss: 5
```

```java
// スポーン候補座標の光レベル検証
private boolean isValidSpawnPos(BlockPos pos, ServerWorld world) {
    int lightLevel = world.getLightLevel(LightType.BLOCK, pos);
    return lightLevel <= config.getInt("mob_spawn.spawn_min_light_level");
}
```

> 松明（光レベル14）やランタン（光レベル15）で照らされた拠点内には湧かない。整備をサボると拠点内にも湧くリスクがある。

### 10-2. 敵対Mob全列挙・ポイント設定

```yaml
mob_points:
  # 雑魚（Common）
  zombie:              { total: 1,  wallet: 5,    category: common }
  skeleton:            { total: 1,  wallet: 5,    category: common }
  spider:              { total: 1,  wallet: 4,    category: common }
  cave_spider:         { total: 2,  wallet: 6,    category: common }
  zombie_villager:     { total: 1,  wallet: 5,    category: common }
  husk:                { total: 2,  wallet: 6,    category: common }
  stray:               { total: 2,  wallet: 6,    category: common }
  drowned:             { total: 2,  wallet: 6,    category: common }
  slime:               { total: 1,  wallet: 3,    category: common }
  magma_cube:          { total: 1,  wallet: 3,    category: common }
  silverfish:          { total: 1,  wallet: 2,    category: common }
  phantom:             { total: 2,  wallet: 5,    category: common }

  # 中堅（Uncommon）
  creeper:             { total: 3,  wallet: 10,   category: uncommon }
  enderman:            { total: 5,  wallet: 15,   category: uncommon }
  witch:               { total: 5,  wallet: 15,   category: uncommon }
  pillager:            { total: 4,  wallet: 12,   category: uncommon }
  vindicator:          { total: 5,  wallet: 15,   category: uncommon }
  bogged:              { total: 3,  wallet: 10,   category: uncommon }
  breeze:              { total: 8,  wallet: 20,   category: uncommon }
  blaze:               { total: 6,  wallet: 18,   category: uncommon }
  ghast:               { total: 5,  wallet: 15,   category: uncommon }
  piglin_brute:        { total: 8,  wallet: 20,   category: uncommon }
  zombified_piglin:    { total: 3,  wallet: 10,   category: uncommon }
  hoglin:              { total: 5,  wallet: 15,   category: uncommon }
  zoglin:              { total: 6,  wallet: 18,   category: uncommon }
  guardian:            { total: 4,  wallet: 12,   category: uncommon }
  shulker:             { total: 6,  wallet: 18,   category: uncommon }

  # 特種（Rare）
  evoker:              { total: 50, wallet: 100,  category: rare }
  ravager:             { total: 40, wallet: 80,   category: rare }
  elder_guardian:      { total: 30, wallet: 60,   category: rare }
  wither_skeleton:     { total: 20, wallet: 50,   category: rare }
  endermite:           { total: 5,  wallet: 15,   category: rare }
  vex:                 { total: 10, wallet: 25,   category: rare }

  # 中ボス（Boss）※GM召喚・イベント限定
  giant_zombie:        { total: 500,  wallet: 1000, category: boss }
  giant_ravager:       { total: 800,  wallet: 1500, category: boss }
  warden:              { total: 2000, wallet: 3000, category: boss }
  ender_dragon:        { total: 1500, wallet: 2000, category: boss }
```

---

## 11. config.yml 設計・GM管理コマンド

### 11-1. config.yml 全項目

```yaml
game:
  total_minutes: 120
  prep_minutes: 5
  final_phase_start: 110
  border_radius: 250
  team_count: 3
  team_size: 4

score:
  death_penalty_normal: 0.20
  death_penalty_pvp_final: 0.30
  vault_deposit_limit: 500
  score_delay_seconds: 30
  loot_multiplier_lateGame: 1.5

environment:
  time_fixed: 18000
  weather_fixed: clear
  do_daylight_cycle: false
  do_weather_cycle: false
  night_vision_permanent: true

mobs:
  giant:
    scale: 3.0
    health: 400
    attack_damage: 12
    knockback_resistance: 1.0
    spawn_interval_minutes: [20, 40, 80]
  warden:
    scale: 2.5
    health: 600
    attack_damage: 12
    darkness_amplifier: 1
    darkness_radius: 8

shop:
  access_mode: base_or_dead
  delivery_mode: inventory_first
  drop_glow: true
  prices:
    totem: 1500
    sharpness4: 600
    protection4: 600
    infinity: 1000
    fire_resistance: 300
    diamond_sword: 500
    diamond_armor_set: 1800
    iron_set: 150
    arrows_64: 50
    steak_32: 30
    anvil: 100
    speed2_30min: 200
    invisibility_10min: 500
    ender_pearls_16: 200
    arrow_slowness: 80
    arrow_weakness: 80
    arrow_poison: 100
    arrow_harming: 150
    splash_slowness: 120
    splash_weakness: 120
    splash_poison: 150
    splash_harming: 200
    lingering_poison: 250
    lingering_harming: 300
  underdog_discount: 0.50
  underdog_use_limit: 2

chaos_rules:
  duration_minutes: 5
  dual_rule_start_minutes: 40
  normal_rule_weight: 0.20
  forbidden_pairs:
    - [pacifist, arrow_rain]
  normal_rule_solo: true

final_phase:
  pvp_kill_score_total: 150
  pvp_kill_score_wallet: 150
  kill_streak_bonus: 50
  ender_dragon_last_hit_total: 1500
  ender_dragon_last_hit_wallet: 2000
  crystal_kill_total: 30
  crystal_kill_wallet: 50
  respawn_safe_radius: 20
  respawn_candidates:
    - [20, 64, 0]
    - [-20, 64, 0]
    - [0, 64, 20]
    - [0, 64, -20]

bases:
  teamA:
    center: [125, 64, 0]
    radius: 20
    bed_pos: [125, 65, 5]
  teamB:
    center: [-125, 64, 0]
    radius: 20
    bed_pos: [-125, 65, 5]
  teamC:
    center: [0, 64, 125]
    radius: 20
    bed_pos: [0, 65, 125]
  protection:
    block_break: true
    explosion_cancel: true
    mob_grief: true

mob_spawn:
  enabled: true
  interval_seconds: 30
  max_mobs_per_team: 40
  max_mobs_total: 120
  spawn_radius_min: 20
  spawn_radius_max: 60
  spawn_min_light_level: 8
  boss_mob_natural_spawn: false
  weights:
    common: 55
    uncommon: 25
    rare: 15
    boss: 5

spectator:
  allowed: true
  gm_exempt: true

disconnect:
  score_keep: true
  rejoin_grace_seconds: 120
  rejoin_restore: true

world:
  reset_mode: manual
```

### 11-2. GMコマンド一覧

**ゲーム進行**

| コマンド | 説明 |
|---|---|
| `/crr ready <player>` | プレイヤーの準備完了を登録 |
| `/crr start` | ゲーム開始 |
| `/crr pause` | ゲームを一時停止 |
| `/crr resume` | ゲームを再開 |
| `/crr reset` | スコア・タイマー・Mob等をリセット |
| `/crr status` | 現在のゲーム状態を一覧表示 |

**チーム管理**

| コマンド | 説明 | 例 |
|---|---|---|
| `/crr team add <player> <team>` | チームに追加 | `/crr team add Steve teamA` |
| `/crr team remove <player>` | チームから外す | |
| `/crr team auto` | 自動均等振り分け | |
| `/crr team list` | 編成を表示 | |
| `/crr team reset` | 全編成をリセット | |

**スコア・設定**

| コマンド | 説明 | 例 |
|---|---|---|
| `/crr reload` | config.ymlを再読み込み | |
| `/crr set <key> <value>` | 任意の設定値を変更 | `/crr set score.death_penalty_normal 0.15` |
| `/crr get <key>` | 設定値を確認 | |
| `/crr score <team> <+/-><pt>` | スコアを手動加減算 | `/crr score teamA +500` |
| `/crr phase <フェーズ名>` | フェーズを強制移行 | `/crr phase final` |
| `/crr glitch <on/off>` | スコアバグ演出を手動切替 | |

**カオスルール**

| コマンド | 説明 | 例 |
|---|---|---|
| `/crr rule <ルール名>` | ルールを強制発動 | `/crr rule low_gravity` |
| `/crr rule skip` | 現在のルールをスキップ | |

**Mob管理**

| コマンド | 説明 | 例 |
|---|---|---|
| `/crr mob set <mob> total <pt>` | 累計Ptを変更 | `/crr mob set zombie total 2` |
| `/crr mob set <mob> wallet <pt>` | 消費Ptを変更 | |
| `/crr mob get <mob>` | 設定を確認 | |
| `/crr mob list [category]` | カテゴリ別一覧 | `/crr mob list rare` |
| `/crr mob spawn <mob> <数>` | 手動スポーン | `/crr mob spawn evoker 2` |
| `/crr mob killall [category]` | カテゴリ別に消去 | `/crr mob killall common` |
| `/crr mob spawn-rate <on/off>` | 自動スポーンの切替 | |
| `/crr mob count` | 現在のMob総数を表示 | |

**ボス**

| コマンド | 説明 |
|---|---|
| `/crr boss spawn <回>` | 中ボスを手動召喚 |
| `/crr boss kill` | 現在の中ボスを即時消去 |
| `/crr dragon spawn` | エンドラを手動召喚 |

**緊急対応**

| コマンド | 説明 |
|---|---|
| `/crr respawn <player>` | 指定プレイヤーを安全地点にTP |
| `/crr kick <player>` | ゲームから除外（スコアはチームに残す） |
| `/crr add <player> <team>` | 途中参加者を追加 |

### 11-3. `/crr status` の表示例

```
=== カオス・レイド・レース ステータス ===
経過時間   : 47:23 / 120:00
現フェーズ : 中盤（2枚同時ルール）
現ルール   : 低重力 + 全員発光
次ルールまで: 02:37
-----------------------------------------
チームA    : 累計 3,820pt / 財布 1,240pt
チームB    : 累計 4,100pt / 財布  980pt
チームC    : 累計 2,650pt / 財布 1,560pt  ← 最下位（救済フラグON）
-----------------------------------------
中ボス     : 第2弾 出現済・討伐済
次中ボス   : 第3弾（80:00 に出現予定）
Mob総数    : 71体 / 上限120体
=========================================
```

---

## 12. チーム管理

### 12-1. チーム割り当て

```yaml
teams:
  assignment_mode: manual
  colors:
    teamA: RED
    teamB: BLUE
    teamC: GREEN
  names:
    teamA: "チームA"
    teamB: "チームB"
    teamC: "チームC"
```

### 12-2. チームカラーの適用範囲
- スコアボード・サイドバーのチーム名
- プレイヤーの名前タグ（頭上表示）
- BossBarのチーム名通知
- チャットの討伐通知

---

## 13. ゲーム開始・終了フロー

### 13-1. 開始フロー

```
/crr start 実行後の自動処理：
  1. 全プレイヤーのインベントリをクリア
  2. 初期アイテムを配布
  3. 全プレイヤーを各チームの拠点座標にTP
  4. スコアをリセット・タイマーを0にセット
  5. 天候・時間を固定
  6. ワールドボーダーを半径250に設定
  7. 暗視エフェクトを全員に付与
  8. Mobスポーンを有効化
  9. 準備フェーズ開始（PvP無効・カオスなし）
  10. BossBarに「準備フェーズ 05:00」表示
  11. Title: §a§lゲームスタート！ / Subtitle: §f5分間で準備を整えろ！
```

### 13-2. 初期配布アイテム

```yaml
initial_items:
  - {item: iron_sword,      count: 1}
  - {item: bow,             count: 1}
  - {item: arrow,           count: 32}
  - {item: iron_helmet,     count: 1}
  - {item: iron_chestplate, count: 1}
  - {item: iron_leggings,   count: 1}
  - {item: iron_boots,      count: 1}
  - {item: cooked_beef,     count: 16}
  - {item: torch,           count: 32}
```

### 13-3. 終了フロー

```
120:00 or エンドラ討伐後：
  1. PvP・Mobスポーンを無効化
  2. 全プレイヤーを移動不可に
  3. スコアバグ演出を解除
  4. 3秒かけてサイドバーに最終スコアを復元表示
  5. チャットに最終順位・各チームスコアを表示
  6. Title: §6§l【GAME SET】/ Subtitle: §f[優勝チーム名] の勝利！
  7. Sound: UI_TOAST_CHALLENGE_COMPLETE（全員）
  8. 花火を10秒間スポーン
  9. /crr reset が叩かれるまで待機状態に移行
```

---

## 14. リスポーン処理

### 14-1. 通常デス（オーバーワールド）

```
デス →
  1. アイテムをその場にドロップ
  2. 消費Pt 20% 没収
  3. 自動リスポーン（待機時間なし）
  4. 拠点のベッド座標にTP（未設定時はチームデフォルトスポーン）
  5. 暗視エフェクトを再付与
  6. ActionBar: §c[デス] 消費Pt -XX pt（残り：XXX pt）
```

### 14-2. エンドデス（最終フェーズ）

```
デス →
  1. アイテムはそのまま保持（ドロップなし）
  2. 消費Pt 30% 没収
  3. 自動リスポーン（待機時間なし）
  4. 敵チーム不在のランダム安全地点にTP
  5. 暗視エフェクトを再付与
  6. リスポーン無敵時間：3秒（ActionBarにカウントダウン表示）
```

### 14-3. リスポーン無敵時間

```java
player.addStatusEffect(new StatusEffectInstance(
    StatusEffects.RESISTANCE, 60, 255, false, false)); // 3秒ダメージ無効
```

---

## 15. カオスルール競合処理

### 15-1. 禁止ペア

```yaml
chaos_rules:
  forbidden_pairs:
    - [pacifist, arrow_rain]  # 不殺の誓い + 矢の雨（即死ループ防止）
  normal_rule_solo: true      # Normalは常に単体発動
```

### 15-2. 抽選フロー

```
1枚目を抽選
  ↓
2枚目を抽選（後半のみ）
  ↓
forbidden_pairs に含まれるか？
  → YES: 2枚目を引き直し（最大3回リトライ）
  → NO:  そのまま発動
  ↓
Normalが含まれるか？
  → YES: 2枚目は引かず単体発動
```

---

## 16. ショップのアクセス制限

```yaml
shop:
  access_mode: base_or_dead  # 拠点内 or リスポーン直後3秒以内
```

- **戦場ドロップ対策:** `setNeverDespawn(true)` ＋ 発光状態でアイテムを保護
- **購入者ロック:** 5秒間はチーム外プレイヤーが拾えない処理を追加

---

## 17. 共有金庫の実装

### 17-1. コマンドUI方式

| コマンド | 説明 |
|---|---|
| `/vault deposit <pt>` | 指定ptを金庫に預ける |
| `/vault withdraw <pt>` | 指定ptを金庫から引き出す |
| `/vault balance` | チームの金庫残高を確認 |
| `/vault log` | 直近10件の入出金履歴を表示 |

### 17-2. 金庫表示例

```
§6[金庫] チームA 共有金庫
預け入れ残高 : 1,240 pt
メンバー別入金：
  - Steve   : 500 pt
  - Alex    : 400 pt
  - Notch   : 340 pt
```

---

## 18. 観戦・配信者対応

```yaml
spectator:
  allowed: true
  gm_exempt: true
```

- 観戦者は `SPECTATOR` モードに固定・スコア集計対象外
- 任意のプレイヤー視点に入れる（バニラSpectator仕様通り）
- GMは `/crr cam <player>` で追尾カメラを観戦者全員に強制可能（オプション）
- GMはカオスルール・デスペナ等の影響を受けない

---

## 19. 異常終了・切断対応

```yaml
disconnect:
  score_keep: true
  rejoin_grace_seconds: 120
  rejoin_restore: true
```

| 状況 | 対処 |
|---|---|
| 猶予時間内に復帰 | インベントリ・座標を復元して継続 |
| 猶予時間切れ・離脱確定 | チャットに通知・チームの人数減として扱う |
| 全員切断 | `/crr pause` を自動実行 |

```java
PlayerEvents.DISCONNECT.register(player -> {
    savePlayerState(player);           // インベントリ・座標をPersistentStateに保存
    scheduleDisconnectCheck(player.getUuid(), 120);
});

PlayerEvents.JOIN.register(player -> {
    if (hasDisconnectedState(player.getUuid())) {
        restorePlayerState(player);
        player.sendMessage(Text.literal("§a[復帰] ゲームに再参加しました"));
    }
});
```

---

## 20. 確定事項・ワールドリセット仕様

### 20-1. 確定事項

| 項目 | 決定内容 |
|---|---|
| ミッション欠片の上限 | 1ルールにつき1個まで（稼ぎすぎ防止） |
| エンドラ戦のPvP | PvP継続。エンドラはお邪魔虫的存在（倒すとボーナス） |
| Wardenの暗闇エフェクト | amplifier:1（薄暗い程度）で確定 |
| スコア公開タイミング | リアルタイム全公開 |
| ワールドリセット | チャンクスナップショット方式で自動化（下記参照） |

### 20-2. ワールドリセット仕様

**方針: チャンクスナップショット方式**

ゲーム開始前に対象チャンクをまるごとバックアップし、`/crr reset` 時に差し替えて再ロードする。

```yaml
world_reset:
  method: snapshot          # snapshot（方法A・チャンクコピー）で確定
  overworld:
    enabled: true
    range: 500              # 中心から±500ブロック（500×500）
    backup_path: "backups/overworld_snapshot"
  end:
    enabled: true
    range: 200              # 中央島のみ（±200ブロックで十分）
    backup_path: "backups/end_snapshot"
```

**リセットフロー（`/crr reset` 実行時）**

```
1. 全プレイヤーをロビー座標に強制TP（チャンクをアンロードするため）
2. オーバーワールドの対象チャンク（約400チャンク）をバックアップから差し替え
3. エンドの対象チャンク（約64チャンク）をバックアップから差し替え
4. エンドラ・エンドクリスタルを再召喚
5. エンダーマンスポーン無効化を再適用
6. スコア・タイマー・Mob・ボーダーをリセット
7. Title: §a§lリセット完了！ 次のゲームの準備ができました
```

**実装イメージ（Fabric）**

```java
// スナップショット保存（/crr snapshot または ゲーム開始時に自動実行）
public void saveSnapshot(ServerWorld world, int range, Path backupPath) {
    ChunkPos min = new ChunkPos(new BlockPos(-range, 0, -range));
    ChunkPos max = new ChunkPos(new BlockPos(range, 0, range));
    // 対象チャンクのregionファイルをbackupPathにコピー
    for (int cx = min.x; cx <= max.x; cx++) {
        for (int cz = min.z; cz <= max.z; cz++) {
            copyChunkToBackup(world, cx, cz, backupPath);
        }
    }
}

// リセット時に差し替え
public void restoreSnapshot(ServerWorld world, Path backupPath) {
    // プレイヤーをTP済みの前提で実行
    world.getChunkManager().threadedAnvilChunkStorage
        // バックアップのregionファイルで上書き → チャンクを再ロード
}
```

> **処理時間の目安:** オーバーワールド400チャンク＋エンド64チャンクで、ファイルコピー自体は数秒以内。チャンク再ロードを含めても**10〜20秒程度**でリセット完了できる見込み。プレイヤーをロビーTPしている間に裏で処理するので体感は短い。

### 20-3. スナップショット関連コマンド

| コマンド | 説明 |
|---|---|
| `/crr snapshot save` | 現在の状態をスナップショットとして保存（ゲーム開始前に実行） |
| `/crr snapshot restore` | スナップショットからワールドを復元（`/crr reset`に内包） |
| `/crr reset` | スナップショット復元＋スコア・Mob等の全リセットを一括実行 |
