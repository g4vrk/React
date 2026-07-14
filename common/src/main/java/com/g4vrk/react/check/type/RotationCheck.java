package com.g4vrk.react.check.type;

import com.g4vrk.react.check.ReactCheck;
import com.g4vrk.react.player.model.rotation.RotationData;
import org.jetbrains.annotations.NotNull;

public interface RotationCheck extends ReactCheck {

    default void onRotation(@NotNull RotationData currentData) {
    }

}
