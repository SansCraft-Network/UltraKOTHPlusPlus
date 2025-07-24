package top.sanscraft.ultrakoth.manager;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class BossBarManager implements Listener {
    private final Plugin plugin;
    private BossBar bossBar;
    private final Set<Player> viewers = new HashSet<>();
    private boolean autoAddNewPlayers = true;

    public BossBarManager(Plugin plugin, String title, BarColor color, BarStyle style) {
        this.plugin = plugin;
        bossBar = Bukkit.createBossBar(title, color, style);
        
        // Register event listener for player joins
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void setTitle(String title) {
        if (bossBar != null) {
            bossBar.setTitle(title);
        }
    }

    public void setProgress(double progress) {
        if (bossBar != null) {
            // Ensure progress is between 0.0 and 1.0
            progress = Math.max(0.0, Math.min(1.0, progress));
            bossBar.setProgress(progress);
        }
    }

    public void setColor(BarColor color) {
        if (bossBar != null) {
            bossBar.setColor(color);
        }
    }

    public void setStyle(BarStyle style) {
        if (bossBar != null) {
            bossBar.setStyle(style);
        }
    }

    public void addViewer(Player player) {
        if (bossBar != null && player != null && player.isOnline()) {
            bossBar.addPlayer(player);
            viewers.add(player);
        }
    }

    public void removeViewer(Player player) {
        if (bossBar != null && player != null) {
            bossBar.removePlayer(player);
            viewers.remove(player);
        }
    }

    public void addAllOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            addViewer(player);
        }
    }

    public void clearViewers() {
        if (bossBar != null) {
            for (Player player : new HashSet<>(viewers)) {
                bossBar.removePlayer(player);
            }
            viewers.clear();
        }
    }

    public void remove() {
        if (bossBar != null) {
            bossBar.removeAll();
            viewers.clear();
            bossBar = null;
        }
    }

    public void setAutoAddNewPlayers(boolean autoAdd) {
        this.autoAddNewPlayers = autoAdd;
    }

    public boolean isAutoAddNewPlayers() {
        return autoAddNewPlayers;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (autoAddNewPlayers && bossBar != null) {
            addViewer(event.getPlayer());
        }
    }

    public Set<Player> getViewers() {
        return new HashSet<>(viewers);
    }

    public boolean isActive() {
        return bossBar != null;
    }

    public String getTitle() {
        return bossBar != null ? bossBar.getTitle() : "";
    }

    public double getProgress() {
        return bossBar != null ? bossBar.getProgress() : 0.0;
    }
}
