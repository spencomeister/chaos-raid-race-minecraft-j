package io.seragl.chaosraidrace.base;

import io.seragl.chaosraidrace.config.GameConfig;
import io.seragl.chaosraidrace.team.TeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Optional;

/**
 * 拠点の保護（ブロック破壊・爆発禁止）を管理するクラス。
 */
public final class BaseProtectionManager {

    private final TeamManager teamManager;

    public BaseProtectionManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    /**
     * 指定座標がいずれかの拠点内かどうかを判定する。
     */
    public boolean isInsideAnyBase(BlockPos pos) {
        for (Map.Entry<String, BlockPos> entry : GameConfig.TEAM_BASE_CENTERS.entrySet()) {
            BlockPos center = entry.getValue();
            double distSq = center.distSqr(pos);
            if (distSq <= GameConfig.BASE_RADIUS * GameConfig.BASE_RADIUS) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定座標がプレイヤーの所属チームの拠点内かどうかを判定する。
     */
    public boolean isInsideOwnBase(BlockPos pos, Player player) {
        return teamManager.getPlayerTeam(player.getUUID()).map(team -> {
            BlockPos center = GameConfig.TEAM_BASE_CENTERS.get(team.getId());
            if (center == null) return false;
            return center.distSqr(pos) <= GameConfig.BASE_RADIUS * GameConfig.BASE_RADIUS;
        }).orElse(false);
    }

    /**
     * プレイヤーが拠点内にいるかどうか。
     */
    public boolean isPlayerInBase(ServerPlayer player) {
        return isInsideAnyBase(player.blockPosition());
    }

    /**
     * プレイヤーが自チームの拠点内にいるかどうか。
     */
    public boolean isPlayerInOwnBase(ServerPlayer player) {
        return isInsideOwnBase(player.blockPosition(), player);
    }

    /**
     * ブロック破壊を許可するかどうかを判定する。
     *
     * @param pos    破壊しようとしているブロック座標
     * @param player 破壊しようとしているプレイヤー
     * @return 許可する場合true
     */
    public boolean canBreakBlock(BlockPos pos, Player player) {
        if (!isInsideAnyBase(pos)) return true;
        if (player instanceof ServerPlayer sp && sp.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) return true; // OP権限
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("§c拠点内のブロックは破壊できません"));
        }
        return false;
    }

    /**
     * 拠点のスポーン地点を取得する。
     */
    public Optional<BlockPos> getTeamSpawnPoint(String teamId) {
        return Optional.ofNullable(GameConfig.TEAM_BED_POSITIONS.get(teamId));
    }
}
