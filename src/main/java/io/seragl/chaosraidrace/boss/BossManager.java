package io.seragl.chaosraidrace.boss;

import io.seragl.chaosraidrace.ChaosRaidRace;
import io.seragl.chaosraidrace.config.GameConfig;
import io.seragl.chaosraidrace.score.ScoreManager;
import io.seragl.chaosraidrace.team.GameTeam;
import io.seragl.chaosraidrace.team.TeamManager;
import io.seragl.chaosraidrace.ui.AnnouncementManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.*;

/**
 * 中ボスの召喚・管理・討伐報酬を処理するマネージャー。
 */
public final class BossManager {

    /** ボス情報定義 */
    private record BossInfo(EntityType<? extends Mob> entityType, float scale, double health,
                            String name, int totalPt, int walletPt) {}

    private static final BossInfo[] BOSSES = {
            new BossInfo(EntityType.ZOMBIE, 3.0f, 300, "巨大ゾンビ", 500, 1000),
            new BossInfo(EntityType.RAVAGER, 3.0f, 450, "巨大ラヴェジャー", 800, 1500),
            new BossInfo(EntityType.WARDEN, 2.5f, 600, "覚醒ウォーデン", 1500, 3000),
    };

    private final TeamManager teamManager;
    private final ScoreManager scoreManager;
    private final AnnouncementManager announcements;
    private LivingEntity currentBoss = null;
    private int currentBossIndex = -1;
    private final Map<String, Double> teamDamageMap = new HashMap<>();

    public BossManager(TeamManager teamManager, ScoreManager scoreManager, AnnouncementManager announcements) {
        this.teamManager = teamManager;
        this.scoreManager = scoreManager;
        this.announcements = announcements;
    }

    /**
     * 指定回のボスを召喚する（0始まり）。
     */
    public void spawnBoss(MinecraftServer server, int bossIndex) {
        if (bossIndex < 0 || bossIndex >= BOSSES.length) return;
        if (currentBoss != null && currentBoss.isAlive()) {
            ChaosRaidRace.LOGGER.warn("既存のボスがまだ生存中です");
            return;
        }

        ServerLevel overworld = server.overworld();
        BossInfo info = BOSSES[bossIndex];
        currentBossIndex = bossIndex;
        teamDamageMap.clear();

        // ボスをスポーン
        Mob boss = info.entityType.create(overworld, EntitySpawnReason.COMMAND);
        if (boss == null) return;

        boss.setPos(0, 80, 0);
        boss.setCustomName(Component.literal("§4§l" + info.name));
        boss.setCustomNameVisible(true);

        // 属性を設定
        var maxHealthAttr = boss.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(info.health);
        }
        boss.setHealth((float) info.health);

        var scaleAttr = boss.getAttribute(Attributes.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(info.scale);
        }

        boss.setPersistenceRequired();
        overworld.addFreshEntity(boss);
        currentBoss = boss;

        // 演出
        announcements.showTitle(server, "§4§l【BOSS出現】", "§f" + info.name + " が現れた！");
        ChaosRaidRace.LOGGER.info("中ボス#{} ({}) を召喚しました", bossIndex + 1, info.name);
    }

    /**
     * 毎ティック処理（ウォーデンの暗闇エフェクト等）。
     */
    public void tick(MinecraftServer server) {
        if (currentBoss == null || !currentBoss.isAlive()) {
            if (currentBoss != null && !currentBoss.isAlive()) {
                onBossDefeated(server);
                currentBoss = null;
            }
            return;
        }

        // ウォーデンの暗闘エフェクト（半径8m以内）
        if (currentBossIndex == 2) { // Warden
            int radius = 8;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                double dist = player.distanceToSqr(currentBoss);
                if (dist <= radius * radius) {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.DARKNESS, 60, 1, false, false));
                } else {
                    player.removeEffect(MobEffects.DARKNESS);
                }
            }
        }
    }

    /**
     * ボスへのダメージを記録する。
     */
    public void recordDamage(ServerPlayer player, double damage) {
        if (currentBoss == null) return;
        Optional<GameTeam> team = teamManager.getPlayerTeam(player.getUUID());
        team.ifPresent(t -> teamDamageMap.merge(t.getId(), damage, Double::sum));
    }

    /**
     * ボスが討伐された時の処理。
     */
    private void onBossDefeated(MinecraftServer server) {
        if (currentBossIndex < 0 || currentBossIndex >= BOSSES.length) return;
        BossInfo info = BOSSES[currentBossIndex];

        // チーム別貢献度ランキング
        List<Map.Entry<String, Double>> ranked = new ArrayList<>(teamDamageMap.entrySet());
        ranked.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        int[] contributionBonuses = {0, 150, 75}; // 1位はラストヒット報酬、2位、3位

        for (int i = 0; i < ranked.size(); i++) {
            String teamId = ranked.get(i).getKey();
            Optional<GameTeam> teamOpt = teamManager.getTeam(teamId);
            if (teamOpt.isEmpty()) continue;
            GameTeam team = teamOpt.get();

            if (i == 0) {
                // ラストヒットチーム
                scoreManager.addScoreRaw(team, info.totalPt, info.walletPt);
                announcements.broadcast(server,
                        "§6§l" + team.getDisplayName() + " がラストヒット！ 累計+" + info.totalPt + "pt / 消費+" + info.walletPt + "pt");
            } else if (i < contributionBonuses.length) {
                scoreManager.addScoreRaw(team, contributionBonuses[i], 0);
                announcements.broadcast(server,
                        "§7" + team.getDisplayName() + " 貢献" + (i + 1) + "位 累計+" + contributionBonuses[i] + "pt");
            }
        }

        ChaosRaidRace.LOGGER.info("中ボス#{} を討伐しました", currentBossIndex + 1);

        // 物理ドロップを散布
        spawnBossDrops(server, currentBossIndex);

        teamDamageMap.clear();
        currentBossIndex = -1;
    }

    /**
     * ボス討伐時の物理ドロップを散布する。
     */
    private void spawnBossDrops(MinecraftServer server, int bossIndex) {
        if (currentBoss == null) return;
        ServerLevel level = server.overworld();
        double bx = currentBoss.getX(), by = currentBoss.getY(), bz = currentBoss.getZ();
        Random rng = new Random();

        List<ItemStack> drops = new ArrayList<>();
        switch (bossIndex) {
            case 0 -> { // 第1弾: トーテム×1、金リンゴ×8、鋭さIV本×1
                drops.add(new ItemStack(Items.TOTEM_OF_UNDYING));
                drops.add(new ItemStack(Items.GOLDEN_APPLE, 8));
                // 鋭さIVエンチャント本
                Holder<Enchantment> sharpness = server.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                book.enchant(sharpness, 4);
                drops.add(book);
            }
            case 1 -> { // 第2弾: トーテム×2、ダイヤ剣(鋭さV)×1、金リンゴ×16、経験値ボトル×32
                drops.add(new ItemStack(Items.TOTEM_OF_UNDYING));
                drops.add(new ItemStack(Items.TOTEM_OF_UNDYING));
                Holder<Enchantment> sharpness = server.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.enchant(sharpness, 5);
                drops.add(sword);
                drops.add(new ItemStack(Items.GOLDEN_APPLE, 16));
                drops.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 32));
            }
            case 2 -> { // 第3弾: トーテム×3、フルダイヤ防具セット×1、金リンゴ×32、経験値ボトル×64
                drops.add(new ItemStack(Items.TOTEM_OF_UNDYING));
                drops.add(new ItemStack(Items.TOTEM_OF_UNDYING));
                drops.add(new ItemStack(Items.TOTEM_OF_UNDYING));
                drops.add(new ItemStack(Items.DIAMOND_HELMET));
                drops.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
                drops.add(new ItemStack(Items.DIAMOND_LEGGINGS));
                drops.add(new ItemStack(Items.DIAMOND_BOOTS));
                drops.add(new ItemStack(Items.GOLDEN_APPLE, 32));
                drops.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 64));
            }
        }

        // ボスの位置を中心に半径5ブロック以内にランダム散布
        for (ItemStack stack : drops) {
            double dx = (rng.nextDouble() - 0.5) * 10.0;
            double dz = (rng.nextDouble() - 0.5) * 10.0;
            ItemEntity itemEntity = new ItemEntity(level, bx + dx, by + 1.0, bz + dz, stack);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }

        ChaosRaidRace.LOGGER.info("ボス#{} のドロップアイテムを散布しました（{}個）", bossIndex + 1, drops.size());
    }

    /**
     * 現在のボスを即時消去する。
     */
    public void killCurrentBoss() {
        if (currentBoss != null && currentBoss.isAlive()) {
            currentBoss.discard();
            currentBoss = null;
            currentBossIndex = -1;
            teamDamageMap.clear();
        }
    }

    /**
     * ボスが現在スポーン中かどうか。
     */
    public boolean isBossActive() {
        return currentBoss != null && currentBoss.isAlive();
    }

    /**
     * 現在のボスの体力割合を返す。
     */
    public float getBossHealthPercent() {
        if (currentBoss == null || !currentBoss.isAlive()) return 0;
        return currentBoss.getHealth() / currentBoss.getMaxHealth();
    }

    /**
     * 現在のボス名を返す。
     */
    public String getCurrentBossName() {
        if (currentBossIndex >= 0 && currentBossIndex < BOSSES.length) {
            return BOSSES[currentBossIndex].name;
        }
        return "";
    }
}
