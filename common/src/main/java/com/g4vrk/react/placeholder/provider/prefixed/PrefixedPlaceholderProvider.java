package com.g4vrk.react.placeholder.provider.prefixed;

import com.g4vrk.react.placeholder.provider.PlaceholderProvider;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class PrefixedPlaceholderProvider
        implements PlaceholderProvider {

    private final String prefix;
    private final Map<String, BiFunction<OfflinePlayer, String, String>> replacements = new Object2ObjectOpenHashMap<>();

    protected PrefixedPlaceholderProvider(
            @NotNull String prefix
    ) {
        this.prefix = prefix;
    }

    public final @NotNull String prefix() {
        return prefix;
    }

    public final @NotNull PrefixedPlaceholderProvider replacement(
            final @NotNull String placeholder,
            final @NotNull BiFunction<OfflinePlayer, String, String> replacement
    ) {
        this.replacements.put(placeholder, replacement);
        return this;
    }

    @Override
    public @NotNull String provide(
            @NotNull OfflinePlayer player,
            @NotNull String placeholder
    ) {
        final int separator = placeholder.indexOf('.');

        if (separator == -1 ||
                !placeholder.regionMatches(0, prefix, 0, separator)) {
            return placeholder;
        }

        final String key = placeholder.substring(separator + 1);

        final BiFunction<OfflinePlayer, String, String> replacement = replacements.get(key);

        if (replacement == null) {
            return placeholder;
        }

        return replacement.apply(player, placeholder);
    }
}