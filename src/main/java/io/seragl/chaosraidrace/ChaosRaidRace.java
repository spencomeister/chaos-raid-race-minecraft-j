package io.seragl.chaosraidrace;

import io.seragl.chaosraidrace.chaos.ChaosRule;
import io.seragl.chaosraidrace.chaos.rules.*;
import io.seragl.chaosraidrace.command.CrrCommand;
import io.seragl.chaosraidrace.command.VaultCommand;
import io.seragl.chaosraidrace.config.ConfigLoader;
import io.seragl.chaosraidrace.config.GameConfig;
import io.seragl.chaosraidrace.game.GameManager;
import io.seragl.chaosraidrace.team.GameTeam;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * カオス・レイド・レース MOD のエントリーポイント。
 * サーバーサイド専用の Fabric MOD として動作する。
 * 全イベントリスナーの登録とサブシステムの配線を行う。
 */
public class ChaosRaidRace implements DedicatedServerModInitializer {

    public static final String MOD_ID = "chaos-raid-race";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final GameManager gameManager = new GameManager();
    private final Random random = new Random();

    /** PacifistRule ダメージ反射の無限ループ防止用フラグ */
    private final Set<UUID> reflectionInProgress = new HashSet<>();

    /** ランダムドロップ用アイテムプール */
    private static final Item[] RANDOM_DROP_POOL = {
            Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT,
            Items.LAPIS_LAZULI, Items.REDSTONE, Items.COAL, Items.STICK,
            Items.ROTTEN_FLESH, Items.BONE, Items.STRING, Items.GUNPOWDER,
            Items.ENDER_PEARL, Items.SLIME_BALL, Items.GLOWSTONE_DUST
    };

    /** 錬金ドロップ用レアアイテムプール */
    private static final Item[] ALCHEMY_RARE_POOL = {
            Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.ENCHANTED_GOLDEN_APPLE
    };

    @Override
    public void onInitializeServer() {
        LOGGER.info("カオス・レイド・レース MOD を初期化しています...");
        ConfigLoader.load(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        registerEvents();
        LOGGER.info("カオス・レイド・レース MOD の初期化が完了しました");
    }

    /**
     * 全イベントリスナーを登録する。
     */
    private void registerEvents() {
        // === サーバー起動完了 ===
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                LOGGER.info("サーバー起動完了 - カオス・レイド・レース待機中"));

        // === サーバーティック（メインループ） ===
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            gameManager.tick(server);
            gameManager.syncHappyMobMultiplier();
        });

        // === コマンド登録 ===
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CrrCommand.register(dispatcher, gameManager);
            VaultCommand.registerVault(dispatcher, gameManager);
            VaultCommand.registerShop(dispatcher, gameManager);
            LOGGER.info("コマンドを登録しました");
        });

        // === ブロック破壊前（拠点保護） ===
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
                gameManager.getBaseProtection().canBreakBlock(pos, player));

        // === ブロック破壊後（カオスルール効果） ===
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel serverLevel)) return;
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            for (ChaosRule rule : gameManager.getChaosRuleManager().getActiveRules()) {
                // 連鎖破壊
                if (rule instanceof ChainBreakRule chainBreak && chainBreak.isActive()) {
                    chainBreak.performChainBreak(serverLevel, pos, state.getBlock());
                }
                // ランダムドロップ
                if (rule instanceof RandomDropRule randomDrop && randomDrop.isActive()) {
                    Item randomItem = RANDOM_DROP_POOL[random.nextInt(RANDOM_DROP_POOL.length)];
                    serverPlayer.drop(new ItemStack(randomItem), false);
                }
                // 錬金術（石系ブロック破壊時、5%でレアドロップ）
                if (rule instanceof AlchemyRule alchemy && alchemy.isActive()) {
                    if (state.getBlock() == Blocks.STONE
                            || state.getBlock() == Blocks.DEEPSLATE
                            || state.getBlock() == Blocks.COBBLESTONE) {
                        if (random.nextDouble() < 0.05) {
                            Item rareItem = ALCHEMY_RARE_POOL[random.nextInt(ALCHEMY_RARE_POOL.length)];
                            serverPlayer.drop(new ItemStack(rareItem), false);
                            serverPlayer.sendOverlayMessage(
                                    Component.literal("§e§l錬金成功！"));
                        }
                    }
                }
            }
        });

        // === ダメージ許可チェック ===
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            // 低重力ルール中の落下ダメージ無効化
            if (source.is(DamageTypes.FALL)) {
                for (ChaosRule rule : gameManager.getChaosRuleManager().getActiveRules()) {
                    if (rule instanceof LowGravityRule lowGrav && lowGrav.isActive()) {
                        return false;
                    }
                }
            }

            // 不殺の誓い（PacifistRule）ダメージ反射
            if (source.getEntity() instanceof ServerPlayer attacker) {
                for (ChaosRule rule : gameManager.getChaosRuleManager().getActiveRules()) {
                    if (rule instanceof PacifistRule pacifist && pacifist.isActive()) {
                        if (!reflectionInProgress.contains(attacker.getUUID())) {
                            reflectionInProgress.add(attacker.getUUID());
                            try {
                                attacker.hurtServer(attacker.level(), entity.damageSources().thorns(entity), amount);
                            } finally {
                                reflectionInProgress.remove(attacker.getUUID());
                            }
                        }
                    }
                }

                // ボスへのダメージ記録
                gameManager.getBossManager().recordDamage(attacker, amount);
            }

            return true;
        });

        // === エンティティ討伐 ===
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, entity, killedEntity, damageSource) -> {
            if (!(entity instanceof ServerPlayer killer)) return;

            // エンダードラゴン討伐
            if (killedEntity.getType() == EntityType.ENDER_DRAGON) {
                gameManager.getTeamManager().getPlayerTeam(killer.getUUID()).ifPresent(team -> {
                    gameManager.getScoreManager().addScoreRaw(team,
                            GameConfig.ENDER_DRAGON_LAST_HIT_TOTAL,
                            GameConfig.ENDER_DRAGON_LAST_HIT_WALLET);
                    gameManager.getAnnouncements().showDragonSlain(
                            level.getServer(), team.getDisplayName());
                    gameManager.getFinalPhaseManager().setDragonDefeated(true);
                });
                return;
            }

            // 最終フェーズPvPキル
            if (killedEntity instanceof ServerPlayer victim
                    && gameManager.getFinalPhaseManager().isActive()) {
                gameManager.getFinalPhaseManager().onPvpKill(killer, victim);
                return;
            }

            // 通常Mob討伐スコア
            gameManager.getMobSpawnManager().getMobDefByType(killedEntity.getType())
                    .ifPresent(mobDef ->
                            gameManager.getScoreManager().addMobKillScore(
                                    killer, mobDef.totalPt(), mobDef.walletPt()));
        });

        // === プレイヤーデス処理（デスペナルティ適用） ===
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                boolean isFinal = gameManager.getFinalPhaseManager().isActive();
                gameManager.getScoreManager().applyDeathPenalty(player, isFinal);
            }
            return true;
        });

        // === リスポーン後処理 ===
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // 暗視エフェクト再付与
            gameManager.getEnvironmentManager().applyNightVision(newPlayer.level().getServer());

            // 最終フェーズ中はエンドの安全地点にTPし無敵付与
            if (gameManager.getFinalPhaseManager().isActive()) {
                BlockPos safePos = gameManager.getFinalPhaseManager().getSafeRespawnPos(newPlayer);
                ServerLevel end = newPlayer.level().getServer().getLevel(Level.END);
                if (end != null) {
                    newPlayer.teleport(new TeleportTransition(
                            end,
                            new Vec3(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5),
                            Vec3.ZERO, newPlayer.getYRot(), newPlayer.getXRot(),
                            TeleportTransition.DO_NOTHING));
                }
                gameManager.getFinalPhaseManager().applyRespawnInvincibility(newPlayer);
            }
        });

        // === プレイヤー接続 ===
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;

            // BossBar追加
            gameManager.getBossBarManager().addPlayer(player);

            // 切断復帰処理
            if (gameManager.getDisconnectManager().hasSavedState(player.getUUID())) {
                gameManager.getDisconnectManager().onReconnect(player);
            }

            // 暗視エフェクト（ゲーム進行中の場合）
            if (gameManager.getTimer().isRunning()) {
                gameManager.getEnvironmentManager().applyNightVision(server);
            }
        });

        // === プレイヤー切断 ===
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;

            // BossBar削除
            gameManager.getBossBarManager().removePlayer(player);

            // ゲーム進行中なら状態保存
            if (gameManager.getTimer().isRunning()) {
                gameManager.getDisconnectManager().onDisconnect(player);
            }
        });
    }
}
