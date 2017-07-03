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

import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.BookDbHelper;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.qif.QifExporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Book;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.gnucash.android.util.TimestampHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(GnucashTestRunner.class) //package is required so that resources can be found in dev mode
@Config(constants = BuildConfig.class,
        sdk = 21,
        packageName = "org.gnucash.android",
        shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class QifExporterTest {
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
     * When there aren't new or modified transactions, the QIF exporter
     * shouldn't create any file.
     */
    @Test
    public void testWithNoTransactionsToExport_shouldNotCreateAnyFile(){
        ExportParams exportParameters = new ExportParams(ExportFormat.QIF);
        exportParameters.setExportStartTime(TimestampHelper.getTimestampFromEpochZero());
        exportParameters.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        exportParameters.setDeleteTransactionsAfterExport(false);
        QifExporter exporter = new QifExporter(exportParameters, mDb);
        assertThat(exporter.generateExport()).isEmpty();
    }

    /**
     * Test that QIF files are generated
     */
    //// FIXME: 20.04.2017 Test failing with NPE
    public void testGenerateQIFExport(){
        AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(mDb);

        Account account = new Account("Basic Account");
        Transaction transaction = new Transaction("One transaction");
        transaction.addSplit(new Split(Money.createZeroInstance("EUR"),account.getUID()));
        account.addTransaction(transaction);

        accountsDbAdapter.addRecord(account);

        ExportParams exportParameters = new ExportParams(ExportFormat.QIF);
        exportParameters.setExportStartTime(TimestampHelper.getTimestampFromEpochZero());
        exportParameters.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        exportParameters.setDeleteTransactionsAfterExport(false);

        QifExporter qifExporter = new QifExporter(exportParameters, mDb);
        List<String> exportedFiles = qifExporter.generateExport();

        assertThat(exportedFiles).hasSize(1);
        File file = new File(exportedFiles.get(0));
        assertThat(file).exists().hasExtension("qif");
        assertThat(file.length()).isGreaterThan(0L);
    }

    /**
     * Test that when more than one currency is in use, multiple QIF files will be generated
     */
    //// FIXME: 20.04.2017 test failing with NPE
    public void multiCurrencyTransactions_shouldResultInMultipleQifFiles(){
        AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(mDb);

        Account account = new Account("Basic Account", Commodity.getInstance("EUR"));
        Transaction transaction = new Transaction("One transaction");
        transaction.addSplit(new Split(Money.createZeroInstance("EUR"),account.getUID()));
        account.addTransaction(transaction);
        accountsDbAdapter.addRecord(account);

        Account foreignAccount = new Account("US Konto", Commodity.getInstance("USD"));
        Transaction multiCulti = new Transaction("Multicurrency");
        Split split = new Split(new Money("12", "USD"), new Money("15", "EUR"), foreignAccount.getUID());
        Split split2 = split.createPair(account.getUID());
        multiCulti.addSplit(split);
        multiCulti.addSplit(split2);
        foreignAccount.addTransaction(multiCulti);

        accountsDbAdapter.addRecord(foreignAccount);

        ExportParams exportParameters = new ExportParams(ExportFormat.QIF);
        exportParameters.setExportStartTime(TimestampHelper.getTimestampFromEpochZero());
        exportParameters.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        exportParameters.setDeleteTransactionsAfterExport(false);

        QifExporter qifExporter = new QifExporter(exportParameters, mDb);
        List<String> exportedFiles = qifExporter.generateExport();

        assertThat(exportedFiles).hasSize(2);
        File file = new File(exportedFiles.get(0));
        assertThat(file).exists().hasExtension("qif");
        assertThat(file.length()).isGreaterThan(0L);
    }

    //@Test
    public void description_and_memo_field_test() {
        // arrange

        String expectedDescription = "my description";
        String expectedMemo = "my memo";

        AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(mDb);
        Account account = new Account("Basic Account");
        Transaction transaction = new Transaction("One transaction");
        transaction.setDescription(expectedDescription);
        transaction.setNote(expectedMemo);
        account.addTransaction(transaction);
        accountsDbAdapter.addRecord(account);

        ExportParams exportParameters = new ExportParams(ExportFormat.QIF);
        exportParameters.setExportStartTime(TimestampHelper.getTimestampFromEpochZero());
        exportParameters.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        exportParameters.setDeleteTransactionsAfterExport(false);

        // act

        QifExporter qifExporter = new QifExporter(exportParameters, mDb);
        List<String> exportedFiles = qifExporter.generateExport();

        // assert

        assertThat(exportedFiles).hasSize(1);
        File file = new File(exportedFiles.get(0));
        assertThat(file).exists().hasExtension("qif");
        StringBuilder fileContentsBuilder = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            fileContentsBuilder.append(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // todo: check the description & memo fields.
        String fileContent = fileContentsBuilder.toString();
        assertThat(fileContent.contains(expectedDescription));
        assertThat(fileContent.contains(expectedMemo));
    }
}