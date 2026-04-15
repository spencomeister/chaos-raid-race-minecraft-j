package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 「空が気になるお年頃」ルール。
 * 視点を強制的に上向きまたは下向きに固定する。
 */
public final class ViewLockRule implements ChaosRule {

    private final Map<UUID, Float> lockedPitch = new HashMap<>();
    private final Random random = new Random();

    @Override
    public String getId() { return "view_lock"; }

    @Override
    public String getName() { return "空が気になるお年頃"; }

    @Override
    public String getTitleText() { return "§e§l視点固定"; }

    @Override
    public String getSubtitleText() { return "§f視点が強制固定された！慣れない方向で戦え！"; }

    @Override
    public void activate(MinecraftServer server) {
        lockedPitch.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.isCreative()) continue;
            // ランダムで上向き(-80)か下向き(+80)を割り当て
            float pitch = random.nextBoolean() ? -80.0f : 80.0f;
            lockedPitch.put(player.getUUID(), pitch);
        }
    }

    @Override
    public void tick(MinecraftServer server) {
        // 10ティックごとに視点を強制
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Float pitch = lockedPitch.get(player.getUUID());
            if (pitch == null || player.isSpectator() || player.isCreative()) continue;

            if (Math.abs(player.getXRot() - pitch) > 5.0f) {
                player.setXRot(pitch);
                // クライアントに位置同期（視点のみ更新）
                var posRot = new PositionMoveRotation(
                        Vec3.ZERO, Vec3.ZERO, player.getYRot(), pitch);
                player.connection.send(new ClientboundPlayerPositionPacket(
                        0, posRot, Set.of()));
            }
        }
    }

    @Override
    public void deactivate(MinecraftServer server) {
        lockedPitch.clear();
    }
}
