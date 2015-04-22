/*
 * Copyright (c) 2014 - 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.export.xml;

import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.TransactionType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.Locale;

/**
 * Collection of helper tags and methods for Gnc XML export
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 */
public abstract class GncXmlHelper {
    public static final String TAG_PREFIX           = "gnc:";

    public static final String ATTR_KEY_CD_TYPE     = "cd:type";
    public static final String ATTR_KEY_TYPE        = "type";
    public static final String ATTR_KEY_VERSION     = "version";
    public static final String ATTR_VALUE_STRING    = "string";
    public static final String ATTR_VALUE_GUID      = "guid";
    public static final String ATTR_VALUE_BOOK      = "book";
    public static final String TAG_GDATE            = "gdate";

    /*
    Qualified GnuCash XML tag names
     */
    public static final String TAG_ROOT             = "gnc-v2";
    public static final String TAG_BOOK             = "gnc:book";
    public static final String TAG_BOOK_ID          = "book:id";
    public static final String TAG_COUNT_DATA       = "gnc:count-data";

    public static final String TAG_COMMODITY        = "gnc:commodity";
    public static final String TAG_NAME             = "act:name";
    public static final String TAG_ACCT_ID          = "act:id";
    public static final String TAG_TYPE             = "act:type";
    public static final String TAG_COMMODITY_ID     = "cmdty:id";
    public static final String TAG_COMMODITY_SPACE  = "cmdty:space";
    public static final String TAG_ACCOUNT_COMMODITY = "act:commodity";
    public static final String TAG_COMMODITY_SCU    = "act:commodity-scu";
    public static final String TAG_PARENT_UID       = "act:parent";
    public static final String TAG_ACCOUNT          = "gnc:account";
    public static final String TAG_SLOT_KEY         = "slot:key";
    public static final String TAG_SLOT_VALUE       = "slot:value";
    public static final String TAG_ACT_SLOTS        = "act:slots";
    public static final String TAG_SLOT             = "slot";
    public static final String TAG_ACCT_DESCRIPTION = "act:description"; //TODO: Use this when we add descriptions to the database

    public static final String TAG_TRANSACTION      = "gnc:transaction";
    public static final String TAG_TRX_ID           = "trn:id";
    public static final String TAG_TRX_CURRENCY     = "trn:currency";
    public static final String TAG_DATE_POSTED      = "trn:date-posted";
    public static final String TAG_DATE             = "ts:date";
    public static final String TAG_DATE_ENTERED     = "trn:date-entered";
    public static final String TAG_TRN_DESCRIPTION  = "trn:description";
    public static final String TAG_TRN_SPLITS       = "trn:splits";
    public static final String TAG_TRN_SPLIT        = "trn:split";
    public static final String TAG_TRN_SLOTS        = "trn:slots";
    public static final String TAG_TEMPLATE_TRANSACTIONS = "gnc:template-transactions";

    public static final String TAG_SPLIT_ID         = "split:id";
    public static final String TAG_SPLIT_MEMO       = "split:memo";
    public static final String TAG_RECONCILED_STATE = "split:reconciled-state";
    public static final String TAG_SPLIT_ACCOUNT    = "split:account";
    public static final String TAG_SPLIT_VALUE      = "split:value";
    public static final String TAG_SPLIT_QUANTITY   = "split:quantity";
    public static final String TAG_SPLIT_SLOTS      = "split:slots";

    //TODO: Remove this in the future when scheduled transactions are improved
    @Deprecated
    public static final String TAG_RECURRENCE_PERIOD = "trn:recurrence_period";

    public static final String TAG_SCHEDULED_ACTION         = "gnc:schedxaction";
    public static final String TAG_SX_ID                    = "sx:id";
    public static final String TAG_SX_NAME                  = "sx:name";
    public static final String TAG_SX_ENABLED               = "sx:enabled";
    public static final String TAG_SX_AUTO_CREATE           = "sx:autoCreate";
    public static final String TAG_SX_AUTO_CREATE_NOTIFY    = "sx:autoCreateNotify";
    public static final String TAG_SX_ADVANCE_CREATE_DAYS   = "sx:advanceCreateDays";
    public static final String TAG_SX_ADVANCE_REMIND_DAYS   = "sx:advanceRemindDays";
    public static final String TAG_SX_INSTANCE_COUNT        = "sx:instanceCount";
    public static final String TAG_SX_START                 = "sx:start";
    public static final String TAG_SX_LAST                  = "sx:last";
    public static final String TAG_SX_END                   = "sx:end";
    public static final String TAG_SX_NUM_OCCUR             = "sx:num-occur";
    public static final String TAG_SX_REM_OCCUR             = "sx:rem-occur";
    public static final String TAG_SX_TAG                   = "sx:tag";
    public static final String TAG_SX_TEMPL_ACTION          = "sx:templ-action"; //FIXME: This tag is unknown to GnuCash desktop. For full compat, we will need to fix it
    public static final String TAG_SX_SCHEDULE              = "sx:schedule";
    public static final String TAG_RECURRENCE               = "gnc:recurrence";
    public static final String TAG_RX_MULT                  = "recurrence:mult";
    public static final String TAG_RX_PERIOD_TYPE           = "recurrence:period_type";
    public static final String TAG_RX_START                 = "recurrence:start";


    public static final String RECURRENCE_VERSION   = "1.0.0";
    public static final String BOOK_VERSION         = "2.0.0";
    public static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US);
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public static final String KEY_PLACEHOLDER      = "placeholder";
    public static final String KEY_COLOR            = "color";
    public static final String KEY_FAVORITE         = "favorite";
    public static final String KEY_NOTES            = "notes";
    public static final String KEY_EXPORTED         = "exported";
    public static final String KEY_SCHEDX_ACTION    = "sched-xaction";
    public static final String KEY_SPLIT_ACCOUNT    = "account";
    public static final String KEY_DEBIT_FORMULA    = "debit-formula";
    public static final String KEY_CREDIT_FORMULA   = "credit-formula";
    public static final String KEY_FROM_SCHED_ACTION        = "from-sched-xaction";
    public static final String KEY_DEFAULT_TRANSFER_ACCOUNT = "default_transfer_account";


    /**
     * Formats dates for the GnuCash XML format
     * @param milliseconds Milliseconds since epoch
     */
    public static String formatDate(long milliseconds){
        return TIME_FORMATTER.format(new Date(milliseconds));
    }

    /**
     * Parses a date string formatted in the format "yyyy-MM-dd HH:mm:ss Z"
     * @param dateString String date representation
     * @return Time in milliseconds since epoch
     * @throws ParseException if the date string could not be parsed e.g. because of different format
     */
    public static long parseDate(String dateString) throws ParseException {
        Date date = TIME_FORMATTER.parse(dateString);
        return date.getTime();
    }

    /**
     * Formats the money amounts into the GnuCash XML format. GnuCash stores debits as positive and credits as negative
     * @param split Split for which the amount is to be formatted
     * @return GnuCash XML representation of amount
     */
    public static String formatMoney(Split split){
        Money amount = split.getType() == TransactionType.DEBIT ? split.getAmount() : split.getAmount().negate();
        BigDecimal decimal = amount.asBigDecimal().multiply(new BigDecimal(100));
        return decimal.stripTrailingZeros().toPlainString() + "/100";
    }

    /**
     * Parses amount strings from GnuCash XML into {@link java.math.BigDecimal}s
     * @param amountString String containing the amount
     * @return BigDecimal with numerical value
     */
    public static BigDecimal parseMoney(String amountString) throws ParseException {
        int pos = amountString.indexOf("/");
        if (pos < 0)
        {
            throw new ParseException("Cannot parse money string : " + amountString, 0);
        }
        BigInteger numerator = new BigInteger(amountString.substring(0, pos));
        int scale = amountString.length() - pos - 2;
        return new BigDecimal(numerator, scale);
    }

    /**
     * Returns a {@link java.text.NumberFormat} for parsing or writing amounts in template splits
     * @return NumberFormat object
     */
    public static NumberFormat getNumberFormatForTemplateSplits(){
        //TODO: Check if GnuCash desktop always using this formatting or if it is device locale specific
        return NumberFormat.getNumberInstance(Locale.GERMANY);
    }

}
