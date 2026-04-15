package io.seragl.chaosraidrace.config;

import com.google.gson.*;
import io.seragl.chaosraidrace.ChaosRaidRace;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * JSON設定ファイルの読み書きを行うユーティリティクラス。
 * {@code config/chaos-raid-race.json} から設定を読み込み、
 * {@link GameConfig} の各フィールドに反映する。
 */
public final class ConfigLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "chaos-raid-race.json";

    private ConfigLoader() {}

    /**
     * 設定ファイルを読み込む。ファイルが存在しない場合はデフォルト値で新規作成する。
     *
     * @param configDir サーバーの config ディレクトリ
     */
    public static void load(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        if (!Files.exists(file)) {
            ChaosRaidRace.LOGGER.info("設定ファイルが見つかりません。デフォルト設定で作成します: {}", file);
            save(configDir);
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            loadFromJson(root);
            ChaosRaidRace.LOGGER.info("設定ファイルを読み込みました: {}", file);
        } catch (IOException | JsonParseException e) {
            ChaosRaidRace.LOGGER.error("設定ファイルの読み込みに失敗しました。デフォルト値を使用します: {}", e.getMessage());
        }
    }

    /**
     * 現在の設定をJSONファイルに書き出す。
     *
     * @param configDir サーバーの config ディレクトリ
     */
    public static void save(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(toJson(), writer);
            }
            ChaosRaidRace.LOGGER.info("設定ファイルを保存しました: {}", file);
        } catch (IOException e) {
            ChaosRaidRace.LOGGER.error("設定ファイルの保存に失敗しました: {}", e.getMessage());
        }
    }

    // ======== JSON → GameConfig ========

    private static void loadFromJson(JsonObject root) {
        // ゲーム全体
        JsonObject game = getObject(root, "game");
        GameConfig.TOTAL_MINUTES = getInt(game, "totalMinutes", GameConfig.TOTAL_MINUTES);
        GameConfig.PREP_MINUTES = getInt(game, "prepMinutes", GameConfig.PREP_MINUTES);
        GameConfig.FINAL_PHASE_START_MINUTES = getInt(game, "finalPhaseStartMinutes", GameConfig.FINAL_PHASE_START_MINUTES);
        GameConfig.BORDER_RADIUS = getInt(game, "borderRadius", GameConfig.BORDER_RADIUS);
        GameConfig.TEAM_COUNT = getInt(game, "teamCount", GameConfig.TEAM_COUNT);
        GameConfig.TEAM_SIZE = getInt(game, "teamSize", GameConfig.TEAM_SIZE);

        // スコア
        JsonObject score = getObject(root, "score");
        GameConfig.DEATH_PENALTY_NORMAL = getDouble(score, "deathPenaltyNormal", GameConfig.DEATH_PENALTY_NORMAL);
        GameConfig.DEATH_PENALTY_PVP_FINAL = getDouble(score, "deathPenaltyPvpFinal", GameConfig.DEATH_PENALTY_PVP_FINAL);
        GameConfig.VAULT_DEPOSIT_LIMIT = getInt(score, "vaultDepositLimit", GameConfig.VAULT_DEPOSIT_LIMIT);
        GameConfig.LOOT_MULTIPLIER_LATE_GAME = getDouble(score, "lootMultiplierLateGame", GameConfig.LOOT_MULTIPLIER_LATE_GAME);

        // 環境
        JsonObject env = getObject(root, "environment");
        GameConfig.TIME_FIXED = getLong(env, "timeFixed", GameConfig.TIME_FIXED);

        // カオスルール
        JsonObject chaos = getObject(root, "chaosRule");
        GameConfig.CHAOS_RULE_DURATION_MINUTES = getInt(chaos, "durationMinutes", GameConfig.CHAOS_RULE_DURATION_MINUTES);
        GameConfig.DUAL_RULE_START_MINUTES = getInt(chaos, "dualRuleStartMinutes", GameConfig.DUAL_RULE_START_MINUTES);
        GameConfig.NORMAL_RULE_WEIGHT = getDouble(chaos, "normalRuleWeight", GameConfig.NORMAL_RULE_WEIGHT);

        // 中ボス
        JsonObject boss = getObject(root, "boss");
        GameConfig.BOSS_SPAWN_MINUTES = getIntArray(boss, "spawnMinutes", GameConfig.BOSS_SPAWN_MINUTES);

        // 最終フェーズ
        JsonObject fp = getObject(root, "finalPhase");
        GameConfig.PVP_KILL_SCORE_TOTAL = getInt(fp, "pvpKillScoreTotal", GameConfig.PVP_KILL_SCORE_TOTAL);
        GameConfig.PVP_KILL_SCORE_WALLET = getInt(fp, "pvpKillScoreWallet", GameConfig.PVP_KILL_SCORE_WALLET);
        GameConfig.KILL_STREAK_BONUS = getInt(fp, "killStreakBonus", GameConfig.KILL_STREAK_BONUS);
        GameConfig.ENDER_DRAGON_LAST_HIT_TOTAL = getInt(fp, "enderDragonLastHitTotal", GameConfig.ENDER_DRAGON_LAST_HIT_TOTAL);
        GameConfig.ENDER_DRAGON_LAST_HIT_WALLET = getInt(fp, "enderDragonLastHitWallet", GameConfig.ENDER_DRAGON_LAST_HIT_WALLET);
        GameConfig.CRYSTAL_KILL_TOTAL = getInt(fp, "crystalKillTotal", GameConfig.CRYSTAL_KILL_TOTAL);
        GameConfig.CRYSTAL_KILL_WALLET = getInt(fp, "crystalKillWallet", GameConfig.CRYSTAL_KILL_WALLET);
        GameConfig.RESPAWN_SAFE_RADIUS = getInt(fp, "respawnSafeRadius", GameConfig.RESPAWN_SAFE_RADIUS);

        // 拠点
        JsonObject base = getObject(root, "base");
        GameConfig.BASE_RADIUS = getInt(base, "radius", GameConfig.BASE_RADIUS);

        // Mobスポーン
        JsonObject mob = getObject(root, "mobSpawn");
        GameConfig.MOB_SPAWN_INTERVAL_SECONDS = getInt(mob, "intervalSeconds", GameConfig.MOB_SPAWN_INTERVAL_SECONDS);
        GameConfig.MAX_MOBS_PER_TEAM = getInt(mob, "maxMobsPerTeam", GameConfig.MAX_MOBS_PER_TEAM);
        GameConfig.MAX_MOBS_TOTAL = getInt(mob, "maxMobsTotal", GameConfig.MAX_MOBS_TOTAL);
        GameConfig.SPAWN_RADIUS_MIN = getInt(mob, "spawnRadiusMin", GameConfig.SPAWN_RADIUS_MIN);
        GameConfig.SPAWN_RADIUS_MAX = getInt(mob, "spawnRadiusMax", GameConfig.SPAWN_RADIUS_MAX);
        GameConfig.SPAWN_MIN_LIGHT_LEVEL = getInt(mob, "spawnMinLightLevel", GameConfig.SPAWN_MIN_LIGHT_LEVEL);

        // ショップ
        JsonObject shop = getObject(root, "shop");
        GameConfig.UNDERDOG_DISCOUNT = getDouble(shop, "underdogDiscount", GameConfig.UNDERDOG_DISCOUNT);
        GameConfig.UNDERDOG_USE_LIMIT = getInt(shop, "underdogUseLimit", GameConfig.UNDERDOG_USE_LIMIT);

        // 切断
        JsonObject disc = getObject(root, "disconnect");
        GameConfig.REJOIN_GRACE_SECONDS = getInt(disc, "rejoinGraceSeconds", GameConfig.REJOIN_GRACE_SECONDS);

        // 座標
        JsonObject coords = getObject(root, "coordinates");
        GameConfig.TEAM_BASE_CENTERS = loadBlockPosMap(coords, "teamBaseCenters", GameConfig.TEAM_BASE_CENTERS);
        GameConfig.TEAM_BED_POSITIONS = loadBlockPosMap(coords, "teamBedPositions", GameConfig.TEAM_BED_POSITIONS);
        GameConfig.END_RESPAWN_CANDIDATES = loadBlockPosList(coords, "endRespawnCandidates", GameConfig.END_RESPAWN_CANDIDATES);
    }

    // ======== GameConfig → JSON ========

    private static JsonObject toJson() {
        JsonObject root = new JsonObject();

        // ゲーム全体
        JsonObject game = new JsonObject();
        game.addProperty("totalMinutes", GameConfig.TOTAL_MINUTES);
        game.addProperty("prepMinutes", GameConfig.PREP_MINUTES);
        game.addProperty("finalPhaseStartMinutes", GameConfig.FINAL_PHASE_START_MINUTES);
        game.addProperty("borderRadius", GameConfig.BORDER_RADIUS);
        game.addProperty("teamCount", GameConfig.TEAM_COUNT);
        game.addProperty("teamSize", GameConfig.TEAM_SIZE);
        root.add("game", game);

        // スコア
        JsonObject score = new JsonObject();
        score.addProperty("deathPenaltyNormal", GameConfig.DEATH_PENALTY_NORMAL);
        score.addProperty("deathPenaltyPvpFinal", GameConfig.DEATH_PENALTY_PVP_FINAL);
        score.addProperty("vaultDepositLimit", GameConfig.VAULT_DEPOSIT_LIMIT);
        score.addProperty("lootMultiplierLateGame", GameConfig.LOOT_MULTIPLIER_LATE_GAME);
        root.add("score", score);

        // 環境
        JsonObject env = new JsonObject();
        env.addProperty("timeFixed", GameConfig.TIME_FIXED);
        root.add("environment", env);

        // カオスルール
        JsonObject chaos = new JsonObject();
        chaos.addProperty("durationMinutes", GameConfig.CHAOS_RULE_DURATION_MINUTES);
        chaos.addProperty("dualRuleStartMinutes", GameConfig.DUAL_RULE_START_MINUTES);
        chaos.addProperty("normalRuleWeight", GameConfig.NORMAL_RULE_WEIGHT);
        root.add("chaosRule", chaos);

        // 中ボス
        JsonObject boss = new JsonObject();
        JsonArray bossArr = new JsonArray();
        for (int m : GameConfig.BOSS_SPAWN_MINUTES) bossArr.add(m);
        boss.add("spawnMinutes", bossArr);
        root.add("boss", boss);

        // 最終フェーズ
        JsonObject fp = new JsonObject();
        fp.addProperty("pvpKillScoreTotal", GameConfig.PVP_KILL_SCORE_TOTAL);
        fp.addProperty("pvpKillScoreWallet", GameConfig.PVP_KILL_SCORE_WALLET);
        fp.addProperty("killStreakBonus", GameConfig.KILL_STREAK_BONUS);
        fp.addProperty("enderDragonLastHitTotal", GameConfig.ENDER_DRAGON_LAST_HIT_TOTAL);
        fp.addProperty("enderDragonLastHitWallet", GameConfig.ENDER_DRAGON_LAST_HIT_WALLET);
        fp.addProperty("crystalKillTotal", GameConfig.CRYSTAL_KILL_TOTAL);
        fp.addProperty("crystalKillWallet", GameConfig.CRYSTAL_KILL_WALLET);
        fp.addProperty("respawnSafeRadius", GameConfig.RESPAWN_SAFE_RADIUS);
        root.add("finalPhase", fp);

        // 拠点
        JsonObject base = new JsonObject();
        base.addProperty("radius", GameConfig.BASE_RADIUS);
        root.add("base", base);

        // Mobスポーン
        JsonObject mob = new JsonObject();
        mob.addProperty("intervalSeconds", GameConfig.MOB_SPAWN_INTERVAL_SECONDS);
        mob.addProperty("maxMobsPerTeam", GameConfig.MAX_MOBS_PER_TEAM);
        mob.addProperty("maxMobsTotal", GameConfig.MAX_MOBS_TOTAL);
        mob.addProperty("spawnRadiusMin", GameConfig.SPAWN_RADIUS_MIN);
        mob.addProperty("spawnRadiusMax", GameConfig.SPAWN_RADIUS_MAX);
        mob.addProperty("spawnMinLightLevel", GameConfig.SPAWN_MIN_LIGHT_LEVEL);
        root.add("mobSpawn", mob);

        // ショップ
        JsonObject shop = new JsonObject();
        shop.addProperty("underdogDiscount", GameConfig.UNDERDOG_DISCOUNT);
        shop.addProperty("underdogUseLimit", GameConfig.UNDERDOG_USE_LIMIT);
        root.add("shop", shop);

        // 切断
        JsonObject disc = new JsonObject();
        disc.addProperty("rejoinGraceSeconds", GameConfig.REJOIN_GRACE_SECONDS);
        root.add("disconnect", disc);

        // 座標
        JsonObject coords = new JsonObject();
        coords.add("teamBaseCenters", saveBlockPosMap(GameConfig.TEAM_BASE_CENTERS));
        coords.add("teamBedPositions", saveBlockPosMap(GameConfig.TEAM_BED_POSITIONS));
        coords.add("endRespawnCandidates", saveBlockPosList(GameConfig.END_RESPAWN_CANDIDATES));
        root.add("coordinates", coords);

        return root;
    }

    // ======== ヘルパーメソッド ========

    private static JsonObject getObject(JsonObject parent, String key) {
        return parent.has(key) ? parent.getAsJsonObject(key) : new JsonObject();
    }

    private static int getInt(JsonObject obj, String key, int defaultValue) {
        try {
            return obj.has(key) ? obj.get(key).getAsInt() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static long getLong(JsonObject obj, String key, long defaultValue) {
        try {
            return obj.has(key) ? obj.get(key).getAsLong() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static double getDouble(JsonObject obj, String key, double defaultValue) {
        try {
            return obj.has(key) ? obj.get(key).getAsDouble() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static int[] getIntArray(JsonObject obj, String key, int[] defaultValue) {
        try {
            if (!obj.has(key)) return defaultValue;
            JsonArray arr = obj.getAsJsonArray(key);
            int[] result = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                result[i] = arr.get(i).getAsInt();
            }
            return result;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static BlockPos jsonToBlockPos(JsonArray arr) {
        return new BlockPos(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt());
    }

    private static JsonArray blockPosToJson(BlockPos pos) {
        JsonArray arr = new JsonArray();
        arr.add(pos.getX());
        arr.add(pos.getY());
        arr.add(pos.getZ());
        return arr;
    }

    private static Map<String, BlockPos> loadBlockPosMap(JsonObject parent, String key, Map<String, BlockPos> defaultValue) {
        try {
            if (!parent.has(key)) return defaultValue;
            JsonObject obj = parent.getAsJsonObject(key);
            Map<String, BlockPos> map = new HashMap<>();
            for (var entry : obj.entrySet()) {
                map.put(entry.getKey(), jsonToBlockPos(entry.getValue().getAsJsonArray()));
            }
            return Map.copyOf(map);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static List<BlockPos> loadBlockPosList(JsonObject parent, String key, List<BlockPos> defaultValue) {
        try {
            if (!parent.has(key)) return defaultValue;
            JsonArray arr = parent.getAsJsonArray(key);
            List<BlockPos> list = new ArrayList<>();
            for (var elem : arr) {
                list.add(jsonToBlockPos(elem.getAsJsonArray()));
            }
            return List.copyOf(list);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static JsonObject saveBlockPosMap(Map<String, BlockPos> map) {
        JsonObject obj = new JsonObject();
        new TreeMap<>(map).forEach((k, v) -> obj.add(k, blockPosToJson(v)));
        return obj;
    }

    private static JsonArray saveBlockPosList(List<BlockPos> list) {
        JsonArray arr = new JsonArray();
        for (BlockPos pos : list) {
            arr.add(blockPosToJson(pos));
        }
        return arr;
    }
}
