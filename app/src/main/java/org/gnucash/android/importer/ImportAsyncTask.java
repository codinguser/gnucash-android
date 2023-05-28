/*
 * Copyright (c) 2014 Ngewi Fet <ngewif@gmail.com>
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
package org.gnucash.android.importer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.ui.util.TaskDelegate;
import org.gnucash.android.util.BookUtils;

import java.io.InputStream;

/**
 * Imports a GnuCash (desktop) account file and displays a progress dialog.
 * The AccountsActivity is opened when importing is done.
 */
public class ImportAsyncTask extends AsyncTask<Uri, Void, Boolean> {
    private final Activity mContext;
    private TaskDelegate mDelegate;
    private ProgressDialog mProgressDialog;

    private String mImportedBookUID;

    public ImportAsyncTask(Activity context){
        this.mContext = context;
    }

    public ImportAsyncTask(Activity context, TaskDelegate delegate){
        this.mContext = context;
        this.mDelegate = delegate;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setTitle(R.string.title_progress_importing_accounts);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.show();

        //these methods must be called after progressDialog.show()
        mProgressDialog.setProgressNumberFormat(null);
        mProgressDialog.setProgressPercentFormat(null);


    }

    @Override
    protected Boolean doInBackground(Uri... uris) {
        try {
            InputStream accountInputStream = mContext.getContentResolver().openInputStream(uris[0]);
            mImportedBookUID = GncXmlImporter.parse(accountInputStream);

        } catch (Exception exception){
            Log.e(ImportAsyncTask.class.getName(), "" + exception.getMessage());
            FirebaseCrashlytics.getInstance().log("Could not open: " + uris[0].toString());
            FirebaseCrashlytics.getInstance().recordException(exception);
            exception.printStackTrace();

            final String err_msg = exception.getLocalizedMessage();
            FirebaseCrashlytics.getInstance().log(err_msg);
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext,
                            mContext.getString(R.string.toast_error_importing_accounts) + "\n" + err_msg,
                            Toast.LENGTH_LONG).show();
                }
            });

            return false;
        }

        Cursor cursor = mContext.getContentResolver().query(uris[0], null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            String displayName = cursor.getString(nameIndex);
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseSchema.BookEntry.COLUMN_DISPLAY_NAME, displayName);
            contentValues.put(DatabaseSchema.BookEntry.COLUMN_SOURCE_URI, uris[0].toString());
            BooksDbAdapter.getInstance().updateRecord(mImportedBookUID, contentValues);

            cursor.close();
        }

        //set the preferences to their default values
        mContext.getSharedPreferences(mImportedBookUID, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(mContext.getString(R.string.key_use_double_entry), true)
                .apply();

        return true;
    }

    @Override
    protected void onPostExecute(Boolean importSuccess) {
        try {
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        } catch (IllegalArgumentException ex){
            //TODO: This is a hack to catch "View not attached to window" exceptions
            //FIXME by moving the creation and display of the progress dialog to the Fragment
        } finally {
            mProgressDialog = null;
        }

        int message = importSuccess ? R.string.toast_success_importing_accounts : R.string.toast_error_importing_accounts;
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();

        if (mImportedBookUID != null)
            BookUtils.loadBook(mImportedBookUID);

        if (mDelegate != null)
            mDelegate.onTaskComplete();
    }
}
