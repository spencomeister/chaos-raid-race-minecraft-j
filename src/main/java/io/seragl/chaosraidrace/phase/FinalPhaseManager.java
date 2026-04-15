package io.seragl.chaosraidrace.phase;

import io.seragl.chaosraidrace.ChaosRaidRace;
import io.seragl.chaosraidrace.config.GameConfig;
import io.seragl.chaosraidrace.score.ScoreManager;
import io.seragl.chaosraidrace.team.GameTeam;
import io.seragl.chaosraidrace.team.TeamManager;
import io.seragl.chaosraidrace.ui.AnnouncementManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 最終フェーズ（エンド決戦）を管理するクラス。
 */
public final class FinalPhaseManager {

    private final TeamManager teamManager;
    private final ScoreManager scoreManager;
    private final AnnouncementManager announcements;

    private boolean active = false;
    private boolean dragonSpawned = false;
    private boolean dragonDefeated = false;
    private int tickCounter = 0;
    private final Map<UUID, Integer> killStreaks = new HashMap<>();

    /** エンドクリスタル追跡用：UUID → 最後の座標 */
    private final Map<UUID, Vec3> trackedCrystals = new HashMap<>();

    // エンドのチーム別スタート地点
    private static final Map<String, BlockPos> END_START_POINTS = Map.of(
            "teamA", new BlockPos(20, 64, 0),
            "teamB", new BlockPos(-20, 64, 0),
            "teamC", new BlockPos(0, 64, 20)
    );

    public FinalPhaseManager(TeamManager teamManager, ScoreManager scoreManager, AnnouncementManager announcements) {
        this.teamManager = teamManager;
        this.scoreManager = scoreManager;
        this.announcements = announcements;
    }

    /**
     * 最終フェーズを開始する。
     */
    public void start(MinecraftServer server) {
        active = true;
        dragonSpawned = false;
        dragonDefeated = false;

        ServerLevel end = server.getLevel(Level.END);
        if (end == null) {
            ChaosRaidRace.LOGGER.error("エンドディメンションが見つかりません");
            return;
        }

        // エンダーマンを一掃
        end.getEntities(EntityType.ENDERMAN, e -> true).forEach(e -> e.discard());
        ChaosRaidRace.LOGGER.info("エンダーマンを一掃しました");

        // keepInventoryを有効化（最終フェーズ中はアイテムロストなし）
        server.getGameRules().set(GameRules.KEEP_INVENTORY, true, server);
        ChaosRaidRace.LOGGER.info("keepInventoryを有効化しました");

        // エンドクリスタルの初期追跡を開始
        trackedCrystals.clear();
        for (EndCrystal crystal : end.getEntities(EntityType.END_CRYSTAL, e -> true)) {
            trackedCrystals.put(crystal.getUUID(), crystal.position());
        }
        ChaosRaidRace.LOGGER.info("エンドクリスタル追跡開始: {}個", trackedCrystals.size());

        // プレイヤーをエンドにTP
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) continue;
            Optional<GameTeam> teamOpt = teamManager.getPlayerTeam(player.getUUID());
            BlockPos target = teamOpt
                    .map(t -> END_START_POINTS.getOrDefault(t.getId(), new BlockPos(0, 64, 0)))
                    .orElse(new BlockPos(0, 64, 0));

            player.teleport(new TeleportTransition(
                    end,
                    new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5),
                    Vec3.ZERO, player.getYRot(), player.getXRot(),
                    TeleportTransition.DO_NOTHING));
        }

        // 演出
        announcements.showFinalPhaseStart(server);

        ChaosRaidRace.LOGGER.info("最終フェーズを開始しました");
    }

    /**
     * エンダードラゴンを召喚する。
     */
    public void spawnDragon(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) return;

        var dragon = EntityType.ENDER_DRAGON.create(end, EntitySpawnReason.COMMAND);
        if (dragon != null) {
            dragon.setPos(0, 80, 0);
            end.addFreshEntity(dragon);
            dragonSpawned = true;
            ChaosRaidRace.LOGGER.info("エンダードラゴンを召喚しました");
        }
    }

    /**
     * PvPキル報酬を処理する。
     */
    public void onPvpKill(ServerPlayer killer, ServerPlayer victim) {
        if (!active) return;

        UUID killerId = killer.getUUID();
        int streak = killStreaks.getOrDefault(killerId, 0) + 1;
        killStreaks.put(killerId, streak);

        // キラー報酬
        int totalReward = GameConfig.PVP_KILL_SCORE_TOTAL + (streak > 1 ? (streak - 1) * GameConfig.KILL_STREAK_BONUS : 0);
        int walletReward = GameConfig.PVP_KILL_SCORE_WALLET + (streak > 1 ? (streak - 1) * GameConfig.KILL_STREAK_BONUS : 0);

        teamManager.getPlayerTeam(killerId).ifPresent(team ->
                scoreManager.addScoreRaw(team, totalReward, walletReward));

        killer.sendSystemMessage(Component.literal(
                "§a[PvPキル] +" + totalReward + "pt" + (streak > 1 ? "（" + streak + "連続キル！）" : "")));

        // 被害者ペナルティ（30%没収）
        scoreManager.applyDeathPenalty(victim, true);
    }

    /**
     * エンドでのリスポーン位置を取得する。
     */
    public BlockPos getSafeRespawnPos(ServerPlayer player) {
        for (BlockPos candidate : GameConfig.END_RESPAWN_CANDIDATES) {
            boolean enemyNearby = false;
            for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) {
                if (other == player) continue;
                Optional<GameTeam> otherTeam = teamManager.getPlayerTeam(other.getUUID());
                Optional<GameTeam> playerTeam = teamManager.getPlayerTeam(player.getUUID());
                if (otherTeam.isPresent() && playerTeam.isPresent()
                        && !otherTeam.get().getId().equals(playerTeam.get().getId())) {
                    if (other.blockPosition().distSqr(candidate) < GameConfig.RESPAWN_SAFE_RADIUS * GameConfig.RESPAWN_SAFE_RADIUS) {
                        enemyNearby = true;
                        break;
                    }
                }
            }
            if (!enemyNearby) return candidate;
        }
        return GameConfig.END_RESPAWN_CANDIDATES.getFirst();
    }

    /**
     * リスポーン無敵を付与する。
     */
    public void applyRespawnInvincibility(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.RESISTANCE, 60, 255, false, false));
    }

    /**
     * 最終フェーズが有効かどうか。
     */
    public boolean isActive() { return active; }

    /**
     * 毎ティック処理（エンダーマン抑制・クリスタル破壊検知）。
     */
    public void tick(MinecraftServer server) {
        if (!active) return;
        tickCounter++;

        ServerLevel end = server.getLevel(Level.END);
        if (end == null) return;

        // エンダーマン抑制: 毎秒エンドのエンダーマンを除去
        if (tickCounter % 20 == 0) {
            end.getEntities(EntityType.ENDERMAN, e -> true).forEach(e -> e.discard());
        }

        // エンドクリスタル破壊検知: 5tickごとにチェック
        if (tickCounter % 5 == 0) {
            Set<UUID> currentCrystals = new HashSet<>();
            for (EndCrystal crystal : end.getEntities(EntityType.END_CRYSTAL, e -> true)) {
                currentCrystals.add(crystal.getUUID());
                trackedCrystals.putIfAbsent(crystal.getUUID(), crystal.position());
            }

            // 消滅したクリスタルを検出
            for (var iter = trackedCrystals.entrySet().iterator(); iter.hasNext(); ) {
                var entry = iter.next();
                if (!currentCrystals.contains(entry.getKey())) {
                    Vec3 pos = entry.getValue();
                    iter.remove();

                    // 最も近いプレイヤーにポイント付与
                    ServerPlayer nearest = findNearestPlayer(server, end, pos);
                    if (nearest != null) {
                        teamManager.getPlayerTeam(nearest.getUUID()).ifPresent(team -> {
                            scoreManager.addScoreRaw(team,
                                    GameConfig.CRYSTAL_KILL_TOTAL, GameConfig.CRYSTAL_KILL_WALLET);
                            nearest.sendOverlayMessage(Component.literal(
                                    "§e[クリスタル破壊] 累計+" + GameConfig.CRYSTAL_KILL_TOTAL
                                            + "pt / 消費+" + GameConfig.CRYSTAL_KILL_WALLET + "pt"));
                        });
                    }
                }
            }
        }
    }

    /**
     * エンド内で指定座標に最も近いプレイヤーを取得する。
     */
    private ServerPlayer findNearestPlayer(MinecraftServer server, ServerLevel end, Vec3 pos) {
        ServerPlayer nearest = null;
        double minDist = Double.MAX_VALUE;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != end || player.isSpectator()) continue;
            double dist = player.position().distanceToSqr(pos);
            if (dist < minDist) {
                minDist = dist;
                nearest = player;
            }
        }
        return nearest;
    }

    /**
     * エンドラが討伐されたかどうか。
     */
    public boolean isDragonDefeated() { return dragonDefeated; }

    /**
     * エンドラ討伐を記録する。
     */
    public void setDragonDefeated(boolean defeated) { this.dragonDefeated = defeated; }

    /**
     * リセットする。
     */
    public void reset() {
        active = false;
        dragonSpawned = false;
        dragonDefeated = false;
        tickCounter = 0;
        killStreaks.clear();
        trackedCrystals.clear();
    }

    /**
     * keepInventoryを無効化する（ゲーム終了時に呼び出す）。
     */
    public void disableKeepInventory(MinecraftServer server) {
        server.getGameRules().set(GameRules.KEEP_INVENTORY, false, server);
    }
}
