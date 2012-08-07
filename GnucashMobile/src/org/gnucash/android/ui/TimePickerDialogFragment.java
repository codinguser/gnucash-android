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
package org.gnucash.android.ui;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class TimePickerDialogFragment extends DialogFragment {
	private OnTimeSetListener mListener = null;
	
	public TimePickerDialogFragment() {
		// nothing to see here, move along
	}
	Calendar mCurrentTime = null;
	
	public TimePickerDialogFragment(OnTimeSetListener listener, long timeMillis){
		mListener = listener;
		if (timeMillis > 0){
			mCurrentTime = new GregorianCalendar();
			mCurrentTime.setTimeInMillis(timeMillis);
		}
	}
	
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
