package org.gnucash.android.util;

import android.support.annotation.NonNull;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.ui.account.AccountsActivity;

/**
 * Utility class for common operations involving books
 */

public class BookUtils {

    /**
     * Activates the book with unique identifer {@code bookUID}, and refreshes the database adapters
     * @param bookUID GUID of the book to be activated
     */
    public static void activateBook(@NonNull String bookUID){
        GnuCashApplication.getBooksDbAdapter().setActive(bookUID);
        GnuCashApplication.initializeDatabaseAdapters();
    }

    /**
     * Loads the book with GUID {@code bookUID} and opens the AccountsActivity
     * @param bookUID GUID of the book to be loaded
     */
    public static void loadBook(@NonNull String bookUID){
        activateBook(bookUID);
        AccountsActivity.start(GnuCashApplication.getAppContext());
    }
}
