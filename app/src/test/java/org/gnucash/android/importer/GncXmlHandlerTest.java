/*
 * Copyright (c) 2016 Àlex Magaz Graça <rivaldi8@gmail.com>
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
package org.gnucash.android.importer;

import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.export.xml.GncXmlHelper;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Imports GnuCash XML files and checks the objects defined in them are imported correctly.
 */
@RunWith(GnucashTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class GncXmlHandlerTest {
    private BooksDbAdapter mBooksDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private AccountsDbAdapter mAccountsDbAdapter;

    @Before
    public void setUp() throws Exception {
        mBooksDbAdapter = BooksDbAdapter.getInstance();
        mBooksDbAdapter.deleteAllRecords();
        assertThat(mBooksDbAdapter.getRecordsCount()).isZero();
    }

    private String importGnuCashXml(String filename) {
        SAXParser parser;
        GncXmlHandler handler = null;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
            XMLReader reader = parser.getXMLReader();
            handler = new GncXmlHandler();
            reader.setContentHandler(handler);
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);
            InputSource inputSource = new InputSource(new BufferedInputStream(inputStream));
            reader.parse(inputSource);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            fail();
        }
        return handler.getBookUID();
    }

    private void setUpDbAdapters(String bookUID) {
        DatabaseHelper databaseHelper = new DatabaseHelper(GnuCashApplication.getAppContext(), bookUID);
        SQLiteDatabase mainDb = databaseHelper.getReadableDatabase();
        mTransactionsDbAdapter = new TransactionsDbAdapter(mainDb, new SplitsDbAdapter(mainDb));
        mAccountsDbAdapter = new AccountsDbAdapter(mainDb, mTransactionsDbAdapter);
    }

    /**
     * Tests basic accounts import.
     *
     * <p>Checks hierarchy and attributes. We should have:</p>
     * <pre>
     * Root
     * |_ Assets
     * |   |_ Cash in wallet
     * |_ Expenses
     *     |_ Dining
     * </pre>
     */
    @Test
    public void testAccountsImport() {
        String bookUID = importGnuCashXml("testAccountsImport.xml");
        setUpDbAdapters(bookUID);

        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(5); // 4 accounts + root

        Account rootAccount = mAccountsDbAdapter.getRecord("308ade8cf0be2b0b05c5eec3114a65fa");
        assertThat(rootAccount.getParentUID()).isNull();
        assertThat(rootAccount.getName()).isEqualTo("Root Account");
        assertThat(rootAccount.isHidden()).isTrue();

        Account assetsAccount = mAccountsDbAdapter.getRecord("3f44d61cb1afd201e8ea5a54ec4fbbff");
        assertThat(assetsAccount.getParentUID()).isEqualTo(rootAccount.getUID());
        assertThat(assetsAccount.getName()).isEqualTo("Assets");
        assertThat(assetsAccount.isHidden()).isFalse();
        assertThat(assetsAccount.isPlaceholderAccount()).isTrue();
        assertThat(assetsAccount.getAccountType()).isEqualTo(AccountType.ASSET);

        Account diningAccount = mAccountsDbAdapter.getRecord("6a7cf8267314992bdddcee56d71a3908");
        assertThat(diningAccount.getParentUID()).isEqualTo("9b607f63aecb1a175556676904432365");
        assertThat(diningAccount.getName()).isEqualTo("Dining");
        assertThat(diningAccount.getDescription()).isEqualTo("Dining");
        assertThat(diningAccount.isHidden()).isFalse();
        assertThat(diningAccount.isPlaceholderAccount()).isFalse();
        assertThat(diningAccount.isFavorite()).isFalse();
        assertThat(diningAccount.getAccountType()).isEqualTo(AccountType.EXPENSE);
        assertThat(diningAccount.getCommodity().getCurrencyCode()).isEqualTo("USD");
        assertThat(diningAccount.getColor()).isEqualTo(Account.DEFAULT_COLOR);
        assertThat(diningAccount.getDefaultTransferAccountUID()).isNull();
    }

    /**
     * Tests importing a simple transaction with default splits.
     *
     * @throws ParseException
     */
    @Test
    public void simpleTransactionImport() throws ParseException {
        String bookUID = importGnuCashXml("simpleTransactionImport.xml");
        setUpDbAdapters(bookUID);

        assertThat(mTransactionsDbAdapter.getRecordsCount()).isEqualTo(1);

        Transaction transaction = mTransactionsDbAdapter.getRecord("b33c8a6160494417558fd143731fc26a");

        // Check attributes
        assertThat(transaction.getDescription()).isEqualTo("Kahuna Burger");
        assertThat(transaction.getCommodity().getCurrencyCode()).isEqualTo("USD");
        assertThat(transaction.getNote()).isEqualTo("");
        assertThat(transaction.getScheduledActionUID()).isNull();
        assertThat(transaction.isExported()).isTrue();
        assertThat(transaction.isTemplate()).isFalse();
        assertThat(transaction.getTimeMillis()).
                isEqualTo(GncXmlHelper.parseDate("2016-08-23 00:00:00 +0200"));
        assertThat(transaction.getCreatedTimestamp().getTime()).
                isEqualTo(GncXmlHelper.parseDate("2016-08-23 12:44:19 +0200"));

        // Check splits
        assertThat(transaction.getSplits().size()).isEqualTo(2);
        // FIXME: don't depend on the order
        Split split1 = transaction.getSplits().get(0);
        assertThat(split1.getUID()).isEqualTo("ad2cbc774fc4e71885d17e6932448e8e");
        assertThat(split1.getAccountUID()).isEqualTo("6a7cf8267314992bdddcee56d71a3908");
        assertThat(split1.getTransactionUID()).isEqualTo("b33c8a6160494417558fd143731fc26a");
        assertThat(split1.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(split1.getMemo()).isNull();
        assertThat(split1.getValue()).isEqualTo(new Money("10", "USD"));
        assertThat(split1.getQuantity()).isEqualTo(new Money("10", "USD"));
        assertThat(split1.getReconcileState()).isEqualTo('n');

        Split split2 = transaction.getSplits().get(1);
        assertThat(split2.getUID()).isEqualTo("61d4d604bc00a59cabff4e8875d00bee");
        assertThat(split2.getAccountUID()).isEqualTo("dae686a1636addc0dae1ae670701aa4a");
        assertThat(split2.getTransactionUID()).isEqualTo("b33c8a6160494417558fd143731fc26a");
        assertThat(split2.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(split2.getMemo()).isNull();
        assertThat(split2.getValue()).isEqualTo(new Money("10", "USD"));
        assertThat(split2.getQuantity()).isEqualTo(new Money("10", "USD"));
        assertThat(split2.getReconcileState()).isEqualTo('n');
        assertThat(split2.isPairOf(split1)).isTrue();
    }

    /**
     * Tests importing a simple scheduled transaction with default splits.
     */
    //@Test Disabled as currently amounts are only read from credit/debit-numeric
    // slots and transactions without amount are ignored.
    public void testImportSimpleScheduledTransaction() throws ParseException {
        String bookUID = importGnuCashXml("testImportSimpleScheduledTransaction.xml");
        setUpDbAdapters(bookUID);

        assertThat(mTransactionsDbAdapter.getTemplateTransactionsCount()).isEqualTo(1);

        Transaction scheduledTransaction =
                mTransactionsDbAdapter.getRecord("b645bef06d0844aece6424ceeec03983");

        // Check attributes
        assertThat(scheduledTransaction.getDescription()).isEqualTo("Los pollos hermanos");
        assertThat(scheduledTransaction.getCommodity().getCurrencyCode()).isEqualTo("USD");
        assertThat(scheduledTransaction.getNote()).isEqualTo("");
        assertThat(scheduledTransaction.getScheduledActionUID()).isNull();
        assertThat(scheduledTransaction.isExported()).isTrue();
        assertThat(scheduledTransaction.isTemplate()).isTrue();
        assertThat(scheduledTransaction.getTimeMillis())
                .isEqualTo(GncXmlHelper.parseDate("2016-08-24 00:00:00 +0200"));
        assertThat(scheduledTransaction.getCreatedTimestamp().getTime())
                .isEqualTo(GncXmlHelper.parseDate("2016-08-24 19:50:15 +0200"));

        // Check splits
        assertThat(scheduledTransaction.getSplits().size()).isEqualTo(2);

        Split split1 = scheduledTransaction.getSplits().get(0);
        assertThat(split1.getUID()).isEqualTo("f66794ef262aac3ae085ecc3030f2769");
        assertThat(split1.getAccountUID()).isEqualTo("6a7cf8267314992bdddcee56d71a3908");
        assertThat(split1.getTransactionUID()).isEqualTo("b645bef06d0844aece6424ceeec03983");
        assertThat(split1.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(split1.getMemo()).isNull();
        assertThat(split1.getValue()).isEqualTo(new Money("20", "USD"));
        // FIXME: the quantity is always 0 as it's set from <split:quantity> instead
        // of from the slots
        //assertThat(split1.getQuantity()).isEqualTo(new Money("20", "USD"));

        Split split2 = scheduledTransaction.getSplits().get(1);
        assertThat(split2.getUID()).isEqualTo("57e2be6ca6b568f8f7c9b2e455e1e21f");
        assertThat(split2.getAccountUID()).isEqualTo("dae686a1636addc0dae1ae670701aa4a");
        assertThat(split2.getTransactionUID()).isEqualTo("b645bef06d0844aece6424ceeec03983");
        assertThat(split2.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(split2.getMemo()).isNull();
        assertThat(split2.getValue()).isEqualTo(new Money("20", "USD"));
        // FIXME: the quantity is always 0 as it's set from <split:quantity> instead
        // of from the slots
        //assertThat(split2.getQuantity()).isEqualTo(new Money("20", "USD"));
        assertThat(split2.isPairOf(split1)).isTrue();
    }

    /**
     * Checks for bug 562 - Scheduled transaction imported with imbalanced splits.
     *
     * <p>Happens when an scheduled transaction is defined with both credit and
     * debit slots in each split.</p>
     */
    @Test
    public void bug562_scheduledTransactionImportedWithImbalancedSplits() throws ParseException {
        String bookUID = importGnuCashXml("bug562_scheduledTransactionImportedWithImbalancedSplits.xml");
        setUpDbAdapters(bookUID);

        assertThat(mTransactionsDbAdapter.getTemplateTransactionsCount()).isEqualTo(1);

        Transaction scheduledTransaction =
                mTransactionsDbAdapter.getRecord("b645bef06d0844aece6424ceeec03983");

        // Ensure it's the correct transaction
        assertThat(scheduledTransaction.getDescription()).isEqualTo("Los pollos hermanos");
        assertThat(scheduledTransaction.isTemplate()).isTrue();

        // Check splits
        assertThat(scheduledTransaction.getSplits().size()).isEqualTo(2);

        Split split1 = scheduledTransaction.getSplits().get(0);
        assertThat(split1.getAccountUID()).isEqualTo("6a7cf8267314992bdddcee56d71a3908");
        assertThat(split1.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(split1.getValue()).isEqualTo(new Money("20", "USD"));
        // FIXME: the quantity is always 0 as it's set from <split:quantity> instead
        // of from the slots
        //assertThat(split1.getQuantity()).isEqualTo(new Money("20", "USD"));

        Split split2 = scheduledTransaction.getSplits().get(1);
        assertThat(split2.getAccountUID()).isEqualTo("dae686a1636addc0dae1ae670701aa4a");
        assertThat(split2.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(split2.getValue()).isEqualTo(new Money("20", "USD"));
        // FIXME: the quantity is always 0 as it's set from <split:quantity> instead
        // of from the slots
        //assertThat(split2.getQuantity()).isEqualTo(new Money("20", "USD"));
        assertThat(split2.isPairOf(split1)).isTrue();
    }
}