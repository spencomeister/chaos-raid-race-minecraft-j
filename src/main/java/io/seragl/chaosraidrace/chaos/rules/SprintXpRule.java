package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;

/**
 * 「爆走経験値」ルール。
 * ダッシュ中のプレイヤーに1秒ごとに経験値オーブを生成する。
 */
public final class SprintXpRule implements ChaosRule {

    private static final int XP_INTERVAL_TICKS = 20; // 1秒ごと
    private int tickCounter = 0;

    @Override
    public String getId() { return "sprint_xp"; }

    @Override
    public String getName() { return "爆走経験値"; }

    @Override
    public String getTitleText() { return "§a§lダッシュ稼ぎ"; }

    @Override
    public String getSubtitleText() { return "§f走れば走るほど経験値が溢れ出す！止まるな！"; }

    @Override
    public void activate(MinecraftServer server) { tickCounter = 0; }

    @Override
    public void tick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % XP_INTERVAL_TICKS != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.isCreative()) continue;
            if (player.isSprinting()) {
                ServerLevel level = (ServerLevel) player.level();
                ExperienceOrb orb = new ExperienceOrb(
                        level,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        5);
                level.addFreshEntity(orb);
            }
        }
    }

    @Override
    public void deactivate(MinecraftServer server) { tickCounter = 0; }
}
