package com.g4vrk.react.player.model.rotation;

import lombok.Value;

@Value
public class Rotation {

    float pitch, yaw;
    float deltaPitch, deltaYaw;

    float jerkPitch, jerkYaw;
    float gcdErrorPitch, gcdErrorYaw;

}