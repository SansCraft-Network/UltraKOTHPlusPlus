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
import org.bukkit.plugin.Plugin;
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
            int x = kothSection.getInt("x");
            int y = kothSection.getInt("y");
            int z = kothSection.getInt("z");
            int radius = kothSection.getInt("radius", 10);
            String region = kothSection.getString("region", "");

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World '" + worldName + "' not found for KOTH '" + name + "'");
                continue;
            }

            KothRegion kothRegion = new KothRegion(name, world, x, y, z, radius, region);
            kothRegions.put(name, kothRegion);
            plugin.getLogger().info("Loaded KOTH region: " + name + " at " + worldName + " (" + x + ", " + y + ", " + z + ") radius: " + radius);
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

        if (inRegion) {
            if (capturingPlayer == null || !capturingPlayer.equals(player.getUniqueId())) {
                // New player entered or different player
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
            }
        } else {
            if (capturingPlayer != null && capturingPlayer.equals(player.getUniqueId())) {
                // Player left the region
                capturingPlayer = null;
                captureProgress = 0;

                if (captureTask != null) {
                    captureTask.cancel();
                    captureTask = null;
                }

                // Update boss bar
                updateBossBar();

                plugin.getLogger().info(player.getName() + " left the KOTH region, capture reset");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (activeKoth == null) return;
        if (capturingPlayer != null && capturingPlayer.equals(event.getPlayer().getUniqueId())) {
            capturingPlayer = null;
            captureProgress = 0;

            if (captureTask != null) {
                captureTask.cancel();
                captureTask = null;
            }

            updateBossBar();
            plugin.getLogger().info(event.getPlayer().getName() + " quit while capturing KOTH, capture reset");
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
        // First check WorldGuard region if available
        if (!region.region.isEmpty() && plugin.getWorldGuardHook().isEnabled()) {
            return plugin.getWorldGuardHook().isInRegion(player, region.region);
        }

        // Fallback to radius-based check
        Location loc = player.getLocation();
        if (!loc.getWorld().equals(region.world)) return false;

        double distance = loc.distance(region.getLocation());
        return distance <= region.radius;
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

    public static class KothRegion {
        public final String name;
        public final World world;
        public final int x, y, z, radius;
        public final String region;
        
        public KothRegion(String name, World world, int x, int y, int z, int radius, String region) {
            this.name = name;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.region = region != null ? region : "";
        }

        public Location getLocation() {
            return new Location(world, x, y, z);
        }
    }
}
