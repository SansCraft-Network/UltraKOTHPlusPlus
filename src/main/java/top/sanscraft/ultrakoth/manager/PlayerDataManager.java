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

    public PlayerDataManager(Plugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        load();
    }

    public void addWin(UUID uuid) {
        kothWins.put(uuid, getWins(uuid) + 1);
        save();
    }

    public int getWins(UUID uuid) {
        return kothWins.getOrDefault(uuid, 0);
    }

    public void load() {
        if (dataConfig.isConfigurationSection("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                kothWins.put(UUID.fromString(key), dataConfig.getInt("players." + key));
            }
        }
    }

    public void save() {
        for (Map.Entry<UUID, Integer> entry : kothWins.entrySet()) {
            dataConfig.set("players." + entry.getKey().toString(), entry.getValue());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
