package io.seragl.chaosraidrace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.seragl.chaosraidrace.game.GameManager;
import io.seragl.chaosraidrace.shop.ShopManager;
import io.seragl.chaosraidrace.team.GameTeam;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.text.NumberFormat;
import java.util.Optional;

/**
 * /vault コマンドと /shop コマンドを登録する。
 */
public final class VaultCommand {

    private VaultCommand() {}

    /**
     * /vault コマンドを登録する。
     */
    public static void registerVault(CommandDispatcher<CommandSourceStack> dispatcher, GameManager gm) {
        dispatcher.register(Commands.literal("vault")
                // /vault deposit <pt>
                .then(Commands.literal("deposit")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        ctx.getSource().sendFailure(Component.literal("§cプレイヤーのみ実行可能です"));
                                        return 0;
                                    }
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    int deposited = gm.getScoreManager().deposit(player.getUUID(), amount);
                                    if (deposited > 0) {
                                        player.sendSystemMessage(Component.literal(
                                                "§6[金庫] " + deposited + "pt を預け入れました"));
                                    } else {
                                        player.sendSystemMessage(Component.literal(
                                                "§c預け入れに失敗しました（残高不足またはチーム未所属）"));
                                    }
                                    return deposited > 0 ? 1 : 0;
                                })))

                // /vault withdraw <pt>
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        ctx.getSource().sendFailure(Component.literal("§cプレイヤーのみ実行可能です"));
                                        return 0;
                                    }
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    int withdrawn = gm.getScoreManager().withdraw(player.getUUID(), amount);
                                    if (withdrawn > 0) {
                                        player.sendSystemMessage(Component.literal(
                                                "§6[金庫] " + withdrawn + "pt を引き出しました"));
                                    } else {
                                        player.sendSystemMessage(Component.literal(
                                                "§c引き出しに失敗しました（残高不足またはチーム未所属）"));
                                    }
                                    return withdrawn > 0 ? 1 : 0;
                                })))

                // /vault balance
                .then(Commands.literal("balance")
                        .executes(ctx -> {
                            if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                ctx.getSource().sendFailure(Component.literal("§cプレイヤーのみ実行可能です"));
                                return 0;
                            }
                            Optional<GameTeam> teamOpt = gm.getTeamManager().getPlayerTeam(player.getUUID());
                            teamOpt.ifPresentOrElse(
                                    team -> {
                                        player.sendSystemMessage(Component.literal(
                                                "§6[金庫] " + team.getDisplayName() + " 共有金庫\n"
                                                        + "§7預け入れ残高 : §f" + NumberFormat.getInstance().format(team.getVaultScore()) + " pt\n"
                                                        + "§7財布残高     : §f" + NumberFormat.getInstance().format(team.getWalletScore()) + " pt"));
                                    },
                                    () -> player.sendSystemMessage(Component.literal("§cチームに所属していません"))
                            );
                            return 1;
                        }))

                // /vault log（簡易版）
                .then(Commands.literal("log")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§7[金庫ログ] 履歴機能は現在準備中です"), false);
                            return 1;
                        }))
        );
    }

    /**
     * /shop コマンドを登録する。
     */
    public static void registerShop(CommandDispatcher<CommandSourceStack> dispatcher, GameManager gm) {
        dispatcher.register(Commands.literal("shop")
                // /shop list
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                gm.getShopManager().showList(player);
                            }
                            return 1;
                        }))

                // /shop buy <itemId>
                .then(Commands.literal("buy")
                        .then(Commands.argument("item", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        ctx.getSource().sendFailure(Component.literal("§cプレイヤーのみ実行可能です"));
                                        return 0;
                                    }
                                    String itemId = StringArgumentType.getString(ctx, "item");

                                    // アクセス制限チェック（拠点内 or リスポーン直後）
                                    if (!gm.getBaseProtection().isPlayerInOwnBase(player)
                                            && !player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
                                        player.sendSystemMessage(Component.literal(
                                                "§cショップは自チームの拠点内でのみ利用可能です"));
                                        return 0;
                                    }

                                    boolean success = gm.getShopManager().purchase(player, itemId);
                                    return success ? 1 : 0;
                                })))
        );
    }
}
