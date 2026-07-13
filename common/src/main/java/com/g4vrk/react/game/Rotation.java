package com.g4vrk.react.game;

import lombok.Value;

@Value
public class Rotation {

    float x, y;
    float deltaX, deltaY;

    float jerkX, jerkY;
    float gcdErrorX, gcdErrorY;

}