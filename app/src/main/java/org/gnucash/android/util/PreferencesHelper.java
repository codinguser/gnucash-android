/*
 * Copyright (c) 2015 Alceu Rodrigues Neto <alceurneto@gmail.com>
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
package org.gnucash.android.util;

import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.ui.settings.PreferenceActivity;

import java.sql.Timestamp;

/**
 * A utility class to deal with Android Preferences in a centralized way.
 */
public final class PreferencesHelper {

    /**
     * Should be not instantiated.
     */
    private PreferencesHelper() {}

    /**
     * Tag for logging
     */
    protected static final String LOG_TAG = "PreferencesHelper";

    /**
     * Preference key for saving the last export time
     */
    public static final String PREFERENCE_LAST_EXPORT_TIME_KEY = "last_export_time";

    /**
     * Set the last export time in UTC time zone.
     * A new export operations will fetch transactions based on this value.
     *
     * @param lastExportTime the last export time to set.
     */
    public static void setLastExportTime(Timestamp lastExportTime) {
        final String utcString = TimestampHelper.getUtcStringFromTimestamp(lastExportTime);
        Log.d(LOG_TAG, "Storing '" + utcString + "' as lastExportTime in Android Preferences.");
        PreferenceActivity.getActiveBookSharedPreferences(GnuCashApplication.getAppContext())
                .edit()
                .putString(PREFERENCE_LAST_EXPORT_TIME_KEY, utcString)
                .apply();
    }

    /**
     * Get the time for the last export operation.
     *
     * @return A {@link Timestamp} with the time.
     */
    public static Timestamp getLastExportTime() {
        final String utcString = PreferenceActivity.getActiveBookSharedPreferences(GnuCashApplication.getAppContext())
                .getString(PREFERENCE_LAST_EXPORT_TIME_KEY,
                        TimestampHelper.getUtcStringFromTimestamp(TimestampHelper.getTimestampFromEpochZero()));
        Log.d(LOG_TAG, "Retrieving '" + utcString + "' as lastExportTime from Android Preferences.");
        return TimestampHelper.getTimestampFromUtcString(utcString);
    }
}