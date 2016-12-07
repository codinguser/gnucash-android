/*
 * Copyright (c) 2016 Ngewi Fet <ngewif@gmail.com>
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

import android.util.Log;

import org.gnucash.android.db.MigrationHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Moves all files from one directory  into another.
 * The destination directory is assumed to already exist
 */
public class RecursiveMoveFiles implements Runnable {
    private static final String LOG_TAG = "RecursiveMoveFiles";

    File mSource;
    File mDestination;

    /**
     * Constructor, specify origin and target directories
     * @param src Source directory/file. If directory, all files within it will be moved
     * @param dst Destination directory/file. If directory, it should already exist
     */
    public RecursiveMoveFiles(File src, File dst){
        mSource = src;
        mDestination = dst;
    }

    /**
     * Copy file from one location to another.
     * Does not support copying of directories
     * @param src Source file
     * @param dst Destination of the file
     * @return {@code true} if the file was successfully copied, {@code false} otherwise
     * @throws IOException
     */
    private boolean copy(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            long bytesCopied = inChannel.transferTo(0, inChannel.size(), outChannel);
            return bytesCopied >= src.length();
        } finally {
            if (inChannel != null)
                inChannel.close();
            outChannel.close();
        }
    }

    /**
     * Recursively copy files from one location to another and deletes the origin files after copy.
     * If the source file is a directory, all of the files in it will be moved.
     * This method will create the destination directory if the {@code src} is also a directory
     * @param src origin file
     * @param dst destination file or directory
     * @return number of files copied (excluding parent directory)
     */
    private int recursiveMove(File src, File dst){
        int copyCount = 0;
        if (src.isDirectory() && src.listFiles() != null){
            dst.mkdirs(); //we assume it works everytime. Great, right?
            for (File file : src.listFiles()) {
                File target = new File(dst, file.getName());
                 copyCount += recursiveMove(file, target);
            }
            src.delete();
        } else {
            try {
                if(copy(src, dst))
                    src.delete();
            } catch (IOException e) {
                Log.d(MigrationHelper.LOG_TAG, "Error moving file: " + src.getAbsolutePath());
            }
        }
        Log.d(LOG_TAG, String.format("Moved %d files from %s to %s", copyCount, src.getPath(), dst.getPath()));
        return copyCount;
    }

    @Override
    public void run() {
        recursiveMove(mSource, mDestination);
    }
}
