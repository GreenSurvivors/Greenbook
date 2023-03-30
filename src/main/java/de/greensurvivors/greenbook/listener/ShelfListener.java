package de.greensurvivors.greenbook.listener;

import de.greensurvivors.greenbook.PermissionUtils;
import de.greensurvivors.greenbook.language.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

//zitate
public class ShelfListener implements Listener {
    private static ShelfListener instance;
    private List<String> books = new ArrayList<>();

    public static ShelfListener inst() {
        if (instance == null) {
            instance = new ShelfListener();
        }
        return instance;
    }

    public void setBooks(List<String> books) {
        this.books = books;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBookshelfInteract(PlayerInteractEvent event){
        Block eBlock = event.getClickedBlock();
        Player ePlayer = event.getPlayer();

        //!shelfEnabled todo

        //don't fire for offhand
        if (event.getHand() == EquipmentSlot.HAND &&
                //right-clicked a block. Should ensure the getBlock() is not null
                event.getAction() == Action.RIGHT_CLICK_BLOCK){

            if (eBlock != null && eBlock.getType() == Material.BOOKSHELF){
                if (PermissionUtils.hasPermission(ePlayer, PermissionUtils.GREENBOOK_SHELF_ADMIN, PermissionUtils.GREENBOOK_SHELF_USE)){
                    if (books.size() > 0){
                        ePlayer.sendMessage(
                            Lang.build(Lang.SHELF_USE.get()).
                                    append(Component.newline()).
                                    append(Lang.build(
                                            books.get(ThreadLocalRandom.current().nextInt(books.size()))
                                    ))
                        );
                    }
                }
            }
        }

    }
}
