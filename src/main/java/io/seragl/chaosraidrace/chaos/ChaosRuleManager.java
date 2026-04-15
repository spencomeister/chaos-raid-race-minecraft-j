package io.seragl.chaosraidrace.chaos;

import io.seragl.chaosraidrace.ChaosRaidRace;
import io.seragl.chaosraidrace.chaos.rules.*;
import io.seragl.chaosraidrace.config.GameConfig;
import io.seragl.chaosraidrace.game.GamePhase;
import io.seragl.chaosraidrace.game.GameTimer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.*;

/**
 * カオスルールの抽選・発動・終了を管理するマネージャー。
 */
public final class ChaosRuleManager {

    /** 禁止ペア（同時発動不可の組み合わせ） */
    private static final String[][] FORBIDDEN_PAIRS = {
            {"pacifist", "arrow_rain"}
    };

    private final GameTimer timer;
    private final List<ChaosRule> allRules = new ArrayList<>();
    private final Deque<ChaosRule> rulePool = new ArrayDeque<>();
    private final List<ChaosRule> activeRules = new ArrayList<>();
    private final Random random = new Random();

    // ルール切り替え管理
    private int nextRuleChangeTick = 0;
    private boolean previewSent = false;
    private boolean countdownStarted = false;
    private List<ChaosRule> nextRules = new ArrayList<>();

    public ChaosRuleManager(GameTimer timer) {
        this.timer = timer;
        initializeRules();
    }

    /**
     * 全ルールを初期化する。
     */
    private void initializeRules() {
        allRules.add(new StopAndDieRule());
        allRules.add(new ViewLockRule());
        allRules.add(new PacifistRule());
        allRules.add(new InventoryShuffleRule());
        allRules.add(new ArrowRainRule());
        allRules.add(new LowGravityRule());
        allRules.add(new ChainBreakRule());
        allRules.add(new RandomDropRule());
        allRules.add(new GlowingRule());
        allRules.add(new AlchemyRule());
        allRules.add(new SprintXpRule());
        allRules.add(new HappyMobRule());
        // Normalは20%の確率で追加
        if (random.nextDouble() < GameConfig.NORMAL_RULE_WEIGHT) {
            allRules.add(new NormalRule());
        }
    }

    /**
     * ルールプールをリシャッフルする。
     */
    private void reshufflePool() {
        List<ChaosRule> shuffled = new ArrayList<>(allRules);
        Collections.shuffle(shuffled, random);
        rulePool.clear();
        rulePool.addAll(shuffled);
        ChaosRaidRace.LOGGER.info("カオスルールプールをリシャッフルしました（{}個）", shuffled.size());
    }

    /**
     * ルールを開始する（ゲーム開始時に呼ぶ）。
     */
    public void start() {
        reshufflePool();
        // 準備フェーズ後の最初のルール変更タイミング
        nextRuleChangeTick = GameConfig.PREP_MINUTES * 60 * 20;
        previewSent = false;
        countdownStarted = false;
        nextRules.clear();
    }

    /**
     * 毎ティック処理。
     */
    public void tick(MinecraftServer server) {
        int currentTick = timer.getElapsedTicks();
        GamePhase phase = timer.getCurrentPhase();

        // 準備フェーズ・最終フェーズ・終了時はルール発動しない
        if (phase == GamePhase.PREPARATION || phase == GamePhase.FINAL || phase == GamePhase.ENDED) {
            return;
        }

        // アクティブルールのtick処理
        for (ChaosRule rule : activeRules) {
            rule.tick(server);
        }

        // 次のルール変更までの管理
        int ticksUntilChange = nextRuleChangeTick - currentTick;
        int secondsUntilChange = ticksUntilChange / 20;

        // T-30秒: 予告
        if (secondsUntilChange == 30 && !previewSent) {
            prepareNextRules(phase);
            showPreview(server);
            previewSent = true;
        }

        // T-10秒: カウントダウン開始
        if (secondsUntilChange <= 10 && secondsUntilChange > 0 && !countdownStarted) {
            countdownStarted = true;
        }

        // カウントダウン中の毎秒表示
        if (countdownStarted && secondsUntilChange > 0 && secondsUntilChange <= 10
                && ticksUntilChange % 20 == 0) {
            showCountdown(server, secondsUntilChange);
        }

        // T-0: ルール発動
        if (currentTick == nextRuleChangeTick) {
            activateNextRules(server);
        }

        // 残り30秒警告（アクティブルール終了30秒前）
        int ruleEndTick = nextRuleChangeTick;
        if (!activeRules.isEmpty() && (ruleEndTick - currentTick) == 30 * 20) {
            showWarning30s(server);
        }
    }

    /**
     * 次のルールを抽選する。
     */
    private void prepareNextRules(GamePhase phase) {
        nextRules.clear();

        // プールが空ならリシャッフル
        if (rulePool.isEmpty()) reshufflePool();

        ChaosRule first = rulePool.poll();
        if (first == null) return;
        nextRules.add(first);

        // 後半は2枚同時
        boolean dualRule = (phase == GamePhase.MIDDLE || phase == GamePhase.LATE);
        if (dualRule && !first.getId().equals("normal")) {
            // 2枚目を抽選（禁止ペアチェック）
            for (int retry = 0; retry < 3; retry++) {
                if (rulePool.isEmpty()) reshufflePool();
                ChaosRule second = rulePool.peek();
                if (second == null) break;

                if (!isForbiddenPair(first.getId(), second.getId())
                        && !second.getId().equals("normal")) {
                    rulePool.poll();
                    nextRules.add(second);
                    break;
                }
                // 禁止ペアの場合はスキップしてリトライ
                rulePool.poll();
                rulePool.addLast(second);
            }
        }
    }

    /**
     * 禁止ペアかどうかチェックする。
     */
    private boolean isForbiddenPair(String id1, String id2) {
        for (String[] pair : FORBIDDEN_PAIRS) {
            if ((pair[0].equals(id1) && pair[1].equals(id2))
                    || (pair[0].equals(id2) && pair[1].equals(id1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 予告を表示する。
     */
    private void showPreview(MinecraftServer server) {
        StringBuilder ruleNames = new StringBuilder();
        for (int i = 0; i < nextRules.size(); i++) {
            if (i > 0) ruleNames.append(" + ");
            ruleNames.append(nextRules.get(i).getName());
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal(
                    "§e§l【予告】30秒後のルール → " + ruleNames));
            player.connection.send(new ClientboundSoundPacket(
                    SoundEvents.NOTE_BLOCK_HAT, SoundSource.MASTER,
                    player.getX(), player.getY(), player.getZ(),
                    0.5f, 0.5f, player.level().getRandom().nextLong()));
        }
        ChaosRaidRace.LOGGER.info("カオスルール予告: {}", ruleNames);
    }

    /**
     * カウントダウン表示。
     */
    private void showCountdown(MinecraftServer server, int seconds) {
        float pitch = 0.5f + (10 - seconds) * 0.15f;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendOverlayMessage(Component.literal("§c" + seconds));
            player.connection.send(new ClientboundSoundPacket(
                    SoundEvents.NOTE_BLOCK_HAT, SoundSource.MASTER,
                    player.getX(), player.getY(), player.getZ(),
                    1.0f, pitch, player.level().getRandom().nextLong()));
        }
    }

    /**
     * ルールを発動する。
     */
    private void activateNextRules(MinecraftServer server) {
        // 現在のルールを終了
        deactivateCurrentRules(server);

        // 新ルールを発動
        activeRules.addAll(nextRules);
        for (ChaosRule rule : activeRules) {
            rule.activate(server);
        }

        // タイトル表示
        StringBuilder titleText = new StringBuilder();
        StringBuilder subtitleText = new StringBuilder();
        for (int i = 0; i < activeRules.size(); i++) {
            if (i > 0) {
                titleText.append(" + ");
                subtitleText.append(" / ");
            }
            titleText.append(activeRules.get(i).getTitleText());
            subtitleText.append(activeRules.get(i).getSubtitleText());
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                    Component.literal(titleText.toString())));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                    Component.literal(subtitleText.toString())));
            player.connection.send(new ClientboundSoundPacket(
                    Holder.direct(SoundEvents.LIGHTNING_BOLT_THUNDER), SoundSource.MASTER,
                    player.getX(), player.getY(), player.getZ(),
                    1.0f, 1.0f, player.level().getRandom().nextLong()));
        }

        // 次のルール変更を5分後にセット
        nextRuleChangeTick = timer.getElapsedTicks() + (GameConfig.CHAOS_RULE_DURATION_MINUTES * 60 * 20);
        previewSent = false;
        countdownStarted = false;
        nextRules.clear();

        ChaosRaidRace.LOGGER.info("カオスルール発動: {}", activeRules.stream()
                .map(ChaosRule::getName).toList());
    }

    /**
     * 現在のルールを終了する。
     */
    public void deactivateCurrentRules(MinecraftServer server) {
        for (ChaosRule rule : activeRules) {
            rule.deactivate(server);
        }
        if (!activeRules.isEmpty()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundSoundPacket(
                        Holder.direct(SoundEvents.PLAYER_LEVELUP), SoundSource.MASTER,
                        player.getX(), player.getY(), player.getZ(),
                        1.0f, 1.0f, player.level().getRandom().nextLong()));
            }
        }
        activeRules.clear();
    }

    /**
     * 残り30秒警告を表示する。
     */
    private void showWarning30s(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendOverlayMessage(Component.literal("§c残り30秒！"));
            player.connection.send(new ClientboundSoundPacket(
                    SoundEvents.NOTE_BLOCK_HAT, SoundSource.MASTER,
                    player.getX(), player.getY(), player.getZ(),
                    1.0f, 2.0f, player.level().getRandom().nextLong()));
        }
    }

    /**
     * ルールを強制発動する（GMコマンド用）。
     */
    public void forceActivateRule(MinecraftServer server, String ruleId) {
        deactivateCurrentRules(server);
        allRules.stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .ifPresent(rule -> {
                    activeRules.add(rule);
                    rule.activate(server);
                    ChaosRaidRace.LOGGER.info("ルールを強制発動: {}", rule.getName());
                });
    }

    /**
     * 現在のルールをスキップする（GMコマンド用）。
     */
    public void skipCurrentRule(MinecraftServer server) {
        deactivateCurrentRules(server);
        nextRuleChangeTick = timer.getElapsedTicks() + 30 * 20; // 30秒後に次のルール
        previewSent = false;
        countdownStarted = false;
        ChaosRaidRace.LOGGER.info("現在のルールをスキップしました");
    }

    /**
     * 現在アクティブなルールを取得する。
     */
    public List<ChaosRule> getActiveRules() {
        return Collections.unmodifiableList(activeRules);
    }

    /**
     * 全停止する。
     */
    public void stopAll(MinecraftServer server) {
        deactivateCurrentRules(server);
        nextRules.clear();
    }

    /**
     * ルール全一覧を返す。
     */
    public List<ChaosRule> getAllRules() {
        return Collections.unmodifiableList(allRules);
    }
}
