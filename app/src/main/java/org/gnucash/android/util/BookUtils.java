package org.gnucash.android.util;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

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
}
