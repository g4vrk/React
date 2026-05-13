package com.g4vrk.react.version;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FoliaChecker {
    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }
}
