package com.g4vrk.react.api.rcp.exception;

import org.jetbrains.annotations.NotNull;

public class InvalidRCPException extends RuntimeException {

    public InvalidRCPException() {
    }

    public InvalidRCPException(
            @NotNull String message
    ) {
        super(message);
    }

    public InvalidRCPException(
            @NotNull String message,
            @NotNull Throwable cause
    ) {
        super(message, cause);
    }

    public InvalidRCPException(
            @NotNull Throwable cause
    ) {
        super(cause);
    }

}
