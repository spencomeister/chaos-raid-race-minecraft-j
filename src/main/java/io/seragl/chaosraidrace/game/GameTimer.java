package io.seragl.chaosraidrace.game;

import io.seragl.chaosraidrace.ChaosRaidRace;
import io.seragl.chaosraidrace.config.GameConfig;

/**
 * ゲーム内タイマーを管理するクラス。
 * サーバーティックベースで経過時間を追跡する。
 */
public final class GameTimer {

    /** 1秒あたりのティック数 */
    private static final int TICKS_PER_SECOND = 20;
    /** 1分あたりのティック数 */
    private static final int TICKS_PER_MINUTE = TICKS_PER_SECOND * 60;

    private int elapsedTicks = 0;
    private boolean running = false;
    private boolean paused = false;

    /**
     * タイマーを開始する。
     */
    public void start() {
        elapsedTicks = 0;
        running = true;
        paused = false;
        ChaosRaidRace.LOGGER.info("ゲームタイマーを開始しました");
    }

    /**
     * 毎ティック呼び出す。
     */
    public void tick() {
        if (running && !paused) {
            elapsedTicks++;
        }
    }

    /**
     * 一時停止する。
     */
    public void pause() {
        paused = true;
        ChaosRaidRace.LOGGER.info("ゲームタイマーを一時停止しました（経過: {}）", formatTime());
    }

    /**
     * 再開する。
     */
    public void resume() {
        paused = false;
        ChaosRaidRace.LOGGER.info("ゲームタイマーを再開しました");
    }

    /**
     * リセットする。
     */
    public void reset() {
        elapsedTicks = 0;
        running = false;
        paused = false;
    }

    /**
     * タイマーが実行中かどうか。
     */
    public boolean isRunning() { return running; }

    /**
     * 一時停止中かどうか。
     */
    public boolean isPaused() { return paused; }

    /**
     * 経過ティック数を返す。
     */
    public int getElapsedTicks() { return elapsedTicks; }

    /**
     * 経過秒数を返す。
     */
    public int getElapsedSeconds() { return elapsedTicks / TICKS_PER_SECOND; }

    /**
     * 経過分数を返す。
     */
    public int getElapsedMinutes() { return elapsedTicks / TICKS_PER_MINUTE; }

    /**
     * 残り秒数を返す。
     */
    public int getRemainingSeconds() {
        int totalSeconds = GameConfig.TOTAL_MINUTES * 60;
        return Math.max(0, totalSeconds - getElapsedSeconds());
    }

    /**
     * ゲーム終了時間に達したかどうか。
     */
    public boolean isTimeUp() {
        return getElapsedMinutes() >= GameConfig.TOTAL_MINUTES;
    }

    /**
     * 特定の分数に到達したかどうか（そのティックで到達した瞬間のみtrue）。
     */
    public boolean justReachedMinute(int minute) {
        int targetTick = minute * TICKS_PER_MINUTE;
        return elapsedTicks == targetTick;
    }

    /**
     * 特定の秒数に到達したかどうか（そのティックで到達した瞬間のみtrue）。
     */
    public boolean justReachedSecond(int totalSeconds) {
        int targetTick = totalSeconds * TICKS_PER_SECOND;
        return elapsedTicks == targetTick;
    }

    /**
     * 毎秒のタイミングかどうか。
     */
    public boolean isSecondBoundary() {
        return elapsedTicks % TICKS_PER_SECOND == 0;
    }

    /**
     * 現在のフェーズを判定する。
     */
    public GamePhase getCurrentPhase() {
        int minutes = getElapsedMinutes();
        if (!running) return GamePhase.WAITING;
        if (minutes < GameConfig.PREP_MINUTES) return GamePhase.PREPARATION;
        if (minutes < GameConfig.DUAL_RULE_START_MINUTES) return GamePhase.EARLY;
        if (minutes < 80) return GamePhase.MIDDLE;
        if (minutes < GameConfig.FINAL_PHASE_START_MINUTES) return GamePhase.LATE;
        if (minutes < GameConfig.TOTAL_MINUTES) return GamePhase.FINAL;
        return GamePhase.ENDED;
    }

    /**
     * 時間をMM:SS形式でフォーマットする。
     */
    public String formatTime() {
        int totalSeconds = getElapsedSeconds();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 残り時間をMM:SS形式でフォーマットする。
     */
    public String formatRemainingTime() {
        int remaining = getRemainingSeconds();
        int minutes = remaining / 60;
        int seconds = remaining % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 経過ティックを強制的に設定する（デバッグ・フェーズ強制遷移用）。
     */
    public void setElapsedTicks(int ticks) {
        this.elapsedTicks = ticks;
    }
}
