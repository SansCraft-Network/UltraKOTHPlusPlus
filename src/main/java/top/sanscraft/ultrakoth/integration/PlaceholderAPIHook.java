package top.sanscraft.ultrakoth.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.sanscraft.ultrakoth.UltraKOTHPlusPlus;
import top.sanscraft.ultrakoth.manager.PlayerDataManager;
import top.sanscraft.ultrakoth.manager.KothManager;

import java.util.UUID;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final UltraKOTHPlusPlus plugin;
    private final PlayerDataManager playerDataManager;

    public PlaceholderAPIHook(UltraKOTHPlusPlus plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @Override
    public boolean register() {
        return super.register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ultrakoth";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SansNom";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        KothManager kothManager = plugin.getKothManager();
        
        // Player-specific placeholders
        if (player != null) {
            switch (identifier.toLowerCase()) {
                case "wins":
                case "player_wins":
                    return String.valueOf(playerDataManager.getWins(player.getUniqueId()));
                    
                case "last_win":
                case "player_last_win":
                    long lastWin = playerDataManager.getLastWinTime(player.getUniqueId());
                    return lastWin > 0 ? String.valueOf(lastWin) : "Never";
                    
                case "is_capturing":
                    UUID capturingPlayer = kothManager.getCapturingPlayer();
                    return String.valueOf(capturingPlayer != null && capturingPlayer.equals(player.getUniqueId()));
                    
                case "rank":
                case "player_rank":
                    return String.valueOf(getPlayerRank(player.getUniqueId()));
            }
        }
        
        // Global placeholders
        switch (identifier.toLowerCase()) {
            case "active":
            case "current_koth":
                String activeKoth = kothManager.getActiveKoth();
                return activeKoth != null ? activeKoth : "None";
                
            case "is_active":
                return String.valueOf(kothManager.getActiveKoth() != null);
                
            case "capturing_player":
                UUID capturingPlayer = kothManager.getCapturingPlayer();
                if (capturingPlayer != null) {
                    Player capturingPlayerObj = plugin.getServer().getPlayer(capturingPlayer);
                    return capturingPlayerObj != null ? capturingPlayerObj.getName() : "Unknown";
                }
                return "None";
                
            case "progress":
            case "capture_progress":
                return String.valueOf(kothManager.getCaptureProgress());
                
            case "progress_percentage":
                int progress = kothManager.getCaptureProgress();
                int total = kothManager.getCaptureTime();
                return total > 0 ? String.format("%.1f", ((double) progress / total) * 100) : "0.0";
                
            case "time_left":
            case "remaining_time":
                int remaining = kothManager.getCaptureTime() - kothManager.getCaptureProgress();
                return String.valueOf(Math.max(0, remaining));
                
            case "total_time":
            case "capture_time":
                return String.valueOf(kothManager.getCaptureTime());
                
            case "total_players":
                return String.valueOf(playerDataManager.getTotalPlayers());
                
            case "total_wins":
                return String.valueOf(playerDataManager.getTotalWins());
                
            case "regions_count":
                return String.valueOf(kothManager.getKothRegions().size());
        }
        
        // Top player placeholders (top_1_name, top_1_wins, etc.)
        if (identifier.toLowerCase().startsWith("top_")) {
            String[] parts = identifier.split("_");
            if (parts.length >= 3) {
                try {
                    int position = Integer.parseInt(parts[1]);
                    String type = parts[2].toLowerCase();
                    
                    return getTopPlayerInfo(position, type);
                } catch (NumberFormatException e) {
                    return "";
                }
            }
        }
        
        return null;
    }

    private int getPlayerRank(UUID playerUUID) {
        int playerWins = playerDataManager.getWins(playerUUID);
        int rank = 1;
        
        for (int wins : playerDataManager.getTopPlayers(Integer.MAX_VALUE).values()) {
            if (wins > playerWins) {
                rank++;
            }
        }
        
        return rank;
    }

    private String getTopPlayerInfo(int position, String type) {
        var topPlayers = playerDataManager.getTopPlayers(position);
        
        if (topPlayers.size() < position) {
            return type.equals("name") ? "None" : "0";
        }
        
        UUID playerUUID = (UUID) topPlayers.keySet().toArray()[position - 1];
        
        switch (type) {
            case "name":
                Player player = plugin.getServer().getPlayer(playerUUID);
                return player != null ? player.getName() : "Unknown";
                
            case "wins":
                return String.valueOf(topPlayers.get(playerUUID));
                
            case "uuid":
                return playerUUID.toString();
                
            default:
                return "";
        }
    }
}
