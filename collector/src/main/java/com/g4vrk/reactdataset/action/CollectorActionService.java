package com.g4vrk.reactdataset.action;

import com.g4vrk.functionalActions.list.ExecutableActionList;
import com.g4vrk.functionalActions.parser.ActionParser;
import com.g4vrk.config.Config;
import com.g4vrk.react.React;
import com.g4vrk.schedula.task.TickSchedule;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.spongepowered.configurate.serialize.SerializationException;

public final class CollectorActionService {
    private final ActionParser<Audience> parser;
    private final Map<String, ExecutableActionList<? super Audience>> actions = new ConcurrentHashMap<String, ExecutableActionList<? super Audience>>();

    public CollectorActionService(ActionParser<Audience> parser) {
        this.parser = parser;
    }

    public void reload(Config config) {
        this.actions.clear();
        this.load(config, "help", "on-show");
        this.load(config, "recording", "on-start");
        this.load(config, "recording", "on-stop");
        this.load(config, "recording", "on-file");
        this.load(config, "recording", "on-complete");
        this.load(config, "recording", "on-error");
        this.load(config, "recording", "on-invalid-label");
        this.load(config, "recording", "on-player-not-found");
        this.load(config, "recording", "on-invalid-count");
        this.load(config, "recording", "on-already-recording");
        this.load(config, "recording", "on-not-recording");
        this.loadWithFallback(config, "recording", "on-resume", "player-on-resume");
        this.load(config, "status", "header");
        this.load(config, "status", "entry");
        this.load(config, "status", "directory");
        this.load(config, "status", "upload");
        this.load(config, "upload", "on-start");
        this.load(config, "upload", "on-disabled");
        this.load(config, "reload", "on-start");
        this.load(config, "reload", "on-finish");
        this.load(config, "reload", "on-error");
    }

    private void loadWithFallback(Config config, String section, String key, String fallbackKey) {
        String id = section + "." + key;
        try {
            List<String> values = config.node(new Object[]{section, key}).getList(String.class, Collections.emptyList());
            if (values.isEmpty()) {
                values = config.node(new Object[]{section, fallbackKey}).getList(String.class, Collections.emptyList());
            }
            this.actions.put(id, (ExecutableActionList<? super Audience>)this.parser.parseAll(values));
        }
        catch (SerializationException ex) {
            throw new IllegalStateException("Failed to load action list: " + id, ex);
        }
    }

    private void load(Config config, String section, String key) {
        String id = section + "." + key;
        try {
            this.actions.put(id, (ExecutableActionList<? super Audience>)this.parser.parseAll(config.node(new Object[]{section, key}).getList(String.class, Collections.emptyList())));
        }
        catch (SerializationException ex) {
            throw new IllegalStateException("Failed to load action list: " + id, ex);
        }
    }

    public void run(String id, CommandSender sender) {
        this.run(id, sender, Collections.emptyMap());
    }

    public void run(String id, CommandSender sender, Map<String, String> placeholders) {
        ExecutableActionList<? super Audience> list = this.actions.get(id);
        if (list == null || list.isEmpty() || sender == null) {
            return;
        }
        Runnable execute = () -> list.run(sender, input -> CollectorActionService.replace(input, placeholders));
        if (Bukkit.isPrimaryThread()) {
            execute.run();
        } else {
            React.INSTANCE.getScheduler().schedule(execute, TickSchedule.instant());
        }
    }

    private static String replace(String input, Map<String, String> placeholders) {
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
