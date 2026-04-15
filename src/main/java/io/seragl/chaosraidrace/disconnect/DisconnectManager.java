package io.seragl.chaosraidrace.disconnect;

import io.seragl.chaosraidrace.ChaosRaidRace;
import io.seragl.chaosraidrace.config.GameConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * プレイヤーの切断・復帰を管理するクラス。
 */
public final class DisconnectManager {

    /**
     * 保存されたプレイヤー状態。
     */
    private record SavedState(List<ItemStack> inventory, BlockPos position, long disconnectTime) {}

    private final Map<UUID, SavedState> savedStates = new HashMap<>();

    /**
     * プレイヤーの切断時に状態を保存する。
     */
    public void onDisconnect(ServerPlayer player) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            items.add(player.getInventory().getItem(i).copy());
        }

        savedStates.put(player.getUUID(), new SavedState(
                items, player.blockPosition(), System.currentTimeMillis()));

        ChaosRaidRace.LOGGER.info("プレイヤー {} の状態を保存しました", player.getName().getString());
    }

    /**
     * プレイヤーの復帰時に状態を復元する。
     *
     * @param player 復帰したプレイヤー
     * @return 復元が行われた場合true
     */
    public boolean onReconnect(ServerPlayer player) {
        SavedState state = savedStates.remove(player.getUUID());
        if (state == null) return false;

        long elapsed = System.currentTimeMillis() - state.disconnectTime;
        long graceMs = GameConfig.REJOIN_GRACE_SECONDS * 1000L;

        if (elapsed > graceMs) {
            // 猶予時間超過
            player.sendSystemMessage(Component.literal(
                    "§c[復帰] 猶予時間を超過しました。インベントリは復元されません。"));
            ChaosRaidRace.LOGGER.info("プレイヤー {} の猶予時間が超過しました", player.getName().getString());
            return false;
        }

        // インベントリ復元
        player.getInventory().clearContent();
        for (int i = 0; i < state.inventory.size() && i < player.getInventory().getContainerSize(); i++) {
            player.getInventory().setItem(i, state.inventory.get(i));
        }

        // 座標復元
        player.teleportTo(state.position.getX() + 0.5, state.position.getY(), state.position.getZ() + 0.5);

        player.sendSystemMessage(Component.literal("§a[復帰] ゲームに再参加しました"));
        ChaosRaidRace.LOGGER.info("プレイヤー {} の状態を復元しました", player.getName().getString());
        return true;
    }

    /**
     * 保存された切断状態があるかどうか。
     */
    public boolean hasSavedState(UUID uuid) {
        return savedStates.containsKey(uuid);
    }

    /**
     * 全状態をクリアする。
     */
    public void reset() {
        savedStates.clear();
    }
}
