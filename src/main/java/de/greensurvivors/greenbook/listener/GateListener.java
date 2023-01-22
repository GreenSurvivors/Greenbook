package de.greensurvivors.greenbook.listener;

import org.bukkit.event.Listener;

public class GateListener implements Listener {
    private static GateListener instance;

    private GateListener() {
    }

    public static GateListener inst() {
        if (instance == null) {
            instance = new GateListener();
        }
        return instance;
    }
}
