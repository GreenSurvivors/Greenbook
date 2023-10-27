package de.greensurvivors.greenbook.listener;

import de.greensurvivors.greenbook.language.Lang;
import de.greensurvivors.greenbook.utils.PermissionUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * enables the player to right-click a bookshelf and read a random quote (book)
 */
public class ShelfListener implements Listener {
    //list of all books a player can read
    private List<String> books = new ArrayList<>();
    //current config values, if sneaking and/or an empty hand is required to read a random book
    private boolean requireSneak = true, requireEmptyHand = false;

    //this class keeps track of its own instance, so it's basically static
    private static ShelfListener instance;

    private ShelfListener(){}

    /**
     * static to instance translator
     */
    public static ShelfListener inst() {
        if (instance == null) {
            instance = new ShelfListener();
        }
        return instance;
    }

    /**
     * set if sneaking is required to get a random quote (book), when right-clicking a bookshelf
     * @param requireSneak true if sneaking will be required from now on
     */
    public void setRequireSneak(boolean requireSneak){
        this.requireSneak = requireSneak;
    }

    /**
     * set if an empty hand is required to get a random quote (book), when right-clicking a bookshelf
     * @param requireEmptyHand true if an empty hand will be required from now on
     */
    public void setRequireEmptyHand(boolean requireEmptyHand){
        this.requireEmptyHand = requireEmptyHand;
    }

    /**
     * set the currently used list of quotes (books)
     * @param books list of strings that randomly come up if a bookshelf was clicked
     */
    public void setBooks(List<String> books) {
        this.books = books;
    }

    /**
     * get the currently used list of books, that come up, if a bookshelf was clicked
     * @return string list of books
     */
    public List<String> getBooks(){
        return books;
    }

    /**
     * get book by id or null
     * @param id id of a book
     * @return return quote (book) with the given id, or null if out of bounds
     */
    public @Nullable String getBook (int id){
        if (id >= 0 && id < books.size()){ //check bounds
            return books.get(id);
        }

        return null;
    }

    /**
     * sends a random quote (book) if a player right-clicks a bookshelf
     * checks if requirements (sneak / empty hand) are meat and if the player has permission
     * @param event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onBookshelfInteract(PlayerInteractEvent event){
        //don't fire for offhand
        if (event.getHand() == EquipmentSlot.HAND &&
                //right-clicked a block. Should ensure the getBlock() is not null
                event.getAction() == Action.RIGHT_CLICK_BLOCK){

            //is the block a bookshelf?
            Block eBlock = event.getClickedBlock();
            if (eBlock != null && eBlock.getType() == Material.BOOKSHELF){

                //check requirements
                Player ePlayer = event.getPlayer();
                if ((requireEmptyHand && !ePlayer.getInventory().getItemInMainHand().getType().isAir()) ||
                        (requireSneak && !ePlayer.isSneaking())) {
                    return;
                }

                //check permission
                if (PermissionUtils.hasPermission(ePlayer, PermissionUtils.GREENBOOK_SHELF_WILDCARD, PermissionUtils.GREENBOOK_SHELF_USE)){
                    //do we have books?
                    if (!books.isEmpty()){
                        //send a random quote
                        ePlayer.sendMessage(
                                //join the components together with new line a separator
                                Lang.join(
                                        //took a book header
                                        List.of(Lang.build(Lang.SHELF_USE.get()),
                                                  Lang.build(books.get(
                                                          ThreadLocalRandom.current().nextInt(books.size())
                                                  ))
                                        )
                                )
                        );
                    }
                }
            }
        }
    }
}
