/*
 * Written By: Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2012 Ngewi Fet
 *
 * This file is part of Gnucash for Android
 * 
 * Gnucash for Android is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, contact:
 *
 * Free Software Foundation           Voice:  +1-617-542-5942
 * 51 Franklin Street, Fifth Floor    Fax:    +1-617-542-2652
 * Boston, MA  02110-1301,  USA       gnu@gnu.org
 */

package org.gnucash.android.ui.widget;

import java.util.Locale;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.receivers.TransactionAppWidgetProvider;
import org.gnucash.android.ui.MainActivity;
import org.gnucash.android.ui.transactions.TransactionsListFragment;

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

public class Configuration extends Activity {
	private AccountsDbAdapter mAccountsDbAdapter;
	private SimpleCursorAdapter mCursorAdapter;
	private int mAppWidgetId;
	
	private Spinner mAccountsSpinner;
	private Button mOkButton;
	private Button mCancelButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.widget_configuration);
		mAccountsSpinner = (Spinner) findViewById(R.id.input_accounts_spinner);
		mOkButton = (Button) findViewById(R.id.btn_save);
		mCancelButton = (Button) findViewById(R.id.btn_cancel);
		
		String[] from = new String[] {DatabaseHelper.KEY_NAME};
		int[] to = new int[] {android.R.id.text1};
		mAccountsDbAdapter = new AccountsDbAdapter(this);
		Cursor cursor = mAccountsDbAdapter.fetchAllAccounts();
		
		mCursorAdapter = new SimpleCursorAdapter(this, 
				android.R.layout.simple_spinner_item, 
				cursor,
				from,
				to, 
				0);
		mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAccountsSpinner.setAdapter(mCursorAdapter);
		
		setResult(RESULT_CANCELED);
		bindListeners();
	}

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
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Configuration.this);
				Editor editor = prefs.edit();
				editor.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID + mAppWidgetId, 
						mAccountsSpinner.getSelectedItemId());				
				editor.commit();	
				
				updateWidget(Configuration.this, mAppWidgetId, mAccountsSpinner.getSelectedItemId());
						
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
	 * @param appWidgetManager
	 */
	public static void updateWidget(Context context, int appWidgetId, long accountId) {
		Log.i("WidgetConfigruation", "Updating widget: " + appWidgetId);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		
		AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(context);
		Account account = accountsDbAdapter.getAccount(accountId);
		accountsDbAdapter.close();
		
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.widget_4x1);
		views.setTextViewText(R.id.account_name, account.getName());
		views.setTextViewText(R.id.transactions_summary, 
				account.getBalance().formattedString(Locale.getDefault()));
		int color = account.getBalance().isNegative() ? R.color.debit_red : R.color.credit_green;
		views.setTextColor(R.id.transactions_summary, context.getResources().getColor(color));
		
		//TODO: start account list activity
		Intent accountViewIntent = new Intent(context, MainActivity.class);
		accountViewIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		accountViewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent accountPendingIntent = PendingIntent
				.getActivity(context, 0, accountViewIntent, 0);
		views.setOnClickPendingIntent(R.id.widget_layout, accountPendingIntent);
		
		//TODO: Start new transaction activity
		Intent newTransactionIntent = new Intent(context, MainActivity.class);
		newTransactionIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		newTransactionIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent
				.getActivity(context, 0, newTransactionIntent, 0);	            
		views.setOnClickPendingIntent(R.id.btn_new_transaction, pendingIntent);
		
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
			
	public static void updateAllWidgets(Context context, long accountId){
		Log.i("WidgetConfigruation", "Updating all widgets");
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		ComponentName componentName = new ComponentName(context, TransactionAppWidgetProvider.class);
		int[] appWidgetIds = widgetManager.getAppWidgetIds(componentName);
		
		for (int widgetId : appWidgetIds) {
			long accId = PreferenceManager
            		.getDefaultSharedPreferences(context)
            		.getLong(TransactionsListFragment.SELECTED_ACCOUNT_ID + widgetId, -1);
            
			if (accId < 0)
				accId = accountId;
			updateWidget(context, widgetId, accId);
		}
		/*
		Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		intent.setClass(context, TransactionAppWidgetProvider.class);
//		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
		context.sendBroadcast(intent);
		*/
		
	}
}
