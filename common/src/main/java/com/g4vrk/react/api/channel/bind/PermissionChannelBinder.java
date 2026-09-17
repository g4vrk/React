package com.g4vrk.react.api.channel.bind;

import com.g4vrk.react.api.channel.impl.AudienceChannel;
import net.kyori.adventure.audience.Audience;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PermissionChannelBinder implements Consumer<Audience> {

    private final String permission;
    private final AudienceChannel<?> channel;

    private final boolean removeWithoutPermission;

    public PermissionChannelBinder(
            final @NotNull String permission,
            final @NotNull AudienceChannel<?> channel,
            final boolean removeWithoutPermission
    ) {

        this.permission = permission;
        this.channel = channel;

        this.removeWithoutPermission = removeWithoutPermission;

    }

    @Override
    public void accept(
            final @NotNull Audience audience
    ) {

        if (audience instanceof final Permissible permissible) {

            if (permissible.hasPermission(this.permission)) {

                this.channel.subscribe(audience);

            }

        } else if (this.removeWithoutPermission) {

            this.channel.remove(audience);

        }

    }

}
