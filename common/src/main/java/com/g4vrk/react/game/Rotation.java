package com.g4vrk.react.game;

import lombok.Value;

@Value
public class Rotation {

    float pitch, yaw;
    float deltaPitch, deltaYaw;

    float jerkPitch, jerkYaw;
    float gcdErrorPitch, gcdErrorYaw;

}