package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.server.MinecraftServer;

/**
 * 「Normal」ルール。
 * 特に何も起きない平和な5分間。
 */
public final class NormalRule implements ChaosRule {

    @Override
    public String getId() { return "normal"; }

    @Override
    public String getName() { return "Normal"; }

    @Override
    public String getTitleText() { return "§7§lつかの間の平和"; }

    @Override
    public String getSubtitleText() { return "§f特に何も起きない。今のうちに作業を片付けろ！"; }

    @Override
    public void activate(MinecraftServer server) { /* 何もなし */ }

    @Override
    public void tick(MinecraftServer server) { /* 何もなし */ }

    @Override
    public void deactivate(MinecraftServer server) { /* 何もなし */ }
}
