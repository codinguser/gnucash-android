/*
 * Copyright (c) 2013 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.export;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;
import org.gnucash.android.R;
import org.gnucash.android.export.ofx.OfxExporter;
import org.gnucash.android.export.qif.QifExporter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.transaction.dialog.TransactionsDeleteConfirmationDialogFragment;

import java.io.*;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Asynchronous task for exporting transactions.
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ExporterAsyncTask extends AsyncTask<ExportParams, Void, Boolean> {
    /**
     * App context
     */
    private final Activity mContext;

    private ProgressDialog mProgressDialog;

    /**
     * Log tag
     */
    public static final String TAG = "ExporterAsyncTask";

    /**
     * Export parameters
     */
    private ExportParams mExportParams;

    public ExporterAsyncTask(Activity context){
        this.mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setTitle(R.string.title_progress_exporting_transactions);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB){
            mProgressDialog.setProgressNumberFormat(null);
            mProgressDialog.setProgressPercentFormat(null);
        }
        mProgressDialog.show();
    }

    /**
     * Generates the appropriate exported transactions file for the given parameters
     * @param params Export parameters
     * @return <code>true</code> if export was successful, <code>false</code> otherwise
     */
    @Override
    protected Boolean doInBackground(ExportParams... params) {
        mExportParams = params[0];

        Exporter mExporter;
        switch (mExportParams.getExportFormat()) {
                case QIF:
                    mExporter = new QifExporter(mExportParams);
                    break;

                case OFX:
                    mExporter = new OfxExporter(mExportParams);
                    break;

                case GNC_XML:
                default:
                    mExporter = new GncXmlExporter(mExportParams);
                    break;
            }

        try {
            if (mExportParams.getExportFormat() == ExportFormat.QIF) {
                File file = new File(mExportParams.getTargetFilepath());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
                try {
                    ((QifExporter)mExporter).generateExport(writer);
                }
                finally {
                    writer.close();
                }
            }
            else {
                writeOutput(mExporter.generateExport());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
            final String err_msg = e.getLocalizedMessage();
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, R.string.toast_export_error,
                            Toast.LENGTH_SHORT).show();
                    Toast.makeText(mContext, err_msg, Toast.LENGTH_LONG).show();
                }
            });
            return false;
        }
        return true;
    }

    /**
     * Transmits the exported transactions to the designated location, either SD card or third-party application
     * @param exportResult Result of background export execution
     */
    @Override
    protected void onPostExecute(Boolean exportResult) {
        if (!exportResult){
            Toast.makeText(mContext,
                    mContext.getString(R.string.toast_export_error, mExportParams.getExportFormat().name()),
                    Toast.LENGTH_LONG).show();
            return;
        }

        switch (mExportParams.getExportTarget()) {
            case SHARING:
                shareFile(mExportParams.getTargetFilepath());
                break;

            case SD_CARD:
                File src = new File(mExportParams.getTargetFilepath());
                File dst = Exporter.createExportFile(mExportParams.getExportFormat());

                try {
                    copyFile(src, dst);
                } catch (IOException e) {
                    Toast.makeText(mContext,
                            mContext.getString(R.string.toast_export_error, mExportParams.getExportFormat().name())
                                    + dst.getAbsolutePath(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, e.getMessage());
                    break;
                }

                //file already exists, just let the user know
                Toast.makeText(mContext,
                        mContext.getString(R.string.toast_format_exported_to, mExportParams.getExportFormat().name())
                                + dst.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
                break;

            default:
                break;
        }

        if (mExportParams.shouldDeleteTransactionsAfterExport()){
            android.support.v4.app.FragmentManager fragmentManager = ((FragmentActivity)mContext).getSupportFragmentManager();
            Fragment currentFragment = ((AccountsActivity)mContext).getCurrentAccountListFragment();

            TransactionsDeleteConfirmationDialogFragment alertFragment =
                    TransactionsDeleteConfirmationDialogFragment.newInstance(R.string.title_confirm_delete, 0);
            alertFragment.setTargetFragment(currentFragment, 0);

            alertFragment.show(fragmentManager, "transactions_delete_confirmation_dialog");
        }

        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();

    }

    /**
     * Writes out the String containing the exported data to disk
     * @param exportOutput String containing exported data
     * @throws IOException if the write fails
     */
    private void writeOutput(String exportOutput) throws IOException {
        File file = new File(mExportParams.getTargetFilepath());

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        writer.write(exportOutput);

        writer.flush();
        writer.close();
    }

    /**
     * Starts an intent chooser to allow the user to select an activity to receive
     * the exported OFX file
     * @param path String path to the file on disk
     */
    private void shareFile(String path){
        String defaultEmail = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(mContext.getString(R.string.key_default_export_email), null);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/xml");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + path));
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, mContext.getString(R.string.title_export_email,
                mExportParams.getExportFormat().name()));
        if (defaultEmail != null && defaultEmail.trim().length() > 0){
            shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{defaultEmail});
        }
        SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();

        shareIntent.putExtra(Intent.EXTRA_TEXT, mContext.getString(R.string.description_export_email)
                + " " + formatter.format(new Date(System.currentTimeMillis())));

        mContext.startActivity(Intent.createChooser(shareIntent, mContext.getString(R.string.title_select_export_destination)));
    }

    /**
     * Copies a file from <code>src</code> to <code>dst</code>
     * @param src Absolute path to the source file
     * @param dst Absolute path to the destination file
     * @throws IOException if the file could not be copied
     */
    public static void copyFile(File src, File dst) throws IOException
    {
        //TODO: Make this asynchronous at some time, t in the future.
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try
        {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally
        {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

}
