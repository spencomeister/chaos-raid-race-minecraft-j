package io.seragl.chaosraidrace.team;

import io.seragl.chaosraidrace.ChaosRaidRace;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/**
 * チームの作成・管理を行うマネージャー。
 */
public final class TeamManager {

    private final Map<String, GameTeam> teams = new LinkedHashMap<>();
    private final Map<UUID, String> playerTeamMap = new HashMap<>();

    /**
     * デフォルトの3チームを初期化する。
     */
    public void initialize() {
        teams.clear();
        playerTeamMap.clear();
        teams.put("teamA", new GameTeam("teamA", "チームA", ChatFormatting.RED));
        teams.put("teamB", new GameTeam("teamB", "チームB", ChatFormatting.BLUE));
        teams.put("teamC", new GameTeam("teamC", "チームC", ChatFormatting.GREEN));
        ChaosRaidRace.LOGGER.info("チームを初期化しました（3チーム）");
    }

    /**
     * プレイヤーをチームに追加する。
     *
     * @param uuid   プレイヤーUUID
     * @param teamId チームID
     * @return 成功した場合true
     */
    public boolean addPlayer(UUID uuid, String teamId) {
        GameTeam team = teams.get(teamId);
        if (team == null) return false;

        // 既存チームから削除
        removePlayer(uuid);

        team.addMember(uuid);
        playerTeamMap.put(uuid, teamId);
        return true;
    }

    /**
     * プレイヤーをチームから削除する。
     */
    public void removePlayer(UUID uuid) {
        String oldTeamId = playerTeamMap.remove(uuid);
        if (oldTeamId != null) {
            GameTeam oldTeam = teams.get(oldTeamId);
            if (oldTeam != null) oldTeam.removeMember(uuid);
        }
    }

    /**
     * プレイヤーが所属するチームを取得する。
     */
    public Optional<GameTeam> getPlayerTeam(UUID uuid) {
        String teamId = playerTeamMap.get(uuid);
        if (teamId == null) return Optional.empty();
        return Optional.ofNullable(teams.get(teamId));
    }

    /**
     * チームIDでチームを取得する。
     */
    public Optional<GameTeam> getTeam(String teamId) {
        return Optional.ofNullable(teams.get(teamId));
    }

    /**
     * 全チームを取得する。
     */
    public Collection<GameTeam> getAllTeams() {
        return Collections.unmodifiableCollection(teams.values());
    }

    /**
     * プレイヤーを自動均等振り分けする。
     */
    public void autoAssign(MinecraftServer server) {
        List<ServerPlayer> unassigned = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!playerTeamMap.containsKey(player.getUUID())) {
                unassigned.add(player);
            }
        }

        Collections.shuffle(unassigned);
        List<GameTeam> teamList = new ArrayList<>(teams.values());
        int idx = 0;
        for (ServerPlayer player : unassigned) {
            // 最も人数の少ないチームに追加
            GameTeam smallest = teamList.stream()
                    .min(Comparator.comparingInt(t -> t.getMembers().size()))
                    .orElse(teamList.get(idx % teamList.size()));
            addPlayer(player.getUUID(), smallest.getId());
            idx++;
        }
        ChaosRaidRace.LOGGER.info("プレイヤーを自動振り分けしました（{}人）", unassigned.size());
    }

    /**
     * 全チーム編成をリセットする。
     */
    public void resetAll() {
        playerTeamMap.clear();
        teams.values().forEach(team -> {
            team.getMembers().forEach(uuid -> {}); // iterate to clear
            team.resetScores();
        });
        initialize();
    }

    /**
     * 最下位チームを特定する。
     */
    public Optional<GameTeam> getLowestScoreTeam() {
        return teams.values().stream()
                .min(Comparator.comparingInt(GameTeam::getTotalScore));
    }

    /**
     * チームをスコアの降順でソートして返す。
     */
    public List<GameTeam> getTeamsRanked() {
        List<GameTeam> ranked = new ArrayList<>(teams.values());
        ranked.sort(Comparator.comparingInt(GameTeam::getTotalScore).reversed());
        return ranked;
    }
}
