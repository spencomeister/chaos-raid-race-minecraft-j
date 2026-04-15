package io.seragl.chaosraidrace.team;

import net.minecraft.ChatFormatting;

import java.util.*;

/**
 * ゲーム内のチーム情報を保持するクラス。
 */
public final class GameTeam {

    private final String id;
    private final String displayName;
    private final ChatFormatting color;
    private final Set<UUID> members = new LinkedHashSet<>();

    // スコア
    private int totalScore = 0;
    private int walletScore = 0;
    private int vaultScore = 0;

    // ミッション欠片
    private int missionFragments = 0;

    // 最下位救済
    private boolean underdogActive = false;
    private int underdogUsesRemaining = 0;

    /**
     * チームを生成する。
     *
     * @param id          内部ID（例: "teamA"）
     * @param displayName 表示名（例: "チームA"）
     * @param color       チームカラー
     */
    public GameTeam(String id, String displayName, ChatFormatting color) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public ChatFormatting getColor() { return color; }
    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }

    /**
     * メンバーを追加する。
     */
    public void addMember(UUID uuid) { members.add(uuid); }

    /**
     * メンバーを削除する。
     */
    public void removeMember(UUID uuid) { members.remove(uuid); }

    /**
     * メンバーかどうか判定する。
     */
    public boolean isMember(UUID uuid) { return members.contains(uuid); }

    // === スコア操作 ===

    public int getTotalScore() { return totalScore; }
    public int getWalletScore() { return walletScore; }
    public int getVaultScore() { return vaultScore; }

    /**
     * 累計スコアを加算する。
     */
    public void addTotalScore(int amount) { totalScore += amount; }

    /**
     * 財布スコアを加算する。
     */
    public void addWalletScore(int amount) {
        walletScore += amount;
        if (walletScore < 0) walletScore = 0;
    }

    /**
     * 金庫に預ける。
     *
     * @param amount 預入額
     * @return 実際に預けた額
     */
    public int depositToVault(int amount) {
        int actual = Math.min(amount, walletScore);
        walletScore -= actual;
        vaultScore += actual;
        return actual;
    }

    /**
     * 金庫から引き出す。
     *
     * @param amount 引出額
     * @return 実際に引き出した額
     */
    public int withdrawFromVault(int amount) {
        int actual = Math.min(amount, vaultScore);
        vaultScore -= actual;
        walletScore += actual;
        return actual;
    }

    /**
     * デスペナルティを適用する。
     *
     * @param penaltyRate 没収割合（0.0〜1.0）
     * @return 没収されたポイント
     */
    public int applyDeathPenalty(double penaltyRate) {
        int penalty = (int) (walletScore * penaltyRate);
        walletScore -= penalty;
        return penalty;
    }

    // === ミッション ===

    public int getMissionFragments() { return missionFragments; }
    public void addMissionFragment() { missionFragments++; }

    // === 最下位救済 ===

    public boolean isUnderdogActive() { return underdogActive; }
    public int getUnderdogUsesRemaining() { return underdogUsesRemaining; }

    /**
     * 最下位救済を有効化する。
     */
    public void activateUnderdog(int uses) {
        underdogActive = true;
        underdogUsesRemaining = uses;
    }

    /**
     * 最下位救済を使用する。
     *
     * @return 使用可能だったか
     */
    public boolean useUnderdog() {
        if (!underdogActive || underdogUsesRemaining <= 0) return false;
        underdogUsesRemaining--;
        if (underdogUsesRemaining <= 0) underdogActive = false;
        return true;
    }

    /**
     * 最下位救済を無効化する。
     */
    public void deactivateUnderdog() {
        underdogActive = false;
        underdogUsesRemaining = 0;
    }

    /**
     * スコアをリセットする。
     */
    public void resetScores() {
        totalScore = 0;
        walletScore = 0;
        vaultScore = 0;
        missionFragments = 0;
        underdogActive = false;
        underdogUsesRemaining = 0;
    }
}
