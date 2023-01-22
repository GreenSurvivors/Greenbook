package de.greensurvivors.greenbook.listener;

import org.bukkit.event.Listener;

public class BridgeListener implements Listener {
    private static BridgeListener instance;

    private BridgeListener() {
    }

    public static BridgeListener inst() {
        if (instance == null) {
            instance = new BridgeListener();
        }
        return instance;
    }

}
