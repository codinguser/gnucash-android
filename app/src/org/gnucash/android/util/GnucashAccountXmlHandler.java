/*
 * Copyright (c) 2013 Ngewi Fet <ngewif@gmail.com>
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

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.db.AccountsDbAdapter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.Currency;

/**
 * Handler for parsing the GnuCash accounts structure file.
 * The discovered accounts are automatically added to the database
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class GnucashAccountXmlHandler extends DefaultHandler {

    /*
     * GnuCash account XML file qualified tag names. Used for matching tags
     */
    public static final String TAG_NAME             = "act:name";
    public static final String TAG_UID              = "act:id";
    public static final String TAG_TYPE             = "act:type";
    public static final String TAG_CURRENCY         = "cmdty:id";
    public static final String TAG_COMMODITY_SPACE  = "cmdty:space";
    public static final String TAG_PARENT_UID       = "act:parent";
    public static final String TAG_ACCOUNT          = "gnc:account";
    public static final String TAG_SLOT_KEY         = "slot:key";
    public static final String TAG_SLOT_VALUE       = "slot:value";

    /**
     * ISO 4217 currency code for "No Currency"
     */
    private static final String NO_CURRENCY_CODE    = "XXX";

    /**
     * Tag for logging
     */
    private static final String LOG_TAG = "GnuCashAccountImporter";

    /**
     * Value for placeholder slots in GnuCash account structure file
     */
    private static final String PLACEHOLDER_KEY = "placeholder";

    /**
     * Value of color slots in GnuCash account structure file
     */
    private static final String COLOR_KEY = "color";

    AccountsDbAdapter mDatabaseAdapter;

    /**
     * StringBuilder for accumulating characters between XML tags
     */
    StringBuilder mContent;

    /**
     * Reference to account which is built when each account tag is parsed in the XML file
     */
    Account mAccount;

    boolean mInColorSlot        = false;
    boolean mInPlaceHolderSlot  = false;
    boolean mISO4217Currency    = false;

    public GnucashAccountXmlHandler(Context context) {
        mDatabaseAdapter = new AccountsDbAdapter(context);
        mContent = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName,
                             String qualifiedName, Attributes attributes) throws SAXException {
        if (qualifiedName.equalsIgnoreCase(TAG_ACCOUNT)) {
            mAccount = new Account("new"); //dummy name, will be replaced when we find name tag
        }
    }

    @Override
    public void endElement(String uri, String localName, String qualifiedName) throws SAXException {
        String characterString = mContent.toString().trim();

        if (qualifiedName.equalsIgnoreCase(TAG_NAME)) {
            mAccount.setName(characterString);
        }

        if (qualifiedName.equalsIgnoreCase(TAG_UID)){
            mAccount.setUID(characterString);
        }

        if (qualifiedName.equalsIgnoreCase(TAG_TYPE)){
            mAccount.setAccountType(Account.AccountType.valueOf(characterString));
        }

        if (qualifiedName.equalsIgnoreCase(TAG_COMMODITY_SPACE)){
            if (characterString.equalsIgnoreCase("ISO4217")){
                mISO4217Currency = true;
            }
        }

        if (qualifiedName.equalsIgnoreCase(TAG_CURRENCY)){
            if (mAccount != null){
                Log.i(LOG_TAG, mAccount.getName() + " account has no currency!");
                String currencyCode = mISO4217Currency ? characterString : NO_CURRENCY_CODE;
                mAccount.setCurrency(Currency.getInstance(currencyCode));
            }
        }

        if (qualifiedName.equalsIgnoreCase(TAG_PARENT_UID)){
            mAccount.setParentUID(characterString);
        }

        if (qualifiedName.equalsIgnoreCase(TAG_ACCOUNT)){
            Log.d(LOG_TAG, "Saving account...");
            mDatabaseAdapter.addAccount(mAccount);

            //reset ISO 4217 flag for next account
            mISO4217Currency = false;
        }

        if (qualifiedName.equalsIgnoreCase(TAG_SLOT_KEY)){
            if (characterString.equals(PLACEHOLDER_KEY)){
                mInPlaceHolderSlot = true;
            }
            if (characterString.equals(COLOR_KEY)){
                mInColorSlot = true;
            }
        }

        if (qualifiedName.equalsIgnoreCase(TAG_SLOT_VALUE)){
            if (mInPlaceHolderSlot){
                if (characterString.equals("true")){
                    Log.d(LOG_TAG, "Setting account placeholder flag");
                    mAccount.setPlaceHolderFlag(true);
                };
                mInPlaceHolderSlot = false;
            }

            if (mInColorSlot){
                Log.d(LOG_TAG, "Setting account color");
                String color = "#" + characterString.trim().replaceAll(".(.)?", "$1").replace("null", "");
                mAccount.setColorCode(color);
                mInColorSlot = false;
            }
        }
        //reset the accumulated characters
        mContent.setLength(0);
    }

    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
        mContent.append(chars, start, length);
    }

    public static void parse(Context context, InputStream accountsInputStream){
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();

            BufferedInputStream bos = new BufferedInputStream(accountsInputStream);

            /** Create handler to handle XML Tags ( extends DefaultHandler ) */

            GnucashAccountXmlHandler handler = new GnucashAccountXmlHandler(context);
            xr.setContentHandler(handler);
            xr.parse(new InputSource(bos));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.toast_error_importing_accounts, Toast.LENGTH_LONG).show();
        }
    }
}
