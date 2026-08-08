package com.g4vrk.react.placeholder.engine;

import com.g4vrk.react.placeholder.Closure;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractPlaceholderEngine implements PlaceholderEngine {

    private final Closure closure;
    private final Pattern pattern;

    protected AbstractPlaceholderEngine(
            @NotNull Closure closure
    ) {
        this.closure = closure;

        this.pattern = Pattern.compile(
                Pattern.quote(closure.head()) +
                "([^" + Pattern.quote(closure.head() + closure.tail()) + "]+)" +
                Pattern.quote(closure.tail())
        );
    }

    protected abstract @NotNull String resolve(
            @NotNull OfflinePlayer player,
            @NotNull String placeholder
    );

    @Override
    public @NotNull String process(
            final @NotNull OfflinePlayer player,
            final @NotNull String text
    ) {
        final Matcher matcher = this.pattern.matcher(text);
        final StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            final String placeholder = matcher.group(1);
            final String replacement = resolve(player, placeholder);

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(replacement)
            );
        }

        matcher.appendTail(result);

        return result.toString();
    }

    @Override
    public @NotNull Component process(
            final @NotNull OfflinePlayer player,
            final @NotNull Component component
    ) {
        return component.replaceText(builder ->
                builder
                        .match(this.pattern)
                        .replacement((match, ignored) ->
                                Component.text(
                                        resolve(player, match.group(1))
                                )
                        )
        );
    }

    protected final @NotNull Closure closure() {
        return this.closure;
    }
}