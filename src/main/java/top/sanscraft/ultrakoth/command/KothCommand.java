package top.sanscraft.ultrakoth.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import top.sanscraft.ultrakoth.UltraKOTHPlusPlus;
import top.sanscraft.ultrakoth.manager.KothManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class KothCommand implements CommandExecutor, TabCompleter {
    private final UltraKOTHPlusPlus plugin;

    public KothCommand(UltraKOTHPlusPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "help":
                sendHelp(sender);
                break;
                
            case "start":
                handleStart(sender, args);
                break;
                
            case "stop":
                handleStop(sender);
                break;
                
            case "status":
                handleStatus(sender);
                break;
                
            case "wins":
                handleWins(sender, args);
                break;
                
            case "list":
                handleList(sender);
                break;
                
            case "reload":
                handleReload(sender);
                break;
                
            case "config":
                handleConfig(sender, args);
                break;
                
            case "create":
                handleCreate(sender, args);
                break;
                
            default:
                sender.sendMessage("§cUnknown subcommand. Use §e/koth help §cfor available commands.");
                break;
        }
        
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== UltraKOTHPlusPlus Commands ===");
        sender.sendMessage("§e/koth help §7- Show this help message");
        sender.sendMessage("§e/koth status §7- Show current KOTH status");
        sender.sendMessage("§e/koth list §7- List all KOTH regions");
        sender.sendMessage("§e/koth wins [player] §7- Show KOTH wins");
        
        if (sender.hasPermission("ultrakoth.admin")) {
            sender.sendMessage("§c=== Admin Commands ===");
            sender.sendMessage("§e/koth start [region] §7- Start a KOTH event");
            sender.sendMessage("§e/koth stop §7- Stop current KOTH event");
            sender.sendMessage("§e/koth reload §7- Reload plugin configuration");
            sender.sendMessage("§e/koth config <setting> [value] §7- View/modify configuration");
            sender.sendMessage("§e/koth create <name> §7- Create a new KOTH region");
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultrakoth.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        KothManager kothManager = plugin.getKothManager();
        
        if (args.length < 2) {
            // Start random KOTH
            kothManager.startRandomKoth();
            sender.sendMessage("§aStarting random KOTH event...");
        } else {
            // Start specific KOTH
            String regionName = args[1];
            if (!kothManager.getKothRegions().containsKey(regionName)) {
                sender.sendMessage("§cKOTH region '" + regionName + "' not found!");
                sender.sendMessage("§eAvailable regions: " + String.join(", ", kothManager.getKothRegions().keySet()));
                return;
            }
            
            kothManager.startKoth(regionName);
            sender.sendMessage("§aStarting KOTH event at " + regionName + "!");
        }
    }

    private void handleStop(CommandSender sender) {
        if (!sender.hasPermission("ultrakoth.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        KothManager kothManager = plugin.getKothManager();
        
        if (kothManager.getActiveKoth() == null) {
            sender.sendMessage("§cNo KOTH event is currently active.");
            return;
        }

        String activeKoth = kothManager.getActiveKoth();
        kothManager.stopKoth();
        sender.sendMessage("§aStopped KOTH event at " + activeKoth + "!");
    }

    private void handleStatus(CommandSender sender) {
        KothManager kothManager = plugin.getKothManager();
        
        if (kothManager.getActiveKoth() == null) {
            sender.sendMessage("§eNo KOTH event is currently active.");
            return;
        }

        String activeKoth = kothManager.getActiveKoth();
        UUID capturingPlayer = kothManager.getCapturingPlayer();
        int progress = kothManager.getCaptureProgress();
        int total = kothManager.getCaptureTime();
        
        sender.sendMessage("§6=== KOTH Status ===");
        sender.sendMessage("§eActive KOTH: §b" + activeKoth);
        
        if (capturingPlayer != null) {
            Player player = plugin.getServer().getPlayer(capturingPlayer);
            String playerName = player != null ? player.getName() : "Unknown";
            sender.sendMessage("§eCaptured by: §a" + playerName);
            sender.sendMessage("§eProgress: §b" + progress + "§7/§b" + total + " §7seconds");
            
            double percentage = ((double) progress / total) * 100;
            sender.sendMessage("§eCompletion: §b" + String.format("%.1f", percentage) + "%");
        } else {
            sender.sendMessage("§eNo one is currently capturing the KOTH.");
        }
    }

    private void handleWins(CommandSender sender, String[] args) {
        Player target;
        
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole must specify a player name.");
                return;
            }
            target = (Player) sender;
        } else {
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer '" + args[1] + "' not found.");
                return;
            }
        }

        int wins = plugin.getPlayerDataManager().getWins(target.getUniqueId());
        
        if (target.equals(sender)) {
            sender.sendMessage("§eYou have won §b" + wins + " §eKOTH events!");
        } else {
            sender.sendMessage("§e" + target.getName() + " has won §b" + wins + " §eKOTH events!");
        }
    }

    private void handleList(CommandSender sender) {
        KothManager kothManager = plugin.getKothManager();
        
        if (kothManager.getKothRegions().isEmpty()) {
            sender.sendMessage("§cNo KOTH regions are configured.");
            return;
        }

        sender.sendMessage("§6=== KOTH Regions ===");
        for (KothManager.KothRegion region : kothManager.getKothRegions().values()) {
            String status = kothManager.getActiveKoth() != null && kothManager.getActiveKoth().equals(region.name) ? 
                    " §c[ACTIVE]" : " §a[INACTIVE]";
            
            sender.sendMessage("§e" + region.name + status);
            
            if (region.isUsingWorldGuard()) {
                sender.sendMessage("  §7Type: §bWorldGuard Region");
                sender.sendMessage("  §7World: §f" + region.world.getName());
                sender.sendMessage("  §7Region: §f" + region.getRegionName());
            } else {
                sender.sendMessage("  §7Type: §bCoordinate/Radius");
                sender.sendMessage("  §7World: §f" + region.world.getName() + 
                                 " §7Location: §f" + region.x + ", " + region.y + ", " + region.z + 
                                 " §7Radius: §f" + region.radius);
            }
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("ultrakoth.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        try {
            plugin.reloadConfig();
            sender.sendMessage("§aConfiguration reloaded successfully!");
            sender.sendMessage("§eNote: You may need to restart the plugin for some changes to take effect.");
        } catch (Exception e) {
            sender.sendMessage("§cError reloading configuration: " + e.getMessage());
            plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - subcommands
            List<String> subcommands = new ArrayList<>(Arrays.asList("help", "status", "wins", "list"));
            
            if (sender.hasPermission("ultrakoth.admin")) {
                subcommands.addAll(Arrays.asList("start", "stop", "reload", "config", "create"));
            }

            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subcommand);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("start") && sender.hasPermission("ultrakoth.admin")) {
                // Region names for start command
                KothManager kothManager = plugin.getKothManager();
                if (kothManager != null && kothManager.getKothRegions() != null) {
                    for (String regionName : kothManager.getKothRegions().keySet()) {
                        if (regionName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(regionName);
                        }
                    }
                }
            } else if (args[0].equalsIgnoreCase("wins")) {
                // Online player names for wins command
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            } else if (args[0].equalsIgnoreCase("config") && sender.hasPermission("ultrakoth.admin")) {
                // Config settings
                List<String> configOptions = Arrays.asList("capture-time", "schedule-enabled", "schedule-interval", 
                    "debug", "bossbar-color", "bossbar-style", "list");
                for (String option : configOptions) {
                    if (option.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(option);
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("config") && sender.hasPermission("ultrakoth.admin")) {
            // Config values
            String setting = args[1].toLowerCase();
            switch (setting) {
                case "schedule-enabled":
                case "debug":
                    completions.addAll(Arrays.asList("true", "false"));
                    break;
                case "bossbar-color":
                    completions.addAll(Arrays.asList("BLUE", "GREEN", "PINK", "PURPLE", "RED", "WHITE", "YELLOW"));
                    break;
                case "bossbar-style":
                    completions.addAll(Arrays.asList("SOLID", "SEGMENTED_6", "SEGMENTED_10", "SEGMENTED_12", "SEGMENTED_20"));
                    break;
                case "capture-time":
                    completions.addAll(Arrays.asList("60", "120", "180", "300", "600"));
                    break;
                case "schedule-interval":
                    completions.addAll(Arrays.asList("30", "60", "90", "120"));
                    break;
            }
        }

        return completions;
    }

    private void handleConfig(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultrakoth.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        if (args.length == 1) {
            // Show config help
            sender.sendMessage("§6=== KOTH Configuration Commands ===");
            sender.sendMessage("§e/koth config capture-time [seconds] §7- Set capture time");
            sender.sendMessage("§e/koth config schedule-enabled [true/false] §7- Enable/disable scheduling");
            sender.sendMessage("§e/koth config schedule-interval [minutes] §7- Set schedule interval");
            sender.sendMessage("§e/koth config debug [true/false] §7- Enable/disable debug mode");
            sender.sendMessage("§e/koth config bossbar-color [color] §7- Set boss bar color");
            sender.sendMessage("§e/koth config bossbar-style [style] §7- Set boss bar style");
            sender.sendMessage("§e/koth config list §7- Show current configuration");
            return;
        }

        String setting = args[1].toLowerCase();

        if (setting.equals("list")) {
            // Show current configuration
            sender.sendMessage("§6=== Current KOTH Configuration ===");
            sender.sendMessage("§eCaptue Time: §b" + plugin.getConfig().getInt("koth.capture-time-seconds", 300) + " seconds");
            sender.sendMessage("§eSchedule Enabled: §b" + plugin.getConfig().getBoolean("koth.schedule.enabled", false));
            sender.sendMessage("§eSchedule Interval: §b" + plugin.getConfig().getInt("koth.schedule.interval-minutes", 60) + " minutes");
            sender.sendMessage("§eDebug Mode: §b" + plugin.getConfig().getBoolean("settings.debug", false));
            sender.sendMessage("§eBoss Bar Color: §b" + plugin.getConfig().getString("koth.bossbar.color", "BLUE"));
            sender.sendMessage("§eBoss Bar Style: §b" + plugin.getConfig().getString("koth.bossbar.style", "SOLID"));
            return;
        }

        if (args.length < 3) {
            // Show current value
            String currentValue = getCurrentConfigValue(setting);
            if (currentValue != null) {
                sender.sendMessage("§eCurrent value for §b" + setting + "§e: §b" + currentValue);
            } else {
                sender.sendMessage("§cUnknown configuration setting: " + setting);
            }
            return;
        }

        String value = args[2];
        boolean success = setConfigValue(setting, value);

        if (success) {
            plugin.saveConfig();
            sender.sendMessage("§aConfiguration updated: §b" + setting + " §a= §b" + value);
            sender.sendMessage("§eUse §b/koth reload §eto apply changes.");
        } else {
            sender.sendMessage("§cFailed to set configuration value. Check the value format.");
        }
    }

    private String getCurrentConfigValue(String setting) {
        switch (setting) {
            case "capture-time":
                return String.valueOf(plugin.getConfig().getInt("koth.capture-time-seconds", 300));
            case "schedule-enabled":
                return String.valueOf(plugin.getConfig().getBoolean("koth.schedule.enabled", false));
            case "schedule-interval":
                return String.valueOf(plugin.getConfig().getInt("koth.schedule.interval-minutes", 60));
            case "debug":
                return String.valueOf(plugin.getConfig().getBoolean("settings.debug", false));
            case "bossbar-color":
                return plugin.getConfig().getString("koth.bossbar.color", "BLUE");
            case "bossbar-style":
                return plugin.getConfig().getString("koth.bossbar.style", "SOLID");
            default:
                return null;
        }
    }

    private boolean setConfigValue(String setting, String value) {
        try {
            switch (setting) {
                case "capture-time":
                    int captureTime = Integer.parseInt(value);
                    if (captureTime <= 0) return false;
                    plugin.getConfig().set("koth.capture-time-seconds", captureTime);
                    return true;
                    
                case "schedule-enabled":
                    boolean scheduleEnabled = Boolean.parseBoolean(value);
                    plugin.getConfig().set("koth.schedule.enabled", scheduleEnabled);
                    return true;
                    
                case "schedule-interval":
                    int interval = Integer.parseInt(value);
                    if (interval <= 0) return false;
                    plugin.getConfig().set("koth.schedule.interval-minutes", interval);
                    return true;
                    
                case "debug":
                    boolean debug = Boolean.parseBoolean(value);
                    plugin.getConfig().set("settings.debug", debug);
                    return true;
                    
                case "bossbar-color":
                    // Validate boss bar color
                    String[] validColors = {"BLUE", "GREEN", "PINK", "PURPLE", "RED", "WHITE", "YELLOW"};
                    boolean validColor = false;
                    for (String color : validColors) {
                        if (color.equalsIgnoreCase(value)) {
                            plugin.getConfig().set("koth.bossbar.color", color.toUpperCase());
                            validColor = true;
                            break;
                        }
                    }
                    return validColor;
                    
                case "bossbar-style":
                    // Validate boss bar style
                    String[] validStyles = {"SOLID", "SEGMENTED_6", "SEGMENTED_10", "SEGMENTED_12", "SEGMENTED_20"};
                    boolean validStyle = false;
                    for (String style : validStyles) {
                        if (style.equalsIgnoreCase(value)) {
                            plugin.getConfig().set("koth.bossbar.style", style.toUpperCase());
                            validStyle = true;
                            break;
                        }
                    }
                    return validStyle;
                    
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultrakoth.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /koth create <name>");
            return;
        }

        Player player = (Player) sender;
        String regionName = args[1];

        // Check if region already exists
        if (plugin.getKothManager().getKothRegions().containsKey(regionName)) {
            sender.sendMessage("§cA KOTH region with that name already exists!");
            return;
        }

        // Check if it's a valid region name
        if (!regionName.matches("[a-zA-Z0-9_-]+")) {
            sender.sendMessage("§cRegion name can only contain letters, numbers, underscores, and hyphens.");
            return;
        }

        sender.sendMessage("§6=== KOTH Region Creation ===");
        sender.sendMessage("§eCreating region: §b" + regionName);
        sender.sendMessage("§eChoose creation method:");
        sender.sendMessage("§a1. §e/koth create " + regionName + " coords <radius> §7- Use your current location with radius");
        sender.sendMessage("§a2. §e/koth create " + regionName + " worldguard <region-name> §7- Use existing WorldGuard region");
        
        if (args.length < 3) {
            sender.sendMessage("§eExample: §b/koth create " + regionName + " coords 15");
            sender.sendMessage("§eExample: §b/koth create " + regionName + " worldguard my_wg_region");
            return;
        }

        String method = args[2].toLowerCase();
        
        if (method.equals("coords")) {
            if (args.length < 4) {
                sender.sendMessage("§cUsage: /koth create " + regionName + " coords <radius>");
                return;
            }
            
            try {
                int radius = Integer.parseInt(args[3]);
                if (radius <= 0 || radius > 100) {
                    sender.sendMessage("§cRadius must be between 1 and 100.");
                    return;
                }
                
                Location loc = player.getLocation();
                
                // Add to config
                plugin.getConfig().set("koth-locations." + regionName + ".use-worldguard", false);
                plugin.getConfig().set("koth-locations." + regionName + ".world", loc.getWorld().getName());
                plugin.getConfig().set("koth-locations." + regionName + ".x", loc.getBlockX());
                plugin.getConfig().set("koth-locations." + regionName + ".y", loc.getBlockY());
                plugin.getConfig().set("koth-locations." + regionName + ".z", loc.getBlockZ());
                plugin.getConfig().set("koth-locations." + regionName + ".radius", radius);
                
                plugin.saveConfig();
                
                sender.sendMessage("§aSuccessfully created KOTH region §b" + regionName + "§a!");
                sender.sendMessage("§eLocation: §f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                sender.sendMessage("§eRadius: §f" + radius + " blocks");
                sender.sendMessage("§eUse §b/koth reload §eto load the new region.");
                
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid radius number.");
            }
            
        } else if (method.equals("worldguard")) {
            if (args.length < 4) {
                sender.sendMessage("§cUsage: /koth create " + regionName + " worldguard <region-name>");
                return;
            }
            
            String wgRegionName = args[3];
            
            // Check if WorldGuard is available
            if (!plugin.getWorldGuardHook().isEnabled()) {
                sender.sendMessage("§cWorldGuard is not available on this server!");
                return;
            }
            
            // Check if the WorldGuard region exists
            String[] playerRegions = plugin.getWorldGuardHook().getRegionsAtLocation(player);
            boolean regionExists = false;
            for (String region : playerRegions) {
                if (region.equalsIgnoreCase(wgRegionName)) {
                    regionExists = true;
                    break;
                }
            }
            
            if (!regionExists) {
                sender.sendMessage("§cWorldGuard region '" + wgRegionName + "' not found at your location!");
                sender.sendMessage("§eYou are currently in regions: §f" + String.join(", ", playerRegions));
                return;
            }
            
            // Add to config
            plugin.getConfig().set("koth-locations." + regionName + ".use-worldguard", true);
            plugin.getConfig().set("koth-locations." + regionName + ".world", player.getWorld().getName());
            plugin.getConfig().set("koth-locations." + regionName + ".region", wgRegionName);
            
            plugin.saveConfig();
            
            sender.sendMessage("§aSuccessfully created KOTH region §b" + regionName + "§a!");
            sender.sendMessage("§eUsing WorldGuard region: §f" + wgRegionName);
            sender.sendMessage("§eWorld: §f" + player.getWorld().getName());
            sender.sendMessage("§eUse §b/koth reload §eto load the new region.");
            
        } else {
            sender.sendMessage("§cInvalid method. Use 'coords' or 'worldguard'.");
        }
    }
}
