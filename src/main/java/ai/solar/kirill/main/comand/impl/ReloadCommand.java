package ai.solar.kirill.main.comand.impl;

import ai.solar.kirill.SolarAI;
import ai.solar.kirill.main.comand.SubCommand;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends SubCommand {

    public ReloadCommand(SolarAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Перезагрузить конфигурацию плагина";
    }

    @Override
    public String getUsage() {
        return "/solar reload";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reloadPluginConfig();
        sender.sendMessage(plugin.getLocaleManager().getMessage("commands.reload.success"));
    }
}
