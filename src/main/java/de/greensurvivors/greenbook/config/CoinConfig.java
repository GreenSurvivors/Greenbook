package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.GreenLogger;
import org.bukkit.Material;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class CoinConfig {
    private static final String COIN_KEY = "coin.";
    private static final String COIN_ITEM_KEY = COIN_KEY + "coin_item";
    private static final ItemStack DEFAULT_COIN_ITEM = new ItemStack(Material.GOLD_NUGGET);

    private static CoinConfig instance;

    private final FileConfiguration configuration;

    private CoinConfig() {
        this.configuration = GreenBook.inst().getConfig();

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

        GreenBook.inst().saveConfig();
    }

    public ItemStack loadCoinItem() {
        Object obj = this.configuration.get(COIN_ITEM_KEY, DEFAULT_COIN_ITEM);

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
