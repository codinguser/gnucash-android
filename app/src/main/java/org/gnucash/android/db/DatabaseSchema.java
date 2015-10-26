/*
 * Copyright (c) 2014 - 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.db;

import android.provider.BaseColumns;

/**
 * Holds the database schema
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class DatabaseSchema {
    /**
     * Database version.
     * With any change to the database schema, this number must increase
     */
    public static final int DATABASE_VERSION = 9;

    /**
     * Database version where Splits were introduced
     */
    public static final int SPLITS_DB_VERSION = 7;

    //no instances are to be instantiated
    private DatabaseSchema(){}

    public interface CommonColumns extends BaseColumns {
        public static final String COLUMN_UID           = "uid";
        public static final String COLUMN_CREATED_AT    = "created_at";
        public static final String COLUMN_MODIFIED_AT   = "modified_at";
    }

    /**
     * Columns for the account tables
     */
    public static abstract class AccountEntry implements CommonColumns {

        public static final String TABLE_NAME                   = "accounts";

        public static final String COLUMN_NAME                  = "name";
        public static final String COLUMN_CURRENCY              = "currency_code";
        public static final String COLUMN_COMMODITY_UID         = "commodity_uid";
        public static final String COLUMN_DESCRIPTION           = "description";
        public static final String COLUMN_PARENT_ACCOUNT_UID    = "parent_account_uid";
        public static final String COLUMN_PLACEHOLDER           = "is_placeholder";
        public static final String COLUMN_COLOR_CODE            = "color_code";
        public static final String COLUMN_FAVORITE              = "favorite";
        public static final String COLUMN_FULL_NAME             = "full_name";
        public static final String COLUMN_TYPE                  = "type";
        public static final String COLUMN_HIDDEN                = "is_hidden";
        public static final String COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID = "default_transfer_account_uid";

        public static final String INDEX_UID                    = "account_uid_index";
    }

    /**
     * Column schema for the transaction table in the database
     */
    public static abstract class TransactionEntry implements CommonColumns {

        public static final String TABLE_NAME                   = "transactions";
        //The actual names of columns for description and notes are unlike the variable names because of legacy
        //We will not change them now for backwards compatibility reasons. But the variable names make sense
        public static final String COLUMN_DESCRIPTION           = "name";
        public static final String COLUMN_NOTES                 = "description";
        public static final String COLUMN_CURRENCY              = "currency_code";
        public static final String COLUMN_COMMODITY_UID         = "commodity_uid";
        public static final String COLUMN_TIMESTAMP             = "timestamp";

        /**
         * Flag for marking transactions which have been exported
         * @deprecated Transactions are exported based on last modified timestamp
         */
        @Deprecated
        public static final String COLUMN_EXPORTED              = "is_exported";
        public static final String COLUMN_TEMPLATE              = "is_template";
        public static final String COLUMN_SCHEDX_ACTION_UID     = "scheduled_action_uid";

        public static final String INDEX_UID                    = "transaction_uid_index";
    }

    /**
     * Column schema for the splits table in the database
     */
    public static abstract class SplitEntry implements CommonColumns {

        public static final String TABLE_NAME                   = "splits";

        public static final String COLUMN_TYPE                  = "type";

        /**
         * The value columns are in the currency of the transaction containing the split
         */
        public static final String COLUMN_VALUE_NUM             = "value_num";
        public static final String COLUMN_VALUE_DENOM           = "value_denom";
        /**
         * The quantity columns are in the currency of the account to which the split belongs
         */
        public static final String COLUMN_QUANTITY_NUM          = "quantity_num";
        public static final String COLUMN_QUANTITY_DENOM        = "quantity_denom";
        public static final String COLUMN_MEMO                  = "memo";
        public static final String COLUMN_ACCOUNT_UID           = "account_uid";
        public static final String COLUMN_TRANSACTION_UID       = "transaction_uid";

        public static final String INDEX_UID                    = "split_uid_index";
    }

    public static abstract class ScheduledActionEntry implements CommonColumns {
        public static final String TABLE_NAME               = "scheduled_actions";

        public static final String COLUMN_TYPE              = "type";
        public static final String COLUMN_ACTION_UID        = "action_uid";
        public static final String COLUMN_START_TIME        = "start_time";
        public static final String COLUMN_END_TIME          = "end_time";
        public static final String COLUMN_LAST_RUN          = "last_run";
        public static final String COLUMN_PERIOD            = "period";
        public static final String COLUMN_TAG               = "tag"; //for any action-specific information
        public static final String COLUMN_ENABLED           = "is_enabled";
        public static final String COLUMN_TOTAL_FREQUENCY   = "total_frequency";
        public static final String COLUMN_EXECUTION_COUNT   = "execution_count";

        public static final String INDEX_UID            = "scheduled_action_uid_index";
    }

    public static abstract class CommodityEntry implements CommonColumns {
        public static final String TABLE_NAME           = "commodities";

        /**
         * The namespace field denotes the namespace for this commodity,
         * either a currency or symbol from a quote source
         */
        public static final String COLUMN_NAMESPACE     = "namespace";

        /**
         * The fullname is the official full name of the currency
         */
        public static final String COLUMN_FULLNAME      = "fullname";

        /**
         * The mnemonic is the official abbreviated designation for the currency
         */
        public static final String COLUMN_MNEMONIC      = "mnemonic";

        public static final String COLUMN_LOCAL_SYMBOL  = "local_symbol";

        /**
         * The fraction is the number of sub-units that the basic commodity can be divided into
         */
        public static final String COLUMN_FRACTION      = "fraction";

        /**
         * A CUSIP is a nine-character alphanumeric code that identifies a North American financial security
         * for the purposes of facilitating clearing and settlement of trades
         */
        public static final String COLUMN_CUSIP         = "cusip";

        /**
         * TRUE if prices are to be downloaded for this commodity from a quote source
         */
        public static final String COLUMN_QUOTE_FLAG    = "quote_flag";

        public static final String INDEX_UID = "commodities_uid_index";
    }


    public static abstract class PriceEntry implements CommonColumns {
        public static final String TABLE_NAME = "prices";

        public static final String COLUMN_COMMODITY_UID = "commodity_guid";
        public static final String COLUMN_CURRENCY_UID  = "currency_guid";
        public static final String COLUMN_DATE          = "date";
        public static final String COLUMN_SOURCE        = "source";
        public static final String COLUMN_TYPE          = "type";
        public static final String COLUMN_VALUE_NUM     = "value_num";
        public static final String COLUMN_VALUE_DENOM   = "value_denom";

        public static final String INDEX_UID = "prices_uid_index";

    }
}
