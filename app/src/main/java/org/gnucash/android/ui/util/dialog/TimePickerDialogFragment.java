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
package org.gnucash.android.ui.util.dialog;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Fragment for displaying a time choose dialog
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class TimePickerDialogFragment extends DialogFragment {
	/**
	 * Listener to notify when the time is set
	 */
	private OnTimeSetListener mListener = null;
	
	/**
	 * Current time to initialize the dialog to, or to notify the listener of.
	 */
	Calendar mCurrentTime = null;
	
	/**
	 * Default constructor
	 * Is required for when the device is rotated while the dialog is open.
	 * If this constructor is not present, the app will crash
	 */
	public TimePickerDialogFragment() {
		// nothing to see here, move along
	}
	
	/**
	 * Overloaded constructor
	 * @param listener {@link OnTimeSetListener} to notify when the time has been set
	 * @param timeMillis Time in milliseconds to initialize the dialog to
	 */
	public static TimePickerDialogFragment newInstance(OnTimeSetListener listener, long timeMillis){
		TimePickerDialogFragment timePickerDialogFragment = new TimePickerDialogFragment();
        timePickerDialogFragment.mListener = listener;
		if (timeMillis > 0){
			timePickerDialogFragment.mCurrentTime = new GregorianCalendar();
			timePickerDialogFragment.mCurrentTime.setTimeInMillis(timeMillis);
		}
        return timePickerDialogFragment;
	}
	
	/**
	 * Creates and returns an Android {@link TimePickerDialog}
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Calendar cal = mCurrentTime == null ? Calendar.getInstance() : mCurrentTime;
		
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		
		return new TimePickerDialog(getActivity(),
				mListener, 
				hour, 
				minute,
				true);
	}
	
}
