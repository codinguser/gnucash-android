/*
 * Copyright (c) 2016 Ngewi Fet <ngewif@gmail.com>
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
package org.gnucash.android.test.unit.db;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.importer.GncXmlImporter;
import org.gnucash.android.model.BaseModel;
import org.gnucash.android.model.Book;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the book database adapter
 */
@RunWith(RobolectricTestRunner.class) //package is required so that resources can be found in dev mode
@Config(sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class BooksDbAdapterTest {

    private BooksDbAdapter mBooksDbAdapter;

    @Before
    public void setUp() {
        mBooksDbAdapter = BooksDbAdapter.getInstance();
        assertThat(mBooksDbAdapter.getRecordsCount()).isEqualTo(1); //there is always a default book after app start
        assertThat(mBooksDbAdapter.getActiveBookUID()).isNotNull();

        mBooksDbAdapter.deleteAllRecords();
        assertThat(mBooksDbAdapter.getRecordsCount()).isZero();
    }

    @Test
    public void addBook(){
        Book book = new Book(BaseModel.generateUID());
        mBooksDbAdapter.addRecord(book, DatabaseAdapter.UpdateMethod.insert);

        assertThat(mBooksDbAdapter.getRecordsCount()).isEqualTo(1);
        assertThat(mBooksDbAdapter.getRecord(book.getUID()).getDisplayName()).isEqualTo("Book 1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void savingBook_requiresRootAccountGUID(){
        Book book = new Book();
        mBooksDbAdapter.addRecord(book);
    }

    @Test
    public void deleteBook(){
        Book book = new Book();
        book.setRootAccountUID(BaseModel.generateUID());
        mBooksDbAdapter.addRecord(book);

        mBooksDbAdapter.deleteRecord(book.getUID());

        assertThat(mBooksDbAdapter.getRecordsCount()).isZero();
    }

    @Test
    public void setBookActive(){
        Book book1 = new Book(BaseModel.generateUID());
        Book book2 = new Book(BaseModel.generateUID());

        mBooksDbAdapter.addRecord(book1);
        mBooksDbAdapter.addRecord(book2);

        mBooksDbAdapter.setActive(book1.getUID());

        assertThat(mBooksDbAdapter.getActiveBookUID()).isEqualTo(book1.getUID());

        mBooksDbAdapter.setActive(book2.getUID());
        assertThat(mBooksDbAdapter.isActive(book2.getUID())).isTrue();
        //setting book2 as active should disable book1 as active
        Book book = mBooksDbAdapter.getRecord(book1.getUID());
        assertThat(book.isActive()).isFalse();
    }

    /**
     * Test that the generated display name has an ordinal greater than the number of
     * book records in the database
     */
    @Test
    public void testGeneratedDisplayName(){
        Book book1 = new Book(BaseModel.generateUID());
        Book book2 = new Book(BaseModel.generateUID());

        mBooksDbAdapter.addRecord(book1);
        mBooksDbAdapter.addRecord(book2);

        assertThat(mBooksDbAdapter.generateDefaultBookName()).isEqualTo("Book 3");
    }

    /**
     * Test that deleting a book record also deletes the book database
     */
    @Test
    public void deletingBook_shouldDeleteDbFile(){
        String bookUID = createNewBookWithDefaultAccounts();
        File dbPath = GnuCashApplication.getAppContext().getDatabasePath(bookUID);
        assertThat(dbPath).exists();
        BooksDbAdapter booksDbAdapter = BooksDbAdapter.getInstance();
        assertThat(booksDbAdapter.getRecord(bookUID)).isNotNull();

        long booksCount = booksDbAdapter.getRecordsCount();
        booksDbAdapter.deleteBook(bookUID);
        assertThat(dbPath).doesNotExist();
        assertThat(booksDbAdapter.getRecordsCount()).isEqualTo(booksCount - 1);
    }

    /**
     * Test that book names never conflict and that the ordinal attached to the book name is
     * increased irrespective of the order in which books are added to and deleted from the db
     */
    @Test
    public void testGeneratedDisplayNames_shouldBeUnique(){
        Book book1 = new Book(BaseModel.generateUID());
        Book book2 = new Book(BaseModel.generateUID());
        Book book3 = new Book(BaseModel.generateUID());

        mBooksDbAdapter.addRecord(book1);
        mBooksDbAdapter.addRecord(book2);
        mBooksDbAdapter.addRecord(book3);

        assertThat(mBooksDbAdapter.getRecordsCount()).isEqualTo(3L);

        mBooksDbAdapter.deleteRecord(book2.getUID());
        assertThat(mBooksDbAdapter.getRecordsCount()).isEqualTo(2L);

        String generatedName = mBooksDbAdapter.generateDefaultBookName();
        assertThat(generatedName).isNotEqualTo(book3.getDisplayName());
        assertThat(generatedName).isEqualTo("Book 4");
    }

    /**
     * Creates a new database with default accounts
     * @return The book UID for the new database
     * @throws RuntimeException if the new books could not be created
     */
    private String createNewBookWithDefaultAccounts(){
        try {
            return GncXmlImporter.parse(GnuCashApplication.getAppContext().getResources().openRawResource(R.raw.default_accounts));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not create default accounts");
        }
    }
}
