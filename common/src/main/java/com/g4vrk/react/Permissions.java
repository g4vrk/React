package com.g4vrk.react;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

@UtilityClass
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
public class Permissions {

    @NotNull String PERMISSION_PREFIX = "react.";

    @NotNull String ALERTS = PERMISSION_PREFIX + "alerts";
    @NotNull String ALERTS_ENABLE_ON_JOIN = ALERTS + ".enable_on_join";

    @NotNull String VERBOSE = PERMISSION_PREFIX + "verbose";
    @NotNull String VERBOSE_ENABLE_ON_JOIN = VERBOSE + ".enable_on_join";
    
    @NotNull String BASE_BYPASS = PERMISSION_PREFIX + "bypass";

    boolean hasBypassForCheck(
            final @NotNull Permissible permissible,
            final @NotNull String checkName
    ) {
        return permissible.hasPermission(BASE_BYPASS + "." + checkName);
    }
}

