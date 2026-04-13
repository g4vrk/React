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

public class DataStatsCommand extends SubCommand {

    public DataStatsCommand(SolarAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "datastats";
    }

    @Override
    public String getDescription() {
        return "Показать статистику собранных данных";
    }

    @Override
    public String getUsage() {
        return "/solar datastats";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                OkHttpClient client = plugin.getHttpClient();
                
                Request request = new Request.Builder()
                    .url(plugin.getServerUrl() + "/stats")
                    .get()
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        
                        Moshi moshi = new Moshi.Builder().build();
                        Type type = Types.newParameterizedType(Map.class, String.class, Object.class);
                        JsonAdapter<Map<String, Object>> adapter = moshi.adapter(type);
                        
                        Map<String, Object> stats = adapter.fromJson(responseBody);
                        
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (stats != null) {
                                int totalDatasets = ((Double) stats.getOrDefault("total_datasets", 0.0)).intValue();
                                int cheaterDatasets = ((Double) stats.getOrDefault("cheater_datasets", 0.0)).intValue();
                                int legitDatasets = ((Double) stats.getOrDefault("legit_datasets", 0.0)).intValue();
                                
                                sender.sendMessage(ChatColor.YELLOW + "=== Статистика данных ===");
                                sender.sendMessage(ChatColor.GREEN + "Всего датасетов: " + ChatColor.WHITE + totalDatasets);
                                sender.sendMessage(ChatColor.RED + "Читеры: " + ChatColor.WHITE + cheaterDatasets);
                                sender.sendMessage(ChatColor.GREEN + "Легиты: " + ChatColor.WHITE + legitDatasets);
                                
                                if (totalDatasets > 0) {
                                    double cheaterPercent = (cheaterDatasets * 100.0) / totalDatasets;
                                    double legitPercent = (legitDatasets * 100.0) / totalDatasets;
                                    sender.sendMessage(ChatColor.GRAY + "Соотношение: " + 
                                        String.format("%.1f%% читеры, %.1f%% легиты", cheaterPercent, legitPercent));
                                }
                                
                                if (totalDatasets >= 5) {
                                    sender.sendMessage(ChatColor.GOLD + "✓ Достаточно данных для обучения!");
                                } else {
                                    sender.sendMessage(ChatColor.YELLOW + "⚠ Рекомендуется собрать минимум 5 датасетов");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "Не удалось получить статистику");
                            }
                        });
                    } else {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(ChatColor.RED + "Ошибка получения статистики: " + response.code());
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