/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.util;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Time;
import android.view.View;

import com.codetroopers.betterpickers.recurrencepicker.RecurrencePickerDialogFragment;
import com.codetroopers.betterpickers.recurrencepicker.RecurrencePickerDialogFragment.OnRecurrenceSetListener;

/**
 * Shows the recurrence dialog when the recurrence view is clicked
 */
public class RecurrenceViewClickListener implements View.OnClickListener{
    private static final String FRAGMENT_TAG_RECURRENCE_PICKER  = "recurrence_picker";

    AppCompatActivity mActivity;
    String mRecurrenceRule;
    OnRecurrenceSetListener mRecurrenceSetListener;

    public RecurrenceViewClickListener(AppCompatActivity activity, String recurrenceRule, OnRecurrenceSetListener recurrenceSetListener){
        this.mActivity = activity;
        this.mRecurrenceRule = recurrenceRule;
        this.mRecurrenceSetListener = recurrenceSetListener;
    }

    @Override
    public void onClick(View v) {
        FragmentManager fm = mActivity.getSupportFragmentManager();
        Bundle b = new Bundle();
        Time t = new Time();
        t.setToNow();
        b.putLong(RecurrencePickerDialogFragment.BUNDLE_START_TIME_MILLIS, t.toMillis(false));
        b.putString(RecurrencePickerDialogFragment.BUNDLE_TIME_ZONE, t.timezone);

        // may be more efficient to serialize and pass in EventRecurrence
        b.putString(RecurrencePickerDialogFragment.BUNDLE_RRULE, mRecurrenceRule);

        RecurrencePickerDialogFragment rpd = (RecurrencePickerDialogFragment) fm.findFragmentByTag(
                FRAGMENT_TAG_RECURRENCE_PICKER);
        if (rpd != null) {
            rpd.dismiss();
        }
        rpd = new RecurrencePickerDialogFragment();
        rpd.setArguments(b);
        rpd.setOnRecurrenceSetListener(mRecurrenceSetListener);
        rpd.show(fm, FRAGMENT_TAG_RECURRENCE_PICKER);
    }
}
