package top.sanscraft.ultrakoth.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.sanscraft.ultrakoth.UltraKOTHPlusPlus;
import top.sanscraft.ultrakoth.manager.PlayerDataManager;
import top.sanscraft.ultrakoth.manager.KothManager;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final UltraKOTHPlusPlus plugin;
    private final PlayerDataManager playerDataManager;

    public PlaceholderAPIHook(UltraKOTHPlusPlus plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "ultrakoth";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "SansNom";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        KothManager kothManager = plugin.getKothManager();
        
        // Player-specific placeholders
        switch (params.toLowerCase()) {
            case "wins":
            case "player_wins":
                return String.valueOf(playerDataManager.getWins(player.getUniqueId()));
                
            case "last_win":
            case "player_last_win":
                long lastWin = playerDataManager.getLastWinTime(player.getUniqueId());
                return lastWin > 0 ? formatTime(System.currentTimeMillis() - lastWin) + " ago" : "Never";
                
            case "is_capturing":
                return String.valueOf(kothManager.getCapturingPlayer() != null && 
                    kothManager.getCapturingPlayer().equals(player.getUniqueId()));
        }

        // Global placeholders
        switch (params.toLowerCase()) {
            case "active":
            case "current_koth":
                String activeKoth = kothManager.getActiveKoth();
                return activeKoth != null ? activeKoth : "None";
                
            case "is_active":
                return String.valueOf(kothManager.getActiveKoth() != null);
                
            case "capturing_player":
                if (kothManager.getCapturingPlayer() != null) {
                    Player capturingPlayer = Bukkit.getPlayer(kothManager.getCapturingPlayer());
                    return capturingPlayer != null ? capturingPlayer.getName() : "Unknown";
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
        
        // Top player placeholders using getTopPlayers method
        if (params.startsWith("top_") && params.contains("_name")) {
            try {
                String[] parts = params.split("_");
                if (parts.length >= 2) {
                    int position = Integer.parseInt(parts[1]);
                    java.util.Map<java.util.UUID, Integer> topPlayers = playerDataManager.getTopPlayers(position);
                    if (topPlayers.size() >= position) {
                        java.util.UUID playerUUID = (java.util.UUID) topPlayers.keySet().toArray()[position - 1];
                        Player topPlayer = Bukkit.getPlayer(playerUUID);
                        return topPlayer != null ? topPlayer.getName() : "Unknown";
                    }
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                // Invalid format or position
            }
            return "None";
        }
        
        if (params.startsWith("top_") && params.contains("_wins")) {
            try {
                String[] parts = params.split("_");
                if (parts.length >= 2) {
                    int position = Integer.parseInt(parts[1]);
                    java.util.Map<java.util.UUID, Integer> topPlayers = playerDataManager.getTopPlayers(position);
                    if (topPlayers.size() >= position) {
                        java.util.UUID playerUUID = (java.util.UUID) topPlayers.keySet().toArray()[position - 1];
                        return String.valueOf(topPlayers.get(playerUUID));
                    }
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                // Invalid format or position
            }
            return "0";
        }
        
        return null; // Placeholder is unknown by the expansion
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return "0s";
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}
