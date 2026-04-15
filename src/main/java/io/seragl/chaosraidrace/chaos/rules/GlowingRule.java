package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * 「透視能力に目覚めた」ルール。
 * 全プレイヤー・Mobに常時GLOWINGエフェクトを付与する。
 */
public final class GlowingRule implements ChaosRule {

    private boolean active = false;

    @Override
    public String getId() { return "glowing"; }

    @Override
    public String getName() { return "透視能力に目覚めた"; }

    @Override
    public String getTitleText() { return "§f§l全員発光"; }

    @Override
    public String getSubtitleText() { return "§f全員が発光！隠れても無駄だ！"; }

    @Override
    public void activate(MinecraftServer server) {
        active = true;
        applyGlowing(server);
    }

    @Override
    public void tick(MinecraftServer server) {
        if (active) {
            applyGlowing(server);
        }
    }

    @Override
    public void deactivate(MinecraftServer server) {
        active = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.removeEffect(MobEffects.GLOWING);
        }
    }

    /**
     * 発光が有効かどうか。
     */
    public boolean isActive() { return active; }

    private void applyGlowing(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) continue;
            if (!player.hasEffect(MobEffects.GLOWING)) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING, 200, 0, false, false));
            }
        }
    }
}
