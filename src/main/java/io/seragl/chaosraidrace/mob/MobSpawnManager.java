package io.seragl.chaosraidrace.mob;

import io.seragl.chaosraidrace.ChaosRaidRace;
import io.seragl.chaosraidrace.config.GameConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LightLayer;

import java.util.*;

/**
 * Mobの自動スポーンを管理するクラス。
 */
public final class MobSpawnManager {

    /**
     * Mobカテゴリ。
     */
    public enum MobCategory {
        COMMON(55), UNCOMMON(25), RARE(15), BOSS(5);
        final int weight;
        MobCategory(int weight) { this.weight = weight; }
    }

    /**
     * Mob定義。
     */
    public record MobDef(EntityType<? extends Mob> type, String name, int totalPt, int walletPt, MobCategory category) {}

    private static final List<MobDef> MOB_DEFS = List.of(
            // Common
            new MobDef(EntityType.ZOMBIE, "zombie", 1, 5, MobCategory.COMMON),
            new MobDef(EntityType.SKELETON, "skeleton", 1, 5, MobCategory.COMMON),
            new MobDef(EntityType.SPIDER, "spider", 1, 4, MobCategory.COMMON),
            new MobDef(EntityType.CAVE_SPIDER, "cave_spider", 2, 6, MobCategory.COMMON),
            new MobDef(EntityType.ZOMBIE_VILLAGER, "zombie_villager", 1, 5, MobCategory.COMMON),
            new MobDef(EntityType.HUSK, "husk", 2, 6, MobCategory.COMMON),
            new MobDef(EntityType.STRAY, "stray", 2, 6, MobCategory.COMMON),
            new MobDef(EntityType.DROWNED, "drowned", 2, 6, MobCategory.COMMON),
            new MobDef(EntityType.SLIME, "slime", 1, 3, MobCategory.COMMON),
            new MobDef(EntityType.MAGMA_CUBE, "magma_cube", 1, 3, MobCategory.COMMON),
            new MobDef(EntityType.SILVERFISH, "silverfish", 1, 2, MobCategory.COMMON),
            new MobDef(EntityType.PHANTOM, "phantom", 2, 5, MobCategory.COMMON),

            // Uncommon
            new MobDef(EntityType.CREEPER, "creeper", 3, 10, MobCategory.UNCOMMON),
            new MobDef(EntityType.ENDERMAN, "enderman", 5, 15, MobCategory.UNCOMMON),
            new MobDef(EntityType.WITCH, "witch", 5, 15, MobCategory.UNCOMMON),
            new MobDef(EntityType.PILLAGER, "pillager", 4, 12, MobCategory.UNCOMMON),
            new MobDef(EntityType.VINDICATOR, "vindicator", 5, 15, MobCategory.UNCOMMON),
            new MobDef(EntityType.BOGGED, "bogged", 3, 10, MobCategory.UNCOMMON),
            new MobDef(EntityType.BREEZE, "breeze", 8, 20, MobCategory.UNCOMMON),
            new MobDef(EntityType.BLAZE, "blaze", 6, 18, MobCategory.UNCOMMON),
            new MobDef(EntityType.GHAST, "ghast", 5, 15, MobCategory.UNCOMMON),
            new MobDef(EntityType.PIGLIN_BRUTE, "piglin_brute", 8, 20, MobCategory.UNCOMMON),
            new MobDef(EntityType.ZOMBIFIED_PIGLIN, "zombified_piglin", 3, 10, MobCategory.UNCOMMON),
            new MobDef(EntityType.HOGLIN, "hoglin", 5, 15, MobCategory.UNCOMMON),
            new MobDef(EntityType.ZOGLIN, "zoglin", 6, 18, MobCategory.UNCOMMON),
            new MobDef(EntityType.GUARDIAN, "guardian", 4, 12, MobCategory.UNCOMMON),
            new MobDef(EntityType.SHULKER, "shulker", 6, 18, MobCategory.UNCOMMON),

            // Rare
            new MobDef(EntityType.EVOKER, "evoker", 50, 100, MobCategory.RARE),
            new MobDef(EntityType.RAVAGER, "ravager", 40, 80, MobCategory.RARE),
            new MobDef(EntityType.ELDER_GUARDIAN, "elder_guardian", 30, 60, MobCategory.RARE),
            new MobDef(EntityType.WITHER_SKELETON, "wither_skeleton", 20, 50, MobCategory.RARE),
            new MobDef(EntityType.ENDERMITE, "endermite", 5, 15, MobCategory.RARE),
            new MobDef(EntityType.VEX, "vex", 10, 25, MobCategory.RARE)
    );

    private boolean enabled = false;
    private int tickCounter = 0;
    private int spawnedMobCount = 0;
    private final Random random = new Random();

    /**
     * 自動スポーンを有効化する。
     */
    public void enable() {
        enabled = true;
        tickCounter = 0;
        ChaosRaidRace.LOGGER.info("Mob自動スポーンを有効化しました");
    }

    /**
     * 自動スポーンを無効化する。
     */
    public void disable() {
        enabled = false;
        ChaosRaidRace.LOGGER.info("Mob自動スポーンを無効化しました");
    }

    /**
     * 毎ティック処理。
     */
    public void tick(MinecraftServer server) {
        if (!enabled) return;
        tickCounter++;

        // スポーン間隔チェック
        if (tickCounter % (GameConfig.MOB_SPAWN_INTERVAL_SECONDS * 20) != 0) return;

        // Mob上限チェック
        if (spawnedMobCount >= GameConfig.MAX_MOBS_TOTAL) return;

        ServerLevel overworld = server.overworld();
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        players.removeIf(p -> p.isSpectator() || p.isCreative());

        if (players.isEmpty()) return;

        // プレイヤーごとに周囲にMobをスポーン
        for (ServerPlayer player : players) {
            if (spawnedMobCount >= GameConfig.MAX_MOBS_TOTAL) break;

            MobDef mobDef = selectMob();
            BlockPos spawnPos = findSpawnPosition(overworld, player);
            if (spawnPos == null) continue;

            Mob mob = mobDef.type.create(overworld, EntitySpawnReason.COMMAND);
            if (mob == null) continue;

            mob.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            mob.setPersistenceRequired();
            overworld.addFreshEntity(mob);
            spawnedMobCount++;
        }
    }

    /**
     * 重み付き抽選でMobを選択する。
     */
    private MobDef selectMob() {
        int totalWeight = MOB_DEFS.stream()
                .mapToInt(m -> m.category.weight)
                .sum();
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (MobDef def : MOB_DEFS) {
            cumulative += def.category.weight;
            if (roll < cumulative) return def;
        }
        return MOB_DEFS.getFirst();
    }

    /**
     * スポーン位置を探す。
     */
    private BlockPos findSpawnPosition(ServerLevel level, ServerPlayer player) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = GameConfig.SPAWN_RADIUS_MIN + random.nextInt(GameConfig.SPAWN_RADIUS_MAX - GameConfig.SPAWN_RADIUS_MIN);
            int dz = GameConfig.SPAWN_RADIUS_MIN + random.nextInt(GameConfig.SPAWN_RADIUS_MAX - GameConfig.SPAWN_RADIUS_MIN);
            if (random.nextBoolean()) dx = -dx;
            if (random.nextBoolean()) dz = -dz;

            int x = (int) player.getX() + dx;
            int z = (int) player.getZ() + dz;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);

            BlockPos pos = new BlockPos(x, y, z);

            // 光レベルチェック
            if (level.getBrightness(LightLayer.BLOCK, pos) <= GameConfig.SPAWN_MIN_LIGHT_LEVEL) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Mob名からポイント定義を取得する。
     */
    public Optional<MobDef> getMobDef(String name) {
        return MOB_DEFS.stream().filter(m -> m.name.equals(name)).findFirst();
    }

    /**
     * EntityTypeからポイント定義を取得する。
     */
    public Optional<MobDef> getMobDefByType(EntityType<?> type) {
        return MOB_DEFS.stream().filter(m -> m.type == type).findFirst();
    }

    /**
     * カテゴリ別のMob一覧を取得する。
     */
    public List<MobDef> getMobsByCategory(MobCategory category) {
        return MOB_DEFS.stream().filter(m -> m.category == category).toList();
    }

    /**
     * スポーン数をリセットする。
     */
    public void resetCount() { spawnedMobCount = 0; }

    /**
     * 自動スポーンが有効かどうか。
     */
    public boolean isEnabled() { return enabled; }
}
