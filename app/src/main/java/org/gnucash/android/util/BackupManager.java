package org.gnucash.android.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.model.Book;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
                Crashlytics.logException(ex);
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
            String backupFile = BookUtils.getBookBackupFileUri(bookUID);
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
            Crashlytics.logException(e);
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
    public static String getBackupFolderPath(String bookUID){
        String baseFolderPath = GnuCashApplication.getAppContext()
                                                  .getExternalFilesDir(null)
                                                  .getAbsolutePath();
        String path = baseFolderPath + "/" + bookUID + "/backups/";
        File file = new File(path);
        if (!file.exists())
            file.mkdirs();
        return path;
    }
}
