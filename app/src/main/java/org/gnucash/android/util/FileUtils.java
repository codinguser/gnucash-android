package org.gnucash.android.util;

import android.support.annotation.NonNull;
import android.util.Log;

import org.gnucash.android.export.ExportAsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Misc methods for dealing with files.
 */
public final class FileUtils {
    private static final String LOG_TAG = "FileUtils";

    public static void zipFiles(List<String> files, String zipFileName) throws IOException {
        OutputStream outputStream = new FileOutputStream(zipFileName);
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        byte[] buffer = new byte[1024];
        for (String fileName : files) {
            File file = new File(fileName);
            FileInputStream fileInputStream = new FileInputStream(file);
            zipOutputStream.putNextEntry(new ZipEntry(file.getName()));

            int length;
            while ((length = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, length);
            }
            zipOutputStream.closeEntry();
            fileInputStream.close();
        }
        zipOutputStream.close();
    }

    /**
     * Moves a file from <code>src</code> to <code>dst</code>
     * @param src Absolute path to the source file
     * @param dst Absolute path to the destination file
     * @throws IOException if the file could not be moved.
     */
    public static void moveFile(String src, String dst) throws IOException {
        File srcFile = new File(src);
        File dstFile = new File(dst);
        FileChannel inChannel = new FileInputStream(srcFile).getChannel();
        FileChannel outChannel = new FileOutputStream(dstFile).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            outChannel.close();
        }
        srcFile.delete();
    }

    /**
     * Move file from a location on disk to an outputstream.
     * The outputstream could be for a URI in the Storage Access Framework
     * @param src Input file (usually newly exported file)
     * @param outputStream Output stream to write to
     * @throws IOException if error occurred while moving the file
     */
    public static void moveFile(@NonNull String src, @NonNull OutputStream outputStream)
            throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        try (FileInputStream inputStream = new FileInputStream(src)) {
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        } finally {
            outputStream.flush();
            outputStream.close();
        }
        Log.i(LOG_TAG, "Deleting temp export file: " + src);
        new File(src).delete();
    }
}
