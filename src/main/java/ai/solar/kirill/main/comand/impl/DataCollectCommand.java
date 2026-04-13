package ai.solar.kirill.main.comand.impl;

import ai.solar.kirill.SolarAI;
import ai.solar.kirill.main.comand.SubCommand;
import ai.solar.kirill.main.service.DataCollectionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DataCollectCommand extends SubCommand {

    public DataCollectCommand(SolarAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "datacollect";
    }

    @Override
    public String getDescription() {
        return "Начать сбор данных для обучения модели";
    }

    @Override
    public String getUsage() {
        return "/solar datacollect <cheat/legit> <ник> [количество_фреймов]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(ChatColor.RED + "Использование: " + getUsage());
            return;
        }

        String type = args[0].toLowerCase();
        String playerName = args[1];
        int maxFrames = 1000;

        if (!type.equals("cheat") && !type.equals("legit")) {
            sender.sendMessage(ChatColor.RED + "Тип должен быть 'cheat' или 'legit'");
            return;
        }

        if (args.length == 3) {
            try {
                maxFrames = Integer.parseInt(args[2]);
                if (maxFrames <= 0) {
                    sender.sendMessage(ChatColor.RED + "Количество фреймов должно быть больше 0!");
                    return;
                }
                if (maxFrames > 100000) {
                    sender.sendMessage(ChatColor.RED + "Максимальное количество фреймов: 100000!");
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Неверное количество фреймов! Используйте число.");
                return;
            }
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок " + playerName + " не найден!");
            return;
        }

        UUID initiatorId;
        if (sender instanceof Player) {
            initiatorId = ((Player) sender).getUniqueId();
        } else {
            sender.sendMessage(ChatColor.RED + "Эта команда может быть выполнена только игроком!");
            return;
        }

        boolean isCheater = type.equals("cheat");
        DataCollectionService service = plugin.getDataCollectionService();
        
        if (service.isCollecting(target.getUniqueId())) {
            service.stopCollection(target.getUniqueId());
            sender.sendMessage(ChatColor.YELLOW + "Сбор данных для " + target.getName() + " остановлен");
        } else {
            service.startCollection(target.getUniqueId(), target.getName(), isCheater, initiatorId, maxFrames);
            sender.sendMessage(ChatColor.GREEN + "Начат сбор данных для " + target.getName() + 
                " как " + (isCheater ? "читер" : "легит") + " (цель: " + maxFrames + " фреймов)");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("cheat", "legit");
        } else if (args.length == 2) {
            return null;
        } else if (args.length == 3) {
            return Arrays.asList("100", "500", "1000", "2000", "5000");
        }
        return super.onTabComplete(sender, args);
    }
}