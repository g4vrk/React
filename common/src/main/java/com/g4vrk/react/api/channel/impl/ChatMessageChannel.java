package com.g4vrk.react.api.channel.impl;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class ChatMessageChannel extends AudienceChannel<Component> {

    @Override
    public void publish(
            final @NotNull Component input
    ) {

        for (final Audience recipient : this.recipients()) {

            if (recipient == null) continue;

            recipient.sendMessage(input);

        }

    }

}
