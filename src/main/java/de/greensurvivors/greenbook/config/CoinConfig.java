package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.GreenLogger;
import org.bukkit.Material;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class CoinConfig {
    private static final String COIN_ITEM_KEY = "coin_item";
    private static final ItemStack DEFAULT_COIN_ITEM = new ItemStack(Material.GOLD_NUGGET);

    private static CoinConfig instance;

    private final File file;
    private final YamlConfiguration configuration;

    private CoinConfig() {
        this.file = new File(GreenBook.inst().getDataFolder(), "coinConfig.yml");
        this.configuration = YamlConfiguration.loadConfiguration(this.file);

        this.configuration.addDefault(COIN_ITEM_KEY, DEFAULT_COIN_ITEM);
    }

    public static CoinConfig inst() {
        if (instance == null) {
            instance = new CoinConfig();
        }

        return instance;
    }

    public void saveCoinItem(ItemStack newCoin) {
        this.configuration.set(COIN_ITEM_KEY, newCoin.serialize());

        // save modified configuration
        this.configuration.options().setHeader(Collections.singletonList(String.format(
                "Coin config file for %s (%s)",
                GreenBook.inst().getName(),
                GreenBook.inst().getDescription().getVersion())));
        this.configuration.options().parseComments(true);

        try {
            this.configuration.save(file);
        } catch (IOException e) {
            GreenLogger.log(Level.SEVERE, "Could not save coin config file.", e);
        }
    }

    public ItemStack loadCoinItem() {
        Object obj = this.configuration.get("coin_item", DEFAULT_COIN_ITEM);

        if (obj instanceof ItemStack itemStack) {
            return itemStack;
        } else if (obj instanceof Map<?, ?> map) {
            Map<String, Object> itemStackMap = new HashMap<>();

            for (Object obj2 : map.keySet()) {
                if (obj2 instanceof String str) {
                    itemStackMap.put(str, map.get(obj2));
                } else {
                    GreenLogger.log(Level.WARNING, "couldn't deserialize head item property: " + obj2 + " of coin, skipping. Reason: not a string.");
                }
            }

            return ItemStack.deserialize(itemStackMap);

        } else if (obj instanceof MemorySection memorySection) {
            return ItemStack.deserialize(memorySection.getValues(false));
        } else {
            GreenLogger.log(Level.WARNING, "couldn't deserialize coin item: " + obj + ", using Default. Reason: not a item stack nor map.");
            return DEFAULT_COIN_ITEM;
        }
    }
}
