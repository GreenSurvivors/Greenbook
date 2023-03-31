package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.listener.PaintingListener;
import org.bukkit.configuration.file.FileConfiguration;

public class PaintingConfig {
    private static final String PAINTING_KEY = "painting.";
    private static final String MOD_RANGE_KEY = PAINTING_KEY + "modify_range";

    private static final int DEFAULT_MOD_RANGE = 16;

    private static PaintingConfig instance;
    private final FileConfiguration configuration;

    private PaintingConfig() {
        this.configuration = GreenBook.inst().getConfig();

        this.configuration.addDefault(MOD_RANGE_KEY, DEFAULT_MOD_RANGE);
    }

    public static PaintingConfig inst() {
        if (instance == null) {
            instance = new PaintingConfig();
        }

        return instance;
    }

    public void saveNewRange(int newRange) {
        this.configuration.set(MOD_RANGE_KEY, newRange);

        GreenBook.inst().saveConfig();
    }

    public void load() {
        PaintingListener.inst().setModifyRange(this.configuration.getInt(MOD_RANGE_KEY, DEFAULT_MOD_RANGE));
    }
}
