package io.seragl.chaosraidrace.chaos.rules;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 「君の荷物は僕の物」ルール。
 * ルール終了時に全参加者のインベントリをランダム再配布する。
 */
public final class InventoryShuffleRule implements ChaosRule {

    @Override
    public String getId() { return "inventory_shuffle"; }

    @Override
    public String getName() { return "君の荷物は僕の物"; }

    @Override
    public String getTitleText() { return "§d§lシャッフル予告"; }

    @Override
    public String getSubtitleText() { return "§fルール終了時に全員のインベントリが入れ替わる！"; }

    @Override
    public void activate(MinecraftServer server) {
        // 予告のみ。終了時にシャッフル
    }

    @Override
    public void tick(MinecraftServer server) { /* 終了時に処理 */ }

    @Override
    public void deactivate(MinecraftServer server) {
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        players.removeIf(p -> p.isSpectator() || p.isCreative());
        if (players.size() < 2) return;

        // 全プレイヤーのインベントリを収集
        List<List<ItemStack>> inventories = new ArrayList<>();
        for (ServerPlayer player : players) {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                items.add(player.getInventory().getItem(i).copy());
            }
            inventories.add(items);
        }

        // シャッフルして再配布
        Collections.shuffle(inventories);
        for (int p = 0; p < players.size(); p++) {
            ServerPlayer player = players.get(p);
            List<ItemStack> newItems = inventories.get(p);
            player.getInventory().clearContent();
            for (int i = 0; i < newItems.size() && i < player.getInventory().getContainerSize(); i++) {
                player.getInventory().setItem(i, newItems.get(i));
            }
            player.sendSystemMessage(Component.literal("§dインベントリがシャッフルされました！"));
        }
    }
}
