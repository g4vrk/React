package com.g4vrk.react.api.addon;

import com.g4vrk.react.api.addon.meta.AddonMetadata;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public abstract class JavaAddon implements Addon {

    private boolean initialized = false;
    private boolean enabled = false;

    private File file;
    private File dir;
    private Logger logger;
    private AddonMetadata metadata;
    private ClassLoader classLoader;

    public JavaAddon() {
    }

    @ApiStatus.Internal
    public final void init(
            final @NotNull File file,
            final @NotNull File dir,
            final @NotNull AddonMetadata metadata,
            final @NotNull Logger logger,
            final @NotNull ClassLoader classLoader
    ) {
        if (this.initialized) return;

        this.file = file;
        this.dir = dir;
        this.metadata = metadata;
        this.logger = logger;
        this.classLoader = classLoader;

        this.initialized = true;
    }

    @Override
    public @NotNull File directory() {
        return dir;
    }

    @Override
    public @NotNull Logger slf4jLogger() {
        return logger;
    }

    @Override
    public @NotNull String name() {
        return metadata().name();
    }

    @Override
    public @NotNull AddonMetadata metadata() {
        return metadata;
    }

    public final void setEnabled(
            final boolean enabled
    ) {
        if (this.enabled != enabled) {
            this.enabled = enabled;

            if (this.enabled) {
                this.onEnable();
            } else {
                this.onDisable();
            }
        }
    }

    @Override
    public @Nullable InputStream getResource(@NotNull String resourcePath) {
        try {
            final URL url = classLoader().getResource(resourcePath);

            if (url == null) {
                return null;
            }

            final URLConnection connection = url.openConnection();

            connection.setUseCaches(false);

            return connection.getInputStream();

        } catch (final IOException ex) {

            return null;

        }
    }

    @Override
    public void saveResource(@NotNull String resourcePath, boolean replace) {

        if (resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        final InputStream in = this.getResource(resourcePath);

        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + file);
        }

        final File outFile = new File(dir, resourcePath);

        int lastIndex = resourcePath.lastIndexOf('/');

        final File outDir = new File(dir, resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } else {
                logger.warn("Could not save {} to {} because {} already exists.", outFile.getName(), outFile, outFile.getName());
            }
        } catch (IOException ex) {
            logger.error("Could not save {} to {}", outFile.getName(), outFile, ex);
        }
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    public final @NotNull ClassLoader classLoader() {
        return classLoader;
    }
}
