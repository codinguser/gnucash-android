/*
 * Copyright (c) 2016 Àlex Magaz Graça <alexandre.magaz@gmail.com>
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
package org.gnucash.android.test.unit.export;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.BookDbHelper;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.export.ofx.OfxExporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Book;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.gnucash.android.util.TimestampHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(GnucashTestRunner.class) //package is required so that resources can be found in dev mode
@Config(constants = BuildConfig.class,
        sdk = 21,
        packageName = "org.gnucash.android",
        shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class OfxExporterTest {
    private SQLiteDatabase mDb;

    @Before
    public void setUp() throws Exception {
        BookDbHelper bookDbHelper = new BookDbHelper(GnuCashApplication.getAppContext());
        BooksDbAdapter booksDbAdapter = new BooksDbAdapter(bookDbHelper.getWritableDatabase());
        Book testBook = new Book("testRootAccountUID");
        booksDbAdapter.addRecord(testBook);
        DatabaseHelper databaseHelper =
                new DatabaseHelper(GnuCashApplication.getAppContext(), testBook.getUID());
        mDb = databaseHelper.getWritableDatabase();
    }

    /**
     * When there aren't new or modified transactions, the OFX exporter
     * shouldn't create any file.
     */
    @Test
    public void testWithNoTransactionsToExport_shouldNotCreateAnyFile(){
        ExportParams exportParameters = new ExportParams(ExportFormat.OFX);
        exportParameters.setExportStartTime(TimestampHelper.getTimestampFromEpochZero());
        exportParameters.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        exportParameters.setDeleteTransactionsAfterExport(false);
        OfxExporter exporter = new OfxExporter(exportParameters, mDb);
        assertThat(exporter.generateExport()).isEmpty();
    }

    /**
     * Test that OFX files are generated
     */
    //FIXME: test failing with NPE
    public void testGenerateOFXExport(){
        AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(mDb);

        Account account = new Account("Basic Account");
        Transaction transaction = new Transaction("One transaction");
        transaction.addSplit(new Split(Money.createZeroInstance("EUR"),account.getUID()));
        account.addTransaction(transaction);

        accountsDbAdapter.addRecord(account);

        ExportParams exportParameters = new ExportParams(ExportFormat.OFX);
        exportParameters.setExportStartTime(TimestampHelper.getTimestampFromEpochZero());
        exportParameters.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        exportParameters.setDeleteTransactionsAfterExport(false);

        OfxExporter ofxExporter = new OfxExporter(exportParameters, mDb);
        List<String> exportedFiles = ofxExporter.generateExport();

        assertThat(exportedFiles).hasSize(1);
        File file = new File(exportedFiles.get(0));
        assertThat(file).exists().hasExtension("ofx");
        assertThat(file.length()).isGreaterThan(0L);
    }
}