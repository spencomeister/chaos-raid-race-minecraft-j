package io.seragl.chaosraidrace.environment;

import io.seragl.chaosraidrace.ChaosRaidRace;
import io.seragl.chaosraidrace.config.GameConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * 環境設定（時間・天候・暗視・ワールドボーダー）を管理するクラス。
 */
public final class EnvironmentManager {

    private int nightVisionTickCounter = 0;

    /**
     * ゲーム開始時の環境を設定する。
     */
    public void setupGameEnvironment(MinecraftServer server) {
        ServerLevel overworld = server.overworld();

        // 時間固定（クロックマネージャー経由）
        var clockHolder = server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
        server.clockManager().setTotalTicks(clockHolder, GameConfig.TIME_FIXED);
        server.getGameRules().set(GameRules.ADVANCE_TIME, false, server);

        // 天候固定（晴れ）
        server.getGameRules().set(GameRules.ADVANCE_WEATHER, false, server);
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withSuppressedOutput(), "weather clear");

        // PvP無効（準備フェーズ）
        server.getGameRules().set(GameRules.PVP, false, server);

        // Mob griefing無効（爆発によるブロック破壊を防止）
        server.getGameRules().set(GameRules.MOB_GRIEFING, false, server);

        // ワールドボーダー
        overworld.getWorldBorder().setSize(GameConfig.BORDER_RADIUS * 2);
        overworld.getWorldBorder().setCenter(0, 0);

        // 暗視エフェクト付与
        applyNightVision(server);

        ChaosRaidRace.LOGGER.info("ゲーム環境を設定しました（時刻固定: {}、天候: 晴れ、ボーダー: {}）",
                GameConfig.TIME_FIXED, GameConfig.BORDER_RADIUS);
    }

    /**
     * 毎ティック処理。
     */
    public void tick(MinecraftServer server) {
        nightVisionTickCounter++;
        // 5秒ごとに暗視を確認・再付与
        if (nightVisionTickCounter % 100 == 0) {
            applyNightVision(server);
        }
    }

    /**
     * PvPの有効・無効を切り替える。
     */
    public void setPvpEnabled(MinecraftServer server, boolean enabled) {
        server.getGameRules().set(GameRules.PVP, enabled, server);
        ChaosRaidRace.LOGGER.info("PvP: {}", enabled ? "有効" : "無効");
    }

    /**
     * 暗視エフェクトを全プレイヤーに付与する。
     */
    public void applyNightVision(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.hasEffect(MobEffects.NIGHT_VISION)
                    || player.getEffect(MobEffects.NIGHT_VISION).getDuration() < 100) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            }
        }
    }

    /**
     * 環境をリセットする。
     */
    public void reset(MinecraftServer server) {
        server.getGameRules().set(GameRules.ADVANCE_TIME, true, server);
        server.getGameRules().set(GameRules.ADVANCE_WEATHER, true, server);
        server.getGameRules().set(GameRules.PVP, true, server);
        server.getGameRules().set(GameRules.MOB_GRIEFING, true, server);
        nightVisionTickCounter = 0;
    }
}
