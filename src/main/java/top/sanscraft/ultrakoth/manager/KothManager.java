package top.sanscraft.ultrakoth.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import top.sanscraft.ultrakoth.UltraKOTHPlusPlus;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class KothManager implements Listener {
    private final UltraKOTHPlusPlus plugin;
    private final Map<String, KothRegion> kothRegions = new HashMap<>();
    private String activeKoth = null;
    private UUID capturingPlayer = null;
    private final LinkedList<UUID> playerQueue = new LinkedList<>(); // Queue of players in the region
    private int captureProgress = 0;
    private int captureTime = 300; // 300 seconds = 5 minutes
    private BukkitTask captureTask = null;
    private BossBarManager bossBarManager = null;

    public KothManager(UltraKOTHPlusPlus plugin) {
        this.plugin = plugin;
        loadKothRegions();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Loaded " + kothRegions.size() + " KOTH regions");
    }

    private void loadKothRegions() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("koth-locations");
        if (section == null) {
            plugin.getLogger().warning("No koth-locations section found in config.yml");
            return;
        }

        for (String name : section.getKeys(false)) {
            ConfigurationSection kothSection = section.getConfigurationSection(name);
            if (kothSection == null) continue;

            String worldName = kothSection.getString("world");
            boolean useWorldGuard = kothSection.getBoolean("use-worldguard", false);
            
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World '" + worldName + "' not found for KOTH '" + name + "'");
                continue;
            }

            KothRegion kothRegion;
            
            if (useWorldGuard) {
                // WorldGuard region mode
                String region = kothSection.getString("region", "");
                if (region.isEmpty()) {
                    plugin.getLogger().warning("WorldGuard region name not specified for KOTH '" + name + "'");
                    continue;
                }
                
                // Verify WorldGuard region exists
                if (!plugin.getWorldGuardHook().isEnabled()) {
                    plugin.getLogger().warning("WorldGuard not available but required for KOTH '" + name + "'");
                    continue;
                }
                
                kothRegion = new KothRegion(name, world, 0, 0, 0, 0, region, true);
                plugin.getLogger().info("Loaded WorldGuard KOTH region: " + name + " using region '" + region + "' in world " + worldName);
                
            } else {
                // Coordinate/radius mode
                int x = kothSection.getInt("x", 0);
                int y = kothSection.getInt("y", 64);
                int z = kothSection.getInt("z", 0);
                int radius = kothSection.getInt("radius", 10);
                
                if (radius <= 0) {
                    plugin.getLogger().warning("Invalid radius for KOTH '" + name + "', using default radius of 10");
                    radius = 10;
                }
                
                kothRegion = new KothRegion(name, world, x, y, z, radius, "", false);
                plugin.getLogger().info("Loaded coordinate KOTH region: " + name + " at " + worldName + " (" + x + ", " + y + ", " + z + ") radius: " + radius);
            }

            kothRegions.put(name, kothRegion);
        }
    }

    public void startRandomKoth() {
        if (kothRegions.isEmpty()) {
            plugin.getLogger().warning("No KOTH regions configured!");
            return;
        }

        if (activeKoth != null) {
            plugin.getLogger().info("KOTH already active: " + activeKoth);
            return;
        }

        List<String> regionNames = new ArrayList<>(kothRegions.keySet());
        String randomRegion = regionNames.get(ThreadLocalRandom.current().nextInt(regionNames.size()));
        startKoth(randomRegion);
    }

    public void startKoth(String regionName) {
        KothRegion region = kothRegions.get(regionName);
        if (region == null) {
            plugin.getLogger().warning("KOTH region '" + regionName + "' not found!");
            return;
        }

        if (activeKoth != null) {
            plugin.getLogger().info("Stopping current KOTH to start new one");
            stopKoth();
        }

        activeKoth = regionName;
        captureProgress = 0;
        capturingPlayer = null;
        captureTime = plugin.getConfig().getInt("koth.capture-time-seconds", 300);

        // Create boss bar
        String title = plugin.getConfig().getString("koth.bossbar.title", "&eKOTH at &b{location}")
                .replace("{location}", regionName)
                .replace("&", "§");
        
        BarColor color = parseBarColor(plugin.getConfig().getString("koth.bossbar.color", "BLUE"));
        BarStyle style = parseBarStyle(plugin.getConfig().getString("koth.bossbar.style", "SOLID"));
        
        bossBarManager = new BossBarManager(plugin, title, color, style);
        plugin.setBossBarManager(bossBarManager);

        // Add all online players to boss bar
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBarManager.addViewer(player);
        }

        // Broadcast start message
        String startMessage = plugin.getConfig().getString("koth.broadcast.start", "&6KOTH has started at &b{location}!")
                .replace("{location}", regionName)
                .replace("&", "§");
        Bukkit.broadcastMessage(startMessage);

        // Play start sound and particles
        playEffects("start", region.getLocation());

        plugin.getLogger().info("Started KOTH at " + regionName + " with " + captureTime + " second capture time");
        
        // Update the scheduler's next KOTH time to account for this manual start
        plugin.updateNextKothTime();
    }

    public void stopKoth() {
        if (activeKoth == null) return;

        if (captureTask != null) {
            captureTask.cancel();
            captureTask = null;
        }

        if (bossBarManager != null) {
            bossBarManager.remove();
            bossBarManager = null;
            plugin.setBossBarManager(null);
        }

        String stoppedKoth = activeKoth;
        activeKoth = null;
        capturingPlayer = null;
        playerQueue.clear(); // Clear the player queue
        captureProgress = 0;

        plugin.getLogger().info("Stopped KOTH: " + stoppedKoth);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (activeKoth == null) return;

        Player player = event.getPlayer();
        KothRegion region = kothRegions.get(activeKoth);
        if (region == null) return;

        boolean inRegion = isPlayerInRegion(player, region);
        UUID playerUUID = player.getUniqueId();
        
        // Debug logging
        if (plugin.getConfig().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("DEBUG: Player " + player.getName() + " movement check:");
            plugin.getLogger().info("  - Active KOTH: " + activeKoth);
            plugin.getLogger().info("  - Player location: " + player.getLocation());
            plugin.getLogger().info("  - Region using WorldGuard: " + region.isUsingWorldGuard());
            if (region.isUsingWorldGuard()) {
                plugin.getLogger().info("  - WorldGuard region name: " + region.getRegionName());
                plugin.getLogger().info("  - WorldGuard enabled: " + plugin.getWorldGuardHook().isEnabled());
                if (plugin.getWorldGuardHook().isEnabled()) {
                    String[] regions = plugin.getWorldGuardHook().getRegionsAtLocation(player);
                    plugin.getLogger().info("  - Player is in regions: " + String.join(", ", regions));
                }
            } else {
                plugin.getLogger().info("  - Region center: " + region.getLocation());
                plugin.getLogger().info("  - Region radius: " + region.radius);
                plugin.getLogger().info("  - Distance to center: " + player.getLocation().distance(region.getLocation()));
            }
            plugin.getLogger().info("  - Player in region: " + inRegion);
            plugin.getLogger().info("  - Current queue: " + playerQueue.size() + " players");
            plugin.getLogger().info("  - Currently capturing: " + (capturingPlayer != null ? Bukkit.getPlayer(capturingPlayer).getName() : "None"));
        }

        if (inRegion) {
            // Player entered the region
            if (!playerQueue.contains(playerUUID)) {
                // Add player to queue if not already in it
                playerQueue.offer(playerUUID);
                plugin.getLogger().info(player.getName() + " entered KOTH region and was added to queue (position: " + playerQueue.size() + ")");
                
                // If this is the first player in queue and no one is currently capturing
                if (capturingPlayer == null && playerQueue.peek().equals(playerUUID)) {
                    startCapturingForPlayer(player, region);
                }
            }
        } else {
            // Player left the region
            if (playerQueue.contains(playerUUID)) {
                playerQueue.remove(playerUUID);
                plugin.getLogger().info(player.getName() + " left KOTH region and was removed from queue");
                
                // If this was the capturing player, stop their capture and start next in queue
                if (capturingPlayer != null && capturingPlayer.equals(playerUUID)) {
                    stopCurrentCapture();
                    startNextPlayerCapture(region);
                }
            }
        }
    }
    
    private void startCapturingForPlayer(Player player, KothRegion region) {
        capturingPlayer = player.getUniqueId();
        captureProgress = 0;

        // Cancel existing task
        if (captureTask != null) {
            captureTask.cancel();
        }

        // Broadcast capture message
        String captureMessage = plugin.getConfig().getString("koth.broadcast.capture", "&a{player} &eis capturing the KOTH!")
                .replace("{player}", player.getName())
                .replace("&", "§");
        Bukkit.broadcastMessage(captureMessage);

        // Start capture task
        startCaptureTask(player);

        // Play capture effects
        playEffects("capture", region.getLocation());
        
        plugin.getLogger().info(player.getName() + " started capturing KOTH");
    }
    
    private void stopCurrentCapture() {
        if (captureTask != null) {
            captureTask.cancel();
            captureTask = null;
        }
        
        Player previousPlayer = capturingPlayer != null ? Bukkit.getPlayer(capturingPlayer) : null;
        String previousPlayerName = previousPlayer != null ? previousPlayer.getName() : "Unknown";
        
        capturingPlayer = null;
        captureProgress = 0;
        
        // Update boss bar
        updateBossBar();
        
        plugin.getLogger().info(previousPlayerName + " stopped capturing KOTH, capture reset");
    }
    
    private void startNextPlayerCapture(KothRegion region) {
        // Start capture for the next player in queue (if any)
        if (!playerQueue.isEmpty()) {
            UUID nextPlayerUUID = playerQueue.peek();
            Player nextPlayer = Bukkit.getPlayer(nextPlayerUUID);
            
            if (nextPlayer != null && nextPlayer.isOnline()) {
                // Verify the next player is still in the region
                if (isPlayerInRegion(nextPlayer, region)) {
                    startCapturingForPlayer(nextPlayer, region);
                } else {
                    // Next player is no longer in region, remove from queue and check the next one
                    playerQueue.poll();
                    startNextPlayerCapture(region);
                }
            } else {
                // Next player is offline, remove from queue and check the next one
                playerQueue.poll();
                startNextPlayerCapture(region);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (activeKoth == null) return;
        
        UUID playerUUID = event.getPlayer().getUniqueId();
        
        // Remove player from queue if they were in it
        if (playerQueue.contains(playerUUID)) {
            playerQueue.remove(playerUUID);
            plugin.getLogger().info(event.getPlayer().getName() + " quit and was removed from KOTH queue");
        }
        
        // If the quitting player was currently capturing
        if (capturingPlayer != null && capturingPlayer.equals(playerUUID)) {
            stopCurrentCapture();
            
            // Start capture for next player in queue
            KothRegion region = kothRegions.get(activeKoth);
            if (region != null) {
                startNextPlayerCapture(region);
            }
            
            plugin.getLogger().info(event.getPlayer().getName() + " quit while capturing KOTH, passing to next player in queue");
        }
    }

    private void startCaptureTask(Player player) {
        captureTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeKoth == null || capturingPlayer == null || !capturingPlayer.equals(player.getUniqueId())) {
                    cancel();
                    return;
                }

                captureProgress++;
                updateBossBar();

                if (captureProgress >= captureTime) {
                    // Player won!
                    winKoth(player);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    private void winKoth(Player player) {
        if (activeKoth == null) return;

        KothRegion region = kothRegions.get(activeKoth);
        
        // Add win to player data
        plugin.getPlayerDataManager().addWin(player.getUniqueId());

        // Broadcast win message
        String winMessage = plugin.getConfig().getString("koth.broadcast.win", "&b{player} &ahas won the KOTH at &e{location}!")
                .replace("{player}", player.getName())
                .replace("{location}", activeKoth)
                .replace("&", "§");
        Bukkit.broadcastMessage(winMessage);

        // Give rewards
        giveRewards(player);

        // Play win effects
        if (region != null) {
            playEffects("win", region.getLocation());
        }

        plugin.getLogger().info(player.getName() + " won KOTH at " + activeKoth);

        // Stop the KOTH
        stopKoth();
    }

    private void giveRewards(Player player) {
        List<String> rewards = plugin.getConfig().getStringList("koth.rewards");
        for (String reward : rewards) {
            String command = reward.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            plugin.getLogger().info("Executed reward command: " + command);
        }
    }

    private void updateBossBar() {
        if (bossBarManager == null) return;

        if (capturingPlayer == null) {
            String title = plugin.getConfig().getString("koth.bossbar.title", "&eKOTH at &b{location}")
                    .replace("{location}", activeKoth)
                    .replace("&", "§");
            bossBarManager.setTitle(title);
            bossBarManager.setProgress(0.0);
        } else {
            Player player = Bukkit.getPlayer(capturingPlayer);
            String playerName = player != null ? player.getName() : "Unknown";
            
            String title = plugin.getConfig().getString("koth.bossbar.title", "&eKOTH being captured by &a{player}")
                    .replace("{player}", playerName)
                    .replace("{location}", activeKoth)
                    .replace("&", "§");
            
            if (plugin.getConfig().getBoolean("koth.bossbar.countdown", true)) {
                int remaining = captureTime - captureProgress;
                title += " §7(" + remaining + "s)";
            }
            
            bossBarManager.setTitle(title);
            bossBarManager.setProgress((double) captureProgress / captureTime);
        }
    }

    private boolean isPlayerInRegion(Player player, KothRegion region) {
        if (region.isUsingWorldGuard()) {
            // Use WorldGuard region detection
            if (!plugin.getWorldGuardHook().isEnabled()) {
                plugin.getLogger().warning("WorldGuard region '" + region.getRegionName() + "' configured but WorldGuard is not available!");
                return false;
            }
            return plugin.getWorldGuardHook().isInRegion(player, region.getRegionName());
        } else {
            // Use coordinate/radius based detection
            Location loc = player.getLocation();
            if (!loc.getWorld().equals(region.world)) return false;

            double distance = loc.distance(region.getLocation());
            return distance <= region.radius;
        }
    }

    private void playEffects(String type, Location location) {
        String soundName = plugin.getConfig().getString("koth.sounds." + type);
        String particleName = plugin.getConfig().getString("koth.particles." + type);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (soundName != null && !soundName.isEmpty()) {
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid sound: " + soundName);
                }
            }

            if (particleName != null && !particleName.isEmpty() && player.getWorld().equals(location.getWorld())) {
                try {
                    player.spawnParticle(org.bukkit.Particle.valueOf(particleName), location, 50, 1, 1, 1, 0.1);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid particle: " + particleName);
                }
            }
        }
    }

    private BarColor parseBarColor(String colorName) {
        try {
            return BarColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid bar color: " + colorName + ", using BLUE");
            return BarColor.BLUE;
        }
    }

    private BarStyle parseBarStyle(String styleName) {
        try {
            return BarStyle.valueOf(styleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid bar style: " + styleName + ", using SOLID");
            return BarStyle.SOLID;
        }
    }

    // Getters
    public String getActiveKoth() {
        return activeKoth;
    }

    public UUID getCapturingPlayer() {
        return capturingPlayer;
    }

    public int getCaptureProgress() {
        return captureProgress;
    }

    public int getCaptureTime() {
        return captureTime;
    }

    public Map<String, KothRegion> getKothRegions() {
        return new HashMap<>(kothRegions);
    }

    public LinkedList<UUID> getPlayerQueue() {
        return new LinkedList<>(playerQueue);
    }

    // Reload method for when configuration changes
    public void reloadRegions() {
        kothRegions.clear();
        loadKothRegions();
        plugin.getLogger().info("Reloaded " + kothRegions.size() + " KOTH regions");
    }

    public static class KothRegion {
        public final String name;
        public final World world;
        public final int x, y, z, radius;
        public final String region;
        public final boolean useWorldGuard;
        
        public KothRegion(String name, World world, int x, int y, int z, int radius, String region, boolean useWorldGuard) {
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.region = region != null ? region : "";
            this.useWorldGuard = useWorldGuard;
        }

        public Location getLocation() {
            return new Location(world, x, y, z);
        }

        public boolean isUsingWorldGuard() {
            return useWorldGuard;
        }

        public String getRegionName() {
            return region;
        }
    }
}
