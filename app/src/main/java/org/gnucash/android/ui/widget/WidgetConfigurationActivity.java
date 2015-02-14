/*
 * Copyright (c) 2012 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.widget;

import java.util.Locale;

import org.gnucash.android.R;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.receivers.TransactionAppWidgetProvider;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.transaction.TransactionsActivity;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Toast;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

/**
 * Activity for configuration which account to display on a widget.
 * The activity is opened each time a widget is added to the homescreen
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class WidgetConfigurationActivity extends Activity {
	private AccountsDbAdapter mAccountsDbAdapter;
    private int mAppWidgetId;
	
	private Spinner mAccountsSpinner;
	private Button mOkButton;
	private Button mCancelButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.widget_configuration);
		setResult(RESULT_CANCELED);
		
		mAccountsSpinner = (Spinner) findViewById(R.id.input_accounts_spinner);
		mOkButton 		= (Button) findViewById(R.id.btn_save);
		mCancelButton 	= (Button) findViewById(R.id.btn_cancel);

		mAccountsDbAdapter = new AccountsDbAdapter(this);
		Cursor cursor = mAccountsDbAdapter.fetchAllRecordsOrderedByFullName();
		
		if (cursor.getCount() <= 0){
			Toast.makeText(this, R.string.error_no_accounts, Toast.LENGTH_LONG).show();
			finish();
		}

        SimpleCursorAdapter cursorAdapter = new QualifiedAccountNameCursorAdapter(this,
                android.R.layout.simple_spinner_item,
                cursor);
		cursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAccountsSpinner.setAdapter(cursorAdapter);
		
		bindListeners();
	}

	@Override
	protected void onDestroy() {		
		super.onDestroy();
		mAccountsDbAdapter.close();
	}
	
	/**
	 * Sets click listeners for the buttons in the dialog
	 */
	private void bindListeners() {
		mOkButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = getIntent();
				Bundle extras = intent.getExtras();
				if (extras != null) {
				    mAppWidgetId = extras.getInt(
				            AppWidgetManager.EXTRA_APPWIDGET_ID, 
				            AppWidgetManager.INVALID_APPWIDGET_ID);
				}
				
				if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID){
					finish();
					return;
				}					
				
				long accountId = mAccountsSpinner.getSelectedItemId();
                String accountUID = mAccountsDbAdapter.getUID(accountId);
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(WidgetConfigurationActivity.this);
				Editor editor = prefs.edit();
				editor.putString(UxArgument.SELECTED_ACCOUNT_UID + mAppWidgetId, accountUID);
				editor.commit();	
				
				updateWidget(WidgetConfigurationActivity.this, mAppWidgetId, accountUID);
						
				Intent resultValue = new Intent();
				resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
				setResult(RESULT_OK, resultValue);
				finish();		
			}
		});
		
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	/**
	 * Updates the widget with id <code>appWidgetId</code> with information from the 
	 * account with record ID <code>accountId</code>
     * If the account has been deleted, then a notice is posted in the widget
     * @param appWidgetId ID of the widget to be updated
     * @param accountUID GUID of the account tied to the widget
	 */
	public static void updateWidget(Context context, int appWidgetId, String accountUID) {
		Log.i("WidgetConfiguration", "Updating widget: " + appWidgetId);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

		AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(context);
		Account account = accountsDbAdapter.getAccount(accountUID);

		
		if (account == null){
			Log.i("WidgetConfiguration", "Account not found, resetting widget " + appWidgetId);
			//if account has been deleted, let the user know
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.widget_4x1);
			views.setTextViewText(R.id.account_name, context.getString(R.string.toast_account_deleted));
			views.setTextViewText(R.id.transactions_summary, "");
            //set it to simply open the app
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    new Intent(context, AccountsActivity.class), 0);
			views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
			views.setOnClickPendingIntent(R.id.btn_new_transaction, pendingIntent);
			appWidgetManager.updateAppWidget(appWidgetId, views);
			Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.remove(UxArgument.SELECTED_ACCOUNT_UID + appWidgetId);
			editor.commit();
			return;
		}
		
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.widget_4x1);
		views.setTextViewText(R.id.account_name, account.getName());
        Money accountBalance = accountsDbAdapter.getAccountBalance(accountUID);

        views.setTextViewText(R.id.transactions_summary,
				accountBalance.formattedString(Locale.getDefault()));
		int color = account.getBalance().isNegative() ? R.color.debit_red : R.color.credit_green;
		views.setTextColor(R.id.transactions_summary, context.getResources().getColor(color));



		Intent accountViewIntent = new Intent(context, TransactionsActivity.class);
		accountViewIntent.setAction(Intent.ACTION_VIEW);
		accountViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
		accountViewIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, accountUID);
		PendingIntent accountPendingIntent = PendingIntent
				.getActivity(context, appWidgetId, accountViewIntent, 0);
		views.setOnClickPendingIntent(R.id.widget_layout, accountPendingIntent);
		
		Intent newTransactionIntent = new Intent(context, TransactionsActivity.class);
		newTransactionIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
		newTransactionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
		newTransactionIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, accountUID);
		PendingIntent pendingIntent = PendingIntent
				.getActivity(context, appWidgetId, newTransactionIntent, 0);	            
		views.setOnClickPendingIntent(R.id.btn_new_transaction, pendingIntent);
		
		appWidgetManager.updateAppWidget(appWidgetId, views);
        accountsDbAdapter.close();
	}
	
	/**
	 * Updates all widgets belonging to the application
	 * @param context Application context
	 */
	public static void updateAllWidgets(Context context){
		Log.i("WidgetConfiguration", "Updating all widgets");
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		ComponentName componentName = new ComponentName(context, TransactionAppWidgetProvider.class);
		int[] appWidgetIds = widgetManager.getAppWidgetIds(componentName);

        SharedPreferences defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		for (int widgetId : appWidgetIds) {
			String accountUID = defaultSharedPrefs
            		.getString(UxArgument.SELECTED_ACCOUNT_UID + widgetId, null);
            
			if (accountUID == null)
				continue;
			updateWidget(context, widgetId, accountUID);
		}
	}
}
