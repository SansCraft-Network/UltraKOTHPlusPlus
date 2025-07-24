package top.sanscraft.ultrakoth.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class WorldGuardHook {
    private boolean enabled;

    public WorldGuardHook() {
        enabled = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        if (enabled) {
            try {
                // Test if WorldGuard API is available
                WorldGuard.getInstance();
            } catch (Exception e) {
                enabled = false;
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if a player is in a specific WorldGuard region
     * @param player The player to check
     * @param regionName The name of the region
     * @return true if player is in the region, false otherwise
     */
    public boolean isInRegion(Player player, String regionName) {
        if (!enabled || regionName == null || regionName.isEmpty()) {
            return false;
        }

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
            
            for (ProtectedRegion region : set) {
                if (region.getId().equalsIgnoreCase(regionName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // If there's any error with WorldGuard integration, return false
            return false;
        }

        return false;
    }

    /**
     * Get all region names at a player's location
     * @param player The player
     * @return Array of region names
     */
    public String[] getRegionsAtLocation(Player player) {
        if (!enabled) {
            return new String[0];
        }

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
            
            return set.getRegions().stream()
                    .map(ProtectedRegion::getId)
                    .toArray(String[]::new);
        } catch (Exception e) {
            return new String[0];
        }
    }
}
