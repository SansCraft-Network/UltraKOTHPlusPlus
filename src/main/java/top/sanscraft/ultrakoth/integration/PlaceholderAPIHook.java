package top.sanscraft.ultrakoth.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.sanscraft.ultrakoth.UltraKOTHPlusPlus;
import top.sanscraft.ultrakoth.manager.PlayerDataManager;
import top.sanscraft.ultrakoth.model.Koth;
import top.sanscraft.ultrakoth.model.PlayerData;

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
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
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

        PlayerData playerData = playerDataManager.getPlayerData(player.getUniqueId());
        
        // Player-specific placeholders
        switch (params.toLowerCase()) {
            case "player_wins":
                return String.valueOf(playerData.getWins());
            case "player_captures":
                return String.valueOf(playerData.getCaptures());
            case "player_time_spent":
                return formatTime(playerData.getTimeSpent());
            case "player_longest_capture":
                return formatTime(playerData.getLongestCapture());
            case "player_current_streak":
                return String.valueOf(playerData.getCurrentStreak());
            case "player_best_streak":
                return String.valueOf(playerData.getBestStreak());
        }

        // Current KOTH placeholders
        Koth currentKoth = plugin.getKothManager().getCurrentKoth();
        if (currentKoth == null) {
            switch (params.toLowerCase()) {
                case "current_koth":
                case "current_koth_name":
                case "current_koth_world":
                case "current_controller":
                case "current_time_left":
                case "current_capture_time":
                case "current_max_time":
                    return "None";
                case "is_koth_active":
                    return "false";
                default:
                    return null;
            }
        }

        switch (params.toLowerCase()) {
            case "current_koth":
            case "current_koth_name":
                return currentKoth.getName();
            case "current_koth_world":
                return currentKoth.getWorld();
            case "current_controller":
                Player controller = currentKoth.getCurrentController();
                return controller != null ? controller.getName() : "None";
            case "current_time_left":
                return formatTime(currentKoth.getTimeLeft());
            case "current_capture_time":
                return formatTime(currentKoth.getCaptureTime());
            case "current_max_time":
                return formatTime(currentKoth.getMaxCaptureTime());
            case "is_koth_active":
                return String.valueOf(currentKoth.isActive());
            case "current_participants":
                return String.valueOf(currentKoth.getParticipants().size());
        }

        // Global statistics
        switch (params.toLowerCase()) {
            case "total_koths_created":
                return String.valueOf(plugin.getKothManager().getKoths().size());
            case "koths_won_today":
                // This would require tracking daily statistics
                return "0";
            case "active_koths":
                return plugin.getKothManager().getCurrentKoth() != null ? "1" : "0";
        }

        // Top player placeholders (1-10)
        if (params.startsWith("top_wins_")) {
            try {
                int position = Integer.parseInt(params.substring(9));
                String topPlayer = playerDataManager.getTopPlayerByWins(position);
                return topPlayer != null ? topPlayer : "None";
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (params.startsWith("top_wins_count_")) {
            try {
                int position = Integer.parseInt(params.substring(15));
                int wins = playerDataManager.getTopWinsCount(position);
                return String.valueOf(wins);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (params.startsWith("top_captures_")) {
            try {
                int position = Integer.parseInt(params.substring(13));
                String topPlayer = playerDataManager.getTopPlayerByCaptures(position);
                return topPlayer != null ? topPlayer : "None";
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (params.startsWith("top_captures_count_")) {
            try {
                int position = Integer.parseInt(params.substring(19));
                int captures = playerDataManager.getTopCapturesCount(position);
                return String.valueOf(captures);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (params.startsWith("top_time_")) {
            try {
                int position = Integer.parseInt(params.substring(9));
                String topPlayer = playerDataManager.getTopPlayerByTime(position);
                return topPlayer != null ? topPlayer : "None";
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (params.startsWith("top_time_count_")) {
            try {
                int position = Integer.parseInt(params.substring(15));
                long time = playerDataManager.getTopTimeCount(position);
                return formatTime(time);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // KOTH-specific placeholders (ultrakoth_koth_<kothname>_<parameter>)
        if (params.startsWith("koth_")) {
            String[] parts = params.split("_", 3);
            if (parts.length >= 3) {
                String kothName = parts[1];
                String parameter = parts[2];
                
                Koth koth = plugin.getKothManager().getKoth(kothName);
                if (koth != null) {
                    switch (parameter.toLowerCase()) {
                        case "name":
                            return koth.getName();
                        case "world":
                            return koth.getWorld();
                        case "active":
                            return String.valueOf(koth.isActive());
                        case "controller":
                            Player controller = koth.getCurrentController();
                            return controller != null ? controller.getName() : "None";
                        case "timeleft":
                            return formatTime(koth.getTimeLeft());
                        case "capturetime":
                            return formatTime(koth.getCaptureTime());
                        case "maxtime":
                            return formatTime(koth.getMaxCaptureTime());
                        case "participants":
                            return String.valueOf(koth.getParticipants().size());
                    }
                }
            }
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
