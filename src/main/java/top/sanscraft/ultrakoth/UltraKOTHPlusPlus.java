package top.sanscraft.ultrakoth;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import top.sanscraft.ultrakoth.command.KothCommand;
import top.sanscraft.ultrakoth.manager.BossBarManager;
import top.sanscraft.ultrakoth.manager.KothManager;
import top.sanscraft.ultrakoth.manager.PlayerDataManager;
import top.sanscraft.ultrakoth.integration.WorldGuardHook;
import top.sanscraft.ultrakoth.integration.PlaceholderAPIHook;

public class UltraKOTHPlusPlus extends JavaPlugin {
    private static UltraKOTHPlusPlus instance;
    private KothManager kothManager;
    private PlayerDataManager playerDataManager;
    private BossBarManager bossBarManager;
    private WorldGuardHook worldGuardHook;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        // Initialize managers
        playerDataManager = new PlayerDataManager(this);
        kothManager = new KothManager(this);
        worldGuardHook = new WorldGuardHook();
        // BossBarManager will be created per KOTH event
        // Register command
        getCommand("koth").setExecutor(new KothCommand(this));
        // Register PlaceholderAPI if present
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(this, playerDataManager).register();
        }
        // Schedule KOTH events if enabled
        if (getConfig().getBoolean("koth.schedule.enabled", false)) {
            int interval = getConfig().getInt("koth.schedule.interval-minutes", 60) * 60 * 20;
            new BukkitRunnable() {
                @Override
                public void run() {
                    kothManager.startRandomKoth();
                }
            }.runTaskTimer(this, interval, interval);
        }
        getLogger().info("UltraKOTHPlusPlus enabled!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.save();
        getLogger().info("UltraKOTHPlusPlus disabled!");
    }

    public static UltraKOTHPlusPlus getInstance() {
        return instance;
    }

    public KothManager getKothManager() {
        return kothManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public void setBossBarManager(BossBarManager bossBarManager) {
        this.bossBarManager = bossBarManager;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }
}
