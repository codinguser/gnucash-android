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

package org.gnucash.android.ui.util.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.squareup.timessquare.CalendarPickerView;

import org.gnucash.android.R;
import org.joda.time.LocalDate;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Dialog for picking date ranges in terms of months.
 * It is currently used for selecting ranges for reports
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class DateRangePickerDialogFragment extends DialogFragment{

    @Bind(R.id.calendar_view) CalendarPickerView mCalendarPickerView;
    @Bind(R.id.btn_save)    Button mDoneButton;
    @Bind(R.id.btn_cancel)  Button mCancelButton;

    private Date mStartRange = LocalDate.now().minusMonths(1).toDate();
    private Date mEndRange = LocalDate.now().toDate();
    private OnDateRangeSetListener mDateRangeSetListener;
    private static final long ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000;

    public static DateRangePickerDialogFragment newInstance(OnDateRangeSetListener dateRangeSetListener){
        DateRangePickerDialogFragment fragment = new DateRangePickerDialogFragment();
        fragment.mDateRangeSetListener = dateRangeSetListener;
        return fragment;
    }

    public static DateRangePickerDialogFragment newInstance(long startDate, long endDate,
                                                            OnDateRangeSetListener dateRangeSetListener){
        DateRangePickerDialogFragment fragment = new DateRangePickerDialogFragment();
        fragment.mStartRange = new Date(startDate);
        fragment.mEndRange = new Date(endDate);
        fragment.mDateRangeSetListener = dateRangeSetListener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_date_range_picker, container, false);
        ButterKnife.bind(this, view);


        Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.YEAR, 1);

        Date today = new Date();
        mCalendarPickerView.init(mStartRange, mEndRange)
                .inMode(CalendarPickerView.SelectionMode.RANGE)
                .withSelectedDate(today);

        mDoneButton.setText(R.string.done_label);
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Date> selectedDates = mCalendarPickerView.getSelectedDates();
                Date startDate = selectedDates.get(0);
                // If only one day is selected (no interval) start and end should be the same (the selected one)
                Date endDate = selectedDates.size() > 1 ? selectedDates.get(selectedDates.size() - 1) : new Date(startDate.getTime());
                // CaledarPicker returns the start of the selected day but we want all transactions of that day to be included.
                // Therefore we have to add 24 hours to the endDate.
                endDate.setTime(endDate.getTime() + ONE_DAY_IN_MILLIS);
                mDateRangeSetListener.onDateRangeSet(startDate, endDate);
                dismiss();
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Pick time range");
        return dialog;
    }

    public interface OnDateRangeSetListener {
        void onDateRangeSet(Date startDate, Date endDate);
    }
}
