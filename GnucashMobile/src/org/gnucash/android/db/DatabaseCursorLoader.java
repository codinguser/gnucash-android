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

package org.gnucash.android.db;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;

public abstract class DatabaseCursorLoader extends AsyncTaskLoader<Cursor> {
	private Cursor mCursor = null;
	protected DatabaseAdapter mDatabaseAdapter = null;
	protected Context mContext = null;
	
	public DatabaseCursorLoader(Context context) {
		super(context);
		mContext = context;
	}

	public abstract Cursor loadInBackground();

	@Override
	public void deliverResult(Cursor data) {
		if (isReset()) {
			if (data != null) {
				onReleaseResources(data);
			}
		}

		Cursor oldCursor = mCursor;
		mCursor = data;

		if (isStarted()) {
			super.deliverResult(data);
		}

		if (oldCursor != null) {
			onReleaseResources(oldCursor);
		}
	}

	@Override
	protected void onStartLoading() {
		if (mCursor != null){
			deliverResult(mCursor);
		}
        
        if (takeContentChanged() || mCursor == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
	}
	
	@Override
	protected void onStopLoading() {
		cancelLoad();
	}
	
	@Override
	public void onCanceled(Cursor data) {
		super.onCanceled(data);
		onReleaseResources(data);
	}
	
	/**
     * Handles a request to completely reset the Loader.
     */
	@Override
	protected void onReset() {
		super.onReset();
		
		onStopLoading();

        // At this point we can release the resources associated with 'mCursor'
        // if needed.
        if (mCursor != null) {
            onReleaseResources(mCursor);
            mCursor = null;
        }	
	}
	
	/**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     * @param c {@link Cursor} to be released
     */
	protected void onReleaseResources(Cursor c) {
		c.close();
		if (mDatabaseAdapter != null){
			mDatabaseAdapter.close();
		}
	}
}
