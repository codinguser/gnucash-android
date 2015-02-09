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
package org.gnucash.android.export.ofx;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Helper class with collection of useful method and constants for the OFX export
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class OfxHelper {
    /**
     * A date formatter used when creating file names for the exported data
     */
    public final static SimpleDateFormat OFX_DATE_FORMATTER = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);

    /**
     * The Transaction ID is usually the client ID sent in a request.
     * Since the data exported is not as a result of a request, we use 0
     */
    public static final String UNSOLICITED_TRANSACTION_ID = "0";

    /**
     * Header for OFX documents
     */
    public static final String OFX_HEADER = "OFXHEADER=\"200\" VERSION=\"211\" SECURITY=\"NONE\" OLDFILEUID=\"NONE\" NEWFILEUID=\"NONE\"";

    /**
     * SGML header for OFX. Used for compatibility with desktop GnuCash
     */
    public static final String OFX_SGML_HEADER = "ENCODING:UTF-8\nOFXHEADER:100\nDATA:OFXSGML\nVERSION:211\nSECURITY:NONE\nCHARSET:UTF-8\nCOMPRESSION:NONE\nOLDFILEUID:NONE\nNEWFILEUID:NONE";

    /*
    * XML tag name constants for the OFX file
     */
    public static final String TAG_TRANSACTION_UID      = "TRNUID";
    public static final String TAG_BANK_MESSAGES_V1     = "BANKMSGSRSV1";
    public static final String TAG_CURRENCY_DEF         = "CURDEF";
    public static final String TAG_BANK_ID              = "BANKID";
    public static final String TAG_ACCOUNT_ID           = "ACCTID";
    public static final String TAG_ACCOUNT_TYPE         = "ACCTTYPE";
    public static final String TAG_BANK_ACCOUNT_FROM    = "BANKACCTFROM";
    public static final String TAG_BALANCE_AMOUNT       = "BALAMT";
    public static final String TAG_DATE_AS_OF           = "DTASOF";
    public static final String TAG_LEDGER_BALANCE       = "LEDGERBAL";
    public static final String TAG_DATE_START           = "DTSTART";
    public static final String TAG_DATE_END             = "DTEND";
    public static final String TAG_TRANSACTION_TYPE     = "TRNTYPE";
    public static final String TAG_DATE_POSTED          = "DTPOSTED";
    public static final String TAG_DATE_USER            = "DTUSER";
    public static final String TAG_TRANSACTION_AMOUNT   = "TRNAMT";
    public static final String TAG_TRANSACTION_FITID    = "FITID";
    public static final String TAG_NAME                 = "NAME";
    public static final String TAG_MEMO                 = "MEMO";
    public static final String TAG_BANK_ACCOUNT_TO      = "BANKACCTTO";
    public static final String TAG_BANK_TRANSACTION_LIST    = "BANKTRANLIST";
    public static final String TAG_STATEMENT_TRANSACTIONS   = "STMTRS";
    public static final String TAG_STATEMENT_TRANSACTION    = "STMTTRN";
    public static final String TAG_STATEMENT_TRANSACTION_RESPONSE = "STMTTRNRS";


    /**
     * ID which will be used as the bank ID for OFX from this app
     */
    public static String APP_ID = "org.gnucash.android";

    /**
     * Returns the current time formatted using the pattern in {@link #OFX_DATE_FORMATTER}
     * @return Current time as a formatted string
     * @see #getOfxFormattedTime(long)
     */
    public static String getFormattedCurrentTime(){
        return getOfxFormattedTime(System.currentTimeMillis());
    }

    /**
     * Returns a formatted string representation of time in <code>milliseconds</code>
     * @param milliseconds Long value representing the time to be formatted
     * @return Formatted string representation of time in <code>milliseconds</code>
     */
    public static String getOfxFormattedTime(long milliseconds){
        Date date = new Date(milliseconds);
        String dateString = OFX_DATE_FORMATTER.format(date);
        TimeZone tz = Calendar.getInstance().getTimeZone();
        int offset = tz.getRawOffset();
        int hours   = (int) (( offset / (1000*60*60)) % 24);
        String sign = offset > 0 ?  "+" : "";
        return dateString + "[" + sign + hours + ":" + tz.getDisplayName(false, TimeZone.SHORT, Locale.getDefault()) + "]";
    }
}
