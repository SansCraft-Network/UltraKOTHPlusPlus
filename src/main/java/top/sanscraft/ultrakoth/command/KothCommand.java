package top.sanscraft.ultrakoth.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.sanscraft.ultrakoth.UltraKOTHPlusPlus;

public class KothCommand implements CommandExecutor {
    private final UltraKOTHPlusPlus plugin;

    public KothCommand(UltraKOTHPlusPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUltraKOTHPlusPlus §7- §f/koth help");
            return true;
        }
        // TODO: Implement subcommands: help, start, stop, setregion, etc.
        return true;
    }
}
