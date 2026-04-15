package io.seragl.chaosraidrace.score;

import io.seragl.chaosraidrace.ChaosRaidRace;
import io.seragl.chaosraidrace.config.GameConfig;
import io.seragl.chaosraidrace.team.GameTeam;
import io.seragl.chaosraidrace.team.TeamManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * スコア（累計Pt・消費Pt・金庫）を管理するマネージャー。
 */
public final class ScoreManager {

    private final TeamManager teamManager;
    private boolean lateGameMultiplierActive = false;
    private boolean happyMobActive = false;

    public ScoreManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    /**
     * Mob討伐ポイントを加算する。
     *
     * @param player   キルしたプレイヤー
     * @param totalPt  累計ポイント
     * @param walletPt 消費ポイント
     */
    public void addMobKillScore(ServerPlayer player, int totalPt, int walletPt) {
        double multiplier = getMultiplier();
        int adjustedTotal = (int) (totalPt * multiplier);
        int adjustedWallet = (int) (walletPt * multiplier);

        Optional<GameTeam> teamOpt = teamManager.getPlayerTeam(player.getUUID());
        teamOpt.ifPresent(team -> {
            team.addTotalScore(adjustedTotal);
            team.addWalletScore(adjustedWallet);
            player.sendOverlayMessage(
                    Component.literal("§a+" + adjustedWallet + " pt (財布) / +" + adjustedTotal + " pt (累計)"));
        });
    }

    /**
     * チームにポイントを直接加算する。
     */
    public void addScore(GameTeam team, int totalPt, int walletPt) {
        double multiplier = getMultiplier();
        team.addTotalScore((int) (totalPt * multiplier));
        team.addWalletScore((int) (walletPt * multiplier));
    }

    /**
     * チームにポイントを倍率なしで直接加算する。
     */
    public void addScoreRaw(GameTeam team, int totalPt, int walletPt) {
        team.addTotalScore(totalPt);
        team.addWalletScore(walletPt);
    }

    /**
     * デスペナルティを適用する。
     */
    public int applyDeathPenalty(ServerPlayer player, boolean isFinalPhase) {
        double rate = isFinalPhase ? GameConfig.DEATH_PENALTY_PVP_FINAL : GameConfig.DEATH_PENALTY_NORMAL;
        Optional<GameTeam> teamOpt = teamManager.getPlayerTeam(player.getUUID());
        if (teamOpt.isPresent()) {
            int penalty = teamOpt.get().applyDeathPenalty(rate);
            player.sendOverlayMessage(
                    Component.literal("§c[デス] 消費Pt -" + penalty + " pt（残り: " + teamOpt.get().getWalletScore() + " pt）"));
            return penalty;
        }
        return 0;
    }

    /**
     * 金庫に預ける。
     */
    public int deposit(UUID playerUuid, int amount) {
        int cappedAmount = Math.min(amount, GameConfig.VAULT_DEPOSIT_LIMIT);
        Optional<GameTeam> teamOpt = teamManager.getPlayerTeam(playerUuid);
        return teamOpt.map(team -> team.depositToVault(cappedAmount)).orElse(0);
    }

    /**
     * 金庫から引き出す。
     */
    public int withdraw(UUID playerUuid, int amount) {
        Optional<GameTeam> teamOpt = teamManager.getPlayerTeam(playerUuid);
        return teamOpt.map(team -> team.withdrawFromVault(amount)).orElse(0);
    }

    /**
     * 終盤倍率を有効化する。
     */
    public void setLateGameMultiplier(boolean active) {
        this.lateGameMultiplierActive = active;
        ChaosRaidRace.LOGGER.info("終盤倍率: {}", active ? "有効" : "無効");
    }

    /**
     * ハッピーモブ倍率を有効化する。
     */
    public void setHappyMobActive(boolean active) {
        this.happyMobActive = active;
    }

    /**
     * 現在の倍率を計算する。
     */
    private double getMultiplier() {
        double multiplier = 1.0;
        if (lateGameMultiplierActive) multiplier *= GameConfig.LOOT_MULTIPLIER_LATE_GAME;
        if (happyMobActive) multiplier *= 2.0;
        return multiplier;
    }
}
