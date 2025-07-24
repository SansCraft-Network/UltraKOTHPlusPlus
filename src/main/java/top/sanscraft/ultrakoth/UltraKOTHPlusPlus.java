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
    private long nextKothTime = 0; // Timestamp of next scheduled KOTH

    @Override
    public void onEnable() {
        getLogger().info("Starting UltraKOTHPlusPlus initialization...");
        
        try {
            instance = this;
            getLogger().info("Plugin instance set successfully");
            
            getLogger().info("Loading default configuration...");
            saveDefaultConfig();
            getLogger().info("Configuration loaded successfully");
            
            // Initialize managers
            getLogger().info("Initializing PlayerDataManager...");
            playerDataManager = new PlayerDataManager(this);
            getLogger().info("PlayerDataManager initialized successfully");
            
            getLogger().info("Initializing KothManager...");
            kothManager = new KothManager(this);
            getLogger().info("KothManager initialized successfully");
            
            getLogger().info("Initializing WorldGuardHook...");
            worldGuardHook = new WorldGuardHook();
            getLogger().info("WorldGuardHook initialized successfully (WorldGuard " + 
                (worldGuardHook.isEnabled() ? "detected" : "not detected") + ")");
            
            // BossBarManager will be created per KOTH event
            
            // Register command
            getLogger().info("Registering KOTH command...");
            if (getCommand("koth") != null) {
                KothCommand kothCommand = new KothCommand(this);
                getCommand("koth").setExecutor(kothCommand);
                getCommand("koth").setTabCompleter(kothCommand);
                getLogger().info("KOTH command registered successfully");
            } else {
                getLogger().severe("Failed to register KOTH command - command not found in plugin.yml!");
            }
            
            // Register PlaceholderAPI if present
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                getLogger().info("PlaceholderAPI detected, registering hook...");
                boolean registered = new PlaceholderAPIHook(this, playerDataManager).register();
                if (registered) {
                    getLogger().info("PlaceholderAPI hook registered successfully");
                } else {
                    getLogger().warning("Failed to register PlaceholderAPI hook");
                }
            } else {
                getLogger().info("PlaceholderAPI not detected - placeholders will not be available");
            }
            
            // Schedule KOTH events if enabled
            boolean scheduleEnabled = getConfig().getBoolean("koth.schedule.enabled", false);
            getLogger().info("KOTH scheduling " + (scheduleEnabled ? "enabled" : "disabled"));
            
            if (scheduleEnabled) {
                int intervalMinutes = getConfig().getInt("koth.schedule.interval-minutes", 60);
                int interval = intervalMinutes * 60 * 20; // Convert to ticks
                getLogger().info("Scheduling KOTH events every " + intervalMinutes + " minutes (" + interval + " ticks)");
                
                // Set the next KOTH time (current time + interval in milliseconds)
                nextKothTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000);
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        getLogger().info("Scheduled KOTH event trigger - attempting to start random KOTH");
                        kothManager.startRandomKoth();
                        
                        // Update next KOTH time for the following event
                        int intervalMinutes = getConfig().getInt("koth.schedule.interval-minutes", 60);
                        nextKothTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000);
                    }
                }.runTaskTimer(this, interval, interval);
                getLogger().info("KOTH scheduler started successfully");
            }
            
            getLogger().info("UltraKOTHPlusPlus enabled successfully!");
            
        } catch (Exception e) {
            getLogger().severe("Critical error during plugin initialization:");
            getLogger().severe("Error: " + e.getMessage());
            e.printStackTrace();
            getLogger().severe("Plugin may not function correctly!");
        }
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.save();
        getLogger().info("UltraKOTHPlusPlus disabled!");
    }

    public static UltraKOTHPlusPlus getInstance() {
        return instance;
    }
    
    public long getNextKothTime() {
        return nextKothTime;
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
