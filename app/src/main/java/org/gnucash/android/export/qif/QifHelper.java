/*
 * Copyright (c) 2013 - 2014 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.export.qif;

import org.gnucash.android.model.AccountType;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class QifHelper {
    /*
    Prefixes for the QIF file
     */
    public static final String PAYEE_PREFIX         = "P";
    public static final String DATE_PREFIX          = "D";
    public static final String AMOUNT_PREFIX        = "T";
    public static final String MEMO_PREFIX          = "M";
    public static final String CATEGORY_PREFIX      = "L";
    public static final String SPLIT_MEMO_PREFIX    = "E";
    public static final String SPLIT_AMOUNT_PREFIX  = "$";
    public static final String SPLIT_CATEGORY_PREFIX    = "S";
    public static final String SPLIT_PERCENTAGE_PREFIX  = "%";
    public static final String ACCOUNT_HEADER           = "!Account";
    public static final String ACCOUNT_NAME_PREFIX      = "N";

    public static final String INTERNAL_CURRENCY_PREFIX = "*";

    public static final String ENTRY_TERMINATOR = "^";
    private static final SimpleDateFormat QIF_DATE_FORMATTER = new SimpleDateFormat("yyyy/M/d");

    /**
     * Formats the date for QIF in the form d MMMM YYYY.
     * For example 25 January 2013
     * @param timeMillis Time in milliseconds since epoch
     * @return Formatted date from the time
     */
    public static final String formatDate(long timeMillis){
        Date date = new Date(timeMillis);
        return QIF_DATE_FORMATTER.format(date);
    }

    /**
     * Returns the QIF header for the transaction based on the account type.
     * By default, the QIF cash header is used
     * @param accountType AccountType of account
     * @return QIF header for the transactions
     */
    public static String getQifHeader(AccountType accountType){
        switch (accountType) {
            case CASH:
                return "!Type:Cash";
            case BANK:
                return "!Type:Bank";
            case CREDIT:
                return "!Type:CCard";
            case ASSET:
                return "!Type:Oth A";
            case LIABILITY:
                return "!Type:Oth L";
            default:
                return "!Type:Cash";
        }
    }

    public static String getQifHeader(String accountType) {
        return getQifHeader(AccountType.valueOf(accountType));
    }

    static String sanitizeQifLine(String line) {
        if (line == null) {
            return "";
        }
        return line.replaceAll("\\r?\\n", " ");
    }
}
