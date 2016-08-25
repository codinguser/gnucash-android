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

import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
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
import org.gnucash.android.service.ScheduledActionService;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.joda.time.DateTime;
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

    public void createAccounts(){
        try {
            String bookUID = GncXmlImporter.parse(GnuCashApplication.getAppContext().getResources().openRawResource(R.raw.default_accounts));
            GnuCashApplication.loadBook(bookUID);
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

        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        transactionsDbAdapter.addRecord(mTemplateTransaction, DatabaseAdapter.UpdateMethod.insert);

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
        scheduledAction.setActionUID(mActionUID);

        scheduledAction.setRecurrence(PeriodType.WEEK, 2);
        ScheduledActionDbAdapter.getInstance().addRecord(scheduledAction, DatabaseAdapter.UpdateMethod.insert);

        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        assertThat(transactionsDbAdapter.getRecordsCount()).isZero();

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledAction);
        ScheduledActionService.processScheduledActions(actions, mDb);

        int weeks = Weeks.weeksBetween(startTime, new DateTime(2016, 8, 29, 10, 0)).getWeeks();
        int expectedTransactionCount = weeks/2;

        assertThat(transactionsDbAdapter.getRecordsCount()).isEqualTo(expectedTransactionCount);
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
     * Test that the end time for scheduled actions should be respected
     */
    @Test
    public void scheduledActionsWithEndTimeInPast_shouldBeExecuted(){
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
    @Test(expected = IllegalArgumentException.class)
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
    }

    //// FIXME: 16.08.2016 Cannot find the file after export. But the export task is called and run
    public void scheduledBackups_shouldRunOnlyOnce(){
        ScheduledAction scheduledBackup = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        scheduledBackup.setStartTime(new DateTime(2016, 2, 17, 17, 0).getMillis());
        scheduledBackup.setRecurrence(PeriodType.MONTH, 1);
        scheduledBackup.setExecutionCount(2);

        ExportParams backupParams = new ExportParams(ExportFormat.XML);
        backupParams.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        scheduledBackup.setTag(backupParams.toCsv());

        File backupFolder = new File(Exporter.getBackupFolderPath(BooksDbAdapter.getInstance().getActiveBookUID()));
        assertThat(backupFolder).exists();
        assertThat(backupFolder.listFiles()).isEmpty();

        List<ScheduledAction> actions = new ArrayList<>();
        actions.add(scheduledBackup);
        ScheduledActionService.processScheduledActions(actions, mDb);

        File[] backupFiles = backupFolder.listFiles();
        assertThat(backupFiles).hasSize(1);
        assertThat(backupFiles[0]).hasExtension("gnca");
    }


    @After
    public void tearDown(){
        TransactionsDbAdapter.getInstance().deleteAllRecords();
    }
}
