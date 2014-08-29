/*
 * Copyright (c) 2014 Ngewi Fet <ngewif@gmail.com>
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
package org.gnucash.android.importer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Importer for Gnucash XML files and GNCA (GnuCash Android) XML files
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class GncXmlImporter {

    /**
     * Parses XML into an already open database.
     * <p>This method is used mainly by the {@link org.gnucash.android.db.DatabaseHelper} for database migrations.<br>
     *     You should probably use {@link #parse(android.content.Context, java.io.InputStream)} instead</p>
     * @param db SQLite Database
     * @param gncXmlInputStream Input stream of GnuCash XML
     */
    public static void parse(SQLiteDatabase db, InputStream gncXmlInputStream) throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        XMLReader xr = sp.getXMLReader();

        BufferedInputStream bos = new BufferedInputStream(gncXmlInputStream);

        /** Create handler to handle XML Tags ( extends DefaultHandler ) */

        GncXmlHandler handler = new GncXmlHandler(db);
        xr.setContentHandler(handler);
        xr.parse(new InputSource(bos));
    }

    /**
     * Parse GnuCash XML input and populates the database
     * @param context Application context
     * @param gncXmlInputStream InputStream source of the GnuCash XML file
     */
    public static void parse(Context context, InputStream gncXmlInputStream) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        XMLReader xr = sp.getXMLReader();

        BufferedInputStream bos = new BufferedInputStream(gncXmlInputStream);

        //TODO: Set an error handler which can log errors

        GncXmlHandler handler = new GncXmlHandler(context, true);
        xr.setContentHandler(handler);
        long startTime = System.nanoTime();
        xr.parse(new InputSource(bos));
        long endTime = System.nanoTime();
        Log.d("Import", String.format("%d ns spent on importing the file", endTime-startTime));
    }
}
