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
     * Name of database storing information about the books in the application
     */
    public static final String BOOK_DATABASE_NAME = "gnucash_books.db";

    /**
     * Version number of database containing information about the books in the application
     */
    public static final int BOOK_DATABASE_VERSION = 1;

    /**
     * Version number of database containing accounts and transactions info.
     * With any change to the database schema, this number must increase
     */
    public static final int DATABASE_VERSION = 13;

    /**
     * Name of the database
     * <p>This was used when the application had only one database per instance.
     * Now there can be multiple databases for each book imported
     * </p>
     * @deprecated Each database uses the GUID of the root account as name
     */
    @Deprecated
    public static final String LEGACY_DATABASE_NAME = "gnucash_db";

    //no instances are to be instantiated
    private DatabaseSchema(){}

    public interface CommonColumns extends BaseColumns {
        public static final String COLUMN_UID           = "uid";
        public static final String COLUMN_CREATED_AT    = "created_at";
        public static final String COLUMN_MODIFIED_AT   = "modified_at";
    }

    public static abstract class BookEntry implements CommonColumns {
        public static final String TABLE_NAME = "books";

        public static final String COLUMN_DISPLAY_NAME  = "name";
        public static final String COLUMN_SOURCE_URI    = "uri";
        public static final String COLUMN_ROOT_GUID     = "root_account_guid";
        public static final String COLUMN_TEMPLATE_GUID = "root_template_guid";
        public static final String COLUMN_ACTIVE        = "is_active";
        public static final String COLUMN_LAST_SYNC     = "last_export_time";
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

        public static final String COLUMN_RECONCILE_STATE       = "reconcile_state";
        public static final String COLUMN_RECONCILE_DATE        = "reconcile_date";

        public static final String INDEX_UID                    = "split_uid_index";
    }

    public static abstract class ScheduledActionEntry implements CommonColumns {
        public static final String TABLE_NAME               = "scheduled_actions";

        public static final String COLUMN_TYPE              = "type";
        public static final String COLUMN_ACTION_UID        = "action_uid";
        public static final String COLUMN_START_TIME        = "start_time";
        public static final String COLUMN_END_TIME          = "end_time";
        public static final String COLUMN_LAST_RUN          = "last_run";

        /**
         * Tag for scheduledAction-specific information e.g. backup parameters for backup
         */
        public static final String COLUMN_TAG               = "tag";
        public static final String COLUMN_ENABLED           = "is_enabled";
        public static final String COLUMN_TOTAL_FREQUENCY   = "total_frequency";

        /**
         * Number of times this scheduledAction has been run. Analogous to instance_count in GnuCash desktop SQL
         */
        public static final String COLUMN_EXECUTION_COUNT   = "execution_count";

        public static final String COLUMN_RECURRENCE_UID    = "recurrence_uid";
        public static final String COLUMN_AUTO_CREATE       = "auto_create";
        public static final String COLUMN_AUTO_NOTIFY       = "auto_notify";
        public static final String COLUMN_ADVANCE_CREATION  = "adv_creation";
        public static final String COLUMN_ADVANCE_NOTIFY    = "adv_notify";
        public static final String COLUMN_TEMPLATE_ACCT_UID = "template_act_uid";


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
        public static final String COLUMN_SMALLEST_FRACTION = "fraction";

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


    public static abstract class BudgetEntry implements CommonColumns {
        public static final String TABLE_NAME           = "budgets";

        public static final String COLUMN_NAME          = "name";
        public static final String COLUMN_DESCRIPTION   = "description";
        public static final String COLUMN_NUM_PERIODS   = "num_periods";
        public static final String COLUMN_RECURRENCE_UID = "recurrence_uid";

        public static final String INDEX_UID = "budgets_uid_index";
    }


    public static abstract class BudgetAmountEntry implements CommonColumns {
        public static final String TABLE_NAME           = "budget_amounts";

        public static final String COLUMN_BUDGET_UID    = "budget_uid";
        public static final String COLUMN_ACCOUNT_UID   = "account_uid";
        public static final String COLUMN_PERIOD_NUM    = "period_num";
        public static final String COLUMN_AMOUNT_NUM    = "amount_num";
        public static final String COLUMN_AMOUNT_DENOM  = "amount_denom";

        public static final String INDEX_UID            = "budget_amounts_uid_index";
    }


    public static abstract class RecurrenceEntry implements CommonColumns {
        public static final String TABLE_NAME           = "recurrences";

        public static final String COLUMN_MULTIPLIER    = "recurrence_mult";
        public static final String COLUMN_PERIOD_TYPE   = "recurrence_period_type";
        public static final String COLUMN_PERIOD_START  = "recurrence_period_start";
        public static final String COLUMN_PERIOD_END    = "recurrence_period_end";
        public static final String COLUMN_BYDAY         = "recurrence_byday";

        public static final String INDEX_UID = "recurrence_uid_index";
    }
}
