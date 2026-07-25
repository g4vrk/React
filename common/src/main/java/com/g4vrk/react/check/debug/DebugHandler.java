package com.g4vrk.react.check.debug;

import com.g4vrk.react.React;
import com.g4vrk.react.check.Check;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@RequiredArgsConstructor
public class DebugHandler {

    private final Check check;

    private final Logger logger = React.INSTANCE.getLogger();

    public final void debug(
            final @NotNull String message
    ) {
        logger.info(format(message));
        debugToSender(check.getPlayer().bukkitPlayer, message);
    }

    public final void debugToSender(
            final @NotNull Audience sender,
            final @NotNull String message
    ) {
        sender.sendMessage(Component.text(format(message)).color(NamedTextColor.GRAY));
    }

    private @NotNull String format(
            final @NotNull String message
    ) {
        return "debug -> " + check.getName() + ": " + message;
    }

}
