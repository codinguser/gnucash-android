/*
 * Copyright (c) 2018 Semyannikov Gleb <nightdevgame@gmail.com>
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

package org.gnucash.android.export.csv;


import java.io.IOException;
import java.io.Writer;

/**
 * Format data to be CSV-compatible
 *
 * @author Semyannikov Gleb <nightdevgame@gmail.com>
 */
public class CsvWriter {
    private Writer writer;

    public CsvWriter(Writer writer){
        this.writer = writer;
    }

    public void write(String str) throws IOException {
        if (str == null || str.length() < 1) {
            return;
        }

        String head = str.substring(0, str.length() - 1);
        char separator = str.charAt(str.length() - 1);
        if (head.indexOf(separator) > -1) {
            head = '"' + head + '"';
        }

        writer.write(head + separator);
    }
}
