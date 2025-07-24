package top.sanscraft.ultrakoth.command;

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
            List<String> subcommands = Arrays.asList("help", "status", "wins", "list");
            
            if (sender.hasPermission("ultrakoth.admin")) {
                subcommands = new ArrayList<>(subcommands);
                subcommands.addAll(Arrays.asList("start", "stop", "reload"));
            }

            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subcommand);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("start") && sender.hasPermission("ultrakoth.admin")) {
                // Region names for start command
                for (String regionName : plugin.getKothManager().getKothRegions().keySet()) {
                    if (regionName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(regionName);
                    }
                }
            } else if (args[0].equalsIgnoreCase("wins")) {
                // Online player names for wins command
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }
}
