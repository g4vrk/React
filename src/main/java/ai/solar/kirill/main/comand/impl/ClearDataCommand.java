package ai.solar.kirill.main.comand.impl;

import ai.solar.kirill.SolarAI;
import ai.solar.kirill.main.comand.SubCommand;
import okhttp3.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ClearDataCommand extends SubCommand {

    public ClearDataCommand(SolarAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "cleardata";
    }

    @Override
    public String getDescription() {
        return "Очистить все данные и модели для нового обучения";
    }

    @Override
    public String getUsage() {
        return "/solar cleardata";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Очистка данных и моделей...");
       
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                OkHttpClient client = plugin.getHttpClient();
                
                Request clearDataRequest = new Request.Builder()
                    .url(plugin.getServerUrl() + "/admin/clear_training_data")
                    .post(RequestBody.create("", MediaType.get("application/json")))
                    .build();
                
                try (Response response = client.newCall(clearDataRequest).execute()) {
                    if (response.isSuccessful()) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(ChatColor.GREEN + "✓ Тренировочные данные очищены");
                        });
                    } else {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(ChatColor.RED + "✗ Ошибка очистки данных: " + response.code());
                        });
                    }
                }
                
                Request clearModelsRequest = new Request.Builder()
                    .url(plugin.getServerUrl() + "/admin/clear_models")
                    .post(RequestBody.create("", MediaType.get("application/json")))
                    .build();
                
                try (Response response = client.newCall(clearModelsRequest).execute()) {
                    if (response.isSuccessful()) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(ChatColor.GREEN + "✓ Модели удалены");
                            sender.sendMessage(ChatColor.GOLD + "Система готова к сбору новых данных!");
                        });
                    } else {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(ChatColor.RED + "✗ Ошибка удаления моделей: " + response.code());
                        });
                    }
                }
                
            } catch (IOException e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "Ошибка соединения с сервером: " + e.getMessage());
                });
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}