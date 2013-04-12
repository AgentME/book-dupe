/**
 *
 * @author Indivisible0
 */
package in.nikitapek.bookdupe;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;

public class ItemCraftListener implements Listener {
    public static BookDupe plugin;

    public ItemCraftListener(BookDupe instance) {
        plugin = instance;
    }

    // Create a method to handle/interact with crafting events.
    @EventHandler
    public void onItemCraft(CraftItemEvent event) {
        // Get the crafting inventory (3x3 matrix) used to craft the item.
        CraftingInventory craftingInventory = event.getInventory();

        // Get the index of the first (and only) Material.WRITTEN_BOOK used in
        // the recipe.
        int writtenBookIndex = craftingInventory.first(Material.WRITTEN_BOOK);

        // Makes sure the recipe contains a WRITTEN_BOOK.
        if (writtenBookIndex == -1)
            return;

        // If the player does not have permissions to copy books, cancels the
        // event.
        if (!event.getWhoClicked().hasPermission("bookdupe.use")) {
            event.setCancelled(true);
            return;
        }

        // ItemStack represention of the book to be cloned.
        ItemStack initialBook = craftingInventory.getItem(writtenBookIndex);

        // Gets the BookMeta data of the book.
        BookMeta book = (BookMeta) initialBook.getItemMeta();

        // If the player does not have permission to copy any book
        // and the book was not written by the player, do not allow
        // the player to copy the book.
        if (!event.getWhoClicked().hasPermission("bookdupe.any")
                && !book.getAuthor().equals(event.getWhoClicked().getName())) {
            event.setCancelled(true);
            return;
        }

        // If the book has enchantments, check to see whether or not they're
        // allowed.
        if (!initialBook.getEnchantments().isEmpty())
            if (plugin.utilManager.getConfigUtil().getValue("allowIllegalEnchants", Boolean.class) == false) {
                event.setCancelled(true);
                return;
            }

        // Get the player's inventory.
        PlayerInventory playerInventory = event.getWhoClicked().getInventory();

        // Gets the index of the first INK_SACK in the recipe.
        int inkSackIndex = craftingInventory.first(Material.INK_SACK);
        // Gets the index of the first FEATHER in the recipe.
        int featherIndex = craftingInventory.first(Material.FEATHER);
        // Gets the index of the first BOOK in the recipe.
        int bookIndex = craftingInventory.first(Material.BOOK);

        // Makes sure the recipe doesn't contain an INK_SACK, FEATHER, and BOOK.
        if (inkSackIndex == -1 || featherIndex == -1 || bookIndex == -1) {
            HashMap<Integer, ? extends ItemStack> map = craftingInventory.all(Material.BOOK_AND_QUILL);
            int amount = map.size();

            // Check only one BOOK_AND_QUILL is in the crafting matrix.
            if (amount != 2)
                return;

            // Adds the original book to the player's inventory.
            playerInventory.addItem(initialBook);

            // Sets the result of the craft to the copied books.
            event.setCurrentItem(getNewBook(initialBook));
        }
        // Handle a non BOOK_AND_QUILL based recipe.
        else {
            // If the player regularly clicked (singular craft).
            if (!event.isShiftClick()) {
                // Adds the original book to the player's inventory.
                playerInventory.addItem(getNewBook(initialBook));
            } else {
                // Gets the amount of INK_SACK in the crafting matrix.
                int inkSackAmount = craftingInventory.getItem(inkSackIndex).getAmount();
                // Gets the amount of FEATHER in the crafting matrix.
                int featherAmount = craftingInventory.getItem(featherIndex).getAmount();
                // Gets the amount of BOOK in the crafting matrix.
                int bookAmount = craftingInventory.getItem(bookIndex).getAmount();

                int lowestAmount = 0;

                // Get the ingredient of which there is the least and loop until
                // that ingredient no longer exists.
                if (inkSackAmount < featherAmount && inkSackAmount < bookAmount) {
                    lowestAmount = inkSackAmount;
                }
                // Otherwise check if the crafting inventory contains less
                // FEATHER than any other ingredient.
                if (featherAmount < inkSackAmount && featherAmount < bookAmount) {
                    lowestAmount = featherAmount;
                    // Otherwise the crafting inventory contains less BOOK than
                    // any
                    // other ingredient.
                } else {
                    lowestAmount = bookAmount;
                }

                // Loops through crafting matrix reducing item amounts
                // one-by-one.
                int itemsLeft = 0;

                itemsLeft = craftingInventory.getItem(inkSackIndex).getAmount()
                        - lowestAmount;

                if (itemsLeft != 0) {
                    craftingInventory.getItem(inkSackIndex).setAmount(itemsLeft);
                } else {
                    craftingInventory.clear(inkSackIndex);
                }

                itemsLeft = craftingInventory.getItem(featherIndex).getAmount()
                        - lowestAmount;

                if (itemsLeft != 0) {
                    craftingInventory.getItem(featherIndex).setAmount(itemsLeft);
                } else {
                    craftingInventory.clear(featherIndex);
                }

                itemsLeft = craftingInventory.getItem(bookIndex).getAmount()
                        - lowestAmount;

                if (itemsLeft != 0) {
                    craftingInventory.getItem(bookIndex).setAmount(itemsLeft);
                } else {
                    craftingInventory.clear(bookIndex);
                }

                // Creates a HashMap to store items which do not fit into the
                // player's inventory.
                HashMap<Integer, ItemStack> leftOver = new HashMap<Integer, ItemStack>();

                // Adds the new books to the player's inventory.
                for (int i = 0; i < lowestAmount; i++) {
                    leftOver.putAll((playerInventory.addItem(getNewBook(initialBook))));

                    if (leftOver.isEmpty()) {
                        continue;
                    }

                    Location loc = event.getWhoClicked().getLocation();
                    ItemStack item = getNewBook(initialBook);
                    event.getWhoClicked().getWorld().dropItem(loc, item);
                }
            }

            // Sets the result of the craft to the copied books.
            event.setCurrentItem(initialBook);
        }
    }

    private ItemStack getNewBook(ItemStack previousBook) {
        // Creates the new book to be returned.
        ItemStack newBook = new ItemStack(Material.WRITTEN_BOOK);

        // Retrieves the BookMeta data.
        BookMeta newBookMeta = (BookMeta) newBook.getItemMeta();
        BookMeta previousBookMeta = (BookMeta) previousBook.getItemMeta();

        // Transfers the author, title, and pages to the new tag.
        newBookMeta.setAuthor(previousBookMeta.getAuthor());
        newBookMeta.setTitle(previousBookMeta.getTitle());
        newBookMeta.setLore(previousBookMeta.getLore());
        newBookMeta.setPages(previousBookMeta.getPages());

        // If the transfer of enchantments is allowed, transfers them.
        if (plugin.utilManager.getConfigUtil().<Boolean> getValue("allowIllegalEnchantTransfer", Boolean.class) == true)
            if (previousBookMeta.hasEnchants())
                newBookMeta.getEnchants().putAll(previousBookMeta.getEnchants());

        newBook.setItemMeta(newBookMeta);
        return newBook;
    }
}
