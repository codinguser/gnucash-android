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

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class DatePickerDialogFragment extends DialogFragment {

	private OnDateSetListener mDateSetListener;
	private Calendar mDate;
	
	public DatePickerDialogFragment() {
		// nothing to see here, move along
	}
	
	public DatePickerDialogFragment(OnDateSetListener callback, long dateMillis) {
		mDateSetListener = (OnDateSetListener) callback;
		if (dateMillis > 0){
			mDate = new GregorianCalendar();
			mDate.setTimeInMillis(dateMillis);
		}
	}

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Calendar cal = mDate == null ? Calendar.getInstance() : mDate;
		
		return new DatePickerDialog(getActivity(),
				mDateSetListener, cal.get(Calendar.YEAR), 
				cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
	}
	
}
