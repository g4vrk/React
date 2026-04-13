package ai.solar.kirill.main.comand.impl;

import ai.solar.kirill.SolarAI;
import ai.solar.kirill.main.comand.SubCommand;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okhttp3.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TrainStatusCommand extends SubCommand {

    public TrainStatusCommand(SolarAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "trainstatus";
    }

    @Override
    public String getDescription() {
        return "Проверить статус обучения модели";
    }

    @Override
    public String getUsage() {
        return "/solar trainstatus";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                OkHttpClient client = plugin.getHttpClient();
                
                Request request = new Request.Builder()
                    .url(plugin.getServerUrl() + "/training_progress")
                    .get()
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        
                        Moshi moshi = new Moshi.Builder().build();
                        Type type = Types.newParameterizedType(Map.class, String.class, Object.class);
                        JsonAdapter<Map<String, Object>> adapter = moshi.adapter(type);
                        
                        Map<String, Object> progress = adapter.fromJson(responseBody);
                        
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (progress != null) {
                                boolean isTraining = (Boolean) progress.getOrDefault("is_training", false);
                                
                                if (isTraining) {
                                    int currentEpoch = ((Double) progress.getOrDefault("current_epoch", 0.0)).intValue();
                                    int totalEpochs = ((Double) progress.getOrDefault("total_epochs", 0.0)).intValue();
                                    String currentModel = (String) progress.getOrDefault("current_model", "Unknown");
                                    double loss = (Double) progress.getOrDefault("loss", 0.0);
                                    double accuracy = (Double) progress.getOrDefault("accuracy", 0.0);
                                    double valAccuracy = (Double) progress.getOrDefault("val_accuracy", 0.0);
                                    
                                    sender.sendMessage(ChatColor.YELLOW + "=== Статус обучения ===");
                                    sender.sendMessage(ChatColor.GREEN + "Статус: " + ChatColor.WHITE + "Обучается");
                                    sender.sendMessage(ChatColor.GREEN + "Модель: " + ChatColor.WHITE + currentModel);
                                    sender.sendMessage(ChatColor.GREEN + "Эпоха: " + ChatColor.WHITE + currentEpoch + "/" + totalEpochs);
                                    sender.sendMessage(ChatColor.GREEN + "Потери: " + ChatColor.WHITE + String.format("%.4f", loss));
                                    sender.sendMessage(ChatColor.GREEN + "Точность (тренировка): " + ChatColor.WHITE + String.format("%.2f%%", accuracy * 100));
                                    sender.sendMessage(ChatColor.GREEN + "Точность (валидация): " + ChatColor.WHITE + String.format("%.2f%%", valAccuracy * 100));
                                } else {
                                    sender.sendMessage(ChatColor.YELLOW + "=== Статус обучения ===");
                                    sender.sendMessage(ChatColor.RED + "Статус: " + ChatColor.WHITE + "Не обучается");
                                    sender.sendMessage(ChatColor.GRAY + "Используйте /solar train для запуска обучения");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "Не удалось получить статус обучения");
                            }
                        });
                    } else {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(ChatColor.RED + "Ошибка получения статуса: " + response.code());
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