package io.seragl.chaosraidrace.game;

import io.seragl.chaosraidrace.ChaosRaidRace;
import io.seragl.chaosraidrace.base.BaseProtectionManager;
import io.seragl.chaosraidrace.boss.BossManager;
import io.seragl.chaosraidrace.chaos.ChaosRuleManager;
import io.seragl.chaosraidrace.config.GameConfig;
import io.seragl.chaosraidrace.disconnect.DisconnectManager;
import io.seragl.chaosraidrace.environment.EnvironmentManager;
import io.seragl.chaosraidrace.mission.MissionManager;
import io.seragl.chaosraidrace.mob.MobSpawnManager;
import io.seragl.chaosraidrace.phase.FinalPhaseManager;
import io.seragl.chaosraidrace.score.ScoreManager;
import io.seragl.chaosraidrace.shop.ShopManager;
import io.seragl.chaosraidrace.team.GameTeam;
import io.seragl.chaosraidrace.team.TeamManager;
import io.seragl.chaosraidrace.ui.AnnouncementManager;
import io.seragl.chaosraidrace.ui.BossBarManager;
import io.seragl.chaosraidrace.ui.ScoreboardDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;

import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;

/**
 * ゲーム全体の進行を管理する中央マネージャー。
 * 各サブシステムを統括し、フェーズ遷移・タイマー・イベントを制御する。
 */
public final class GameManager {

    // サブシステム
    private final GameTimer timer = new GameTimer();
    private final TeamManager teamManager = new TeamManager();
    private final ScoreManager scoreManager = new ScoreManager(teamManager);
    private final AnnouncementManager announcements = new AnnouncementManager();
    private final BossBarManager bossBarManager = new BossBarManager();
    private final ScoreboardDisplay scoreboardDisplay = new ScoreboardDisplay(teamManager);
    private final ChaosRuleManager chaosRuleManager = new ChaosRuleManager(timer);
    private final BossManager bossManager = new BossManager(teamManager, scoreManager, announcements);
    private final ShopManager shopManager = new ShopManager(teamManager);
    private final EnvironmentManager environmentManager = new EnvironmentManager();
    private final BaseProtectionManager baseProtection = new BaseProtectionManager(teamManager);
    private final MobSpawnManager mobSpawnManager = new MobSpawnManager();
    private final MissionManager missionManager = new MissionManager(teamManager, scoreManager, announcements);
    private final FinalPhaseManager finalPhaseManager = new FinalPhaseManager(teamManager, scoreManager, announcements);
    private final DisconnectManager disconnectManager = new DisconnectManager();

    private GamePhase lastPhase = GamePhase.WAITING;

    /** Warden前兆演出用の遅延スポーンカウンター（-1=無効） */
    private int wardenSpawnDelay = -1;

    /** ゲーム終了セレモニー用カウンター（-1=無効） */
    private int endCeremonyTick = -1;

    // === アクセサ ===
    public GameTimer getTimer() { return timer; }
    public TeamManager getTeamManager() { return teamManager; }
    public ScoreManager getScoreManager() { return scoreManager; }
    public AnnouncementManager getAnnouncements() { return announcements; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public ScoreboardDisplay getScoreboardDisplay() { return scoreboardDisplay; }
    public ChaosRuleManager getChaosRuleManager() { return chaosRuleManager; }
    public BossManager getBossManager() { return bossManager; }
    public ShopManager getShopManager() { return shopManager; }
    public EnvironmentManager getEnvironmentManager() { return environmentManager; }
    public BaseProtectionManager getBaseProtection() { return baseProtection; }
    public MobSpawnManager getMobSpawnManager() { return mobSpawnManager; }
    public MissionManager getMissionManager() { return missionManager; }
    public FinalPhaseManager getFinalPhaseManager() { return finalPhaseManager; }
    public DisconnectManager getDisconnectManager() { return disconnectManager; }

    /**
     * ゲームを開始する。
     */
    public void startGame(MinecraftServer server) {
        ChaosRaidRace.LOGGER.info("=== カオス・レイド・レース ゲーム開始 ===");

        // チーム初期化
        teamManager.initialize();

        // 環境設定
        environmentManager.setupGameEnvironment(server);

        // スコアボード初期化
        scoreboardDisplay.initialize(server);

        // BossBar設定
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            bossBarManager.addPlayer(player);
        }
        bossBarManager.show();

        // プレイヤー初期設定
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // インベントリクリア
            player.getInventory().clearContent();

            // 初期アイテム配布
            distributeInitialItems(player);

            // チーム拠点にTP
            Optional<GameTeam> team = teamManager.getPlayerTeam(player.getUUID());
            team.ifPresent(t -> {
                BlockPos spawn = GameConfig.TEAM_BASE_CENTERS.getOrDefault(t.getId(), BlockPos.ZERO);
                player.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            });
        }

        // Mobスポーン有効化
        mobSpawnManager.enable();

        // カオスルール開始準備
        chaosRuleManager.start();

        // タイマー開始
        timer.start();
        lastPhase = GamePhase.PREPARATION;

        // 演出
        announcements.showGameStart(server);
    }

    /**
     * 毎ティックの処理。
     */
    public void tick(MinecraftServer server) {
        if (!timer.isRunning() || timer.isPaused()) return;

        timer.tick();
        GamePhase currentPhase = timer.getCurrentPhase();

        // フェーズ遷移チェック
        if (currentPhase != lastPhase) {
            onPhaseChange(server, lastPhase, currentPhase);
            lastPhase = currentPhase;
        }

        // 各サブシステムのtick
        environmentManager.tick(server);
        chaosRuleManager.tick(server);
        bossManager.tick(server);
        mobSpawnManager.tick(server);
        missionManager.checkCompletion(server);
        finalPhaseManager.tick(server);

        // UI更新（毎秒）
        if (timer.isSecondBoundary()) {
            updateUI(server);
        }

        // 中ボス出現チェック
        checkBossSpawn(server);

        // Warden前兆演出
        checkWardenPreSpawn(server);

        // Warden遅延スポーン処理
        if (wardenSpawnDelay > 0) {
            wardenSpawnDelay--;
            if (wardenSpawnDelay == 0) {
                bossManager.spawnBoss(server, 2);
                wardenSpawnDelay = -1;
            }
        }

        // ゲーム終了セレモニー処理
        if (endCeremonyTick >= 0) {
            tickEndCeremony(server);
        }

        // 中間スコア発表（60分）
        if (timer.justReachedMinute(60)) {
            showMidScoreReport(server);
        }

        // 終盤Pt1.5倍（100分）
        if (timer.justReachedMinute(100)) {
            scoreManager.setLateGameMultiplier(true);
            announcements.broadcast(server, "§6§l全討伐Pt 1.5倍！ラストスパート！");
        }

        // 最終フェーズ警告
        if (timer.justReachedMinute(108)) {
            announcements.broadcast(server, "§c§l【最終フェーズまで 2:00】装備を整えろ！");
        }
        if (timer.justReachedSecond(109 * 60 + 30)) {
            announcements.broadcast(server, "§4§l【警告】30秒後、エンドへ強制転送されます！");
        }

        // ゲーム終了
        if (currentPhase == GamePhase.ENDED) {
            endGame(server);
        }
    }

    /**
     * フェーズ遷移時の処理。
     */
    private void onPhaseChange(MinecraftServer server, GamePhase from, GamePhase to) {
        ChaosRaidRace.LOGGER.info("フェーズ遷移: {} → {}", from.getDisplayName(), to.getDisplayName());

        switch (to) {
            case EARLY -> {
                // 準備フェーズ終了 → PvP解禁
                environmentManager.setPvpEnabled(server, true);
                announcements.broadcast(server, "§e§lPvP解禁！カオスルール開始！");
            }
            case MIDDLE -> {
                announcements.broadcast(server, "§6§l後半戦突入！ルール2枚同時発動解禁！");
            }
            case LATE -> {
                announcements.broadcast(server, "§c§l終盤戦！最終フェーズに向けて備えろ！");
            }
            case FINAL -> {
                // カオスルール停止
                chaosRuleManager.stopAll(server);
                // 最終フェーズ開始
                finalPhaseManager.start(server);
                finalPhaseManager.spawnDragon(server);
                // スコアバグ演出ON
                scoreboardDisplay.setGlitchMode(true);
            }
            case ENDED -> {
                // endGame で処理
            }
            default -> {}
        }
    }

    /**
     * ゲームを終了する。
     */
    public void endGame(MinecraftServer server) {
        if (!timer.isRunning()) return;

        ChaosRaidRace.LOGGER.info("=== カオス・レイド・レース ゲーム終了 ===");

        // 全システム停止
        chaosRuleManager.stopAll(server);
        mobSpawnManager.disable();
        environmentManager.setPvpEnabled(server, false);
        finalPhaseManager.disableKeepInventory(server);

        // プレイヤーを移動不可にする
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, 600, 255, false, false));
            player.addEffect(new MobEffectInstance(
                    MobEffects.MINING_FATIGUE, 600, 255, false, false));
            player.addEffect(new MobEffectInstance(
                    MobEffects.JUMP_BOOST, 600, 128, false, false));
        }

        // サウンド再生
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSoundPacket(
                    Holder.direct(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE), SoundSource.MASTER,
                    player.getX(), player.getY(), player.getZ(),
                    1.0f, 1.0f, 0L));
        }

        // Title表示
        List<GameTeam> ranked = teamManager.getTeamsRanked();
        if (!ranked.isEmpty()) {
            GameTeam winner = ranked.getFirst();
            announcements.showGameEnd(server, winner.getDisplayName());
        }

        // セレモニーカウンター開始（バグ演出解除→スコア復元→花火→最終結果表示）
        endCeremonyTick = 0;
        // バグ演出は3秒後に解除
        scoreboardDisplay.setGlitchMode(true);
    }

    /**
     * ゲーム終了セレモニーのtick処理。
     */
    private void tickEndCeremony(MinecraftServer server) {
        endCeremonyTick++;

        // 3秒後（60tick）: バグ演出解除 → スコア復元
        if (endCeremonyTick == 60) {
            scoreboardDisplay.setGlitchMode(false);
            scoreboardDisplay.update(server, timer.formatTime());
        }

        // 3〜13秒間（60〜260tick）: 花火を20tickごとにスポーン
        if (endCeremonyTick >= 60 && endCeremonyTick <= 260 && endCeremonyTick % 20 == 0) {
            spawnCeremonyFireworks(server);
        }

        // 5秒後（100tick）: 最終結果をチャットに表示
        if (endCeremonyTick == 100) {
            List<GameTeam> ranked = teamManager.getTeamsRanked();
            StringBuilder sb = new StringBuilder("§6=== 最終結果 ===\n");
            for (int i = 0; i < ranked.size(); i++) {
                GameTeam t = ranked.get(i);
                sb.append("§f").append(i + 1).append("位 ")
                        .append(t.getColor()).append(t.getDisplayName())
                        .append("§f: ").append(NumberFormat.getInstance().format(t.getTotalScore()))
                        .append(" pt\n");
            }
            sb.append("§6================");
            announcements.broadcast(server, sb.toString());
        }

        // 15秒後（300tick）: セレモニー終了
        if (endCeremonyTick >= 300) {
            endCeremonyTick = -1;
            // 移動制限エフェクトを除去
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.removeEffect(MobEffects.SLOWNESS);
                player.removeEffect(MobEffects.MINING_FATIGUE);
                player.removeEffect(MobEffects.JUMP_BOOST);
            }
            timer.reset();
            ChaosRaidRace.LOGGER.info("ゲーム終了セレモニーが完了しました");
        }
    }

    /**
     * セレモニー用の花火を打ち上げる。
     */
    private void spawnCeremonyFireworks(MinecraftServer server) {
        ServerLevel level = server.overworld();
        // エンドにプレイヤーがいる場合はエンドに打ち上げ
        ServerLevel end = server.getLevel(net.minecraft.world.level.Level.END);
        ServerLevel targetLevel = (end != null && finalPhaseManager.isActive()) ? end : level;

        java.util.Random rng = new java.util.Random();
        int[] colors = {0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFF00FF, 0x00FFFF, 0xFFFFFF};

        for (int i = 0; i < 5; i++) {
            double x = rng.nextDouble() * 40 - 20;
            double z = rng.nextDouble() * 40 - 20;

            FireworkExplosion explosion = new FireworkExplosion(
                    FireworkExplosion.Shape.LARGE_BALL,
                    it.unimi.dsi.fastutil.ints.IntList.of(colors[rng.nextInt(colors.length)]),
                    it.unimi.dsi.fastutil.ints.IntList.of(colors[rng.nextInt(colors.length)]),
                    true, true);

            ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
            fireworkStack.set(DataComponents.FIREWORKS,
                    new Fireworks(1, List.of(explosion)));

            FireworkRocketEntity rocket = new FireworkRocketEntity(
                    targetLevel, x, 70.0, z, fireworkStack);
            targetLevel.addFreshEntity(rocket);
        }
    }

    /**
     * ゲームを一時停止する。
     */
    public void pauseGame() {
        timer.pause();
    }

    /**
     * ゲームを再開する。
     */
    public void resumeGame() {
        timer.resume();
    }

    /**
     * 全リセットする。
     */
    public void resetGame(MinecraftServer server) {
        timer.reset();
        teamManager.resetAll();
        chaosRuleManager.stopAll(server);
        bossManager.killCurrentBoss();
        mobSpawnManager.disable();
        mobSpawnManager.resetCount();
        finalPhaseManager.reset();
        disconnectManager.reset();
        scoreboardDisplay.remove(server);
        bossBarManager.reset();
        lastPhase = GamePhase.WAITING;
        wardenSpawnDelay = -1;
        endCeremonyTick = -1;
        ChaosRaidRace.LOGGER.info("ゲームをリセットしました");
    }

    /**
     * 中ボス出現チェック。
     */
    private void checkBossSpawn(MinecraftServer server) {
        for (int i = 0; i < GameConfig.BOSS_SPAWN_MINUTES.length; i++) {
            if (timer.justReachedMinute(GameConfig.BOSS_SPAWN_MINUTES[i])) {
                if (i == 2) {
                    // Warden（第3弾）は前兆演出後に遅延スポーン
                    // 全員に暗闇3秒付与
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        player.addEffect(new MobEffectInstance(
                                MobEffects.DARKNESS, 60, 0, false, false));
                    }
                    // 3秒後（60tick）にスポーン
                    wardenSpawnDelay = 60;
                } else {
                    bossManager.spawnBoss(server, i);
                }
            }
        }
    }

    /**
     * Warden前兆演出チェック（T-5:00, T-3:00）。
     */
    private void checkWardenPreSpawn(MinecraftServer server) {
        // T-5:00（75分）: 警告チャット
        if (timer.justReachedMinute(75)) {
            announcements.broadcast(server, "§4§l【最終警告】地の底から何かが来る...");
        }
        // T-3:00（77分）: 地鳴りSE
        if (timer.justReachedMinute(77)) {
            announcements.broadcast(server, "§4§l地面が揺れている...");
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundSoundPacket(
                        Holder.direct(SoundEvents.WARDEN_AMBIENT), SoundSource.HOSTILE,
                        player.getX(), player.getY(), player.getZ(),
                        1.0f, 0.5f, 0L));
            }
        }
    }

    /**
     * 中間スコア発表。
     */
    private void showMidScoreReport(MinecraftServer server) {
        List<GameTeam> ranked = teamManager.getTeamsRanked();
        StringBuilder sb = new StringBuilder("§6=== 中間スコア発表（60分経過）===\n");
        for (int i = 0; i < ranked.size(); i++) {
            GameTeam t = ranked.get(i);
            sb.append(i + 1).append("位 ").append(t.getColor())
                    .append(t.getDisplayName()).append("§f: ")
                    .append(NumberFormat.getInstance().format(t.getTotalScore())).append(" pt");
            if (i == ranked.size() - 1) {
                sb.append("  ← 最下位チームに救済発動");
            }
            sb.append("\n");
        }
        sb.append("§6================================");

        announcements.showMidScoreReport(server, sb.toString());

        // 最下位救済
        if (!ranked.isEmpty()) {
            GameTeam lowest = ranked.getLast();
            lowest.activateUnderdog(GameConfig.UNDERDOG_USE_LIMIT);
            announcements.broadcast(server,
                    "§a[最下位救済] " + lowest.getDisplayName() + "にショップ半額券を付与！（2回まで）");
        }
    }

    /**
     * UI更新（毎秒）。
     */
    private void updateUI(MinecraftServer server) {
        GamePhase phase = timer.getCurrentPhase();
        float progress = (float) timer.getElapsedSeconds() / (GameConfig.TOTAL_MINUTES * 60.0f);

        // BossBar更新
        if (bossManager.isBossActive()) {
            bossBarManager.showBossBar(bossManager.getCurrentBossName(), bossManager.getBossHealthPercent());
        } else {
            bossBarManager.updateMainBar(phase, timer.formatTime(), timer.formatRemainingTime(), progress);
        }

        // サイドバー更新
        scoreboardDisplay.update(server, timer.formatTime());
    }

    /**
     * 初期アイテムを配布する。
     */
    private void distributeInitialItems(ServerPlayer player) {
        player.getInventory().add(new ItemStack(Items.IRON_SWORD));
        player.getInventory().add(new ItemStack(Items.BOW));
        player.getInventory().add(new ItemStack(Items.ARROW, 32));
        player.getInventory().add(new ItemStack(Items.IRON_HELMET));
        player.getInventory().add(new ItemStack(Items.IRON_CHESTPLATE));
        player.getInventory().add(new ItemStack(Items.IRON_LEGGINGS));
        player.getInventory().add(new ItemStack(Items.IRON_BOOTS));
        player.getInventory().add(new ItemStack(Items.COOKED_BEEF, 16));
        player.getInventory().add(new ItemStack(Items.TORCH, 32));
    }

    /**
     * カオスルールの「ハッピーモブ」倍率をScoreManagerに反映する。
     */
    public void syncHappyMobMultiplier() {
        boolean happyMobActive = chaosRuleManager.getActiveRules().stream()
                .anyMatch(r -> r.getId().equals("happy_mob"));
        scoreManager.setHappyMobActive(happyMobActive);
    }
}
