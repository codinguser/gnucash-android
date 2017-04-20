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
package org.gnucash.android.test.unit.service;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.importer.GncXmlImporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.Recurrence;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.service.ScheduledActionService;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.gnucash.android.util.BookUtils;
import org.gnucash.android.util.TimestampHelper;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.Weeks;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the the scheduled actions service runs as expected
 */
@RunWith(GnucashTestRunner.class) //package is required so that resources can be found in dev mode
@Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android",
        shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class ScheduledActionServiceTest {

    private static String mActionUID;
    private SQLiteDatabase mDb;

    private static Account mBaseAccount = new Account("Base Account");
    private static Account mTransferAccount = new Account("Transfer Account");

    private static Transaction mTemplateTransaction;
    private TransactionsDbAdapter mTransactionsDbAdapter;

    public void createAccounts(){
        try {
            String bookUID = GncXmlImporter.parse(GnuCashApplication.getAppContext().getResources().openRawResource(R.raw.default_accounts));
            BookUtils.loadBook(bookUID);
            //initAdapters(bookUID);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not create default accounts");
        }
    }

    @BeforeClass
    public static void makeAccounts(){
        mTemplateTransaction = new Transaction("Recurring Transaction");
        mTemplateTransaction.setTemplate(true);

        mActionUID = mTemplateTransaction.getUID();
    }

    @Before
    public void setUp(){
        mDb = GnuCashApplication.getActiveDb();
        new CommoditiesDbAdapter(mDb); //initializes commodity static values
        mBaseAccount.setCommodity(Commodity.DEFAULT_COMMODITY);
        mTransferAccount.setCommodity(Commodity.DEFAULT_COMMODITY);
        mTemplateTransaction.setCommodity(Commodity.DEFAULT_COMMODITY);

        Split split1 = new Split(new Money(BigDecimal.TEN, Commodity.DEFAULT_COMMODITY), mBaseAccount.getUID());
        Split split2 = split1.createPair(mTransferAccount.getUID());

        mTemplateTransaction.addSplit(split1);
        mTemplateTransaction.addSplit(split2);

        AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
        accountsDbAdapter.addRecord(mBaseAccount);
        accountsDbAdapter.addRecord(mTransferAccount);

        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
        mTransactionsDbAdapter.addRecord(mTemplateTransaction, DatabaseAdapter.UpdateMethod.insert);
    }

    @Test
    public void disabledScheduledActions_shouldNotRun(){
        Recurrence recurrence = new Recurrence(PeriodType.WEEK);
        ScheduledAction scheduledAction1 = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        scheduledAction1.setStartTime(System.currentTimeMillis() - 100000);
        scheduledAction1.setEnabled(false);
        scheduledAction1.setActionUID(mActionUID);
        scheduledAction1.setRecurrence(recurrence);

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledAction1);

        TransactionsDbAdapter trxnAdapter = TransactionsDbAdapter.getInstance();

        assertThat(trxnAdapter.getRecordsCount()).isZero();
        ScheduledActionService.processScheduledActions(actions, mDb);
        assertThat(trxnAdapter.getRecordsCount()).isZero();
    }

    @Test
    public void futureScheduledActions_shouldNotRun(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        scheduledAction.setStartTime(System.currentTimeMillis() + 100000);
        scheduledAction.setEnabled(true);
        scheduledAction.setRecurrence(new Recurrence(PeriodType.MONTH));
        scheduledAction.setActionUID(mActionUID);

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledAction);

        TransactionsDbAdapter trxnAdapter = TransactionsDbAdapter.getInstance();

        assertThat(trxnAdapter.getRecordsCount()).isZero();
        ScheduledActionService.processScheduledActions(actions, mDb);
        assertThat(trxnAdapter.getRecordsCount()).isZero();
    }

    /**
     * Transactions whose execution count has reached or exceeded the planned execution count
     */
    @Test
    public void exceededExecutionCounts_shouldNotRun(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        scheduledAction.setActionUID(mActionUID);
        scheduledAction.setStartTime(new DateTime(2015, 5, 31, 14, 0).getMillis());
        scheduledAction.setEnabled(true);
        scheduledAction.setRecurrence(new Recurrence(PeriodType.WEEK));
        scheduledAction.setTotalPlannedExecutionCount(4);
        scheduledAction.setExecutionCount(4);

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledAction);

        TransactionsDbAdapter trxnAdapter = TransactionsDbAdapter.getInstance();
        assertThat(trxnAdapter.getRecordsCount()).isZero();
        ScheduledActionService.processScheduledActions(actions, mDb);
        assertThat(trxnAdapter.getRecordsCount()).isZero();
    }

    /**
     * Test that normal scheduled transactions would lead to new transaction entries
     */
    @Test
    public void missedScheduledTransactions_shouldBeGenerated(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        DateTime startTime = new DateTime(2016, 6, 6, 9, 0);
        scheduledAction.setStartTime(startTime.getMillis());
        DateTime endTime = new DateTime(2016, 9, 12, 8, 0); //end just before last appointment
        scheduledAction.setEndTime(endTime.getMillis());

        scheduledAction.setActionUID(mActionUID);

        int multiplier = 2;
        scheduledAction.setRecurrence(PeriodType.WEEK, multiplier);
        ScheduledActionDbAdapter.getInstance().addRecord(scheduledAction, DatabaseAdapter.UpdateMethod.insert);

        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        assertThat(transactionsDbAdapter.getRecordsCount()).isZero();

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledAction);
        ScheduledActionService.processScheduledActions(actions, mDb);

        assertThat(transactionsDbAdapter.getRecordsCount()).isEqualTo(7);
    }

    public void endTimeInTheFuture_shouldExecuteOnlyUntilPresent(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        DateTime startTime = new DateTime(2016, 6, 6, 9, 0);
        scheduledAction.setStartTime(startTime.getMillis());
        scheduledAction.setActionUID(mActionUID);

        scheduledAction.setRecurrence(PeriodType.WEEK, 2);
        scheduledAction.setEndTime(new DateTime(2017, 8, 16, 9, 0).getMillis());
        ScheduledActionDbAdapter.getInstance().addRecord(scheduledAction, DatabaseAdapter.UpdateMethod.insert);

        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        assertThat(transactionsDbAdapter.getRecordsCount()).isZero();

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledAction);
        ScheduledActionService.processScheduledActions(actions, mDb);

        int weeks = Weeks.weeksBetween(startTime, new DateTime(2016, 8, 29, 10, 0)).getWeeks();
        int expectedTransactionCount = weeks/2; //multiplier from the PeriodType

        assertThat(transactionsDbAdapter.getRecordsCount()).isEqualTo(expectedTransactionCount);
    }

    /**
     * Test that if the end time of a scheduled transaction has passed, but the schedule was missed
     * (either because the book was not opened or similar) then the scheduled transactions for the
     * relevant period should still be executed even though end time has passed.
     * <p>This holds only for transactions. Backups will be skipped</p>
     */
    @Test
    public void scheduledTransactionsWithEndTimeInPast_shouldBeExecuted(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        DateTime startTime = new DateTime(2016, 6, 6, 9, 0);
        scheduledAction.setStartTime(startTime.getMillis());
        scheduledAction.setActionUID(mActionUID);

        scheduledAction.setRecurrence(PeriodType.WEEK, 2);
        scheduledAction.setEndTime(new DateTime(2016, 8, 8, 9, 0).getMillis());
        ScheduledActionDbAdapter.getInstance().addRecord(scheduledAction, DatabaseAdapter.UpdateMethod.insert);

        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        assertThat(transactionsDbAdapter.getRecordsCount()).isZero();

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledAction);
        ScheduledActionService.processScheduledActions(actions, mDb);

        int expectedCount = 5;
        assertThat(scheduledAction.getExecutionCount()).isEqualTo(expectedCount);
        assertThat(transactionsDbAdapter.getRecordsCount()).isEqualTo(expectedCount); //would be 6 if the end time is not respected
    }

    /**
     * Test that only scheduled actions with action UIDs are processed
     */
    @Test //(expected = IllegalArgumentException.class)
    public void recurringTransactions_shouldHaveScheduledActionUID(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        DateTime startTime = new DateTime(2016, 7, 4, 12 ,0);
        scheduledAction.setStartTime(startTime.getMillis());
        scheduledAction.setRecurrence(PeriodType.MONTH, 1);

        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        assertThat(transactionsDbAdapter.getRecordsCount()).isZero();

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledAction);
        ScheduledActionService.processScheduledActions(actions, mDb);

        //no change in the database since no action UID was specified
        assertThat(transactionsDbAdapter.getRecordsCount()).isZero();
    }

    /**
     * Scheduled backups should run only once.
     *
     * <p>Backups may have been missed since the last run, but still only
     * one should be done.</p>
     *
     * <p>For example, if we have set up a daily backup, the last one
     * was done on Monday and it's Thursday, two backups have been
     * missed. Doing the two missed backups plus today's wouldn't be
     * useful, so just one should be done.</p>
     */
    @Test
    public void scheduledBackups_shouldRunOnlyOnce(){
        ScheduledAction scheduledBackup = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        scheduledBackup.setStartTime(LocalDateTime.now()
                .minusMonths(4).minusDays(2).toDate().getTime());
        scheduledBackup.setRecurrence(PeriodType.MONTH, 1);
        scheduledBackup.setExecutionCount(2);
        scheduledBackup.setLastRun(LocalDateTime.now().minusMonths(2).toDate().getTime());
        long previousLastRun = scheduledBackup.getLastRunTime();

        ExportParams backupParams = new ExportParams(ExportFormat.XML);
        backupParams.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        scheduledBackup.setTag(backupParams.toCsv());

        File backupFolder = new File(Exporter.getExportFolderPath(BooksDbAdapter.getInstance().getActiveBookUID()));
        assertThat(backupFolder).exists();
        assertThat(backupFolder.listFiles()).isEmpty();

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledBackup);

        // Check there's not a backup for each missed run
        ScheduledActionService.processScheduledActions(actions, mDb);
        assertThat(scheduledBackup.getExecutionCount()).isEqualTo(3);
        assertThat(scheduledBackup.getLastRunTime()).isGreaterThan(previousLastRun);
        File[] backupFiles = backupFolder.listFiles();
        assertThat(backupFiles).hasSize(1);
        assertThat(backupFiles[0]).exists().hasExtension("gnca");

        // Check also across service runs
        previousLastRun = scheduledBackup.getLastRunTime();
        ScheduledActionService.processScheduledActions(actions, mDb);
        assertThat(scheduledBackup.getExecutionCount()).isEqualTo(3);
        assertThat(scheduledBackup.getLastRunTime()).isEqualTo(previousLastRun);
        backupFiles = backupFolder.listFiles();
        assertThat(backupFiles).hasSize(1);
        assertThat(backupFiles[0]).exists().hasExtension("gnca");
    }

    /**
     * Tests that a scheduled backup isn't executed before the next scheduled
     * execution according to its recurrence.
     *
     * <p>Tests for bug https://github.com/codinguser/gnucash-android/issues/583</p>
     */
    @Test
    public void scheduledBackups_shouldNotRunBeforeNextScheduledExecution(){
        ScheduledAction scheduledBackup = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        scheduledBackup.setStartTime(LocalDateTime.now().minusDays(2).toDate().getTime());
        scheduledBackup.setLastRun(scheduledBackup.getStartTime());
        long previousLastRun = scheduledBackup.getLastRunTime();
        scheduledBackup.setExecutionCount(1);
        scheduledBackup.setRecurrence(PeriodType.WEEK, 1);

        ExportParams backupParams = new ExportParams(ExportFormat.XML);
        backupParams.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        scheduledBackup.setTag(backupParams.toCsv());

        File backupFolder = new File(
                Exporter.getExportFolderPath(BooksDbAdapter.getInstance().getActiveBookUID()));
        assertThat(backupFolder).exists();
        assertThat(backupFolder.listFiles()).isEmpty();

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledBackup);
        ScheduledActionService.processScheduledActions(actions, mDb);

        assertThat(scheduledBackup.getExecutionCount()).isEqualTo(1);
        assertThat(scheduledBackup.getLastRunTime()).isEqualTo(previousLastRun);
        assertThat(backupFolder.listFiles()).hasSize(0);
    }

    /**
     * Tests that an scheduled backup doesn't include transactions added or modified
     * previous to the last run.
     */
    @Test
    public void scheduledBackups_shouldNotIncludeTransactionsPreviousToTheLastRun() {
        ScheduledAction scheduledBackup = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        scheduledBackup.setStartTime(LocalDateTime.now().minusDays(15).toDate().getTime());
        scheduledBackup.setLastRun(LocalDateTime.now().minusDays(8).toDate().getTime());
        long previousLastRun = scheduledBackup.getLastRunTime();
        scheduledBackup.setExecutionCount(1);
        scheduledBackup.setRecurrence(PeriodType.WEEK, 1);
        ExportParams backupParams = new ExportParams(ExportFormat.QIF);
        backupParams.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        backupParams.setExportStartTime(new Timestamp(scheduledBackup.getStartTime()));
        scheduledBackup.setTag(backupParams.toCsv());

        // Create a transaction with a modified date previous to the last run
        Transaction transaction = new Transaction("Tandoori express");
        Split split = new Split(new Money("10", Commodity.DEFAULT_COMMODITY.getCurrencyCode()),
                                mBaseAccount.getUID());
        split.setType(TransactionType.DEBIT);
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(mTransferAccount.getUID()));
        mTransactionsDbAdapter.addRecord(transaction);
        // We set the date directly in the database as the corresponding field
        // is ignored when the object is stored. It's set through a trigger instead.
        setTransactionInDbModifiedTimestamp(transaction.getUID(),
                new Timestamp(LocalDateTime.now().minusDays(9).toDate().getTime()));

        File backupFolder = new File(
                Exporter.getExportFolderPath(BooksDbAdapter.getInstance().getActiveBookUID()));
        assertThat(backupFolder).exists();
        assertThat(backupFolder.listFiles()).isEmpty();

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledBackup);
        ScheduledActionService.processScheduledActions(actions, mDb);

        assertThat(scheduledBackup.getExecutionCount()).isEqualTo(2);
        assertThat(scheduledBackup.getLastRunTime()).isGreaterThan(previousLastRun);
        assertThat(backupFolder.listFiles()).hasSize(0);
    }

    /**
     * Sets the transaction modified timestamp directly in the database.
     *
     * @param transactionUID UID of the transaction to set the modified timestamp.
     * @param timestamp new modified timestamp.
     */
    private void setTransactionInDbModifiedTimestamp(String transactionUID, Timestamp timestamp) {
        ContentValues values = new ContentValues();
        values.put(DatabaseSchema.TransactionEntry.COLUMN_MODIFIED_AT,
                   TimestampHelper.getUtcStringFromTimestamp(timestamp));
        mTransactionsDbAdapter.updateTransaction(values, "uid = ?",
                                                 new String[]{transactionUID});
    }

    /**
     * Tests that an scheduled backup includes transactions added or modified
     * after the last run.
     */
    @Test
    public void scheduledBackups_shouldIncludeTransactionsAfterTheLastRun() {
        ScheduledAction scheduledBackup = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        scheduledBackup.setStartTime(LocalDateTime.now().minusDays(15).toDate().getTime());
        scheduledBackup.setLastRun(LocalDateTime.now().minusDays(8).toDate().getTime());
        long previousLastRun = scheduledBackup.getLastRunTime();
        scheduledBackup.setExecutionCount(1);
        scheduledBackup.setRecurrence(PeriodType.WEEK, 1);
        ExportParams backupParams = new ExportParams(ExportFormat.QIF);
        backupParams.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        backupParams.setExportStartTime(new Timestamp(scheduledBackup.getStartTime()));
        scheduledBackup.setTag(backupParams.toCsv());

        Transaction transaction = new Transaction("Orient palace");
        Split split = new Split(new Money("10", Commodity.DEFAULT_COMMODITY.getCurrencyCode()),
                mBaseAccount.getUID());
        split.setType(TransactionType.DEBIT);
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(mTransferAccount.getUID()));
        mTransactionsDbAdapter.addRecord(transaction);

        File backupFolder = new File(
                Exporter.getExportFolderPath(BooksDbAdapter.getInstance().getActiveBookUID()));
        assertThat(backupFolder).exists();
        assertThat(backupFolder.listFiles()).isEmpty();

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledBackup);
        ScheduledActionService.processScheduledActions(actions, mDb);

        assertThat(scheduledBackup.getExecutionCount()).isEqualTo(2);
        assertThat(scheduledBackup.getLastRunTime()).isGreaterThan(previousLastRun);
        assertThat(backupFolder.listFiles()).hasSize(1);
    }

    @After
    public void tearDown(){
        TransactionsDbAdapter.getInstance().deleteAllRecords();
    }
}
