package org.gnucash.android.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.xml.GncXmlExporter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class BackupManager {
    private static final String LOG_TAG = "BackupManager";

    /**
     * Perform an automatic backup of all books in the database.
     * This method is run everytime the service is executed
     */
    public static void backupAllBooks() {
        BooksDbAdapter booksDbAdapter = BooksDbAdapter.getInstance();
        List<String> bookUIDs = booksDbAdapter.getAllBookUIDs();
        Context context = GnuCashApplication.getAppContext();

        for (String bookUID : bookUIDs) {
            String backupFile = BookUtils.getBookBackupFileUri(bookUID);
            if (backupFile == null){
                GncXmlExporter.createBackup(bookUID);
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
                Crashlytics.logException(ex);
            }
        }
    }
}
