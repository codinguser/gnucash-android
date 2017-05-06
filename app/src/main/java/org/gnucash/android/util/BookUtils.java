package org.gnucash.android.util;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.settings.PreferenceActivity;

/**
 * Utility class for common operations involving books
 */

public class BookUtils {
    public static final String KEY_BACKUP_FILE = "book_backup_file_key";

    /**
     * Return the backup file for the book
     * @param bookUID Unique ID of the book
     * @return DocumentFile for book backups
     */
    @Nullable
    public static String getBookBackupFileUri(String bookUID){
        SharedPreferences sharedPreferences = PreferenceActivity.getBookSharedPreferences(bookUID);
        return sharedPreferences.getString(KEY_BACKUP_FILE, null);
    }

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
