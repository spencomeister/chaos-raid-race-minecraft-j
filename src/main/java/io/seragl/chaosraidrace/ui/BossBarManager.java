package io.seragl.chaosraidrace.ui;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import io.seragl.chaosraidrace.game.GamePhase;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.*;

/**
 * ゲーム情報をBossBarに表示するマネージャー。
 */
public final class BossBarManager {

    private final ServerBossEvent mainBar = new ServerBossEvent(
            java.util.UUID.randomUUID(),
            Component.literal("カオス・レイド・レース"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    private final ServerBossEvent ruleBar = new ServerBossEvent(
            java.util.UUID.randomUUID(),
            Component.literal(""),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.PROGRESS);

    /**
     * BossBarにプレイヤーを追加する。
     */
    public void addPlayer(ServerPlayer player) {
        mainBar.addPlayer(player);
        ruleBar.addPlayer(player);
    }

    /**
     * BossBarからプレイヤーを削除する。
     */
    public void removePlayer(ServerPlayer player) {
        mainBar.removePlayer(player);
        ruleBar.removePlayer(player);
    }

    /**
     * メインBar（経過時間・フェーズ）を更新する。
     */
    public void updateMainBar(GamePhase phase, String elapsedTime, String remainingTime, float progress) {
        mainBar.setName(Component.literal(
                "§d" + phase.getDisplayName() + " §7| §f" + elapsedTime + " / " + remainingTime));
        mainBar.setProgress(Math.max(0, Math.min(1, progress)));
    }

    /**
     * ルールBarを更新する。
     */
    public void updateRuleBar(List<ChaosRule> activeRules, int secondsRemaining) {
        if (activeRules.isEmpty()) {
            ruleBar.setVisible(false);
            return;
        }

        ruleBar.setVisible(true);
        StringBuilder names = new StringBuilder("現在のルール: ");
        for (int i = 0; i < activeRules.size(); i++) {
            if (i > 0) names.append(" + ");
            names.append(activeRules.get(i).getName());
        }
        names.append(" §7| 残り: ").append(formatSeconds(secondsRemaining));
        ruleBar.setName(Component.literal(names.toString()));

        float progress = secondsRemaining / (float) (5 * 60);
        ruleBar.setProgress(Math.max(0, Math.min(1, progress)));
    }

    /**
     * 次のルール予告をBossBarに表示する。
     */
    public void showPreview(String ruleName, int secondsUntil) {
        ruleBar.setVisible(true);
        ruleBar.setName(Component.literal(
                "§e次のルール: " + ruleName + " まで " + formatSeconds(secondsUntil)));
        ruleBar.setColor(BossEvent.BossBarColor.YELLOW);
    }

    /**
     * ボスBarを設定する（中ボス用）。
     */
    public void showBossBar(String bossName, float healthPercent) {
        mainBar.setName(Component.literal("§4" + bossName));
        mainBar.setColor(BossEvent.BossBarColor.RED);
        mainBar.setProgress(Math.max(0, Math.min(1, healthPercent)));
    }

    /**
     * BossBarの表示をリセットする。
     */
    public void reset() {
        mainBar.setVisible(false);
        ruleBar.setVisible(false);
        mainBar.removeAllPlayers();
        ruleBar.removeAllPlayers();
    }

    /**
     * 全BossBarを表示する。
     */
    public void show() {
        mainBar.setVisible(true);
    }

    private static String formatSeconds(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
