package com.g4vrk.react.placeholder;

import org.jetbrains.annotations.NotNull;

public record Closure(@NotNull String head, @NotNull String tail) {

    private static final Closure BRACKETS = new Closure("{", "}");
    private static final Closure PERCENT = new Closure("%", "%");
    private static final Closure TAG_LIKE = new Closure("<", ">");

    public @NotNull String applyTo(
            final @NotNull String text
    ) {
        return head + text + tail;
    }

    public static @NotNull Closure brackets() {
        return BRACKETS;
    }

    public static @NotNull Closure percent() {
        return PERCENT;
    }

    public static @NotNull Closure tagLike() {
        return TAG_LIKE;
    }

}