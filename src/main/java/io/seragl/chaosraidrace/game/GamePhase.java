package io.seragl.chaosraidrace.game;

/**
 * ゲームの進行フェーズを定義する列挙型。
 */
public enum GamePhase {
    /** ゲーム未開始・待機状態 */
    WAITING("待機中"),
    /** 準備フェーズ（0〜5分）：PvP無効・カオスなし */
    PREPARATION("準備フェーズ"),
    /** 序盤（5〜40分）：カオスルール1枚 */
    EARLY("序盤"),
    /** 中盤（40〜80分）：カオスルール2枚同時 */
    MIDDLE("中盤"),
    /** 終盤（80〜110分）：カオスルール2枚・Pt1.5倍 */
    LATE("終盤"),
    /** 最終決戦（110〜120分）：エンド・PvP総力戦 */
    FINAL("最終決戦"),
    /** ゲーム終了 */
    ENDED("終了");

    private final String displayName;

    GamePhase(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 表示用の日本語名を返す。
     */
    public String getDisplayName() {
        return displayName;
    }
}
