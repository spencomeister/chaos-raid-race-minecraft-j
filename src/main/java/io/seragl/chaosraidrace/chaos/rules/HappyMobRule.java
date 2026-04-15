package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.server.MinecraftServer;

/**
 * 「モブのボーナス期」ルール。
 * この5分間、全ての討伐ポイントが2倍になる。
 * ScoreManagerと連携して倍率を適用する。
 */
public final class HappyMobRule implements ChaosRule {

    private boolean active = false;

    @Override
    public String getId() { return "happy_mob"; }

    @Override
    public String getName() { return "モブのボーナス期"; }

    @Override
    public String getTitleText() { return "§6§lハッピーモブ"; }

    @Override
    public String getSubtitleText() { return "§fこの5分間、全ての討伐ポイントが2倍！稼げ！"; }

    @Override
    public void activate(MinecraftServer server) { active = true; }

    @Override
    public void tick(MinecraftServer server) { /* ScoreManagerで倍率処理 */ }

    @Override
    public void deactivate(MinecraftServer server) { active = false; }

    /**
     * ハッピーモブが有効かどうか。
     */
    public boolean isActive() { return active; }
}
