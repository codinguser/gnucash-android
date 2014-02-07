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
package org.gnucash.android.receivers;

import org.gnucash.android.ui.transactions.TransactionsListFragment;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * {@link AppWidgetProvider} which is responsible for managing widgets on the homescreen
 * It receives broadcasts related to updating and deleting widgets
 * Widgets can also be updated manually by calling {@link WidgetConfigurationActivity#updateAllWidgets(Context)}
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class TransactionAppWidgetProvider extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            
            long accountId = PreferenceManager
            		.getDefaultSharedPreferences(context)
            		.getLong(TransactionsListFragment.SELECTED_ACCOUNT_ID + appWidgetId, -1);
            
            if (accountId <= 0)
            	return;
            
            WidgetConfigurationActivity.updateWidget(context, appWidgetId, accountId);            
        }
	}

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        WidgetConfigurationActivity.updateAllWidgets(context);
    }

    @Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);		
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		
		for (int appWidgetId : appWidgetIds) {
			editor.remove(TransactionsListFragment.SELECTED_ACCOUNT_ID + appWidgetId);			
		}
		editor.commit();		
	}
}
