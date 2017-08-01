package org.gnucash.android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Misc methods for dealing with files.
 */
public final class FileUtils {
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
}
