package io.seragl.chaosraidrace.mission;

import io.seragl.chaosraidrace.ChaosRaidRace;
import io.seragl.chaosraidrace.chaos.ChaosRule;
import io.seragl.chaosraidrace.score.ScoreManager;
import io.seragl.chaosraidrace.team.GameTeam;
import io.seragl.chaosraidrace.team.TeamManager;
import io.seragl.chaosraidrace.ui.AnnouncementManager;
import net.minecraft.server.MinecraftServer;

import java.util.*;

/**
 * カオスルール連動ミッション（ルール・コレクター）を管理するクラス。
 */
public final class MissionManager {

    /**
     * ミッション定義。
     */
    public record Mission(String ruleId, String description, int rewardTotal, int rewardWallet) {}

    private static final List<Mission> MISSIONS = List.of(
            new Mission("stop_and_die", "チーム全員が一度も爆発を受けずに生存", 200, 0),
            new Mission("view_lock", "この5分間にMobを10体討伐", 0, 300),
            new Mission("pacifist", "チーム全員がデスなし", 300, 0),
            new Mission("arrow_rain", "屋内で敵を5体倒す", 0, 200),
            new Mission("low_gravity", "空中でMobを3体倒す", 150, 0),
            new Mission("chain_break", "石を合計200個採掘", 0, 150),
            new Mission("glowing", "発光状態のMobを15体倒す", 250, 0),
            new Mission("alchemy", "レアドロップを3個獲得", 0, 500),
            new Mission("happy_mob", "中堅以上のMobを5体討伐", 400, 0),
            new Mission("normal", "5分間で一番多くのMobを倒したチームが勝利", 100, 0)
    );

    private final TeamManager teamManager;
    private final ScoreManager scoreManager;
    private final AnnouncementManager announcements;

    // 現在のアクティブミッション
    private final List<Mission> activeMissions = new ArrayList<>();
    // チーム別の進捗追跡
    private final Map<String, Map<String, Integer>> teamProgress = new HashMap<>();
    // 達成済みミッション
    private final Set<String> completedMissions = new HashSet<>();

    public MissionManager(TeamManager teamManager, ScoreManager scoreManager, AnnouncementManager announcements) {
        this.teamManager = teamManager;
        this.scoreManager = scoreManager;
        this.announcements = announcements;
    }

    /**
     * ルールが変更された時にミッションを設定する。
     */
    public void onRuleChange(List<ChaosRule> newRules, MinecraftServer server) {
        activeMissions.clear();
        teamProgress.clear();
        completedMissions.clear();

        for (ChaosRule rule : newRules) {
            MISSIONS.stream()
                    .filter(m -> m.ruleId.equals(rule.getId()))
                    .findFirst()
                    .ifPresent(mission -> {
                        activeMissions.add(mission);
                        announcements.broadcast(server,
                                "§b[ミッション] " + mission.description);
                    });
        }

        // チーム別の進捗を初期化
        for (GameTeam team : teamManager.getAllTeams()) {
            teamProgress.put(team.getId(), new HashMap<>());
        }
    }

    /**
     * ミッションの進捗を加算する。
     */
    public void addProgress(String teamId, String missionRuleId, int amount) {
        Map<String, Integer> progress = teamProgress.get(teamId);
        if (progress != null) {
            progress.merge(missionRuleId, amount, Integer::sum);
        }
    }

    /**
     * ミッション達成チェック（毎ティック or イベント時に呼ぶ）。
     */
    public void checkCompletion(MinecraftServer server) {
        for (Mission mission : activeMissions) {
            for (GameTeam team : teamManager.getAllTeams()) {
                String key = team.getId() + ":" + mission.ruleId;
                if (completedMissions.contains(key)) continue;

                Map<String, Integer> progress = teamProgress.get(team.getId());
                if (progress == null) continue;

                int current = progress.getOrDefault(mission.ruleId, 0);
                int target = getMissionTarget(mission.ruleId);

                if (current >= target) {
                    completedMissions.add(key);
                    team.addMissionFragment();
                    scoreManager.addScoreRaw(team, mission.rewardTotal, mission.rewardWallet);
                    announcements.broadcast(server,
                            "§a[ミッション達成！] " + team.getDisplayName() + ": " + mission.description + " クリア！(欠片+1)");
                    ChaosRaidRace.LOGGER.info("ミッション達成: {} - {}", team.getDisplayName(), mission.description);
                }
            }
        }
    }

    /**
     * ミッションの目標値を返す。
     */
    private int getMissionTarget(String ruleId) {
        return switch (ruleId) {
            case "stop_and_die" -> 1; // 全員生存（特殊チェック）
            case "view_lock" -> 10;
            case "pacifist" -> 1; // 全員デスなし（特殊チェック）
            case "arrow_rain" -> 5;
            case "low_gravity" -> 3;
            case "chain_break" -> 200;
            case "glowing" -> 15;
            case "alchemy" -> 3;
            case "happy_mob" -> 5;
            case "normal" -> 1; // 最多討伐（特殊チェック）
            default -> Integer.MAX_VALUE;
        };
    }
}
