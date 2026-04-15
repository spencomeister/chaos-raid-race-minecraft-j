package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.server.MinecraftServer;

/**
 * 「石ころ錬金術師」ルール。
 * 石を掘ると5%の確率でレアドロップが出る。
 * 実際の処理はBlockBreakイベントリスナーから呼び出される。
 */
public final class AlchemyRule implements ChaosRule {

    private boolean active = false;

    @Override
    public String getId() { return "alchemy"; }

    @Override
    public String getName() { return "石ころ錬金術師"; }

    @Override
    public String getTitleText() { return "§e§l錬金タイム"; }

    @Override
    public String getSubtitleText() { return "§f石を掘ると5%の確率でレアドロップ！掘り進め！"; }

    @Override
    public void activate(MinecraftServer server) { active = true; }

    @Override
    public void tick(MinecraftServer server) { /* イベントリスナーで処理 */ }

    @Override
    public void deactivate(MinecraftServer server) { active = false; }

    /**
     * 錬金ルールが有効かどうか。
     */
    public boolean isActive() { return active; }
}
