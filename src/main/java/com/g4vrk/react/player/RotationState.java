package com.g4vrk.react.player;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public final class RotationState {
    private float yaw;
    private float pitch;

    private float deltaYaw;
    private float deltaPitch;

    private float accelYaw;
    private float accelPitch;
}