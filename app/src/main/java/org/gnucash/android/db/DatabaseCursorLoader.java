/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.db;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;

import org.gnucash.android.db.adapter.DatabaseAdapter;

/**
 * Abstract base class for asynchronously loads records from a database and manages the cursor.
 * In order to use this class, you must subclass it and implement the
 * {@link #loadInBackground()} method to load the particular records from the database.
 * Ideally, the database has {@link DatabaseAdapter} which is used for managing access to the
 * records from the database
 * @author Ngewi Fet <ngewif@gmail.com>
 * @see DatabaseAdapter
 */
public abstract class DatabaseCursorLoader extends AsyncTaskLoader<Cursor> {
	/**
	 * Cursor which will hold the loaded data set.
	 * The cursor will be returned from the {@link #loadInBackground()} method
	 */
	private Cursor mCursor = null;
	
	/**
	 * {@link DatabaseAdapter} which will be used to load the records from the database
	 */
	protected DatabaseAdapter mDatabaseAdapter = null;
	
	/**
	 * A content observer which monitors the cursor and provides notifications when
	 * the dataset backing the cursor changes. You need to register the oberserver on
	 * your cursor using {@link #registerContentObserver(Cursor)}
	 */
	protected final Loader.ForceLoadContentObserver mObserver;
	
	/**
	 * Constructor
	 * Initializes the content observer
	 * @param context Application context
	 */
	public DatabaseCursorLoader(Context context) {
		super(context);
		mObserver = new ForceLoadContentObserver();
	}

	/**
	 * Asynchronously loads the results from the database. 
	 */
	public abstract Cursor loadInBackground();

	/**
	 * Registers the content observer for the cursor. 
	 * @param cursor {@link Cursor} whose content is to be observed for changes
	 */
	protected void registerContentObserver(Cursor cursor){
		cursor.registerContentObserver(mObserver);
	}
	
	@Override
	public void deliverResult(Cursor data) {
		if (isReset()) {
			if (data != null) {
				onReleaseResources(data);
			}
			return;
		}

		Cursor oldCursor = mCursor;
		mCursor = data;

		if (isStarted()) {
			super.deliverResult(data);
		}

		if (oldCursor != null && oldCursor != data && !oldCursor.isClosed()) {
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
        if (mCursor != null && !mCursor.isClosed()) {
            onReleaseResources(mCursor);           
        }	
        mCursor = null;
	}
	
	/**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     * @param c {@link Cursor} to be released
     */
	protected void onReleaseResources(Cursor c) {
		if (c != null)
			c.close();
	}
}
