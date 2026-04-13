package ai.solar.kirill.main.service;

import ai.solar.kirill.SolarAI;
import ai.solar.kirill.utils.game.Frame;
import ai.solar.kirill.utils.igrok.PlayerEntity;
import ai.solar.kirill.utils.igrok.PlayerRegistry;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

public class DataCollectionService {
    
    private final SolarAI plugin;
    private final Map<UUID, CollectionSession> activeSessions = new ConcurrentHashMap<>();
    private final OkHttpClient httpClient;
    private final JsonAdapter<Map<String, Object>> jsonAdapter;
    
    public DataCollectionService(SolarAI plugin) {
        this.plugin = plugin;
        this.httpClient = plugin.getHttpClient();
        
        Moshi moshi = new Moshi.Builder().build();
        Type type = Types.newParameterizedType(Map.class, String.class, Object.class);
        this.jsonAdapter = moshi.adapter(type);
    }
    
    public void startCollection(UUID playerId, String playerName, boolean isCheater, UUID initiatorId, int maxFrames) {
        Player player = Bukkit.getPlayer(playerId);
        Player initiator = Bukkit.getPlayer(initiatorId);
        if (player == null || initiator == null) return;
        
        stopCollection(playerId);
        
        CollectionSession session = new CollectionSession(playerId, playerName, isCheater, initiatorId, maxFrames);
        activeSessions.put(playerId, session);
        
        BossBar bossBar = Bukkit.createBossBar(
            ChatColor.GOLD + "Сбор данных: " + (isCheater ? "ЧИТЕР" : "ЛЕГИТ") + " | Игрок: " + playerName + " | Фреймов: 0/" + maxFrames,
            isCheater ? BarColor.RED : BarColor.GREEN,
            BarStyle.SOLID
        );
        bossBar.addPlayer(initiator);
        session.setBossBar(bossBar);
        
        BukkitTask updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            updateBossBar(session);
        }, 0L, 20L); 
        session.setUpdateTask(updateTask);
        
        initiator.sendMessage(ChatColor.GREEN + "Начат сбор данных с игрока " + playerName + "! Цель: " + maxFrames + " фреймов.");
    }
    
    public void stopCollection(UUID playerId) {
        CollectionSession session = activeSessions.remove(playerId);
        if (session == null) return;
        
        Player initiator = Bukkit.getPlayer(session.getInitiatorId());
        
        if (session.getUpdateTask() != null) {
            session.getUpdateTask().cancel();
        }
        
        if (session.getBossBar() != null) {
            session.getBossBar().removeAll();
        }
        
        if (session.getCollectedFrames() > 0) {
            uploadCollectedData(session);
            if (initiator != null) {
                initiator.sendMessage(ChatColor.YELLOW + "Сбор данных с игрока " + session.getPlayerName() + " завершен! Собрано фреймов: " + session.getCollectedFrames());
            }
        }
    }
    
    public boolean isCollecting(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }
    
    public void onFrameCollected(UUID playerId, Frame frame) {
        CollectionSession session = activeSessions.get(playerId);
        if (session == null) return;
        
        session.addFrame(frame);
        
        if (session.getCollectedFrames() >= session.getMaxFrames()) {
            stopCollection(playerId);
        }
    }
    
    private void updateBossBar(CollectionSession session) {
        if (session.getBossBar() == null) return;
        
        Player initiator = Bukkit.getPlayer(session.getInitiatorId());
        if (initiator == null) {
            stopCollection(session.getPlayerId());
            return;
        }
        
        int frames = session.getCollectedFrames();
        double progress = Math.min(1.0, (double) frames / session.getMaxFrames());
        
        session.getBossBar().setProgress(progress);
        session.getBossBar().setTitle(
            ChatColor.GOLD + "Сбор данных: " + (session.isCheater() ? "ЧИТЕР" : "ЛЕГИТ") + 
            " | Игрок: " + session.getPlayerName() + " | Фреймов: " + frames + "/" + session.getMaxFrames()
        );
    }
    
    private void uploadCollectedData(CollectionSession session) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
          
                PlayerEntity entity = PlayerRegistry.getPlayer(session.getPlayerId());
                if (entity == null || entity.getFrames().isEmpty()) return;
                
                List<Frame> frames = entity.getFrames();
                
                List<Map<String, Object>> frameData = new ArrayList<>();
                for (Frame frame : frames) {
                    Map<String, Object> frameMap = new HashMap<>();
                    frameMap.put("x", frame.getX());
                    frameMap.put("y", frame.getY());
                    frameMap.put("deltaX", frame.getDeltaX());
                    frameMap.put("deltaY", frame.getDeltaY());
                    frameMap.put("jerkX", frame.getJerkX());
                    frameMap.put("jerkY", frame.getJerkY());
                    frameMap.put("gcdErrorX", frame.getGcdErrorX());
                    frameMap.put("gcdErrorY", frame.getGcdErrorY());
                    frameData.add(frameMap);
                }
                
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("player_name", session.getPlayerName());
                requestData.put("is_cheater", session.isCheater());
                requestData.put("frames", frameData);
                
                String json = jsonAdapter.toJson(requestData);
                
                RequestBody body = RequestBody.create(
                    json, 
                    MediaType.get("application/json; charset=utf-8")
                );
                
                Request request = new Request.Builder()
                    .url(plugin.getServerUrl() + "/upload")
                    .post(body)
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                     
                    } else {
                      
                    }
                }
                
            } catch (IOException e) {
           
            }
        });
    }
    
    public void shutdown() {
       
        for (UUID playerId : activeSessions.keySet()) {
            stopCollection(playerId);
        }
    }
    
    private static class CollectionSession {
        private final UUID playerId;
        private final String playerName;
        private final boolean isCheater;
        private final UUID initiatorId;
        private final int maxFrames;
        private int collectedFrames = 0;
        private BossBar bossBar;
        private BukkitTask updateTask;
        
        public CollectionSession(UUID playerId, String playerName, boolean isCheater, UUID initiatorId, int maxFrames) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.isCheater = isCheater;
            this.initiatorId = initiatorId;
            this.maxFrames = maxFrames;
        }
        
        public void addFrame(Frame frame) {
            collectedFrames++;
        }
        
        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public boolean isCheater() { return isCheater; }
        public UUID getInitiatorId() { return initiatorId; }
        public int getMaxFrames() { return maxFrames; }
        public int getCollectedFrames() { return collectedFrames; }
        public BossBar getBossBar() { return bossBar; }
        public void setBossBar(BossBar bossBar) { this.bossBar = bossBar; }
        public BukkitTask getUpdateTask() { return updateTask; }
        public void setUpdateTask(BukkitTask updateTask) { this.updateTask = updateTask; }
    }
}