package com.g4vrk.react.ml.auth;

import com.g4vrk.react.ml.auth.type.AuthType;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class AuthSettings {

    boolean enabled;
    @NotNull AuthType type;

    @NotNull String value;

    @NotNull String headerName;

    @NotNull String queryParameter;

    @NotNull String bodyField;

}
