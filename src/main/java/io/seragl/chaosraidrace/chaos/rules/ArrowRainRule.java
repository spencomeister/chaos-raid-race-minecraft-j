package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.LightLayer;

/**
 * 「お空からのお仕置き」ルール。
 * 屋外のプレイヤー近くに空から矢が降り注ぐ。
 */
public final class ArrowRainRule implements ChaosRule {

    private static final int ARROW_INTERVAL_TICKS = 20; // 1秒ごと

    private int tickCounter = 0;

    @Override
    public String getId() { return "arrow_rain"; }

    @Override
    public String getName() { return "お空からのお仕置き"; }

    @Override
    public String getTitleText() { return "§c§l弓矢の雨"; }

    @Override
    public String getSubtitleText() { return "§f空から矢が降ってくる！屋根を探せ！"; }

    @Override
    public void activate(MinecraftServer server) { tickCounter = 0; }

    @Override
    public void tick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % ARROW_INTERVAL_TICKS != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.isCreative()) continue;

            ServerLevel level = (ServerLevel) player.level();
            BlockPos pos = player.blockPosition();

            // 空が見える場所（SkyLight が届く）かチェック
            if (level.getBrightness(LightLayer.SKY, pos.above(2)) > 0) {
                // プレイヤーの上方に矢をスポーン
                double x = player.getX() + (level.getRandom().nextDouble() - 0.5) * 6;
                double z = player.getZ() + (level.getRandom().nextDouble() - 0.5) * 6;
                double y = player.getY() + 20;

                Arrow arrow = new Arrow(EntityType.ARROW, level);
                arrow.setPos(x, y, z);
                arrow.setDeltaMovement(0, -2.0, 0);
                arrow.setBaseDamage(2.0);
                level.addFreshEntity(arrow);
            }
        }
    }

    @Override
    public void deactivate(MinecraftServer server) { tickCounter = 0; }
}
