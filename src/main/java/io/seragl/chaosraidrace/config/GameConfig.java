package io.seragl.chaosraidrace.config;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

/**
 * ゲーム設定を保持するクラス。
 * 値はデフォルト値で初期化され、起動時に {@code config/chaos-raid-race.json} から上書きされる。
 * 設定ファイルの読み書きは {@link ConfigLoader} が担当する。
 */
public final class GameConfig {

    // === ゲーム全体 ===
    /** ゲーム総時間（分） */
    public static int TOTAL_MINUTES = 120;
    /** 準備フェーズの時間（分） */
    public static int PREP_MINUTES = 5;
    /** 最終フェーズ開始時間（分） */
    public static int FINAL_PHASE_START_MINUTES = 110;
    /** ワールドボーダー半径 */
    public static int BORDER_RADIUS = 250;
    /** チーム数 */
    public static int TEAM_COUNT = 3;
    /** 1チームの人数 */
    public static int TEAM_SIZE = 4;

    // === スコア ===
    /** 通常デスペナルティ（消費Ptの割合） */
    public static double DEATH_PENALTY_NORMAL = 0.20;
    /** 最終フェーズPvPデスペナルティ */
    public static double DEATH_PENALTY_PVP_FINAL = 0.30;
    /** 金庫預け入れ上限 */
    public static int VAULT_DEPOSIT_LIMIT = 500;
    /** 終盤の討伐Pt倍率 */
    public static double LOOT_MULTIPLIER_LATE_GAME = 1.5;

    // === 環境 ===
    /** 固定時刻（18000=真夜中） */
    public static long TIME_FIXED = 18000L;

    // === カオスルール ===
    /** ルール持続時間（分） */
    public static int CHAOS_RULE_DURATION_MINUTES = 5;
    /** 2枚同時発動開始（分） */
    public static int DUAL_RULE_START_MINUTES = 40;
    /** Normalルールの出現率 */
    public static double NORMAL_RULE_WEIGHT = 0.20;

    // === 中ボス ===
    /** 中ボス出現時刻（分） */
    public static int[] BOSS_SPAWN_MINUTES = {20, 40, 80};

    // === 最終フェーズ ===
    /** PvPキルポイント（累計） */
    public static int PVP_KILL_SCORE_TOTAL = 150;
    /** PvPキルポイント（消費） */
    public static int PVP_KILL_SCORE_WALLET = 150;
    /** 連続キルボーナス加算値 */
    public static int KILL_STREAK_BONUS = 50;
    /** エンドラ討伐ラストヒット（累計） */
    public static int ENDER_DRAGON_LAST_HIT_TOTAL = 1500;
    /** エンドラ討伐ラストヒット（消費） */
    public static int ENDER_DRAGON_LAST_HIT_WALLET = 2000;
    /** クリスタル破壊ポイント（累計） */
    public static int CRYSTAL_KILL_TOTAL = 30;
    /** クリスタル破壊ポイント（消費） */
    public static int CRYSTAL_KILL_WALLET = 50;
    /** リスポーン安全半径 */
    public static int RESPAWN_SAFE_RADIUS = 20;

    // === 拠点 ===
    /** 拠点半径 */
    public static int BASE_RADIUS = 20;

    // === Mobスポーン ===
    /** スポーン間隔（秒） */
    public static int MOB_SPAWN_INTERVAL_SECONDS = 30;
    /** チーム別Mob上限 */
    public static int MAX_MOBS_PER_TEAM = 40;
    /** 全体Mob上限 */
    public static int MAX_MOBS_TOTAL = 120;
    /** スポーン最小半径 */
    public static int SPAWN_RADIUS_MIN = 20;
    /** スポーン最大半径 */
    public static int SPAWN_RADIUS_MAX = 60;
    /** スポーン最大光レベル */
    public static int SPAWN_MIN_LIGHT_LEVEL = 8;

    // === ショップ ===
    /** 最下位チーム割引率 */
    public static double UNDERDOG_DISCOUNT = 0.50;
    /** 最下位割引使用回数上限 */
    public static int UNDERDOG_USE_LIMIT = 2;

    // === 切断 ===
    /** 再接続猶予時間（秒） */
    public static int REJOIN_GRACE_SECONDS = 120;

    // === 拠点座標 ===
    public static Map<String, BlockPos> TEAM_BASE_CENTERS = Map.of(
            "teamA", new BlockPos(125, 64, 0),
            "teamB", new BlockPos(-125, 64, 0),
            "teamC", new BlockPos(0, 64, 125)
    );
    public static Map<String, BlockPos> TEAM_BED_POSITIONS = Map.of(
            "teamA", new BlockPos(125, 65, 5),
            "teamB", new BlockPos(-125, 65, 5),
            "teamC", new BlockPos(0, 65, 125)
    );

    // === エンドリスポーン候補地点 ===
    public static List<BlockPos> END_RESPAWN_CANDIDATES = List.of(
            new BlockPos(20, 64, 0),
            new BlockPos(-20, 64, 0),
            new BlockPos(0, 64, 20),
            new BlockPos(0, 64, -20)
    );

    private GameConfig() {
        // インスタンス化禁止
    }
}
