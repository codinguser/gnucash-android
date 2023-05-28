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


import androidx.annotation.NonNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Format data to be CSV-compatible
 *
 * @author Semyannikov Gleb <nightdevgame@gmail.com>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class CsvWriter extends BufferedWriter {
    private String separator = ",";

    public CsvWriter(Writer writer){
        super(writer);
    }

    public CsvWriter(Writer writer, String separator){
        super(writer);
        this.separator = separator;
    }

    @Override
    public void write(@NonNull String str) throws IOException {
        this.write(str, 0, str.length());
    }

    /**
     * Writes a CSV token and the separator to the underlying output stream.
     *
     * The token **MUST NOT** not contain the CSV separator. If the separator is found in the token, then
     * the token will be escaped as specified by RFC 4180
     * @param token Token to be written to file
     * @throws IOException if the token could not be written to the underlying stream
     */
    public void writeToken(String token) throws IOException {
        if (token == null || token.isEmpty()){
            write(separator);
        } else {
            token = escape(token);
            write(token + separator);
        }
    }

    /**
     * Escape any CSV separators by surrounding the token in double quotes
     * @param token String token to be written to CSV
     * @return Escaped CSV token
     */
    @NonNull
    private String escape(@NonNull String token) {
        if (token.contains(separator)){
            return "\"" + token + "\"";
        }
        return token;
    }

    /**
     * Writes a token to the CSV file and appends end of line to it.
     *
     * The token **MUST NOT** not contain the CSV separator. If the separator is found in the token, then
     * the token will be escaped as specified by RFC 4180
     * @param token The token to be written to the file
     * @throws IOException if token could not be written to underlying writer
     */
    public void writeEndToken(String token) throws IOException {
        if (token != null && !token.isEmpty()) {
            write(escape(token));
        }
        this.newLine();
    }

}
