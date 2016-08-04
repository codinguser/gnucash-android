/*
 * Copyright (c) 2014 Ngewi Fet <ngewif@gmail.com>
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
package org.gnucash.android.importer;

import android.util.Log;

import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.util.PreferencesHelper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Importer for Gnucash XML files and GNCA (GnuCash Android) XML files
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class GncXmlImporter {

    /**
     * Parse GnuCash XML input and populates the database
     * @param gncXmlInputStream InputStream source of the GnuCash XML file
     * @return GUID of the book into which the XML was imported
     */
    public static String parse(InputStream gncXmlInputStream) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        XMLReader xr = sp.getXMLReader();

        BufferedInputStream bos;
        PushbackInputStream pb = new PushbackInputStream( gncXmlInputStream, 2 ); //we need a pushbackstream to look ahead
        byte [] signature = new byte[2];
        pb.read( signature ); //read the signature
        pb.unread( signature ); //push back the signature to the stream
        if( signature[ 0 ] == (byte) 0x1f && signature[ 1 ] == (byte) 0x8b ) //check if matches standard gzip magic number
            bos = new BufferedInputStream(new GZIPInputStream(pb));
        else
            bos = new BufferedInputStream(pb);

        //TODO: Set an error handler which can log errors
        Log.d(GncXmlImporter.class.getSimpleName(), "Start import");
        GncXmlHandler handler = new GncXmlHandler();
        xr.setContentHandler(handler);
        long startTime = System.nanoTime();
        xr.parse(new InputSource(bos));
        long endTime = System.nanoTime();
        Log.d(GncXmlImporter.class.getSimpleName(), String.format("%d ns spent on importing the file", endTime-startTime));

        String bookUID = handler.getBookUID();
        PreferencesHelper.setLastExportTime(
                TransactionsDbAdapter.getInstance().getTimestampOfLastModification(),
                bookUID
        );

        return bookUID;
    }
}
