package com.g4vrk.react.check;

import lombok.AccessLevel;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Setter(AccessLevel.PROTECTED)
public abstract class AbstractCheck implements ReactCheck {

    private String name = "DEFAULT";
    private boolean experimental;

    public AbstractCheck() {
    }

    public AbstractCheck(
            @NotNull String name,
            boolean experimental
    ) {
        this.name = name;
        this.experimental = experimental;
    }

    @Override
    public @NotNull String getName() {
        return Objects.requireNonNull(name);
    }

    @Override
    public boolean experimental() {
        return experimental;
    }
}
