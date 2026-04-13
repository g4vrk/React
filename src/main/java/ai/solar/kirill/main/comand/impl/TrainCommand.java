package ai.solar.kirill.main.comand.impl;

import ai.solar.kirill.SolarAI;
import ai.solar.kirill.main.comand.SubCommand;
import okhttp3.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class TrainCommand extends SubCommand {

    public TrainCommand(SolarAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "train";
    }

    @Override
    public String getDescription() {
        return "Запустить обучение модели на собранных данных";
    }

    @Override
    public String getUsage() {
        return "/solar train";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Запуск обучения модели...");
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                OkHttpClient client = plugin.getHttpClient();
                
                Request request = new Request.Builder()
                    .url(plugin.getServerUrl() + "/train")
                    .post(RequestBody.create("", MediaType.get("application/json")))
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(ChatColor.GREEN + "✓ Обучение модели запущено!");
                            sender.sendMessage(ChatColor.GOLD + "Процесс может занять несколько минут...");
                            sender.sendMessage(ChatColor.GRAY + "Проверить прогресс: /solar trainstatus");
                        });
                    } else {
                        String errorMsg = response.body() != null ? response.body().string() : "Unknown error";
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(ChatColor.RED + "✗ Ошибка запуска обучения: " + response.code());
                            sender.sendMessage(ChatColor.RED + "Детали: " + errorMsg);
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