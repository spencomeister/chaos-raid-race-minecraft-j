package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.server.MinecraftServer;

/**
 * 「右の頬を出せ」（不殺の誓い）ルール。
 * 与えたダメージが自分にも返ってくる。
 * 実際のダメージ反映はイベントリスナー側で処理する。
 */
public final class PacifistRule implements ChaosRule {

    private boolean active = false;

    @Override
    public String getId() { return "pacifist"; }

    @Override
    public String getName() { return "右の頬を出せ"; }

    @Override
    public String getTitleText() { return "§a§l不殺の誓い"; }

    @Override
    public String getSubtitleText() { return "§f与えたダメージは自分にも返ってくる！慎重に！"; }

    @Override
    public void activate(MinecraftServer server) { active = true; }

    @Override
    public void tick(MinecraftServer server) { /* イベントリスナーで処理 */ }

    @Override
    public void deactivate(MinecraftServer server) { active = false; }

    /**
     * 不殺の誓いが有効かどうか。
     */
    public boolean isActive() { return active; }
}
