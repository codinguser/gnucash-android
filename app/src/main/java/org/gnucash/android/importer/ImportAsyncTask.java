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
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.R;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.util.TaskDelegate;

import java.io.InputStream;

/**
 * Imports a GnuCash (desktop) account file and displays a progress dialog.
 * The AccountsActivity is opened when importing is done.
 */
public class ImportAsyncTask extends AsyncTask<InputStream, Void, Boolean> {
    private final Activity context;
    private TaskDelegate mDelegate;
    private ProgressDialog progressDialog;

    public ImportAsyncTask(Activity context){
        this.context = context;
    }

    public ImportAsyncTask(Activity context, TaskDelegate delegate){
        this.context = context;
        this.mDelegate = delegate;
    }

    @TargetApi(11)
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(R.string.title_progress_importing_accounts);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.show();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB){
            //these methods must be called after progressDialog.show()
            progressDialog.setProgressNumberFormat(null);
            progressDialog.setProgressPercentFormat(null);
        }

    }

    @Override
    protected Boolean doInBackground(InputStream... inputStreams) {
        try {
            GncXmlImporter.parse(inputStreams[0]);
        } catch (Exception exception){
            Log.e(ImportAsyncTask.class.getName(), "" + exception.getMessage());
            Crashlytics.logException(exception);
            exception.printStackTrace();

            final String err_msg = exception.getLocalizedMessage();
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context,
                            context.getString(R.string.toast_error_importing_accounts) + "\n" + err_msg,
                            Toast.LENGTH_LONG).show();
                }
            });

            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean importSuccess) {
        if (mDelegate != null)
            mDelegate.onTaskComplete();

        try {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
        } catch (IllegalArgumentException ex){
            //TODO: This is a hack to catch "View not attached to window" exceptions
            //FIXME by moving the creation and display of the progress dialog to the Fragment
        } finally {
            progressDialog = null;
        }

        int message = importSuccess ? R.string.toast_success_importing_accounts : R.string.toast_error_importing_accounts;
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

        AccountsActivity.start(context);
    }
}
