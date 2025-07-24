package top.sanscraft.ultrakoth.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class WorldGuardHook {
    private boolean enabled;

    public WorldGuardHook() {
        enabled = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    public boolean isEnabled() {
        return enabled;
    }

    // TODO: Add region check logic using WorldGuard API
    public boolean isInRegion(Player player, String region) {
        if (!enabled) return false;
        // Implement WorldGuard region check
        return false;
    }
}
