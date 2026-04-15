package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 「止まると死ぬぜ？」ルール。
 * 3秒以上静止するとTNT相当の小爆発が発生する。
 */
public final class StopAndDieRule implements ChaosRule {

    private static final int STILLNESS_THRESHOLD_TICKS = 60; // 3秒

    private final Map<UUID, BlockPos> lastPositions = new HashMap<>();
    private final Map<UUID, Integer> stillnessTicks = new HashMap<>();

    @Override
    public String getId() { return "stop_and_die"; }

    @Override
    public String getName() { return "止まると死ぬぜ？"; }

    @Override
    public String getTitleText() { return "§c§l静止禁止"; }

    @Override
    public String getSubtitleText() { return "§f3秒以上止まると足元が爆発する！動き続けろ！"; }

    @Override
    public void activate(MinecraftServer server) {
        lastPositions.clear();
        stillnessTicks.clear();
    }

    @Override
    public void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator() || player.isCreative()) continue;

            UUID uuid = player.getUUID();
            BlockPos currentPos = player.blockPosition();
            BlockPos lastPos = lastPositions.get(uuid);

            if (lastPos != null && lastPos.equals(currentPos)) {
                int ticks = stillnessTicks.getOrDefault(uuid, 0) + 1;
                stillnessTicks.put(uuid, ticks);

                if (ticks >= STILLNESS_THRESHOLD_TICKS) {
                    // 爆発（ブロック破壊なし、ダメージあり）
                    ServerLevel level = (ServerLevel) player.level();
                    level.explode(null, player.getX(), player.getY(), player.getZ(),
                            2.0f, Level.ExplosionInteraction.NONE);
                    stillnessTicks.put(uuid, 0);
                }
            } else {
                stillnessTicks.put(uuid, 0);
            }
            lastPositions.put(uuid, currentPos);
        }
    }

    @Override
    public void deactivate(MinecraftServer server) {
        lastPositions.clear();
        stillnessTicks.clear();
    }
}
