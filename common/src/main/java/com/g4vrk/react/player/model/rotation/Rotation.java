package com.g4vrk.react.player.model.rotation;

import lombok.Value;

@Value
public class Rotation {

    float y, x;
    float deltaY, deltaX;

    float jerkY, jerkX;
    float gcdErrorY, gcdErrorX;

}