package ai.solar.kirill.main.comand.impl;

import ai.solar.kirill.SolarAI;
import ai.solar.kirill.main.comand.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MenuCommand extends SubCommand {

    public MenuCommand(SolarAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "menu";
    }

    @Override
    public String getDescription() {
        return "Открыть меню нарушителей";
    }

    @Override
    public String getUsage() {
        return "/solar menu";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("player-only-command"));
            return;
        }

        Player player = (Player) sender;
        plugin.getMenuManager().openMenu(player, 0);
    }
}
