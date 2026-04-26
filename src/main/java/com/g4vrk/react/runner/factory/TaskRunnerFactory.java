package com.g4vrk.react.runner.factory;

import com.g4vrk.react.runner.TaskRunner;
import org.jetbrains.annotations.NotNull;

public interface TaskRunnerFactory {
    @NotNull TaskRunner create();
}
