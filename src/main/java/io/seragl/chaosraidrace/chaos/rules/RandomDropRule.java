package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.server.MinecraftServer;

/**
 * 「中身はお楽しみ」ルール。
 * ブロック破壊時のドロップをランダムアイテムに差し替える。
 * 実際の処理はBlockBreakイベントリスナーから呼び出される。
 */
public final class RandomDropRule implements ChaosRule {

    private boolean active = false;

    @Override
    public String getId() { return "random_drop"; }

    @Override
    public String getName() { return "中身はお楽しみ"; }

    @Override
    public String getTitleText() { return "§d§lランダムドロップ"; }

    @Override
    public String getSubtitleText() { return "§fブロックを壊すと何が出るか分からない！"; }

    @Override
    public void activate(MinecraftServer server) { active = true; }

    @Override
    public void tick(MinecraftServer server) { /* イベントリスナーで処理 */ }

    @Override
    public void deactivate(MinecraftServer server) { active = false; }

    /**
     * ランダムドロップが有効かどうか。
     */
    public boolean isActive() { return active; }
}
