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
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.ofx.OfxExporter;
import org.gnucash.android.export.qif.QifExporter;
import org.gnucash.android.export.qif.QifHelper;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.settings.SettingsActivity;
import org.gnucash.android.ui.transaction.TransactionsActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
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

                case XML:
                default:
                    mExporter = new GncXmlExporter(mExportParams);
                    break;
            }

        try {
            File file = new File(mExportParams.getTargetFilepath());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            try {
                mExporter.generateExport(writer);
                writer.flush();
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

        switch (mExportParams.getExportTarget()) {
            case SHARING:
                shareFile(mExportParams.getTargetFilepath());
                break;

            case DROPBOX:
                copyExportToDropbox();
                break;

            case GOOGLE_DRIVE:
                copyExportToGoogleDrive();
                break;

            case SD_CARD:
            default:
                copyExportToSDCard();
                break;
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

        if (mExportParams.shouldDeleteTransactionsAfterExport()) {
            backupAndDeleteTransactions();

            //now refresh the respective views
            if (mContext instanceof AccountsActivity){
                ((AccountsActivity) mContext).getCurrentAccountListFragment().refresh();
            }
            if (mContext instanceof TransactionsActivity){
                ((TransactionsActivity) mContext).refresh();
            }
        }

        if (mContext instanceof Activity) {
            if (mProgressDialog != null && mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        }
    }

    private void copyExportToGoogleDrive(){
        final GoogleApiClient googleApiClient = SettingsActivity.getGoogleApiClient(GnuCashApplication.getAppContext());
        googleApiClient.blockingConnect();
        final ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
                ResultCallback<DriveFolder.DriveFileResult>() {
                    @Override
                    public void onResult(DriveFolder.DriveFileResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.e(TAG, "Error while trying to sync to Google Drive");
                            return;
                        }
                        Log.i(TAG, "Created a file with content: " + result.getDriveFile().getDriveId());
                    }
                };

        Drive.DriveApi.newDriveContents(googleApiClient).setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
            @Override
            public void onResult(DriveApi.DriveContentsResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.e(TAG, "Error while trying to create new file contents");
                    return;
                }
                final DriveContents driveContents = result.getDriveContents();
                // write content to DriveContents
                OutputStream outputStream = driveContents.getOutputStream();
                File exportedFile = new File(mExportParams.getTargetFilepath());

                try {
                    FileInputStream fileInputStream = new FileInputStream(exportedFile);
                    byte[] buffer = new byte[1024];
                    int count = 0;

                    while ((count = fileInputStream.read(buffer)) >= 0) {
                        outputStream.write(buffer, 0, count);
                    }
                    fileInputStream.close();
                    outputStream.flush();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }

                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(exportedFile.getName())
                        .setMimeType(getExportMimeType())
                        .build();

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                String folderId = sharedPreferences.getString(mContext.getString(R.string.key_google_drive_app_folder_id), "");
                DriveFolder folder = Drive.DriveApi.getFolder(googleApiClient, DriveId.decodeFromString(folderId));
                // create a file on root folder
                folder.createFile(googleApiClient, changeSet, driveContents)
                        .setResultCallback(fileCallback);
            }
        });
    }

    /**
     * Returns the mime type for the configured export format
     * @return MIME type as string
     */
    public String getExportMimeType(){
        switch (mExportParams.getExportFormat()){
            case OFX:
            case XML:
                return "text/xml";
            case QIF:
            default:
                return "text/plain";
        }
    }

    private void copyExportToDropbox() {
        String dropboxAppKey = mContext.getString(R.string.dropbox_app_key, SettingsActivity.DROPBOX_APP_KEY);
        String dropboxAppSecret = mContext.getString(R.string.dropbox_app_secret, SettingsActivity.DROPBOX_APP_SECRET);
        DbxAccountManager mDbxAcctMgr = DbxAccountManager.getInstance(mContext.getApplicationContext(),
                dropboxAppKey, dropboxAppSecret);
        DbxFile dbExportFile = null;
        try {
            DbxFileSystem dbxFileSystem = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
            File exportedFile = new File(mExportParams.getTargetFilepath());
            dbExportFile = dbxFileSystem.create(new DbxPath(exportedFile.getName()));
            dbExportFile.writeFromExistingFile(exportedFile, false);
        } catch (DbxException.Unauthorized unauthorized) {
            unauthorized.printStackTrace();
            Log.e(TAG, unauthorized.getMessage());
            if (mContext instanceof Activity){
                Toast.makeText(mContext, "DropBox access not authorized. Check your Settings", Toast.LENGTH_LONG).show();
            }
        } catch (DbxException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        } finally {
            if (dbExportFile != null) {
                dbExportFile.close();
            }
        }
    }

    private void copyExportToSDCard() {
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
            return;
        }

        if (mContext instanceof Activity) {
            //file already exists, just let the user know
            Toast.makeText(mContext,
                    mContext.getString(R.string.toast_format_exported_to, mExportParams.getExportFormat().name())
                            + dst.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Backups of the database, saves opening balances (if necessary)
     * and deletes all non-template transactions in the database.
     */
    private void backupAndDeleteTransactions(){
        GncXmlExporter.createBackup(); //create backup before deleting everything
        List<Transaction> openingBalances = new ArrayList<Transaction>();
        boolean preserveOpeningBalances = GnuCashApplication.shouldSaveOpeningBalances(false);
        if (preserveOpeningBalances) {
            openingBalances = AccountsDbAdapter.getInstance().getAllOpeningBalanceTransactions();
        }

        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        transactionsDbAdapter.deleteAllNonTemplateTransactions();

        if (preserveOpeningBalances) {
            transactionsDbAdapter.bulkAddTransactions(openingBalances);
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
        shareIntent.setType("text/xml");
        ArrayList<Uri> exportFiles = new ArrayList<>();
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
        ArrayList<String> splitFiles = new ArrayList<>();
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
