package com.g4vrk.react.api.channel.impl;

import com.g4vrk.react.api.channel.Channel;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public abstract class AudienceChannel<I> implements Channel<I> {

    private final Set<Audience> recipients = new ObjectOpenHashSet<>();

    public boolean subscribe(
            final @NotNull Audience recipient
    ) {

        return this.recipients.add(recipient);

    }

    public boolean remove(
            final @NotNull Audience recipient
    ) {

        return this.recipients.remove(recipient);

    }

    public @NotNull Set<Audience> recipients() {

        return this.recipients;

    }

    @Override
    public abstract void publish(final @NotNull I input);

}
