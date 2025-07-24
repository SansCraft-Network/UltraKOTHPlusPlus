package top.sanscraft.ultrakoth.manager;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class BossBarManager {
    private final Plugin plugin;
    private BossBar bossBar;
    private final Set<Player> viewers = new HashSet<>();

    public BossBarManager(Plugin plugin, String title, BarColor color, BarStyle style) {
        this.plugin = plugin;
        bossBar = Bukkit.createBossBar(title, color, style);
    }

    public void setTitle(String title) {
        bossBar.setTitle(title);
    }

    public void setProgress(double progress) {
        bossBar.setProgress(progress);
    }

    public void addViewer(Player player) {
        bossBar.addPlayer(player);
        viewers.add(player);
    }

    public void removeViewer(Player player) {
        bossBar.removePlayer(player);
        viewers.remove(player);
    }

    public void clearViewers() {
        for (Player player : viewers) {
            bossBar.removePlayer(player);
        }
        viewers.clear();
    }

    public void remove() {
        bossBar.removeAll();
    }
}
