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
    public static final int DATABASE_VERSION = 15;

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
        public static final String COLUMN_GUID = "guid";
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
        public static final String COLUMN_CURRENCY_CODE = "code";
        public static final String COLUMN_COMMODITY_GUID        = "commodity_guid";
        public static final String COLUMN_COMMODITY_SCU         = "commodity_scu";
        public static final String COLUMN_NON_STD_SCU           = "non_std_scu";
        public static final String COLUMN_DESCRIPTION           = "description";
        public static final String COLUMN_PARENT_GUID = "parent_guid";
        public static final String COLUMN_ACCOUNT_TYPE = "account_type";

        public static final String INDEX_UID                    = "account_uid_index";

    }

    /**
     * Column schema for the transaction table in the database
     */
    public static abstract class TransactionEntry implements CommonColumns {

        public static final String TABLE_NAME                   = "transactions";
        //The actual names of columns for description and notes are unlike the variable names because of legacy
        //We will not change them now for backwards compatibility reasons. But the variable names make sense
        //public static final String COLUMN_DESCRIPTION           = "name";
        public static final String COLUMN_DESCRIPTION           = "description";
        public static final String COLUMN_NUM                   = "num";
        public static final String COLUMN_COMMODITY_GUID        = "currency_guid";
        public static final String COLUMN_POST_DATE             = "post_date";
        public static final String COLUMN_ENTER_DATE            = "enter_date";


        public static final String INDEX_UID                    = "transaction_uid_index";
    }

    /**
     * Column schema for the splits table in the database
     */
    public static abstract class SplitEntry implements CommonColumns {

        public static final String TABLE_NAME                   = "splits";

        public static final String COLUMN_ACTION                = "action";
        public static final String COLUMN_MEMO                  = "memo";

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

        public static final String COLUMN_ACCOUNT_GUID          = "account_guid";
        public static final String COLUMN_TRANSACTION_GUID      = "tx_guid";

        public static final String COLUMN_RECONCILE_STATE       = "reconcile_state";
        public static final String COLUMN_RECONCILE_DATE        = "reconcile_date";
        public static final String COLUMN_LOT_GUID              = "lot_guid";

        public static final String INDEX_UID                    = "split_uid_index";
    }


    public static abstract class LotEntry implements CommonColumns {

        public static final String TABLE_NAME = "lots";

        public static final String COLUMN_ACCOUNT_GUID  = "account_guid";
        public static final String COLUMN_IS_CLOSED     = "is_closed";
    }

    public static abstract class ScheduledTransactionEntry implements CommonColumns {
        public static final String TABLE_NAME                   = "schedxactions";

        public static final String COLUMN_NAME                  = "name";
        public static final String COLUMN_ENABLED               = "enabled";
        public static final String COLUMN_START_DATE            = "start_date";
        public static final String COLUMN_END_DATE              = "end_date";
        public static final String COLUMN_LAST_OCCURRENCE       = "last_occur";
        public static final String COLUMN_NUM_OCCURRENCES       = "num_occur";
        public static final String COLUMN_REMAINING_OCCURRENCES = "num_occur";
        public static final String COLUMN_AUTO_CREATE           = "auto_create";
        public static final String COLUMN_AUTO_NOTIFY           = "auto_notify";
        public static final String COLUMN_ADVANCE_CREATION      = "adv_creation";
        public static final String COLUMN_ADVANCE_NOTIFY        = "adv_notify";
        public static final String COLUMN_INSTANCE_COUNT        = "instance_count";

        public static final String COLUMN_TEMPLATE_ACCT_UID     = "template_act_uid";

        public static final String INDEX_UID                = "scheduled_transaction_uid_index";
    }

    public static abstract class ScheduledExportEntry implements CommonColumns {
        public static final String TABLE_NAME               = "scheduled_actions";

        public static final String COLUMN_START_TIME        = "start_time";
        public static final String COLUMN_END_TIME          = "end_time";
        public static final String COLUMN_LAST_RUN_TIME = "last_run";
        public static final String COLUMN_RECURRENCE_RULE   = "rrule";

        /**
         * Tag for scheduledAction-specific information e.g. backup parameters for backup
         */
        public static final String COLUMN_EXPORT_PARAMS     = "export_params";
        public static final String COLUMN_ENABLED           = "enabled";

        /**
         * Number of times this scheduledAction has been run.
         * Analogous to instance_count in GnuCash desktop SQL
         */
        public static final String COLUMN_EXECUTION_COUNT   = "execution_count";
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
        public static final String COLUMN_QUOTE_SOURCE    = "quote_source";
        public static final String COLUMN_QUOTE_TZ    = "quote_tz";

        public static final String INDEX_UID = "commodities_uid_index";
    }


    public static abstract class PriceEntry implements CommonColumns {
        public static final String TABLE_NAME = "prices";

        public static final String COLUMN_COMMODITY_GUID = "commodity_guid";
        public static final String COLUMN_CURRENCY_GUID = "currency_guid";
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

        public static final String INDEX_UID = "budgets_uid_index";
    }


    public static abstract class BudgetAmountEntry implements CommonColumns {
        public static final String TABLE_NAME           = "budget_amounts";

        public static final String COLUMN_BUDGET_GUID = "budget_guid";
        public static final String COLUMN_ACCOUNT_GUID = "account_guid";
        public static final String COLUMN_PERIOD_NUM    = "period_num";
        public static final String COLUMN_AMOUNT_NUM    = "amount_num";
        public static final String COLUMN_AMOUNT_DENOM  = "amount_denom";

        public static final String INDEX_UID            = "budget_amounts_uid_index";
    }


    public static abstract class RecurrenceEntry implements CommonColumns {
        public static final String TABLE_NAME           = "recurrences";

        public static final String COLUMN_OBJECT_GUID   = "obj_guid";
        public static final String COLUMN_MULTIPLIER    = "recurrence_mult";
        public static final String COLUMN_PERIOD_TYPE   = "recurrence_period_type";
        public static final String COLUMN_PERIOD_START  = "recurrence_period_start";
//        public static final String COLUMN_PERIOD_END    = "recurrence_period_end";
//        public static final String COLUMN_BYDAY         = "recurrence_byday";

        public static final String INDEX_UID = "recurrence_uid_index";
    }

    public static abstract class SlotEntry implements CommonColumns {

        public static class Account {
            public static final String COLUMN_PLACEHOLDER           = "is_placeholder";
            public static final String COLUMN_COLOR_CODE            = "color_code";
            public static final String COLUMN_FAVORITE              = "favorite";
            public static final String COLUMN_FULL_NAME             = "full_name";
            public static final String COLUMN_HIDDEN                = "is_hidden";
            public static final String COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID = "default_transfer_account_uid";

        }

        public static class Transaction {

            /**
             * Flag for marking transactions which have been exported
             * @deprecated Transactions are exported based on last modified timestamp
             */
            @Deprecated
            public static final String COLUMN_EXPORTED              = "is_exported";
            public static final String COLUMN_TEMPLATE              = "is_template";
            public static final String COLUMN_SCHEDX_ACTION_UID     = "scheduled_action_uid";
        }
    }
}


/*
 TODO: Migration plan
 General:
    - There are no _ids anymore (if we absolutely need them, we can alias _ROWID_ and keep going.
    - Long term, migrate to ViewModel for loading data from the database.

 Accounts:
    - Lots of attributes moved to slots table. Join them and create view when accessing accounts

 Transactions:
    - There is no "name" entry in the transactions table. Only description. Previously we had both.
    - Migration name and description. Concatenate them if necessary

Splits
    - split "type" is now known as "action"
    - splits should use the sign of the amount to denote if CREDIT or DEBIT

Lots
    - Create new table. Do nothing with it for now

SchedXactions:
    - New table for scheduled transactions matching GnuCash SQL
    - New table for backup schedules
    - Migrate by going through all entries in existing table and converting them to new entries in
      the respective new tables.

Commodities:
    - Added some new options columns for quote-src and quote-tz
    - Local symbol column might not exist in desktop database

Budgets:
    - Recurrences period_end removed. Handle that differently
    - Rename of GUID columns from uid to guid
    - Removed recurrence UID. Now saved in slots table
 **/