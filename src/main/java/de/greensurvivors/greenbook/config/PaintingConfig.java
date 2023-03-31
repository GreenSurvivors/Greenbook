package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.listener.PaintingListener;

import java.util.List;

/**
 * keeps track of the modification range of paintings in main config
 */
public class PaintingConfig {
    //config key root, never use this directly!
    private static final String PAINTING_KEY = "painting.";
    //config key of the range
    private static final String MOD_RANGE_KEY = PAINTING_KEY + "modify_range";

    //default modifying range
    private static final int DEFAULT_MOD_RANGE = 16;

    //this class keeps track of its own instance, so it's basically static
    private static PaintingConfig instance;

    /**
     * add default range to main config (doesn't save to file yet)
     */
    private PaintingConfig() {
        GreenBook.inst().getConfig().addDefault(MOD_RANGE_KEY, DEFAULT_MOD_RANGE);
    }

    /**
     * static to instance translator
     */
    public static PaintingConfig inst() {
        if (instance == null) {
            instance = new PaintingConfig();
        }

        return instance;
    }

    /**
     * save the new modifying range to  main config file
     * @param newRange range the player can still switch paintings
     */
    public void saveNewRange(int newRange) {
        GreenBook.inst().getConfig().set(MOD_RANGE_KEY, newRange);

        //set header, defaults and keep commands (just in case)
        GreenBook.inst().getConfig().options().setHeader(List.of(GreenBook.inst().getName() + " " + GreenBook.inst().getDescription().getVersion()));
        GreenBook.inst().getConfig().options().copyDefaults(true);
        GreenBook.inst().getConfig().options().parseComments(true);

        //save config to file
        GreenBook.inst().saveConfig();
    }

    /**
     * loads maximum modification range (might be default) from main config
     */
    public void load() {
        PaintingListener.inst().setModifyRange(GreenBook.inst().getConfig().getInt(MOD_RANGE_KEY, DEFAULT_MOD_RANGE));
    }
}
