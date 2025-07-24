package top.sanscraft.ultrakoth.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private final Plugin plugin;
    private final File dataFile;
    private final YamlConfiguration dataConfig;
    private final Map<UUID, Integer> kothWins = new HashMap<>();
    private final Map<UUID, Long> lastWinTime = new HashMap<>();

    public PlayerDataManager(Plugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        
        // Ensure the plugin data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Create data file if it doesn't exist
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                plugin.getLogger().info("Created new data.yml file");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml file: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        load();
    }

    /**
     * Add a win for a player
     * @param uuid Player's UUID
     */
    public void addWin(UUID uuid) {
        int currentWins = getWins(uuid);
        kothWins.put(uuid, currentWins + 1);
        lastWinTime.put(uuid, System.currentTimeMillis());
        save();
        plugin.getLogger().info("Added KOTH win for player " + uuid + " (total: " + (currentWins + 1) + ")");
    }

    /**
     * Get the number of wins for a player
     * @param uuid Player's UUID
     * @return Number of wins
     */
    public int getWins(UUID uuid) {
        return kothWins.getOrDefault(uuid, 0);
    }

    /**
     * Get the last win time for a player
     * @param uuid Player's UUID
     * @return Timestamp of last win, or 0 if never won
     */
    public long getLastWinTime(UUID uuid) {
        return lastWinTime.getOrDefault(uuid, 0L);
    }

    /**
     * Set wins for a player (admin command)
     * @param uuid Player's UUID
     * @param wins Number of wins to set
     */
    public void setWins(UUID uuid, int wins) {
        if (wins < 0) wins = 0;
        kothWins.put(uuid, wins);
        save();
        plugin.getLogger().info("Set KOTH wins for player " + uuid + " to " + wins);
    }

    /**
     * Get top players by wins
     * @param limit Maximum number of players to return
     * @return Map of UUID to wins, sorted by wins descending
     */
    public Map<UUID, Integer> getTopPlayers(int limit) {
        return kothWins.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(HashMap::new, 
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        HashMap::putAll);
    }

    /**
     * Load player data from file
     */
    public void load() {
        try {
            kothWins.clear();
            lastWinTime.clear();
            
            if (dataConfig.isConfigurationSection("players")) {
                for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        int wins = dataConfig.getInt("players." + key + ".wins", 0);
                        long lastWin = dataConfig.getLong("players." + key + ".last-win", 0L);
                        
                        kothWins.put(uuid, wins);
                        if (lastWin > 0) {
                            lastWinTime.put(uuid, lastWin);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in data file: " + key);
                    }
                }
            }
            
            plugin.getLogger().info("Loaded data for " + kothWins.size() + " players");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading player data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save player data to file
     */
    public void save() {
        try {
            // Clear existing data
            dataConfig.set("players", null);
            
            // Save current data
            for (Map.Entry<UUID, Integer> entry : kothWins.entrySet()) {
                String uuidString = entry.getKey().toString();
                dataConfig.set("players." + uuidString + ".wins", entry.getValue());
                
                Long lastWin = lastWinTime.get(entry.getKey());
                if (lastWin != null && lastWin > 0) {
                    dataConfig.set("players." + uuidString + ".last-win", lastWin);
                }
            }
            
            dataConfig.save(dataFile);
            
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get total number of players with wins
     * @return Number of players
     */
    public int getTotalPlayers() {
        return kothWins.size();
    }

    /**
     * Get total wins across all players
     * @return Total wins
     */
    public int getTotalWins() {
        return kothWins.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Reset all player data
     */
    public void reset() {
        kothWins.clear();
        lastWinTime.clear();
        save();
        plugin.getLogger().info("Reset all player data");
    }
}
