package de.greensurvivors.greenbook.listener;

import org.bukkit.event.Listener;

//zitate
public class ShelfListener implements Listener {
    private static ShelfListener instance;

    private ShelfListener() {
    }

    public static ShelfListener inst() {
        if (instance == null) {
            instance = new ShelfListener();
        }
        return instance;
    }
}
