/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gnucash.android.db.adapter;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema.BookEntry;
import org.gnucash.android.model.Book;
import org.gnucash.android.util.TimestampHelper;

/**
 * Database adapter for creating/modifying book entries
 */
public class BooksDbAdapter extends DatabaseAdapter<Book> {

    /**
     * Opens the database adapter with an existing database
     * @param db        SQLiteDatabase object
     */
    public BooksDbAdapter(SQLiteDatabase db) {
        super(db, BookEntry.TABLE_NAME, new String[] {
                BookEntry.COLUMN_DISPLAY_NAME,
                BookEntry.COLUMN_ROOT_GUID,
                BookEntry.COLUMN_TEMPLATE_GUID,
                BookEntry.COLUMN_SOURCE_URI,
                BookEntry.COLUMN_ACTIVE,
                BookEntry.COLUMN_UID,
                BookEntry.COLUMN_LAST_SYNC
        });
    }

    /**
     * Return the application instance of the books database adapter
     * @return Books database adapter
     */
    public static BooksDbAdapter getInstance(){
        return GnuCashApplication.getBooksDbAdapter();
    }

    @Override
    public Book buildModelInstance(@NonNull Cursor cursor) {
        String rootAccountGUID = cursor.getString(cursor.getColumnIndexOrThrow(BookEntry.COLUMN_ROOT_GUID));
        String rootTemplateGUID =  cursor.getString(cursor.getColumnIndexOrThrow(BookEntry.COLUMN_TEMPLATE_GUID));
        String uriString = cursor.getString(cursor.getColumnIndexOrThrow(BookEntry.COLUMN_SOURCE_URI));
        String displayName = cursor.getString(cursor.getColumnIndexOrThrow(BookEntry.COLUMN_DISPLAY_NAME));
        int active = cursor.getInt(cursor.getColumnIndexOrThrow(BookEntry.COLUMN_ACTIVE));
        String lastSync = cursor.getString(cursor.getColumnIndexOrThrow(BookEntry.COLUMN_LAST_SYNC));

        Book book = new Book(rootAccountGUID);
        book.setDisplayName(displayName);
        book.setRootTemplateUID(rootTemplateGUID);
        book.setSourceUri(Uri.parse(uriString));
        book.setActive(active > 0);
        book.setLastSync(TimestampHelper.getTimestampFromUtcString(lastSync));

        populateBaseModelAttributes(cursor, book);
        return book;
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull final Book book) {
        stmt.clearBindings();
        stmt.bindString(1, book.getDisplayName());
        stmt.bindString(2, book.getRootAccountUID());
        stmt.bindString(3, book.getRootTemplateUID());
        if (book.getSourceUri() != null)
            stmt.bindString(4, book.getSourceUri().toString());
        stmt.bindLong(5, book.isActive() ? 1L : 0L);
        stmt.bindString(6, book.getUID());
        stmt.bindString(7, TimestampHelper.getUtcStringFromTimestamp(book.getLastSync()));
        return stmt;
    }

    /**
     * Sets the book with unique identifier {@code uid} as active and all others as inactive
     * @param bookUID Unique identifier of the book
     */
    public void setActive(String bookUID){
        ContentValues contentValues = new ContentValues();
        contentValues.put(BookEntry.COLUMN_ACTIVE, 0);
        mDb.update(mTableName, contentValues, null, null); //disable all

        contentValues.clear();
        contentValues.put(BookEntry.COLUMN_ACTIVE, 1);
        mDb.update(mTableName, contentValues, BookEntry.COLUMN_UID + " = ?", new String[]{bookUID});
    }

    /**
     * Checks if the book is active or not
     * @param bookUID GUID of the book
     * @return {@code true} if the book is active, {@code false} otherwise
     */
    public boolean isActive(String bookUID){
        String isActive = getAttribute(bookUID, BookEntry.COLUMN_ACTIVE);
        return Integer.parseInt(isActive) > 0;
    }

    /**
     * Returns the root account GUID of the current active book
     * @return GUID of the root account
     */
    public @NonNull String getActiveRootAccountUID(){
        Cursor cursor = mDb.query(mTableName, new String[]{BookEntry.COLUMN_ROOT_GUID},
                BookEntry.COLUMN_ACTIVE + "= 1", null, null, null, null, "1");
        try{
            if (cursor.moveToFirst()){
                return cursor.getString(cursor.getColumnIndexOrThrow(BookEntry.COLUMN_ROOT_GUID));
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    /**
     * Returns the GUID of the current active book
     * @return GUID of the active book
     */
    public @NonNull String getActiveBookUID(){
        Cursor cursor = mDb.query(mTableName, new String[]{BookEntry.COLUMN_UID},
                BookEntry.COLUMN_ACTIVE + "= 1", null, null, null, null, "1");
        try{
            if (cursor.moveToFirst()){
                return cursor.getString(cursor.getColumnIndexOrThrow(BookEntry.COLUMN_UID));
            }
        } finally {
            cursor.close();
        }
        return null;
    }


    /**
     * Return the name of the currently active book.
     * Or a generic name if there is no active book (should never happen)
     * @return Display name of the book
     */
    public @NonNull String getActiveBookDisplayName(){
        Cursor cursor = mDb.query(mTableName,
                new String[]{BookEntry.COLUMN_DISPLAY_NAME}, BookEntry.COLUMN_ACTIVE + " = 1",
                null, null, null, null);
        try {
            if (cursor.moveToFirst()){
                return cursor.getString(cursor.getColumnIndexOrThrow(BookEntry.COLUMN_DISPLAY_NAME));
            }
        } finally {
            cursor.close();
        }
        return "Book1";
    }

    /**
     * Generates a new default name for a new book
     * @return String with default name
     */
    public @NonNull String generateDefaultBookName() {
        long bookCount = getRecordsCount() + 1;

        String sql = "SELECT COUNT(*) FROM " + mTableName + " WHERE " + BookEntry.COLUMN_DISPLAY_NAME + " = ?";
        SQLiteStatement statement = mDb.compileStatement(sql);

        while (true) {
            String name = "Book" + " " + bookCount;

            statement.clearBindings();
            statement.bindString(1, name);
            long nameCount = statement.simpleQueryForLong();

            if (nameCount == 0) {
                return name;
            }

            bookCount++;
        }

    }



}
