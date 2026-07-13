package com.g4vrk.react.game;

import lombok.Value;

@Value
public class Rotation {
    float x;
    float y;

    float deltaX;
    float deltaY;

    float jerkX;
    float jerkY;

    float gcdErrorX;
    float gcdErrorY;
}