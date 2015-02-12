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

package org.gnucash.android.ui.transaction.dialog;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Fragment for displaying a date picker dialog
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class DatePickerDialogFragment extends DialogFragment {

	/**
	 * Listener to notify of events in the dialog
	 */
	private OnDateSetListener mDateSetListener;
	
	/**
	 * Date selected in the dialog or to which the dialog is initialized
	 */
	private Calendar mDate;
	
	/**
	 * Default Constructor
	 * Is required for when the device is rotated while the dialog is open.
	 * If this constructor is not present, the app will crash
	 */
	public DatePickerDialogFragment() {
		//nothing to see here, move along
	}
	
	/**
	 * Overloaded constructor
	 * @param callback Listener to notify when the date is set and the dialog is closed
	 * @param dateMillis Time in milliseconds to which to initialize the dialog
	 */
	public static DatePickerDialogFragment newInstance(OnDateSetListener callback, long dateMillis) {
		DatePickerDialogFragment datePickerDialogFragment = new DatePickerDialogFragment();
        datePickerDialogFragment.mDateSetListener = callback;
		if (dateMillis > 0){
			datePickerDialogFragment.mDate = new GregorianCalendar();
			datePickerDialogFragment.mDate.setTimeInMillis(dateMillis);
		}
        return datePickerDialogFragment;
	}

	/**
	 * Creates and returns an Android {@link DatePickerDialog}
	 */
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Calendar cal = mDate == null ? Calendar.getInstance() : mDate;
		
		return new DatePickerDialog(getActivity(),
				mDateSetListener, cal.get(Calendar.YEAR), 
				cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
	}
	
}
