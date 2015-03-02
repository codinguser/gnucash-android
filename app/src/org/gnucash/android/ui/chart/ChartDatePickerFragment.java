/*
 * Copyright (c) 2015 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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

package org.gnucash.android.ui.chart;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;

import java.lang.reflect.Field;
import java.util.Calendar;

/**
 * Fragment for displaying a date picker dialog.
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class ChartDatePickerFragment extends DialogFragment {

    private static final String TAG = ChartDatePickerFragment.class.getSimpleName();

    private OnDateSetListener callback;
    private Calendar mCalendar = Calendar.getInstance();
    private long minDate;
    private long maxDate;

    /**
     * Required for when the device is rotated while the dialog is open.
     * If this constructor is not present, the app will crash
     */
    public ChartDatePickerFragment() {}

    /**
     * Creates the date picker fragment without day field.
     * @param callback the listener to notify when the date is set and the dialog is closed
     * @param time the dialog init time in milliseconds
     * @param minDate the earliest allowed date
     * @param maxDate the latest allowed date
     */
    public ChartDatePickerFragment(OnDateSetListener callback, long time, long minDate, long maxDate) {
        this.callback = callback;
        mCalendar.setTimeInMillis(time);
        this.minDate = minDate;
        this.maxDate = maxDate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DatePickerDialog dialog = new DatePickerDialog(getActivity(), callback,
                mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));

        try {
            Field datePickerField = dialog.getClass().getDeclaredField("mDatePicker");
            datePickerField.setAccessible(true);
            DatePicker datePicker = (DatePicker) datePickerField.get(dialog);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
                datePicker.setMinDate(minDate);
                datePicker.setMaxDate(maxDate);
            }

            for (Field field : datePicker.getClass().getDeclaredFields()) {
                if (field.getName().equals("mDaySpinner") || field.getName().equals("mDayPicker")) {
                    field.setAccessible(true);
                    ((View) field.get(datePicker)).setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e.getMessage());
        }

        return dialog;
    }

}
