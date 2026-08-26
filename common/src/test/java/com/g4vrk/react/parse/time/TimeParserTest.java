package com.g4vrk.react.parse.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeParserTest {

    @Test
    void parsesMilliseconds() {
        assertEquals(Duration.ofMillis(250), TimeParser.parseDuration("250ms"));
    }
}
