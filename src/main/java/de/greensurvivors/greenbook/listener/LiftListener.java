package de.greensurvivors.greenbook.listener;

import org.bukkit.event.Listener;

public class LiftListener implements Listener {
    private static LiftListener instance;

    private LiftListener() {
    }

    public static LiftListener inst() {
        if (instance == null) {
            instance = new LiftListener();
        }
        return instance;
    }
}
