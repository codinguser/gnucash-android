/* Copyright (c) 2018 Àlex Magaz Graça <alexandre.magaz@gmail.com>
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

package org.gnucash.android.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.model.Book;
import org.gnucash.android.receivers.PeriodicJobReceiver;
import org.gnucash.android.ui.settings.PreferenceActivity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;


/**
 * Deals with all backup-related tasks.
 */
public class BackupManager {
    private static final String LOG_TAG = "BackupManager";
    public static final String KEY_BACKUP_FILE = "book_backup_file_key";

    /**
     * Perform an automatic backup of all books in the database.
     * This method is run every time the service is executed
     */
    static void backupAllBooks() {
        BooksDbAdapter booksDbAdapter = BooksDbAdapter.getInstance();
        List<String> bookUIDs = booksDbAdapter.getAllBookUIDs();
        Context context = GnuCashApplication.getAppContext();

        for (String bookUID : bookUIDs) {
            String backupFile = getBookBackupFileUri(bookUID);
            if (backupFile == null){
                backupBook(bookUID);
                continue;
            }

            try (BufferedOutputStream bufferedOutputStream =
                    new BufferedOutputStream(context.getContentResolver().openOutputStream(Uri.parse(backupFile)))){
                GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bufferedOutputStream);
                OutputStreamWriter writer = new OutputStreamWriter(gzipOutputStream);
                ExportParams params = new ExportParams(ExportFormat.XML);
                new GncXmlExporter(params).generateExport(writer);
                writer.close();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Auto backup failed for book " + bookUID);
                ex.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(ex);
            }
        }
    }

    /**
     * Backs up the active book to the directory {@link #getBackupFolderPath(String)}.
     *
     * @return {@code true} if backup was successful, {@code false} otherwise
     */
    public static boolean backupActiveBook() {
        return backupBook(BooksDbAdapter.getInstance().getActiveBookUID());
    }

    /**
     * Backs up the book with UID {@code bookUID} to the directory
     * {@link #getBackupFolderPath(String)}.
     *
     * @param bookUID Unique ID of the book
     * @return {@code true} if backup was successful, {@code false} otherwise
     */
    public static boolean backupBook(String bookUID){
        OutputStream outputStream;
        try {
            String backupFile = getBookBackupFileUri(bookUID);
            if (backupFile != null){
                outputStream = GnuCashApplication.getAppContext().getContentResolver().openOutputStream(Uri.parse(backupFile));
            } else { //no Uri set by user, use default location on SD card
                backupFile = getBackupFilePath(bookUID);
                outputStream = new FileOutputStream(backupFile);
            }

            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bufferedOutputStream);
            OutputStreamWriter writer = new OutputStreamWriter(gzipOutputStream);

            ExportParams params = new ExportParams(ExportFormat.XML);
            new GncXmlExporter(params).generateExport(writer);
            writer.close();
            return true;
        } catch (IOException | Exporter.ExporterException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Log.e("GncXmlExporter", "Error creating XML  backup", e);
            return false;
        }
    }

    /**
     * Returns the full path of a file to make database backup of the specified book.
     * Backups are done in XML format and are Gzipped (with ".gnca" extension).
     * @param bookUID GUID of the book
     * @return the file path for backups of the database.
     * @see #getBackupFolderPath(String)
     */
    private static String getBackupFilePath(String bookUID){
        Book book = BooksDbAdapter.getInstance().getRecord(bookUID);
        return getBackupFolderPath(book.getUID())
               + Exporter.buildExportFilename(ExportFormat.XML, book.getDisplayName());
    }

    /**
     * Returns the path to the backups folder for the book with GUID {@code bookUID}.
     *
     * <p>Each book has its own backup folder.</p>
     *
     * @return Absolute path to backup folder for the book
     */
    private static String getBackupFolderPath(String bookUID){
        String baseFolderPath = GnuCashApplication.getAppContext()
                                                  .getExternalFilesDir(null)
                                                  .getAbsolutePath();
        String path = baseFolderPath + "/" + bookUID + "/backups/";
        File file = new File(path);
        if (!file.exists())
            file.mkdirs();
        return path;
    }

    /**
     * Return the user-set backup file URI for the book with UID {@code bookUID}.
     * @param bookUID Unique ID of the book
     * @return DocumentFile for book backups, or null if the user hasn't set any.
     */
    @Nullable
    public static String getBookBackupFileUri(String bookUID){
        SharedPreferences sharedPreferences = PreferenceActivity.getBookSharedPreferences(bookUID);
        return sharedPreferences.getString(KEY_BACKUP_FILE, null);
    }

    public static List<File> getBackupList(String bookUID) {
        File[] backupFiles = new File(getBackupFolderPath(bookUID)).listFiles();
        Arrays.sort(backupFiles);
        List<File> backupFilesList = Arrays.asList(backupFiles);
        Collections.reverse(backupFilesList);
        return  backupFilesList;
    }

    public static void schedulePeriodicBackups(Context context) {
        Log.i(LOG_TAG, "Scheduling backup job");
        Intent intent = new Intent(context, PeriodicJobReceiver.class);
        intent.setAction(PeriodicJobReceiver.ACTION_BACKUP);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_DAY, alarmIntent);
    }
}
