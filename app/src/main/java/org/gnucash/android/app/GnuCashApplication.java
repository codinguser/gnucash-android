/*
 * Copyright (c) 2013 - 2014 Ngewi Fet <ngewif@gmail.com>
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
package org.gnucash.android.app;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.facebook.stetho.Stetho;
import com.uservoice.uservoicesdk.Config;
import com.uservoice.uservoicesdk.UserVoice;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.R;
import org.gnucash.android.db.BookDbHelper;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.BudgetAmountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetsDbAdapter;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.adapter.PricesDbAdapter;
import org.gnucash.android.db.adapter.RecurrenceDbAdapter;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.service.ScheduledActionService;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.settings.PreferenceActivity;

import java.util.Currency;
import java.util.Locale;

import io.fabric.sdk.android.Fabric;

/**
 * An {@link Application} subclass for retrieving static context
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class GnuCashApplication extends Application{

    /**
     * Lifetime of passcode session
     */
    public static final long SESSION_TIMEOUT = 5 * 1000;

    /**
     * Init time of passcode session
     */
    public static long PASSCODE_SESSION_INIT_TIME = 0L;

    private static Context context;

    private static AccountsDbAdapter mAccountsDbAdapter;

    private static TransactionsDbAdapter mTransactionsDbAdapter;

    private static SplitsDbAdapter mSplitsDbAdapter;

    private static ScheduledActionDbAdapter mScheduledActionDbAdapter;

    private static CommoditiesDbAdapter mCommoditiesDbAdapter;

    private static PricesDbAdapter mPricesDbAdapter;

    private static BudgetsDbAdapter mBudgetsDbAdapter;

    private static BudgetAmountsDbAdapter mBudgetAmountsDbAdapter;

    private static RecurrenceDbAdapter mRecurrenceDbAdapter;

    private static BooksDbAdapter mBooksDbAdapter;
    private static DatabaseHelper mDbHelper;

    /**
     * Returns darker version of specified <code>color</code>.
     * Use for theming the status bar color when setting the color of the actionBar
     */
    public static int darken(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        return Color.HSVToColor(hsv);
    }

    @Override
    public void onCreate(){
        super.onCreate();
        GnuCashApplication.context = getApplicationContext();

        Fabric.with(this, new Crashlytics.Builder().core(
                new CrashlyticsCore.Builder().disabled(!isCrashlyticsEnabled()).build())
                .build());

        setUpUserVoice();

        BookDbHelper bookDbHelper = new BookDbHelper(getApplicationContext());
        mBooksDbAdapter = new BooksDbAdapter(bookDbHelper.getWritableDatabase());

        initializeDatabaseAdapters();
        setDefaultCurrencyCode(getDefaultCurrencyCode());

        if (BuildConfig.DEBUG && !isRoboUnitTest())
            setUpRemoteDebuggingFromChrome();
    }

    /**
     * Initialize database adapter singletons for use in the application
     * This method should be called every time a new book is opened
     */
    private static void initializeDatabaseAdapters() {
        if (mDbHelper != null){ //close if open
            mDbHelper.getReadableDatabase().close();
        }

        mDbHelper = new DatabaseHelper(getAppContext(),
                mBooksDbAdapter.getActiveBookUID());
        SQLiteDatabase mainDb;
        try {
            mainDb = mDbHelper.getWritableDatabase();
        } catch (SQLException e) {
            Crashlytics.logException(e);
            Log.e("GnuCashApplication", "Error getting database: " + e.getMessage(), e);
            mainDb = mDbHelper.getReadableDatabase();
        }

        mSplitsDbAdapter            = new SplitsDbAdapter(mainDb);
        mTransactionsDbAdapter      = new TransactionsDbAdapter(mainDb, mSplitsDbAdapter);
        mAccountsDbAdapter          = new AccountsDbAdapter(mainDb, mTransactionsDbAdapter);
        mRecurrenceDbAdapter        = new RecurrenceDbAdapter(mainDb);
        mScheduledActionDbAdapter   = new ScheduledActionDbAdapter(mainDb, mRecurrenceDbAdapter);
        mPricesDbAdapter            = new PricesDbAdapter(mainDb);
        mCommoditiesDbAdapter       = new CommoditiesDbAdapter(mainDb);
        mBudgetAmountsDbAdapter     = new BudgetAmountsDbAdapter(mainDb);
        mBudgetsDbAdapter           = new BudgetsDbAdapter(mainDb, mBudgetAmountsDbAdapter, mRecurrenceDbAdapter);
    }

    public static AccountsDbAdapter getAccountsDbAdapter() {
        return mAccountsDbAdapter;
    }

    public static TransactionsDbAdapter getTransactionDbAdapter() {
        return mTransactionsDbAdapter;
    }

    public static SplitsDbAdapter getSplitsDbAdapter() {
        return mSplitsDbAdapter;
    }

    public static ScheduledActionDbAdapter getScheduledEventDbAdapter(){
        return mScheduledActionDbAdapter;
    }

    public static CommoditiesDbAdapter getCommoditiesDbAdapter(){
        return mCommoditiesDbAdapter;
    }

    public static PricesDbAdapter getPricesDbAdapter(){
        return mPricesDbAdapter;
    }

    public static BudgetsDbAdapter getBudgetDbAdapter() {
        return mBudgetsDbAdapter;
    }

    public static RecurrenceDbAdapter getRecurrenceDbAdapter() {
        return mRecurrenceDbAdapter;
    }

    public static BudgetAmountsDbAdapter getBudgetAmountsDbAdapter(){
        return mBudgetAmountsDbAdapter;
    }

    public static BooksDbAdapter getBooksDbAdapter(){
        return mBooksDbAdapter;
    }

    /**
     * Loads the book with GUID {@code bookUID} and opens the AccountsActivity
     * @param bookUID GUID of the book to be loaded
     */
    public static void loadBook(@NonNull String bookUID){
        activateBook(bookUID);
        AccountsActivity.start(getAppContext());
    }

    /**
     * Activates the book with unique identifer {@code bookUID}, and refreshes the database adapters
     * @param bookUID GUID of the book to be activated
     */
    public static void activateBook(@NonNull String bookUID){
        mBooksDbAdapter.setActive(bookUID);
        initializeDatabaseAdapters();
    }

    /**
     * Returns the currently active database in the application
     * @return Currently active {@link SQLiteDatabase}
     */
    public static SQLiteDatabase getActiveDb(){
        return mDbHelper.getWritableDatabase();
    }

    /**
     * Returns the application context
     * @return Application {@link Context} object
     */
    public static Context getAppContext() {
        return GnuCashApplication.context;
    }

    /**
     * Checks if crashlytics is enabled
     * @return {@code true} if crashlytics is enabled, {@code false} otherwise
     */
    public static boolean isCrashlyticsEnabled(){
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.key_enable_crashlytics), false);
    }

    /**
     * Returns {@code true} if the app is being run by robolectric
     * @return {@code true} if in unit testing, {@code false} otherwise
     */
    public static boolean isRoboUnitTest(){
        return "robolectric".equals(Build.FINGERPRINT);
    }

    /**
     * Returns <code>true</code> if double entry is enabled in the app settings, <code>false</code> otherwise.
     * If the value is not set, the default value can be specified in the parameters.
     * @return <code>true</code> if double entry is enabled, <code>false</code> otherwise
     */
    public static boolean isDoubleEntryEnabled(){
        SharedPreferences sharedPrefs = PreferenceActivity.getActiveBookSharedPreferences();
        return sharedPrefs.getBoolean(context.getString(R.string.key_use_double_entry), true);
    }

    /**
     * Returns <code>true</code> if setting is enabled to save opening balances after deleting transactions,
     * <code>false</code> otherwise.
     * @param defaultValue Default value to return if double entry is not explicitly set
     * @return <code>true</code> if opening balances should be saved, <code>false</code> otherwise
     */
    public static boolean shouldSaveOpeningBalances(boolean defaultValue){
        SharedPreferences sharedPrefs = PreferenceActivity.getActiveBookSharedPreferences();
        return sharedPrefs.getBoolean(context.getString(R.string.key_save_opening_balances), defaultValue);
    }

    /**
     * Returns the default currency code for the application. <br/>
     * What value is actually returned is determined in this order of priority:<ul>
     *     <li>User currency preference (manually set be user in the app)</li>
     *     <li>Default currency for the device locale</li>
     *     <li>United States Dollars</li>
     * </ul>
     *
     * @return Default currency code string for the application
     */
    public static String getDefaultCurrencyCode(){
        Locale locale = getDefaultLocale();

        String currencyCode = "USD"; //start with USD as the default
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try { //there are some strange locales out there
            currencyCode = Currency.getInstance(locale).getCurrencyCode();
        } catch (Throwable e) {
            Crashlytics.logException(e);
            Log.e(context.getString(R.string.app_name), e.getMessage(), e);
        } finally {
            currencyCode = prefs.getString(context.getString(R.string.key_default_currency), currencyCode);
        }
        return currencyCode;
    }

    /**
     * Sets the default currency for the application in all relevant places:
     * <ul>
     *     <li>Shared preferences</li>
     *     <li>{@link Money#DEFAULT_CURRENCY_CODE}</li>
     *     <li>{@link Commodity#DEFAULT_COMMODITY}</li>
     * </ul>
     * @param currencyCode ISO 4217 currency code
     * @see #getDefaultCurrencyCode()
     */
    public static void setDefaultCurrencyCode(@NonNull String currencyCode){
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(getAppContext().getString(R.string.key_default_currency), currencyCode)
                .apply();
        Money.DEFAULT_CURRENCY_CODE = currencyCode;
        Commodity.DEFAULT_COMMODITY = mCommoditiesDbAdapter.getCommodity(currencyCode);
    }

    /**
     * Returns the default locale which is used for currencies, while handling special cases for
     * locales which are not supported for currency such as en_GB
     * @return The default locale for this device
     */
    public static Locale getDefaultLocale() {
        Locale locale = Locale.getDefault();
        //sometimes the locale en_UK is returned which causes a crash with Currency
        if (locale.getCountry().equals("UK")) {
            locale = new Locale(locale.getLanguage(), "GB");
        }

        //for unsupported locale es_LG
        if (locale.getCountry().equals("LG")){
            locale = new Locale(locale.getLanguage(), "ES");
        }

        if (locale.getCountry().equals("en")){
            locale = Locale.US;
        }
        return locale;
    }

    /**
     * Starts the service for scheduled events and schedules an alarm to call the service twice daily.
     * <p>If the alarm already exists, this method does nothing. If not, the alarm will be created
     * Hence, there is no harm in calling the method repeatedly</p>
     * @param context Application context
     */
    public static void startScheduledActionExecutionService(Context context){
        Intent alarmIntent = new Intent(context, ScheduledActionService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, alarmIntent, PendingIntent.FLAG_NO_CREATE);

        if (pendingIntent != null) //if service is already scheduled, just return
            return;
        else
            pendingIntent = PendingIntent.getService(context, 0, alarmIntent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_HALF_DAY, pendingIntent);

        context.startService(alarmIntent); //run the service the first time
    }

    /**
     * Sets up UserVoice.
     *
     * <p>Allows users to contact with us and access help topics.</p>
     */
    private void setUpUserVoice() {
        // Set this up once when your application launches
        Config config = new Config("gnucash.uservoice.com");
        config.setTopicId(107400);
        config.setForumId(320493);
        config.putUserTrait("app_version_name", BuildConfig.VERSION_NAME);
        config.putUserTrait("app_version_code", BuildConfig.VERSION_CODE);
        config.putUserTrait("android_version", Build.VERSION.RELEASE);
        // config.identifyUser("USER_ID", "User Name", "email@example.com");
        UserVoice.init(config, this);
    }

    /**
     * Sets up Stetho to enable remote debugging from Chrome developer tools.
     *
     * <p>Among other things, allows access to the database and preferences.
     * See http://facebook.github.io/stetho/#features</p>
     */
    private void setUpRemoteDebuggingFromChrome() {
        Stetho.Initializer initializer =
                Stetho.newInitializerBuilder(this)
                        .enableWebKitInspector(
                                Stetho.defaultInspectorModulesProvider(this))
                        .build();
        Stetho.initialize(initializer);
    }
}