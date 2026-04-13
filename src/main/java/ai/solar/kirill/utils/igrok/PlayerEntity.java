package ai.solar.kirill.utils.igrok;

import ai.solar.kirill.utils.game.Frame;
import org.bukkit.scheduler.BukkitTask;
import ai.solar.kirill.SolarAI;
import ai.solar.kirill.main.service.AnalyseServise;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public final class PlayerEntity {
    private final UUID uuid;
    private final String name;

    private float lastYaw = 0.0F;
    private float lastPitch = 0.0F;
    private float lastDeltaYaw = 0.0F;
    private float lastDeltaPitch = 0.0F;
    private float lastAccelYaw = 0.0F;
    private float lastAccelPitch = 0.0F;
    private final List<Frame> frames = new LinkedList<>();
    private List<Frame> lastAnalyzedFrames = null;

    private volatile boolean isProcessingFlag = false;
    private long combatTagUntil = 0L;
    private BukkitTask postCombatAnalysisTask = null;

    public PlayerEntity(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public void setLastYaw(float lastYaw) {
        this.lastYaw = lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public void setLastPitch(float lastPitch) {
        this.lastPitch = lastPitch;
    }

    public float getLastDeltaYaw() {
        return lastDeltaYaw;
    }

    public void setLastDeltaYaw(float lastDeltaYaw) {
        this.lastDeltaYaw = lastDeltaYaw;
    }

    public float getLastDeltaPitch() {
        return lastDeltaPitch;
    }

    public void setLastDeltaPitch(float lastDeltaPitch) {
        this.lastDeltaPitch = lastDeltaPitch;
    }

    public float getLastAccelYaw() {
        return lastAccelYaw;
    }

    public void setLastAccelYaw(float lastAccelYaw) {
        this.lastAccelYaw = lastAccelYaw;
    }

    public float getLastAccelPitch() {
        return lastAccelPitch;
    }

    public void setLastAccelPitch(float lastAccelPitch) {
        this.lastAccelPitch = lastAccelPitch;
    }

    public List<Frame> getFrames() {
        return frames;
    }

    public List<Frame> getLastAnalyzedFrames() {
        return lastAnalyzedFrames;
    }

    public void setLastAnalyzedFrames(List<Frame> frames) {
        this.lastAnalyzedFrames = new ArrayList<>(frames);
    }

    public boolean isProcessingFlag() {
        return isProcessingFlag;
    }

    public void setProcessingFlag(boolean processingFlag) {
        isProcessingFlag = processingFlag;
    }

    public boolean isInCombat() {
        return System.currentTimeMillis() < combatTagUntil;
    }

    public void tagCombat(long durationTicks) {
        this.combatTagUntil = System.currentTimeMillis() + (durationTicks * 50);

        if (postCombatAnalysisTask != null) {
            postCombatAnalysisTask.cancel();
        }

        postCombatAnalysisTask = SolarAI.getInstance().getServer().getScheduler().runTaskLaterAsynchronously(
                SolarAI.getInstance(),
                () -> {
                    int framesToAnalyze = SolarAI.getInstance().getConfig().getInt("neural-processor.collection.frame-count", 150);
                    if (this.getFrames().size() >= framesToAnalyze) {
                        AnalyseServise.analyze(this);
                    }
                    this.getFrames().clear();
                },
                durationTicks
        );
    }
}