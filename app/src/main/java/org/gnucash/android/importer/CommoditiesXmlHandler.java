package org.gnucash.android.importer;

import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.db.CommoditiesDbAdapter;
import org.gnucash.android.model.Commodity;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.List;

/**
 * XML stream handler for parsing currencies to add to the database
 */
public class CommoditiesXmlHandler extends DefaultHandler {

    private List<Commodity> mCommodities;

    private CommoditiesDbAdapter mCommoditiesDbAdapter;

    public CommoditiesXmlHandler(SQLiteDatabase db){
        //TODO: initialize adapter
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        String isoCode      = attributes.getValue("isocode");
        String fullname     = attributes.getValue("fullname");
        String namespace    = attributes.getValue("namespace");
        String cusip        = attributes.getValue("exchange-code");
        String smallestFraction = attributes.getValue("smallest-fraction");


        Commodity commodity = new Commodity(fullname, isoCode, Integer.parseInt(smallestFraction));
        commodity.setNamespace(Commodity.Namespace.valueOf(namespace));
        commodity.setCusip(cusip);

        mCommodities.add(commodity);
    }

    @Override
    public void endDocument() throws SAXException {
        //TODO: bulk add commodities
    }
}
