package de.greensurvivors.greenbook.config;

import de.greensurvivors.greenbook.GreenBook;
import de.greensurvivors.greenbook.GreenLogger;
import org.bukkit.Material;
import org.bukkit.configuration.MemorySection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * keeps track of the coin item in main config
 */
public class CoinConfig {
    //config key root, never use this directly!
    private static final String COIN_KEY = "coin.";
    //config key of the Item
    private static final String COIN_ITEM_KEY = COIN_KEY + "coin_item";
    //default Item, if no was configured
    private static final ItemStack DEFAULT_COIN_ITEM = new ItemStack(Material.GOLD_NUGGET);

    //this class keeps track of its own instance, so it's basically static
    private static CoinConfig instance;

    /**
     * add default item to main config (doesn't save to file yet)
     */
    private CoinConfig() {
        GreenBook.inst().getConfig().addDefault(COIN_ITEM_KEY, DEFAULT_COIN_ITEM);
    }

    /**
     * static to instance translator
     */
    public static CoinConfig inst() {
        if (instance == null) {
            instance = new CoinConfig();
        }

        return instance;
    }

    /**
     * save a new Coin-Item to main config
     */
    public void saveCoinItem(@NotNull ItemStack newCoin) {
        //deserialize item and set it under its config key
        GreenBook.inst().getConfig().set(COIN_ITEM_KEY, newCoin.serialize());

        //set header, defaults and keep commands (just in case)
        GreenBook.inst().getConfig().options().setHeader(List.of(GreenBook.inst().getName() + " " + GreenBook.inst().getDescription().getVersion()));
        GreenBook.inst().getConfig().options().copyDefaults(true);
        GreenBook.inst().getConfig().options().parseComments(true);

        //save main config
        GreenBook.inst().saveConfig();
    }

    /**
     * get coin item saved in main config or the default one
     *
     * @return coin item saved in main config or the default one, of no was configured or faulty
     */
    public @NotNull ItemStack loadCoinItem() {
        //please note: we are not using config.getItemstack since it's buggy.
        Object obj = GreenBook.inst().getConfig().get(COIN_ITEM_KEY, DEFAULT_COIN_ITEM);

        //try to get itemstack from unknown type
        if (obj instanceof ItemStack itemStack) {
            //it's already an itemstack. nice!
            return itemStack;
        } else if (obj instanceof Map<?, ?> map) {
            //map, decent
            Map<String, Object> itemStackMap = new HashMap<>();

            //check if all elements are ready for deserialization
            for (Object obj2 : map.keySet()) {
                if (obj2 instanceof String str) {
                    itemStackMap.put(str, map.get(obj2));
                } else {
                    GreenLogger.log(Level.WARNING, "couldn't deserialize head item property: " + obj2 + " of coin, skipping. Reason: not a string.");
                }
            }

            return ItemStack.deserialize(itemStackMap);

        } else if (obj instanceof MemorySection memorySection) {
            //yikes, a MemorySection. try to get a map version of it, but all sub values might return as memory section again
            //on the other hand, getting deep values would result from
            //  Itemstack
            //      property (might be MemorySection again)
            //          subproperty
            //              value
            //to
            //  Itemstack
            //      property.subproperty
            //          value
            //not ideal in both cases and is prone to fail if complex items where given.
            //might want to implement a methode in future if errors come up
            return ItemStack.deserialize(memorySection.getValues(false));
        } else {
            //de fuck is this object? Better return the default item.
            GreenLogger.log(Level.WARNING, "couldn't deserialize coin item: " + obj + ", using Default. Reason: not a item stack nor map.");
            return DEFAULT_COIN_ITEM;
        }
    }
}
