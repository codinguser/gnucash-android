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
