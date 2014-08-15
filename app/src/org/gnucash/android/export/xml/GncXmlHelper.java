package org.gnucash.android.export.xml;

import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.TransactionType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Date: 17.07.2014
 *
 * @author Ngewi
 */
public abstract class GncXmlHelper {
    public static final String TAG_PREFIX           = "gnc:";

    public static final String ATTR_KEY_CD_TYPE     = "cd:type";
    public static final String ATTR_KEY_TYPE        = "type";
    public static final String ATTR_KEY_VERSION     = "version";
    public static final String ATTR_VALUE_STRING    = "string";
    public static final String ATTR_VALUE_GUID      = "guid";
    public static final String ATTR_VALUE_BOOK      = "book";
    public static final String ATTR_VALUE_GDATE     = "gdate";

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
    public static final String TAG_COMMODITY_SCU    = "act:commodity-scu";
    public static final String TAG_PARENT_UID       = "act:parent";
    public static final String TAG_ACCOUNT          = "gnc:account";
    public static final String TAG_SLOT_KEY         = "slot:key";
    public static final String TAG_SLOT_VALUE       = "slot:value";
    public static final String TAG_ACT_SLOTS        = "act:slots";
    public static final String TAG_SLOT             = "slot";
    public static final String TAG_ACCT_DESCRIPTION = "act:description";

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

    public static final String TAG_SPLIT_ID         = "split:id";
    public static final String TAG_SPLIT_MEMO       = "split:memo";
    public static final String TAG_RECONCILED_STATE = "split:reconciled_state";
    public static final String TAG_SPLIT_ACCOUNT    = "split:account";
    public static final String TAG_SPLIT_VALUE      = "split:value";
    public static final String TAG_SPLIT_QUANTITY   = "split:quantity";

    //TODO: Remove this in the future when scheduled transactions are improved
    public static final String TAG_RECURRENCE_PERIOD = "trn:recurrence_period";

    public static final String BOOK_VERSION         = "2.0.0";
    public static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");


    public static final String KEY_PLACEHOLDER      = "placeholder";
    public static final String KEY_COLOR            = "color";
    public static final String KEY_FAVORITE         = "favorite";
    public static final String KEY_NOTES            = "notes";
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
    public static BigDecimal parseMoney(String amountString){
        String[] tokens = amountString.split("/");
        BigDecimal numerator = new BigDecimal(tokens[0]);
        BigDecimal denominator = new BigDecimal(tokens[1]);

        return numerator.divide(denominator);
    }

    /**
     * Helper method for creating slot key-value pairs in the GnuCash XML structure.
     * <p>This method is only a helper for creating slots whose values are of string type</p>
     * @param doc {@link org.w3c.dom.Document} for creating nodes
     * @param key Slot key as string
     * @param value Slot value as String
     * @return Element node containing the key-value pair
     */
    public static Element createSlot(Document doc, String key, String value, String valueType){
        Element slotNode  = doc.createElement(TAG_SLOT);
        Element slotKeyNode = doc.createElement(TAG_SLOT_KEY);
        slotKeyNode.appendChild(doc.createTextNode(key));
        Element slotValueNode = doc.createElement(TAG_SLOT_VALUE);
        slotValueNode.setAttribute(ATTR_KEY_TYPE, valueType);
        slotValueNode.appendChild(doc.createTextNode(value));
        slotNode.appendChild(slotKeyNode);
        slotNode.appendChild(slotValueNode);

        return slotNode;
    }
}
