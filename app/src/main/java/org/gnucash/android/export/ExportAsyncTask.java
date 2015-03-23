/*
 * Copyright (c) 2013 - 2015 Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2014 Yongxin Wang <fefe.wyx@gmail.com>
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

import android.annotation.TargetApi;
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
import org.gnucash.android.export.qif.QifHelper;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.transaction.dialog.TransactionsDeleteConfirmationDialogFragment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Asynchronous task for exporting transactions.
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ExportAsyncTask extends AsyncTask<ExportParams, Void, Boolean> {
    /**
     * App context
     */
    private final Context mContext;

    private ProgressDialog mProgressDialog;

    /**
     * Log tag
     */
    public static final String TAG = "ExporterAsyncTask";

    /**
     * Export parameters
     */
    private ExportParams mExportParams;

    public ExportAsyncTask(Context context){
        this.mContext = context;
    }

    @Override
    @TargetApi(11)
    protected void onPreExecute() {
        super.onPreExecute();
        if (mContext instanceof Activity) {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setTitle(R.string.title_progress_exporting_transactions);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                mProgressDialog.setProgressNumberFormat(null);
                mProgressDialog.setProgressPercentFormat(null);
            }
            mProgressDialog.show();
        }
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
            File file = new File(mExportParams.getTargetFilepath());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            try {
                mExporter.generateExport(writer);
            }
            finally {
                writer.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "" + e.getMessage());
            final String err_msg = e.getLocalizedMessage();
            if (mContext instanceof Activity) {
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, R.string.toast_export_error,
                                Toast.LENGTH_SHORT).show();
                        if (err_msg != null) {
                            Toast.makeText(mContext, err_msg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
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
        if (mContext instanceof Activity) {
            if (!exportResult) {
                Toast.makeText(mContext,
                        mContext.getString(R.string.toast_export_error, mExportParams.getExportFormat().name()),
                        Toast.LENGTH_LONG).show();
                return;
            }
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
                    if (mContext instanceof Activity) {
                        Toast.makeText(mContext,
                                mContext.getString(R.string.toast_export_error, mExportParams.getExportFormat().name())
                                        + dst.getAbsolutePath(),
                                Toast.LENGTH_LONG).show();
                        Log.e(TAG, e.getMessage());
                    } else {
                        Log.e(TAG, e.getMessage());
                    }
                    break;
                }

                if (mContext instanceof Activity) {
                    //file already exists, just let the user know
                    Toast.makeText(mContext,
                            mContext.getString(R.string.toast_format_exported_to, mExportParams.getExportFormat().name())
                                    + dst.getAbsolutePath(),
                            Toast.LENGTH_LONG).show();
                }
                break;

            default:
                break;
        }

        if (mContext instanceof Activity) {
            if (mExportParams.shouldDeleteTransactionsAfterExport()) {
                android.support.v4.app.FragmentManager fragmentManager = ((FragmentActivity) mContext).getSupportFragmentManager();
                Fragment currentFragment = ((AccountsActivity) mContext).getCurrentAccountListFragment();

                TransactionsDeleteConfirmationDialogFragment alertFragment =
                        TransactionsDeleteConfirmationDialogFragment.newInstance(R.string.title_confirm_delete, 0);
                alertFragment.setTargetFragment(currentFragment, 0);

                alertFragment.show(fragmentManager, "transactions_delete_confirmation_dialog");
            }

            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        }
    }

    /**
     * Starts an intent chooser to allow the user to select an activity to receive
     * the exported OFX file
     * @param path String path to the file on disk
     */
    private void shareFile(String path) {
        String defaultEmail = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString(mContext.getString(R.string.key_default_export_email), null);
        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType("application/xml");
        ArrayList<Uri> exportFiles = new ArrayList<Uri>();
        if (mExportParams.getExportFormat() == ExportFormat.QIF) {
            try {
                List<String> splitFiles = splitQIF(new File(path), new File(path));
                for (String file : splitFiles) {
                    exportFiles.add(Uri.parse("file://" + file));
                }
            } catch (IOException e) {
                Log.e(TAG, "error split up files in shareFile");
                e.printStackTrace();
                return;
            }
        } else {
            exportFiles.add(Uri.parse("file://" + path));
        }
        shareIntent.putExtra(Intent.EXTRA_STREAM, exportFiles);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, mContext.getString(R.string.title_export_email,
                mExportParams.getExportFormat().name()));
        if (defaultEmail != null && defaultEmail.trim().length() > 0) {
            shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{defaultEmail});
        }
        SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();

        ArrayList<CharSequence> extraText = new ArrayList<CharSequence>();
        extraText.add(mContext.getString(R.string.description_export_email)
                + " " + formatter.format(new Date(System.currentTimeMillis())));
        shareIntent.putExtra(Intent.EXTRA_TEXT, extraText);

        if (mContext instanceof Activity)
            mContext.startActivity(Intent.createChooser(shareIntent, mContext.getString(R.string.title_select_export_destination)));
    }

    /**
     * Copies a file from <code>src</code> to <code>dst</code>
     * @param src Absolute path to the source file
     * @param dst Absolute path to the destination file
     * @throws IOException if the file could not be copied
     */
    public void copyFile(File src, File dst) throws IOException {
        //TODO: Make this asynchronous at some time, t in the future.
        if (mExportParams.getExportFormat() == ExportFormat.QIF) {
            splitQIF(src, dst);
        } else {
            FileChannel inChannel = new FileInputStream(src).getChannel();
            FileChannel outChannel = new FileOutputStream(dst).getChannel();
            try {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } finally {
                if (inChannel != null)
                    inChannel.close();
                outChannel.close();
            }
        }
    }

    /**
     * Copies a file from <code>src</code> to <code>dst</code>
     * @param src Absolute path to the source file
     * @param dst Absolute path to the destination file
     * @throws IOException if the file could not be copied
     */
    private static List<String> splitQIF(File src, File dst) throws IOException {
        // split only at the last dot
        String[] pathParts = dst.getPath().split("(?=\\.[^\\.]+$)");
        ArrayList<String> splitFiles = new ArrayList<String>();
        String line;
        BufferedReader in = new BufferedReader(new FileReader(src));
        BufferedWriter out = null;
        try {
            while ((line = in.readLine()) != null) {
                if (line.startsWith(QifHelper.INTERNAL_CURRENCY_PREFIX)) {
                    String currencyCode = line.substring(1);
                    if (out != null) {
                        out.close();
                    }
                    String newFileName = pathParts[0] + "_" + currencyCode + pathParts[1];
                    splitFiles.add(newFileName);
                    out = new BufferedWriter(new FileWriter(newFileName));
                } else {
                    if (out == null) {
                        throw new IllegalArgumentException(src.getPath() + " format is not correct");
                    }
                    out.append(line).append('\n');
                }
            }
        } finally {
            in.close();
            if (out != null) {
                out.close();
            }
        }
        return splitFiles;
    }
}
