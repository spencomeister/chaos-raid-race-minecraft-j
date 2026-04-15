package io.seragl.chaosraidrace.ui;

import io.seragl.chaosraidrace.team.GameTeam;
import io.seragl.chaosraidrace.team.TeamManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.text.NumberFormat;
import java.util.*;

/**
 * サイドバーのスコアボード表示を管理するクラス。
 */
public final class ScoreboardDisplay {

    private static final String OBJECTIVE_NAME = "crr_sidebar";
    private final TeamManager teamManager;
    private boolean glitchMode = false;
    private final Random random = new Random();
    private final String[] glitchChars = {"#", "?", "!", "X", "%", "@", "$", "&", "*"};

    public ScoreboardDisplay(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    /**
     * スコアボードを初期化する。
     */
    public void initialize(MinecraftServer server) {
        ServerScoreboard scoreboard = server.getScoreboard();

        // 既存のObjectiveを削除
        Objective existing = scoreboard.getObjective(OBJECTIVE_NAME);
        if (existing != null) {
            scoreboard.removeObjective(existing);
        }

        // 新しいObjectiveを作成
        Objective objective = scoreboard.addObjective(
                OBJECTIVE_NAME,
                ObjectiveCriteria.DUMMY,
                Component.literal("§6§lカオス・レイド・レース"),
                ObjectiveCriteria.RenderType.INTEGER,
                true,
                null);
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
    }

    /**
     * スコアボードを更新する。
     */
    public void update(MinecraftServer server, String timeDisplay) {
        ServerScoreboard scoreboard = server.getScoreboard();

        // Objectiveを再作成してリセット
        Objective existing = scoreboard.getObjective(OBJECTIVE_NAME);
        if (existing != null) {
            scoreboard.removeObjective(existing);
        }
        Objective objective = scoreboard.addObjective(
                OBJECTIVE_NAME,
                ObjectiveCriteria.DUMMY,
                Component.literal("§6§lカオス・レイド・レース"),
                ObjectiveCriteria.RenderType.INTEGER,
                true,
                null);
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);

        int line = 10;

        // 時間表示
        scoreboard.getOrCreatePlayerScore(
                ScoreHolder.forNameOnly("§7経過: §f" + timeDisplay),
                objective).set(line--);

        // 空行
        scoreboard.getOrCreatePlayerScore(
                ScoreHolder.forNameOnly("§7----------"),
                objective).set(line--);

        // チームスコア
        List<GameTeam> ranked = teamManager.getTeamsRanked();
        for (GameTeam team : ranked) {
            String scoreText;
            if (glitchMode) {
                scoreText = generateGlitchScore(ranked);
            } else {
                scoreText = NumberFormat.getInstance().format(team.getTotalScore()) + " pt";
            }
            String display = team.getColor() + team.getDisplayName() + "§f: " + scoreText;
            scoreboard.getOrCreatePlayerScore(
                    ScoreHolder.forNameOnly(display),
                    objective).set(line--);
        }
    }

    /**
     * バグ演出モードを切り替える。
     */
    public void setGlitchMode(boolean enabled) {
        this.glitchMode = enabled;
    }

    /**
     * バグ演出中かどうか。
     */
    public boolean isGlitchMode() { return glitchMode; }

    /**
     * バグ演出用のランダム文字列を生成する。
     * 全チーム同じ桁数にする。
     */
    private String generateGlitchScore(List<GameTeam> teams) {
        int maxDigits = teams.stream()
                .mapToInt(t -> String.valueOf(t.getTotalScore()).length())
                .max()
                .orElse(4);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxDigits; i++) {
            sb.append(glitchChars[random.nextInt(glitchChars.length)]);
        }
        return sb + " pt";
    }

    /**
     * スコアボードを削除する。
     */
    public void remove(MinecraftServer server) {
        ServerScoreboard scoreboard = server.getScoreboard();
        Objective existing = scoreboard.getObjective(OBJECTIVE_NAME);
        if (existing != null) {
            scoreboard.removeObjective(existing);
        }
    }
}
