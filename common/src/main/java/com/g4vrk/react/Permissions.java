package com.g4vrk.react;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

@UtilityClass
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
public class Permissions {

    public @NotNull String PERMISSION_PREFIX = "react.";

    public @NotNull String ALERTS = PERMISSION_PREFIX + "alerts";
    public @NotNull String ALERTS_ENABLE_ON_JOIN = ALERTS + ".enable_on_join";

    public @NotNull String BASE_BYPASS = PERMISSION_PREFIX + "bypass";

    public boolean hasBypassForCheck(
            final @NotNull Permissible permissible,
            final @NotNull String checkName
    ) {
        return permissible.hasPermission(BASE_BYPASS + "." + checkName);
    }
}

