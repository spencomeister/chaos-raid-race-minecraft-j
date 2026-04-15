package io.seragl.chaosraidrace.ui;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * タイトル・チャット・サウンドなどのアナウンスを管理するクラス。
 */
public final class AnnouncementManager {

    /**
     * 全プレイヤーにタイトルを表示する。
     */
    public void showTitle(MinecraftServer server, String title, String subtitle) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        }
    }

    /**
     * 全プレイヤーにチャットメッセージを送信する。
     */
    public void broadcast(MinecraftServer server, String message) {
        Component component = Component.literal(message);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(component);
        }
    }

    /**
     * 全プレイヤーにActionBarメッセージを表示する。
     */
    public void showActionBar(MinecraftServer server, String message) {
        Component component = Component.literal(message);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendOverlayMessage(component);
        }
    }

    /**
     * 全プレイヤーにサウンドを再生する。
     */
    public void playSound(MinecraftServer server, SoundEvent sound, float volume, float pitch) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSoundPacket(
                    Holder.direct(sound), SoundSource.MASTER,
                    player.getX(), player.getY(), player.getZ(),
                    volume, pitch, player.level().getRandom().nextLong()));
        }
    }

    /**
     * ゲーム開始演出を表示する。
     */
    public void showGameStart(MinecraftServer server) {
        showTitle(server, "§a§lゲームスタート！", "§f5分間で準備を整えろ！");
        playSound(server, SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    /**
     * ゲーム終了演出を表示する。
     */
    public void showGameEnd(MinecraftServer server, String winnerTeamName) {
        showTitle(server, "§6§l【GAME SET】", "§f" + winnerTeamName + " の勝利！");
        playSound(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    /**
     * 中間スコア発表を表示する。
     */
    public void showMidScoreReport(MinecraftServer server, String report) {
        broadcast(server, report);
        playSound(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
    }

    /**
     * 最終フェーズ突入演出を表示する。
     */
    public void showFinalPhaseStart(MinecraftServer server) {
        showTitle(server, "§4§l奪い合え。生き残れ。",
                 "§fスコアは見えない。信じられるのは自分の手だけだ。");
        playSound(server, SoundEvents.WITHER_SPAWN, 1.0f, 1.0f);
    }

    /**
     * エンドラ討伐演出を表示する。
     */
    public void showDragonSlain(MinecraftServer server, String teamName) {
        showTitle(server, "§5§l【DRAGON SLAIN】",
                 "§f" + teamName + " がエンダードラゴンを討伐！ボーナスPt獲得！");
    }

    /**
     * 初心者ガイドをエンドで表示する。
     */
    public void showEndGuides(MinecraftServer server) {
        String[] guides = {
                "§b[ガイド①] エンドクリスタル（柱の上の光る球）を先に全部壊そう！回復が止まる！",
                "§b[ガイド②] 弓矢で遠くから水晶を狙うのが安全！",
                "§b[ガイド③] エンドラが降りてきたら近接攻撃のチャンス！頭を狙え！",
                "§b[ガイド④] 奈落（黒い虚空）に落ちると即死！端には近づくな！",
                "§b[ガイド⑤] 敵チームのプレイヤーも倒せばポイントになる！"
        };
        for (String guide : guides) {
            broadcast(server, guide);
        }
    }
}
