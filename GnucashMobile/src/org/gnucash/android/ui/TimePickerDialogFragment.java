/*
 * Written By: Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2012 Ngewi Fet
 *
 * This file is part of Gnucash for Android
 * 
 * Gnucash for Android is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, contact:
 *
 * Free Software Foundation           Voice:  +1-617-542-5942
 * 51 Franklin Street, Fifth Floor    Fax:    +1-617-542-2652
 * Boston, MA  02110-1301,  USA       gnu@gnu.org
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
