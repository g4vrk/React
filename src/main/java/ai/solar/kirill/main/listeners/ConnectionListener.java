package ai.solar.kirill.main.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import ai.solar.kirill.SolarAI;
import ai.solar.kirill.utils.igrok.PlayerEntity;
import ai.solar.kirill.utils.igrok.PlayerRegistry;

public class ConnectionListener extends PacketListenerAbstract {

    @Override
    public void onUserLogin(UserLoginEvent event) {
        User user = event.getUser();
        if (user != null && user.getUUID() != null && user.getName() != null) {
            PlayerEntity entity = new PlayerEntity(user.getUUID(), user.getName());
            PlayerRegistry.addPlayer(user.getUUID(), entity);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        User user = event.getUser();
        if (user != null && user.getUUID() != null) {
            PlayerRegistry.removePlayer(user.getUUID());
            SolarAI.getInstance().getViolationManager().clearPlayerData(user.getUUID());
        }
    }
}