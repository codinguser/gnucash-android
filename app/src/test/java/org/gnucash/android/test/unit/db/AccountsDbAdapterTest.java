package org.gnucash.android.test.unit.db;

import android.database.sqlite.SQLiteDatabase;

import org.assertj.core.data.Index;
import org.gnucash.android.BuildConfig;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.BudgetAmountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetsDbAdapter;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.adapter.PricesDbAdapter;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.importer.GncXmlImporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.BudgetAmount;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.Price;
import org.gnucash.android.model.Recurrence;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.test.unit.util.GnucashTestRunner;
import org.gnucash.android.test.unit.util.ShadowCrashlytics;
import org.gnucash.android.test.unit.util.ShadowUserVoice;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(GnucashTestRunner.class) //package is required so that resources can be found in dev mode
@Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class AccountsDbAdapterTest{

	private static final String BRAVO_ACCOUNT_NAME = "Bravo";
	private static final String ALPHA_ACCOUNT_NAME = "Alpha";
    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;

	@Before
	public void setUp() throws Exception {
        initAdapters(null);
	}

    /**
     * Initialize database adapters for a specific book.
     * This method should be called everytime a new book is loaded into the database
     * @param bookUID GUID of the GnuCash book
     */
    private void initAdapters(String bookUID){
        if (bookUID == null){
            mSplitsDbAdapter = SplitsDbAdapter.getInstance();
            mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
            mAccountsDbAdapter = AccountsDbAdapter.getInstance();
        } else {
            DatabaseHelper databaseHelper = new DatabaseHelper(GnuCashApplication.getAppContext(), bookUID);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            mSplitsDbAdapter = new SplitsDbAdapter(db);
            mTransactionsDbAdapter = new TransactionsDbAdapter(db, mSplitsDbAdapter);
            mAccountsDbAdapter = new AccountsDbAdapter(db, mTransactionsDbAdapter);
            BooksDbAdapter.getInstance().setActive(bookUID);
        }
    }

    /**
     * Test that the list of accounts is always returned sorted alphabetically
     */
    @Test
	public void shouldBeAlphabeticallySortedByDefault(){
        Account first = new Account(ALPHA_ACCOUNT_NAME);
        Account second = new Account(BRAVO_ACCOUNT_NAME);
        //purposefully added the second after the first
        mAccountsDbAdapter.addRecord(second);
        mAccountsDbAdapter.addRecord(first);

		List<Account> accountsList = mAccountsDbAdapter.getAllRecords();
		assertEquals(2, accountsList.size());
		//bravo was saved first, but alpha should be first alphabetically
        assertThat(accountsList).contains(first, Index.atIndex(0));
        assertThat(accountsList).contains(second, Index.atIndex(1));
	}

    @Test
    public void bulkAddAccountsShouldNotModifyTransactions(){
        Account account1 = new Account("AlphaAccount");
        Account account2 = new Account("BetaAccount");
        Transaction transaction = new Transaction("MyTransaction");
        Split split = new Split(Money.getZeroInstance(), account1.getUID());
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(account2.getUID()));
        account1.addTransaction(transaction);
        account2.addTransaction(transaction);

        List<Account> accounts = new ArrayList<>();
        accounts.add(account1);
        accounts.add(account2);

        mAccountsDbAdapter.bulkAddRecords(accounts);

        SplitsDbAdapter splitsDbAdapter = SplitsDbAdapter.getInstance();
        assertThat(splitsDbAdapter.getSplitsForTransactionInAccount(transaction.getUID(), account1.getUID())).hasSize(1);
        assertThat(splitsDbAdapter.getSplitsForTransactionInAccount(transaction.getUID(), account2.getUID())).hasSize(1);

        assertThat(mAccountsDbAdapter.getRecord(account1.getUID()).getTransactions()).hasSize(1);
    }

    @Test
    public void shouldAddAccountsToDatabase(){
        Account account1 = new Account("AlphaAccount");
        Account account2 = new Account("BetaAccount");
        Transaction transaction = new Transaction("MyTransaction");
        Split split = new Split(Money.getZeroInstance(), account1.getUID());
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(account2.getUID()));
        account1.addTransaction(transaction);
        account2.addTransaction(transaction);

        mAccountsDbAdapter.addRecord(account1);
        mAccountsDbAdapter.addRecord(account2);

        Account firstAccount = mAccountsDbAdapter.getRecord(account1.getUID());
        assertThat(firstAccount).isNotNull();
        assertThat(firstAccount.getUID()).isEqualTo(account1.getUID());
        assertThat(firstAccount.getFullName()).isEqualTo(account1.getFullName());

        Account secondAccount = mAccountsDbAdapter.getRecord(account2.getUID());
        assertThat(secondAccount).isNotNull();
        assertThat(secondAccount.getUID()).isEqualTo(account2.getUID());

        assertThat(mTransactionsDbAdapter.getRecordsCount()).isEqualTo(1);
    }

    /**
     * Tests the foreign key constraint "ON DELETE CASCADE" between accounts and splits
     */
    @Test
    public void shouldDeleteSplitsWhenAccountDeleted(){
        Account first = new Account(ALPHA_ACCOUNT_NAME);
        first.setUID(ALPHA_ACCOUNT_NAME);
        Account second = new Account(BRAVO_ACCOUNT_NAME);
        second.setUID(BRAVO_ACCOUNT_NAME);

        mAccountsDbAdapter.addRecord(second);
        mAccountsDbAdapter.addRecord(first);

        Transaction transaction = new Transaction("TestTrn");
        Split split = new Split(Money.getZeroInstance(), ALPHA_ACCOUNT_NAME);
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(BRAVO_ACCOUNT_NAME));

        mTransactionsDbAdapter.addRecord(transaction);

        mAccountsDbAdapter.deleteRecord(ALPHA_ACCOUNT_NAME);

        Transaction trxn = mTransactionsDbAdapter.getRecord(transaction.getUID());
        assertThat(trxn.getSplits().size()).isEqualTo(1);
        assertThat(trxn.getSplits().get(0).getAccountUID()).isEqualTo(BRAVO_ACCOUNT_NAME);
    }

    /**
     * Tests that a ROOT account will always be created in the system
     */
    @Test
    public void shouldCreateDefaultRootAccount(){
        Account account = new Account("Some account");
        mAccountsDbAdapter.addRecord(account);
        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(2L);

        List<Account> accounts = mAccountsDbAdapter.getSimpleAccountList();
        assertThat(accounts).extracting("mAccountType").contains(AccountType.ROOT);

        String rootAccountUID = mAccountsDbAdapter.getOrCreateGnuCashRootAccountUID();
        assertThat(rootAccountUID).isEqualTo(accounts.get(1).getParentUID());
    }

    @Test
    public void shouldUpdateFullNameAfterParentChange(){
        Account parent = new Account("Test");
        Account child = new Account("Child");

        mAccountsDbAdapter.addRecord(parent);
        mAccountsDbAdapter.addRecord(child);

        child.setParentUID(parent.getUID());
        mAccountsDbAdapter.addRecord(child);

        child = mAccountsDbAdapter.getRecord(child.getUID());
        parent = mAccountsDbAdapter.getRecord(parent.getUID());

        assertThat(mAccountsDbAdapter.getSubAccountCount(parent.getUID())).isEqualTo(1);
        assertThat(parent.getUID()).isEqualTo(child.getParentUID());

        assertThat(child.getFullName()).isEqualTo("Test:Child");
    }

    @Test
    public void shouldAddTransactionsAndSplitsWhenAddingAccounts(){
        Account account = new Account("Test");
        mAccountsDbAdapter.addRecord(account);

        Transaction transaction = new Transaction("Test description");
        Split split = new Split(Money.getZeroInstance(), account.getUID());
        transaction.addSplit(split);
        Account account1 = new Account("Transfer account");
        transaction.addSplit(split.createPair(account1.getUID()));
        account1.addTransaction(transaction);

        mAccountsDbAdapter.addRecord(account1);

        assertThat(mTransactionsDbAdapter.getRecordsCount()).isEqualTo(1);
        assertThat(mSplitsDbAdapter.getRecordsCount()).isEqualTo(2);
        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(3); //ROOT account automatically added

    }

    @Test
    public void shouldClearAllTablesWhenDeletingAllAccounts(){
        Account account = new Account("Test");
        Transaction transaction = new Transaction("Test description");
        Split split = new Split(Money.getZeroInstance(), account.getUID());
        transaction.addSplit(split);
        Account account2 = new Account("Transfer account");
        transaction.addSplit(split.createPair(account2.getUID()));

        mAccountsDbAdapter.addRecord(account);
        mAccountsDbAdapter.addRecord(account2);

        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        scheduledAction.setActionUID("Test-uid");
        scheduledAction.setRecurrence(new Recurrence(PeriodType.WEEK));
        ScheduledActionDbAdapter scheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();

        scheduledActionDbAdapter.addRecord(scheduledAction);

        Budget budget = new Budget("Test");
        BudgetAmount budgetAmount = new BudgetAmount(Money.getZeroInstance(), account.getUID());
        budget.addBudgetAmount(budgetAmount);
        budget.setRecurrence(new Recurrence(PeriodType.MONTH));
        BudgetsDbAdapter.getInstance().addRecord(budget);

        mAccountsDbAdapter.deleteAllRecords();

        assertThat(mAccountsDbAdapter.getRecordsCount()).isZero();
        assertThat(mTransactionsDbAdapter.getRecordsCount()).isZero();
        assertThat(mSplitsDbAdapter.getRecordsCount()).isZero();
        assertThat(scheduledActionDbAdapter.getRecordsCount()).isZero();
        assertThat(BudgetAmountsDbAdapter.getInstance().getRecordsCount()).isZero();
        assertThat(BudgetsDbAdapter.getInstance().getRecordsCount()).isZero();
        assertThat(PricesDbAdapter.getInstance().getRecordsCount()).isZero(); //prices should remain
        assertThat(CommoditiesDbAdapter.getInstance().getRecordsCount()).isGreaterThan(50); //commodities should remain
    }

    @Test
    public void simpleAccountListShouldNotContainTransactions(){
        Account account = new Account("Test");
        Transaction transaction = new Transaction("Test description");
        Split split = new Split(Money.getZeroInstance(), account.getUID());
        transaction.addSplit(split);
        Account account1 = new Account("Transfer");
        transaction.addSplit(split.createPair(account1.getUID()));

        mAccountsDbAdapter.addRecord(account);
        mAccountsDbAdapter.addRecord(account1);

        List<Account> accounts = mAccountsDbAdapter.getSimpleAccountList();
        for (Account testAcct : accounts) {
            assertThat(testAcct.getTransactionCount()).isZero();
        }
    }

    @Test
    public void shouldComputeAccountBalanceCorrectly(){
        Account account = new Account("Test", Commodity.USD);
        account.setAccountType(AccountType.ASSET); //debit normal account balance
        Account transferAcct = new Account("Transfer");

        mAccountsDbAdapter.addRecord(account);
        mAccountsDbAdapter.addRecord(transferAcct);

        Transaction transaction = new Transaction("Test description");
        mTransactionsDbAdapter.addRecord(transaction);
        Split split = new Split(new Money(BigDecimal.TEN, Commodity.USD), account.getUID());
        split.setTransactionUID(transaction.getUID());
        split.setType(TransactionType.DEBIT);
        mSplitsDbAdapter.addRecord(split);

        split = new Split(new Money("4.99", "USD"), account.getUID());
        split.setTransactionUID(transaction.getUID());
        split.setType(TransactionType.DEBIT);
        mSplitsDbAdapter.addRecord(split);

        split = new Split(new Money("1.19", "USD"), account.getUID());
        split.setTransactionUID(transaction.getUID());
        split.setType(TransactionType.CREDIT);
        mSplitsDbAdapter.addRecord(split);

        split = new Split(new Money("3.49", "EUR"), account.getUID());
        split.setTransactionUID(transaction.getUID());
        split.setType(TransactionType.DEBIT);
        mSplitsDbAdapter.addRecord(split);

        split = new Split(new Money("8.39", "USD"), transferAcct.getUID());
        split.setTransactionUID(transaction.getUID());
        mSplitsDbAdapter.addRecord(split);

        //balance computation ignores the currency of the split
        Money balance = mAccountsDbAdapter.getAccountBalance(account.getUID());
        Money expectedBalance = new Money("17.29", "USD"); //EUR splits should be ignored

        assertThat(balance).isEqualTo(expectedBalance);
    }

    /**
     * Test creating an account hierarchy by specifying fully qualified name
     */
    @Test
    public void shouldCreateAccountHierarchy(){
        String uid = mAccountsDbAdapter.createAccountHierarchy("Assets:Current Assets:Cash in Wallet", AccountType.ASSET);

        List<Account> accounts = mAccountsDbAdapter.getAllRecords();
        assertThat(accounts).hasSize(3);
        assertThat(accounts).extracting("mUID").contains(uid);
    }

    @Test
    public void shouldRecursivelyDeleteAccount(){
        Account account = new Account("Parent");
        Account account2 = new Account("Child");
        account2.setParentUID(account.getUID());

        Transaction transaction = new Transaction("Random");
        account2.addTransaction(transaction);

        Split split = new Split(Money.getZeroInstance(), account.getUID());
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(account2.getUID()));

        mAccountsDbAdapter.addRecord(account);
        mAccountsDbAdapter.addRecord(account2);

        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(3);
        assertThat(mTransactionsDbAdapter.getRecordsCount()).isEqualTo(1);
        assertThat(mSplitsDbAdapter.getRecordsCount()).isEqualTo(2);

        boolean result = mAccountsDbAdapter.recursiveDeleteAccount(mAccountsDbAdapter.getID(account.getUID()));
        assertThat(result).isTrue();

        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(1); //the root account
        assertThat(mTransactionsDbAdapter.getRecordsCount()).isZero();
        assertThat(mSplitsDbAdapter.getRecordsCount()).isZero();

    }

    @Test
    public void shouldGetDescendantAccounts(){
        loadDefaultAccounts();

        String uid = mAccountsDbAdapter.findAccountUidByFullName("Expenses:Auto");
        List<String> descendants = mAccountsDbAdapter.getDescendantAccountUIDs(uid, null, null);

        assertThat(descendants).hasSize(4);
    }

    @Test
    public void shouldReassignDescendantAccounts(){
        loadDefaultAccounts();

        String savingsAcctUID = mAccountsDbAdapter.findAccountUidByFullName("Assets:Current Assets:Savings Account");

        String currentAssetsUID = mAccountsDbAdapter.findAccountUidByFullName("Assets:Current Assets");
        String assetsUID = mAccountsDbAdapter.findAccountUidByFullName("Assets");

        assertThat(mAccountsDbAdapter.getParentAccountUID(savingsAcctUID)).isEqualTo(currentAssetsUID);
        mAccountsDbAdapter.reassignDescendantAccounts(currentAssetsUID, assetsUID);
        assertThat(mAccountsDbAdapter.getParentAccountUID(savingsAcctUID)).isEqualTo(assetsUID);

        assertThat(mAccountsDbAdapter.getFullyQualifiedAccountName(savingsAcctUID)).isEqualTo("Assets:Savings Account");

    }

    @Test
    public void shouldCreateImbalanceAccountOnDemand(){
        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(1L);

        Currency usd = Currency.getInstance("USD");
        String imbalanceUID = mAccountsDbAdapter.getImbalanceAccountUID(usd);
        assertThat(imbalanceUID).isNull();
        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(1L);

        imbalanceUID = mAccountsDbAdapter.getOrCreateImbalanceAccountUID(usd);
        assertThat(imbalanceUID).isNotNull().isNotEmpty();
        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(2);
    }

    @Test
    public void editingAccountShouldNotDeleteTemplateSplits(){
        Account account = new Account("First", Commodity.EUR);
        Account transferAccount = new Account("Transfer", Commodity.EUR);

        mAccountsDbAdapter.addRecord(account);
        mAccountsDbAdapter.addRecord(transferAccount);

        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(3); //plus root account

        Money money = new Money(BigDecimal.TEN, Commodity.EUR);
        Transaction transaction = new Transaction("Template");
        transaction.setTemplate(true);
        transaction.setCommodity(Commodity.EUR);
        Split split = new Split(money, account.getUID());
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(transferAccount.getUID()));

        mTransactionsDbAdapter.addRecord(transaction);
        List<Transaction> transactions = mTransactionsDbAdapter.getAllRecords();

        assertThat(mTransactionsDbAdapter.getScheduledTransactionsForAccount(account.getUID())).hasSize(1);

        //edit the account
        account.setName("Edited account");
        mAccountsDbAdapter.addRecord(account);

        assertThat(mTransactionsDbAdapter.getScheduledTransactionsForAccount(account.getUID())).hasSize(1);
        assertThat(mSplitsDbAdapter.getSplitsForTransaction(transaction.getUID())).hasSize(2);
    }

    @Test
    public void testGetCurrenciesInUse(){
        int expectedSize = 1; //there is already a root account in the database
        List<Currency> currencies = mAccountsDbAdapter.getCurrenciesInUse();
        assertThat(currencies).hasSize(expectedSize);

        Account account = new Account("Dummy", Commodity.USD);
        mAccountsDbAdapter.addRecord(account); //default currency is also USD
        assertThat(mAccountsDbAdapter.getCurrenciesInUse()).hasSize(expectedSize);

        account = new Account("Dummy", Commodity.EUR);
        mAccountsDbAdapter.addRecord(account);
        assertThat(mAccountsDbAdapter.getCurrenciesInUse()).hasSize(++expectedSize);

    }

    /**
     * Opening an XML file should set the default currency to that used by the most accounts in the file
     */
    @Test
    public void importingXml_shouldSetDefaultCurrency(){
        GnuCashApplication.setDefaultCurrencyCode("JPY");

        assertThat(GnuCashApplication.getDefaultCurrencyCode()).isEqualTo("JPY");
        assertThat(Commodity.DEFAULT_COMMODITY).isEqualTo(Commodity.JPY);

        mAccountsDbAdapter.deleteAllRecords();
        loadDefaultAccounts();

        assertThat(GnuCashApplication.getDefaultCurrencyCode()).isNotEqualTo("JPY");

        Currency currency = Currency.getInstance(GnuCashApplication.getDefaultLocale());
        String expectedCode = currency.getCurrencyCode();
        Commodity expectedDefaultCommodity = CommoditiesDbAdapter.getInstance().getCommodity(expectedCode);

        assertThat(GnuCashApplication.getDefaultCurrencyCode()).isEqualTo(expectedCode);
        assertThat(Commodity.DEFAULT_COMMODITY).isEqualTo(expectedDefaultCommodity);

        System.out.println("Default currency is now: " + expectedCode);
    }

    /**
     * Loads the default accounts from file resource
     */
    private void loadDefaultAccounts(){
        try {
            String bookUID = GncXmlImporter.parse(GnuCashApplication.getAppContext().getResources().openRawResource(R.raw.default_accounts));
            initAdapters(bookUID);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not create default accounts");
        }
    }


	@After
	public void tearDown() throws Exception {
		mAccountsDbAdapter.deleteAllRecords();
	}
}
