package com.g4vrk.react.api.addon.internal;

import com.g4vrk.react.api.addon.JavaAddon;
import com.g4vrk.react.api.addon.exception.InvalidAddonException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

public class JavaAddonClassLoader extends URLClassLoader {

    private final JavaAddon javaAddon;

    public JavaAddonClassLoader(
            @Nullable ClassLoader parent,
            @NotNull String mainClass,
            @NotNull URL[] urls
    ) throws InvalidAddonException {

        super(urls, parent);

        try {
            final Class<?> addonMainClass;

            try {

                addonMainClass = loadClass(mainClass);

            } catch (final ClassNotFoundException ex) {

                throw new InvalidAddonException("Cannot find addon main class '" + mainClass + "'", ex);

            }

            final Class<? extends JavaAddon> javaAddonClass;

            try {

                javaAddonClass = addonMainClass.asSubclass(JavaAddon.class);

            } catch (final ClassCastException ex) {

                throw new InvalidAddonException("Addon main class '" + mainClass + "' does not extend JavaAddon", ex);

            }

            this.javaAddon = javaAddonClass.getDeclaredConstructor().newInstance();

        } catch (final IllegalAccessException ex) {

            throw new InvalidAddonException("Cannot access the no-args constructor of addon main class '" + mainClass + "'", ex);

        } catch (final InstantiationException ex) {

            throw new InvalidAddonException("Abnormal addon type", ex);

        } catch (final InvocationTargetException | NoSuchMethodException ex) {

            throw new InvalidAddonException(ex);

        }
    }

    public @NotNull JavaAddon addon() {
        return javaAddon;
    }

}
