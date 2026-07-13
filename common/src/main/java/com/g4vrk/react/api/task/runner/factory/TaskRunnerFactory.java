package com.g4vrk.react.api.task.runner.factory;

import com.g4vrk.react.api.task.runner.TaskRunner;
import org.jetbrains.annotations.NotNull;

public interface TaskRunnerFactory {
    @NotNull TaskRunner create();
}
