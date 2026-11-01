package com.g4vrk.react.api.rcp.internal;

import com.g4vrk.react.api.rcp.RCPLifecycle;
import com.g4vrk.react.api.rcp.environment.RCPEnvironment;
import com.g4vrk.react.api.rcp.exception.InvalidRCPException;
import com.g4vrk.react.api.rcp.provider.RCPProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

public class JavaRCPClassLoader extends URLClassLoader {

    private final RCPLifecycle lifecycle;

    public JavaRCPClassLoader(
            @Nullable ClassLoader parent,
            @NotNull String mainClass,
            @NotNull URL[] urls,
            @NotNull RCPEnvironment environment
    ) throws InvalidRCPException {

        super(urls, parent);

        try {

            this.lifecycle = createLifecycle(mainClass, environment);

        } catch (final InvalidRCPException | LinkageError ex) {

            try {

                close();

            } catch (final IOException closeException) {

                ex.addSuppressed(closeException);

            }

            throw ex;
        }
    }

    private @NotNull RCPLifecycle createLifecycle(
            @NotNull String mainClass,
            @NotNull RCPEnvironment environment
    ) throws InvalidRCPException {

        final Class<?> lifecycleMainClass;

        try {

            lifecycleMainClass = loadClass(mainClass);

        } catch (final ClassNotFoundException ex) {

            throw new InvalidRCPException(
                    "Cannot find RCP main class '" + mainClass + "'",
                    ex
            );
        }

        final Class<? extends RCPLifecycle> lifecycleClass;

        try {

            lifecycleClass = lifecycleMainClass.asSubclass(RCPLifecycle.class);

        } catch (final ClassCastException ex) {

            throw new InvalidRCPException(
                    "RCP main class '" + mainClass + "' does not implement RCPLifecycle",
                    ex
            );
        }

        try {

            if (RCPProvider.class.isAssignableFrom(lifecycleClass)) {

                return lifecycleClass
                        .asSubclass(RCPProvider.class)
                        .getDeclaredConstructor(RCPEnvironment.class)
                        .newInstance(environment);
            }

            return lifecycleClass
                    .getDeclaredConstructor()
                    .newInstance();

        } catch (final IllegalAccessException ex) {

            throw new InvalidRCPException(
                    "Cannot access the constructor of RCP main class '" + mainClass + "'",
                    ex
            );

        } catch (final InstantiationException ex) {

            throw new InvalidRCPException("Abnormal RCP lifecycle type", ex);

        } catch (final InvocationTargetException ex) {

            throw new InvalidRCPException(
                    "Cannot initialize RCP main class '" + mainClass + "'",
                    ex.getCause() == null ? ex : ex.getCause()
            );

        } catch (final NoSuchMethodException ex) {

            throw new InvalidRCPException(
                    "Cannot find a suitable constructor for RCP main class '" + mainClass + "'",
                    ex
            );
        }
    }

    public @NotNull RCPLifecycle lifecycle() {
        return lifecycle;
    }
}