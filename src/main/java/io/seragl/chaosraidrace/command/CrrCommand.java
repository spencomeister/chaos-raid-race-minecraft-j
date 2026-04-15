package io.seragl.chaosraidrace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.seragl.chaosraidrace.chaos.ChaosRule;
import io.seragl.chaosraidrace.game.GameManager;
import io.seragl.chaosraidrace.game.GamePhase;
import io.seragl.chaosraidrace.mob.MobSpawnManager;
import io.seragl.chaosraidrace.team.GameTeam;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.text.NumberFormat;
import java.util.List;

/**
 * /crr コマンドのルート。全サブコマンドを登録する。
 */
public final class CrrCommand {

    private CrrCommand() {}

    /**
     * /crr コマンドを登録する。
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, GameManager gm) {
        dispatcher.register(Commands.literal("crr")
                .requires(s -> s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))

                // /crr start
                .then(Commands.literal("start")
                        .executes(ctx -> {
                            gm.startGame(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("§aゲームを開始しました"), true);
                            return 1;
                        }))

                // /crr pause
                .then(Commands.literal("pause")
                        .executes(ctx -> {
                            gm.pauseGame();
                            ctx.getSource().sendSuccess(() -> Component.literal("§eゲームを一時停止しました"), true);
                            return 1;
                        }))

                // /crr resume
                .then(Commands.literal("resume")
                        .executes(ctx -> {
                            gm.resumeGame();
                            ctx.getSource().sendSuccess(() -> Component.literal("§aゲームを再開しました"), true);
                            return 1;
                        }))

                // /crr reset
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            gm.resetGame(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("§cゲームをリセットしました"), true);
                            return 1;
                        }))

                // /crr status
                .then(Commands.literal("status")
                        .executes(ctx -> {
                            showStatus(ctx.getSource(), gm);
                            return 1;
                        }))

                // /crr team add <player> <team>
                .then(Commands.literal("team")
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("team", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                                    String teamId = StringArgumentType.getString(ctx, "team");
                                                    boolean success = gm.getTeamManager().addPlayer(player.getUUID(), teamId);
                                                    if (success) {
                                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                                "§a" + player.getName().getString() + " を " + teamId + " に追加しました"), true);
                                                    } else {
                                                        ctx.getSource().sendFailure(Component.literal("§cチームが見つかりません: " + teamId));
                                                    }
                                                    return success ? 1 : 0;
                                                }))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                            gm.getTeamManager().removePlayer(player.getUUID());
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "§a" + player.getName().getString() + " をチームから外しました"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("auto")
                                .executes(ctx -> {
                                    gm.getTeamManager().autoAssign(ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a自動振り分けを実行しました"), true);
                                    return 1;
                                }))
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    showTeamList(ctx.getSource(), gm);
                                    return 1;
                                }))
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    gm.getTeamManager().resetAll();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§c全編成をリセットしました"), true);
                                    return 1;
                                })))

                // /crr score <team> <amount>
                .then(Commands.literal("score")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            String teamId = StringArgumentType.getString(ctx, "team");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            gm.getTeamManager().getTeam(teamId).ifPresentOrElse(
                                                    team -> {
                                                        gm.getScoreManager().addScoreRaw(team, amount, 0);
                                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                                "§a" + teamId + " に累計" + amount + "ptを加算しました"), true);
                                                    },
                                                    () -> ctx.getSource().sendFailure(Component.literal("§cチームが見つかりません"))
                                            );
                                            return 1;
                                        }))))

                // /crr phase <phase>
                .then(Commands.literal("phase")
                        .then(Commands.argument("phase", StringArgumentType.word())
                                .executes(ctx -> {
                                    String phaseName = StringArgumentType.getString(ctx, "phase");
                                    return forcePhase(ctx.getSource(), gm, phaseName);
                                })))

                // /crr glitch <on/off>
                .then(Commands.literal("glitch")
                        .then(Commands.argument("toggle", StringArgumentType.word())
                                .executes(ctx -> {
                                    String toggle = StringArgumentType.getString(ctx, "toggle");
                                    gm.getScoreboardDisplay().setGlitchMode(toggle.equals("on"));
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§eスコアバグ演出: " + toggle), true);
                                    return 1;
                                })))

                // /crr rule <ruleId>
                .then(Commands.literal("rule")
                        .then(Commands.literal("skip")
                                .executes(ctx -> {
                                    gm.getChaosRuleManager().skipCurrentRule(ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal("§eルールをスキップしました"), true);
                                    return 1;
                                }))
                        .then(Commands.argument("ruleId", StringArgumentType.word())
                                .executes(ctx -> {
                                    String ruleId = StringArgumentType.getString(ctx, "ruleId");
                                    gm.getChaosRuleManager().forceActivateRule(ctx.getSource().getServer(), ruleId);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§eルールを強制発動: " + ruleId), true);
                                    return 1;
                                })))

                // /crr boss spawn <回>
                .then(Commands.literal("boss")
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("index", IntegerArgumentType.integer(1, 3))
                                        .executes(ctx -> {
                                            int index = IntegerArgumentType.getInteger(ctx, "index") - 1;
                                            gm.getBossManager().spawnBoss(ctx.getSource().getServer(), index);
                                            return 1;
                                        })))
                        .then(Commands.literal("kill")
                                .executes(ctx -> {
                                    gm.getBossManager().killCurrentBoss();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§cボスを消去しました"), true);
                                    return 1;
                                })))

                // /crr dragon spawn
                .then(Commands.literal("dragon")
                        .then(Commands.literal("spawn")
                                .executes(ctx -> {
                                    gm.getFinalPhaseManager().spawnDragon(ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal("§5エンドラを召喚しました"), true);
                                    return 1;
                                })))

                // /crr mob spawn-rate <on/off>
                .then(Commands.literal("mob")
                        .then(Commands.literal("spawn-rate")
                                .then(Commands.argument("toggle", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String toggle = StringArgumentType.getString(ctx, "toggle");
                                            if (toggle.equals("on")) gm.getMobSpawnManager().enable();
                                            else gm.getMobSpawnManager().disable();
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "§eMobスポーン: " + toggle), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("list")
                                .then(Commands.argument("category", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String cat = StringArgumentType.getString(ctx, "category");
                                            showMobList(ctx.getSource(), gm, cat);
                                            return 1;
                                        }))))

                // /crr shop
                .then(Commands.literal("shop")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                gm.getShopManager().showList(player);
                            }
                            return 1;
                        }))

                // /crr respawn <player>
                .then(Commands.literal("respawn")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                    player.teleportTo(0, 80, 0);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§a" + player.getName().getString() + " をリスポーンさせました"), true);
                                    return 1;
                                })))
        );
    }

    /**
     * /crr status の表示。
     */
    private static void showStatus(CommandSourceStack source, GameManager gm) {
        GamePhase phase = gm.getTimer().getCurrentPhase();
        StringBuilder sb = new StringBuilder();
        sb.append("§6=== カオス・レイド・レース ステータス ===\n");
        sb.append("§7経過時間   : §f").append(gm.getTimer().formatTime())
                .append(" / ").append(String.format("%02d:00", 120)).append("\n");
        sb.append("§7現フェーズ : §f").append(phase.getDisplayName()).append("\n");

        // カオスルール
        List<ChaosRule> rules = gm.getChaosRuleManager().getActiveRules();
        if (!rules.isEmpty()) {
            sb.append("§7現ルール   : §f");
            for (int i = 0; i < rules.size(); i++) {
                if (i > 0) sb.append(" + ");
                sb.append(rules.get(i).getName());
            }
            sb.append("\n");
        }

        sb.append("§7-----------------------------------------\n");

        // チームスコア
        for (GameTeam team : gm.getTeamManager().getTeamsRanked()) {
            sb.append(team.getColor()).append(team.getDisplayName())
                    .append("§f: 累計 ").append(NumberFormat.getInstance().format(team.getTotalScore())).append("pt")
                    .append(" / 財布 ").append(NumberFormat.getInstance().format(team.getWalletScore())).append("pt\n");
        }

        sb.append("§7=========================================");
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
    }

    /**
     * チーム編成一覧を表示する。
     */
    private static void showTeamList(CommandSourceStack source, GameManager gm) {
        StringBuilder sb = new StringBuilder("§6=== チーム編成 ===\n");
        for (GameTeam team : gm.getTeamManager().getAllTeams()) {
            sb.append(team.getColor()).append(team.getDisplayName()).append("§f: ");
            if (team.getMembers().isEmpty()) {
                sb.append("(なし)");
            } else {
                List<String> names = team.getMembers().stream()
                        .map(uuid -> {
                            ServerPlayer p = source.getServer().getPlayerList().getPlayer(uuid);
                            return p != null ? p.getName().getString() : uuid.toString().substring(0, 8);
                        })
                        .toList();
                sb.append(String.join(", ", names));
            }
            sb.append("\n");
        }
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
    }

    /**
     * フェーズを強制移行する。
     */
    private static int forcePhase(CommandSourceStack source, GameManager gm, String phaseName) {
        int targetMinute = switch (phaseName.toLowerCase()) {
            case "prep", "preparation" -> 0;
            case "early" -> 5;
            case "middle" -> 40;
            case "late" -> 80;
            case "final" -> 110;
            default -> -1;
        };

        if (targetMinute < 0) {
            source.sendFailure(Component.literal("§c不明なフェーズ: " + phaseName));
            return 0;
        }

        gm.getTimer().setElapsedTicks(targetMinute * 60 * 20);
        source.sendSuccess(() -> Component.literal("§eフェーズを " + phaseName + " に移行しました"), true);
        return 1;
    }

    /**
     * Mob一覧を表示する。
     */
    private static void showMobList(CommandSourceStack source, GameManager gm, String categoryName) {
        try {
            MobSpawnManager.MobCategory category = MobSpawnManager.MobCategory.valueOf(categoryName.toUpperCase());
            List<MobSpawnManager.MobDef> mobs = gm.getMobSpawnManager().getMobsByCategory(category);
            StringBuilder sb = new StringBuilder("§6=== Mob一覧 (" + categoryName + ") ===\n");
            for (MobSpawnManager.MobDef mob : mobs) {
                sb.append("§7").append(mob.name())
                        .append(" §f累計:").append(mob.totalPt())
                        .append(" 消費:").append(mob.walletPt()).append("\n");
            }
            source.sendSuccess(() -> Component.literal(sb.toString()), false);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("§c不明なカテゴリ: " + categoryName));
        }
    }
}
