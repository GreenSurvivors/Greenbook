package de.greensurvivors.greenbook.listener;

import org.bukkit.event.Listener;

//switch paitings
public class PaintingListener implements Listener {

    private static PaintingListener instance;

    private PaintingListener() {
    }

    public static PaintingListener inst() {
        if (instance == null) {
            instance = new PaintingListener();
        }
        return instance;
    }
}
