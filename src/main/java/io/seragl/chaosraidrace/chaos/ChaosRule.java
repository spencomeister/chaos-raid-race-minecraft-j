package io.seragl.chaosraidrace.chaos;

import net.minecraft.server.MinecraftServer;

/**
 * カオスルールの共通インターフェース。
 */
public interface ChaosRule {

    /**
     * ルールのID（内部名）を返す。
     */
    String getId();

    /**
     * ルール名（日本語）を返す。
     */
    String getName();

    /**
     * タイトル表示用のフォーマット済みテキストを返す。
     */
    String getTitleText();

    /**
     * サブタイトル表示用の説明文を返す。
     */
    String getSubtitleText();

    /**
     * ルールを発動する。
     */
    void activate(MinecraftServer server);

    /**
     * 毎ティック処理（ルール発動中のみ呼ばれる）。
     */
    void tick(MinecraftServer server);

    /**
     * ルールを終了する。
     */
    void deactivate(MinecraftServer server);
}
