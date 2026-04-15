package io.seragl.chaosraidrace.shop;

import io.seragl.chaosraidrace.team.GameTeam;
import io.seragl.chaosraidrace.team.TeamManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.*;

/**
 * ショップシステムを管理するクラス。
 */
public final class ShopManager {

    /**
     * ショップ商品定義。
     */
    public record ShopItem(String id, String displayName, int price, ItemProvider provider) {}

    @FunctionalInterface
    public interface ItemProvider {
        /** 購入時にプレイヤーにアイテムを付与する。 */
        void provide(ServerPlayer player);
    }

    private final TeamManager teamManager;
    private final List<ShopItem> items = new ArrayList<>();

    public ShopManager(TeamManager teamManager) {
        this.teamManager = teamManager;
        registerItems();
    }

    /**
     * 全商品を登録する。
     */
    private void registerItems() {
        // 超目玉
        items.add(new ShopItem("totem", "不死のトーテム", 1500,
                p -> p.getInventory().add(new ItemStack(Items.TOTEM_OF_UNDYING))));

        // 補給
        items.add(new ShopItem("iron_set", "鉄装備セット", 150, p -> {
            p.getInventory().add(new ItemStack(Items.IRON_HELMET));
            p.getInventory().add(new ItemStack(Items.IRON_CHESTPLATE));
            p.getInventory().add(new ItemStack(Items.IRON_LEGGINGS));
            p.getInventory().add(new ItemStack(Items.IRON_BOOTS));
            p.getInventory().add(new ItemStack(Items.IRON_SWORD));
        }));

        items.add(new ShopItem("arrows_64", "矢（64本）", 50,
                p -> p.getInventory().add(new ItemStack(Items.ARROW, 64))));

        items.add(new ShopItem("steak_32", "ステーキ（32個）", 30,
                p -> p.getInventory().add(new ItemStack(Items.COOKED_BEEF, 32))));

        // 強化
        items.add(new ShopItem("diamond_sword", "ダイヤ剣", 500,
                p -> p.getInventory().add(new ItemStack(Items.DIAMOND_SWORD))));

        items.add(new ShopItem("diamond_armor_set", "ダイヤ防具セット", 1800, p -> {
            p.getInventory().add(new ItemStack(Items.DIAMOND_HELMET));
            p.getInventory().add(new ItemStack(Items.DIAMOND_CHESTPLATE));
            p.getInventory().add(new ItemStack(Items.DIAMOND_LEGGINGS));
            p.getInventory().add(new ItemStack(Items.DIAMOND_BOOTS));
        }));

        // 特殊
        items.add(new ShopItem("anvil", "金床", 100,
                p -> p.getInventory().add(new ItemStack(Items.ANVIL))));

        items.add(new ShopItem("ender_pearls_16", "エンダーパール（16個）", 200,
                p -> p.getInventory().add(new ItemStack(Items.ENDER_PEARL, 16))));

        items.add(new ShopItem("speed2_30min", "スピードII（30分）", 200,
                p -> p.addEffect(new MobEffectInstance(MobEffects.SPEED, 36000, 1, false, true))));

        items.add(new ShopItem("invisibility_10min", "不可視（10分）", 500,
                p -> p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12000, 0, false, true))));

        // 状態異常矢
        items.add(new ShopItem("arrow_slowness", "鈍化の矢（8本）", 80,
                p -> p.getInventory().add(new ItemStack(Items.SPECTRAL_ARROW, 8)))); // 簡略化

        items.add(new ShopItem("arrow_weakness", "弱体化の矢（8本）", 80,
                p -> p.getInventory().add(new ItemStack(Items.SPECTRAL_ARROW, 8))));

        items.add(new ShopItem("arrow_poison", "毒の矢（8本）", 100,
                p -> p.getInventory().add(new ItemStack(Items.SPECTRAL_ARROW, 8))));

        items.add(new ShopItem("arrow_harming", "即ダメの矢（8本）", 150,
                p -> p.getInventory().add(new ItemStack(Items.SPECTRAL_ARROW, 8))));

        // ポーション
        items.add(new ShopItem("splash_slowness", "鈍化スプラッシュ", 120,
                p -> p.getInventory().add(new ItemStack(Items.SPLASH_POTION))));

        items.add(new ShopItem("splash_weakness", "弱体化スプラッシュ", 120,
                p -> p.getInventory().add(new ItemStack(Items.SPLASH_POTION))));

        items.add(new ShopItem("splash_poison", "毒スプラッシュ", 150,
                p -> p.getInventory().add(new ItemStack(Items.SPLASH_POTION))));

        items.add(new ShopItem("splash_harming", "即ダメスプラッシュ", 200,
                p -> p.getInventory().add(new ItemStack(Items.SPLASH_POTION))));

        items.add(new ShopItem("lingering_poison", "毒の残留ポーション", 250,
                p -> p.getInventory().add(new ItemStack(Items.LINGERING_POTION))));

        items.add(new ShopItem("lingering_harming", "即ダメ残留ポーション", 300,
                p -> p.getInventory().add(new ItemStack(Items.LINGERING_POTION))));

        // 強化付与（エンチャント本）
        items.add(new ShopItem("sharpness4", "鋭さIV（エンチャント本）", 600, p -> {
            Holder<Enchantment> ench = p.level().getServer().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
            p.getInventory().add(EnchantmentHelper.createBook(new EnchantmentInstance(ench, 4)));
        }));

        items.add(new ShopItem("protection4", "ダメージ軽減IV（エンチャント本）", 600, p -> {
            Holder<Enchantment> ench = p.level().getServer().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
            p.getInventory().add(EnchantmentHelper.createBook(new EnchantmentInstance(ench, 4)));
        }));

        items.add(new ShopItem("infinity", "無限I（エンチャント本）", 1000, p -> {
            Holder<Enchantment> ench = p.level().getServer().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY);
            p.getInventory().add(EnchantmentHelper.createBook(new EnchantmentInstance(ench, 1)));
        }));

        items.add(new ShopItem("fire_resistance", "火炎耐性", 300,
                p -> p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 36000, 0, false, true))));
    }

    /**
     * 商品を購入する。
     *
     * @param player 購入者
     * @param itemId 商品ID
     * @return 購入成功した場合true
     */
    public boolean purchase(ServerPlayer player, String itemId) {
        Optional<ShopItem> itemOpt = items.stream()
                .filter(i -> i.id().equals(itemId))
                .findFirst();

        if (itemOpt.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c商品が見つかりません: " + itemId));
            return false;
        }

        ShopItem item = itemOpt.get();
        Optional<GameTeam> teamOpt = teamManager.getPlayerTeam(player.getUUID());
        if (teamOpt.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cチームに所属していません"));
            return false;
        }

        GameTeam team = teamOpt.get();
        int price = item.price();

        // 最下位割引適用
        if (team.isUnderdogActive()) {
            price = (int) (price * 0.5);
        }

        if (team.getWalletScore() < price) {
            player.sendSystemMessage(Component.literal(
                    "§cポイントが足りません（必要: " + price + "pt / 所持: " + team.getWalletScore() + "pt）"));
            return false;
        }

        // 購入処理
        team.addWalletScore(-price);
        if (team.isUnderdogActive()) {
            team.useUnderdog();
        }
        item.provider().provide(player);
        player.sendSystemMessage(Component.literal(
                "§a【購入完了】" + item.displayName() + "（-" + price + "pt）"));
        return true;
    }

    /**
     * 商品一覧をプレイヤーに表示する。
     */
    public void showList(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6§l=== ショップ ==="));
        for (ShopItem item : items) {
            player.sendSystemMessage(Component.literal(
                    "§7" + item.id() + " §f" + item.displayName() + " §e" + item.price() + "pt"));
        }
    }

    /**
     * 商品一覧を返す。
     */
    public List<ShopItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
