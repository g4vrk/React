package ai.solar.kirill.main.comand;

import ai.solar.kirill.main.comand.impl.CrashCommand;
import ai.solar.kirill.main.comand.impl.ReloadCommand;
import ai.solar.kirill.main.comand.impl.MenuCommand;
import ai.solar.kirill.main.comand.impl.DataCollectCommand;
import ai.solar.kirill.main.comand.impl.ClearDataCommand;
import ai.solar.kirill.main.comand.impl.TrainCommand;
import ai.solar.kirill.main.comand.impl.TrainStatusCommand;
import ai.solar.kirill.main.comand.impl.DataStatsCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import ai.solar.kirill.SolarAI;
import ai.solar.kirill.main.comand.impl.AlertCommand;

import java.util.*;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final SolarAI plugin;

    public CommandManager(SolarAI plugin) {
        this.plugin = plugin;
        registerSubCommand(new AlertCommand(plugin));
        registerSubCommand(new CrashCommand(plugin));
        registerSubCommand(new ReloadCommand(plugin));
        registerSubCommand(new MenuCommand(plugin));
        registerSubCommand(new DataCollectCommand(plugin));
        registerSubCommand(new ClearDataCommand(plugin));
        registerSubCommand(new TrainCommand(plugin));
        registerSubCommand(new TrainStatusCommand(plugin));
        registerSubCommand(new DataStatsCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("unknown-subcommand"));
            return true;
        }

        if (!sender.hasPermission("solarai.admin") && !subCommandName.equals("token")) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("no-permission"));
            return true;
        }

        String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subCommandArgs);

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        String header = plugin.getLocaleManager().getMessage("help.header");
        String pattern = plugin.getLocaleManager().getMessage("help.entry");

        boolean isPlayer = sender instanceof Player;
        if (!isPlayer) {
            header = ChatColor.stripColor(header);
            pattern = ChatColor.stripColor(pattern);
        }

        sender.sendMessage(header);

        for (SubCommand subCommand : subCommands.values()) {
            String usage = subCommand.getUsage();
            String description = subCommand.getDescription();

            if (!isPlayer) {
                usage = ChatColor.stripColor(usage);
                description = ChatColor.stripColor(description);
            }

            String line = pattern
                    .replace("%usage%", usage)
                    .replace("%description%", description);
            sender.sendMessage(line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("solarai.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], subCommands.keySet(), new ArrayList<>());
        }

        if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.onTabComplete(sender, subCommandArgs);
            }
        }

        return Collections.emptyList();
    }
}

