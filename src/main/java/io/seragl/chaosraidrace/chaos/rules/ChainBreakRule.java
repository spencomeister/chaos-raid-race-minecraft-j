package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * 「お豆腐建築ハンター」ルール。
 * ブロックを1つ壊すと隣接する同種ブロックが連鎖崩壊する。
 * 実際の処理はBlockBreakイベントリスナーから呼び出される。
 */
public final class ChainBreakRule implements ChaosRule {

    private static final int MAX_CHAIN = 32;

    private boolean active = false;

    @Override
    public String getId() { return "chain_break"; }

    @Override
    public String getName() { return "お豆腐建築ハンター"; }

    @Override
    public String getTitleText() { return "§6§l連鎖破壊"; }

    @Override
    public String getSubtitleText() { return "§fブロックを1つ壊すと隣接する同種も連鎖崩壊！"; }

    @Override
    public void activate(MinecraftServer server) { active = true; }

    @Override
    public void tick(MinecraftServer server) { /* イベントリスナーで処理 */ }

    @Override
    public void deactivate(MinecraftServer server) { active = false; }

    /**
     * 連鎖破壊が有効かどうか。
     */
    public boolean isActive() { return active; }

    /**
     * 連鎖破壊を実行する。
     *
     * @param level 対象ワールド
     * @param pos   起点座標
     * @param block 対象ブロック種
     */
    public void performChainBreak(ServerLevel level, BlockPos pos, Block block) {
        if (!active) return;

        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(pos);
        visited.add(pos);
        int count = 0;

        while (!queue.isEmpty() && count < MAX_CHAIN) {
            BlockPos current = queue.poll();
            BlockState state = level.getBlockState(current);
            if (state.getBlock() == block) {
                level.destroyBlock(current, true);
                count++;

                // 6方向の隣接ブロックを探索
                for (BlockPos neighbor : List.of(
                        current.above(), current.below(),
                        current.north(), current.south(),
                        current.east(), current.west())) {
                    if (!visited.contains(neighbor)
                            && level.getBlockState(neighbor).getBlock() == block) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
    }
}
