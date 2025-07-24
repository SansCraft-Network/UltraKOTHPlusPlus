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
    private BukkitTask schedulerTask = null; // Track the scheduler task

    @Override
    public void onEnable() {
        getLogger().info("Starting UltraKOTHPlusPlus initialization...");
        
        instance = this;
        getLogger().info("Plugin instance set successfully");
        
        getLogger().info("Loading default configuration...");
        saveDefaultConfig();
        getLogger().info("Configuration loaded successfully");
        
        // Initialize WorldGuardHook first (required by KothManager)
        getLogger().info("Initializing WorldGuardHook...");
        try {
            worldGuardHook = new WorldGuardHook();
            getLogger().info("WorldGuardHook initialized successfully (WorldGuard " + 
                (worldGuardHook.isEnabled() ? "detected" : "not detected") + ")");
        } catch (Exception e) {
            getLogger().warning("Failed to initialize WorldGuardHook: " + e.getMessage());
            getLogger().warning("WorldGuard integration will be disabled");
            worldGuardHook = null;
        }
        
        // Initialize PlayerDataManager with error handling
        getLogger().info("Initializing PlayerDataManager...");
        try {
            playerDataManager = new PlayerDataManager(this);
            getLogger().info("PlayerDataManager initialized successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize PlayerDataManager: " + e.getMessage());
            throw new RuntimeException("PlayerDataManager initialization failed", e);
        }
        
        // Initialize KothManager with error handling (after WorldGuardHook)
        getLogger().info("Initializing KothManager...");
        try {
            kothManager = new KothManager(this);
            getLogger().info("KothManager initialized successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize KothManager: " + e.getMessage());
            throw new RuntimeException("KothManager initialization failed", e);
        }
        
        // Register command with error handling
        getLogger().info("Registering KOTH command...");
        try {
            if (getCommand("koth") != null) {
                KothCommand kothCommand = new KothCommand(this);
                getCommand("koth").setExecutor(kothCommand);
                getCommand("koth").setTabCompleter(kothCommand);
                getLogger().info("KOTH command registered successfully");
            } else {
                getLogger().severe("Failed to register KOTH command - command not found in plugin.yml!");
                throw new RuntimeException("KOTH command not found in plugin.yml");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to register KOTH command: " + e.getMessage());
            throw new RuntimeException("Command registration failed", e);
        }
        
        // Register PlaceholderAPI if present
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("PlaceholderAPI detected, registering hook...");
            try {
                boolean registered = new PlaceholderAPIHook(this, playerDataManager).register();
                if (registered) {
                    getLogger().info("PlaceholderAPI hook registered successfully");
                } else {
                    getLogger().warning("Failed to register PlaceholderAPI hook");
                }
            } catch (Exception e) {
                getLogger().warning("Error registering PlaceholderAPI hook: " + e.getMessage());
                getLogger().warning("PlaceholderAPI integration will be disabled");
            }
        } else {
            getLogger().info("PlaceholderAPI not detected - placeholders will not be available");
        }
        
        // Schedule KOTH events if enabled
        startScheduler();
        
        getLogger().info("UltraKOTHPlusPlus enabled successfully!");
    }

    @Override
    public void onDisable() {
        stopScheduler();
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

    // Scheduler management methods
    public void startScheduler() {
        stopScheduler(); // Stop existing scheduler first
        
        boolean scheduleEnabled = getConfig().getBoolean("koth.schedule.enabled", false);
        getLogger().info("KOTH scheduling " + (scheduleEnabled ? "enabled" : "disabled"));
        
        if (scheduleEnabled) {
            try {
                int intervalMinutes = getConfig().getInt("koth.schedule.interval-minutes", 60);
                int interval = intervalMinutes * 60 * 20; // Convert to ticks
                getLogger().info("Scheduling KOTH events every " + intervalMinutes + " minutes (" + interval + " ticks)");
                
                // Set the next KOTH time (current time + interval in milliseconds)
                nextKothTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000);
                
                schedulerTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        getLogger().info("Scheduled KOTH event trigger - attempting to start random KOTH");
                        try {
                            kothManager.startRandomKoth();
                            
                            // Update next KOTH time for the following event
                            int intervalMinutes = getConfig().getInt("koth.schedule.interval-minutes", 60);
                            nextKothTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000);
                        } catch (Exception e) {
                            getLogger().warning("Error during scheduled KOTH start: " + e.getMessage());
                        }
                    }
                }.runTaskTimer(this, interval, interval);
                getLogger().info("KOTH scheduler started successfully");
            } catch (Exception e) {
                getLogger().warning("Failed to start KOTH scheduler: " + e.getMessage());
                getLogger().warning("Automatic KOTH events will be disabled");
            }
        }
    }

    public void stopScheduler() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
            getLogger().info("KOTH scheduler stopped");
        }
    }

    public void updateNextKothTime() {
        if (getConfig().getBoolean("koth.schedule.enabled", false)) {
            int intervalMinutes = getConfig().getInt("koth.schedule.interval-minutes", 60);
            nextKothTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000);
            getLogger().info("Updated next KOTH time due to manual start");
        }
    }
}
