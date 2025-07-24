package top.sanscraft.ultrakoth.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.sanscraft.ultrakoth.UltraKOTHPlusPlus;
import top.sanscraft.ultrakoth.manager.PlayerDataManager;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final UltraKOTHPlusPlus plugin;
    private final PlayerDataManager playerDataManager;

    public PlaceholderAPIHook(UltraKOTHPlusPlus plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public boolean register() {
        return super.register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ultrakoth";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SansNom";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        if (identifier.equals("wins")) {
            return String.valueOf(playerDataManager.getWins(player.getUniqueId()));
        }
        // TODO: Add more placeholders (e.g., current_koth, time_left, etc.)
        return null;
    }
}
