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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.homescreen.WidgetConfigurationActivity;
import org.gnucash.android.ui.settings.PreferenceActivity;

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
		for (int appWidgetId : appWidgetIds) {
			WidgetConfigurationActivity.updateWidget(context, appWidgetId);
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
		for (int appWidgetId : appWidgetIds) {
			WidgetConfigurationActivity.removeWidgetConfiguration(context, appWidgetId);
		}
	}
}
