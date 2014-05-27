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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.gnucash.android.R;

/**
 * An {@link Application} subclass for retrieving static context
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class GnuCashApplication extends Application{

    private static Context context;

    public void onCreate(){
        super.onCreate();
        GnuCashApplication.context = getApplicationContext();
    }

    /**
     * Returns the application context
     * @return Application {@link Context} object
     */
    public static Context getAppContext() {
        return GnuCashApplication.context;
    }

    /**
     * Returns <code>true</code> if double entry is enabled in the app settings, <code>false</code> otherwise.
     * If the value is not set, the default value can be specified in the parameters.
     * @param defaultValue Default value to return if double entry is not explicitly set
     * @return <code>true</code> if double entry is enabled, <code>false</code> otherwise
     */
    public static boolean isDoubleEntryEnabled(boolean defaultValue){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPrefs.getBoolean(context.getString(R.string.key_use_double_entry), defaultValue);
    }
}