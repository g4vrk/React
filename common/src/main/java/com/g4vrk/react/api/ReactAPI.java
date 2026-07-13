package com.g4vrk.react.api;

import com.g4vrk.react.api.task.runner.factory.TaskRunnerFactory;
import org.jetbrains.annotations.NotNull;

public interface ReactAPI {

    @NotNull TaskRunnerFactory getTaskRunnerFactory();

}
