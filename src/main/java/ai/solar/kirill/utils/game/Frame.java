package ai.solar.kirill.utils.game;

import lombok.Value;

@Value
public class Frame {
    float x;
    float y;
    float deltaX;
    float deltaY;
    float jerkX;
    float jerkY;
    float gcdErrorX;
    float gcdErrorY;
}