/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
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

import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.CommoditiesDbAdapter;
import org.gnucash.android.model.Commodity;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * XML stream handler for parsing currencies to add to the database
 */
public class CommoditiesXmlHandler extends DefaultHandler {

    public static final String TAG_CURRENCY         = "currency";
    public static final String ATTR_ISO_CODE        = "isocode";
    public static final String ATTR_FULL_NAME       = "fullname";
    public static final String ATTR_NAMESPACE       = "namespace";
    public static final String ATTR_EXCHANGE_CODE   = "exchange-code";
    public static final String ATTR_SMALLEST_FRACTION = "smallest-fraction";
    public static final String ATTR_LOCAL_SYMBOL = "local-symbol";
    /**
     * List of commodities parsed from the XML file.
     * They will be all added to db at once at the end of the document
     */
    private List<Commodity> mCommodities;

    private CommoditiesDbAdapter mCommoditiesDbAdapter;

    public CommoditiesXmlHandler(SQLiteDatabase db){
        if (db == null){
            mCommoditiesDbAdapter = GnuCashApplication.getCommoditiesDbAdapter();
        } else {
            mCommoditiesDbAdapter = new CommoditiesDbAdapter(db);
        }
        mCommodities = new ArrayList<>();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals(TAG_CURRENCY)) {
            String isoCode = attributes.getValue(ATTR_ISO_CODE);
            String fullname = attributes.getValue(ATTR_FULL_NAME);
            String namespace = attributes.getValue(ATTR_NAMESPACE);
            String cusip = attributes.getValue(ATTR_EXCHANGE_CODE);
            //TODO: investigate how up-to-date the currency XML list is and use of parts-per-unit vs smallest-fraction.
            //some currencies like XAF have smallest fraction 100, but parts-per-unit of 1.
            // However java.util.Currency agrees only with the parts-per-unit although we use smallest-fraction in the app
            // This could lead to inconsistencies over time
            String smallestFraction = attributes.getValue(ATTR_SMALLEST_FRACTION);
            String localSymbol = attributes.getValue(ATTR_LOCAL_SYMBOL);

            Commodity commodity = new Commodity(fullname, isoCode, Integer.parseInt(smallestFraction));
            commodity.setNamespace(Commodity.Namespace.valueOf(namespace));
            commodity.setCusip(cusip);
            commodity.setLocalSymbol(localSymbol);

            mCommodities.add(commodity);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        mCommoditiesDbAdapter.bulkAddRecords(mCommodities);
    }
}
