package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * 「月面着陸」ルール。
 * JumpBoost IVを付与し、落下ダメージを無効化する。
 */
public final class LowGravityRule implements ChaosRule {

    private boolean active = false;

    @Override
    public String getId() { return "low_gravity"; }

    @Override
    public String getName() { return "月面着陸"; }

    @Override
    public String getTitleText() { return "§b§l低重力"; }

    @Override
    public String getSubtitleText() { return "§fジャンプ力5倍・落下ダメージ無効！空中を駆けろ！"; }

    @Override
    public void activate(MinecraftServer server) {
        active = true;
        applyEffects(server);
    }

    @Override
    public void tick(MinecraftServer server) {
        // 5秒ごとに効果を再付与（新規参加者・デス復帰対応）
        if (active) {
            applyEffects(server);
        }
    }

    @Override
    public void deactivate(MinecraftServer server) {
        active = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.removeEffect(MobEffects.JUMP_BOOST);
        }
    }

    /**
     * 低重力が有効かどうか（落下ダメージキャンセル判定用）。
     */
    public boolean isActive() { return active; }

    private void applyEffects(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.isCreative()) continue;
            if (!player.hasEffect(MobEffects.JUMP_BOOST)) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.JUMP_BOOST, 200, 3, false, false));
            }
        }
    }
}
