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

package org.gnucash.android.db;

import android.provider.BaseColumns;

/**
 * Holds the
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class DatabaseSchema {
    /**
     * Database version.
     * With any change to the database schema, this number must increase
     */
    static final int DATABASE_VERSION = 7;

    /**
     * Database version where Splits were introduced
     */
    public static final int SPLITS_DB_VERSION = 7;

    //no instances are to be instantiated
    private DatabaseSchema(){}

    public interface CommonColumns extends BaseColumns {
        public static final String COLUMN_UID       = "uid";
    }

    /**
     * Columns for the account tables
     */
    public static abstract class AccountEntry implements CommonColumns {

        public static final String TABLE_NAME                   = "accounts";

        public static final String COLUMN_NAME                  = "name";
        public static final String COLUMN_CURRENCY              = "currency_code";
        public static final String COLUMN_PARENT_ACCOUNT_UID    = "parent_account_uid";
        public static final String COLUMN_PLACEHOLDER           = "is_placeholder";
        public static final String COLUMN_COLOR_CODE            = "color_code";
        public static final String COLUMN_FAVORITE              = "favorite";
        public static final String COLUMN_FULL_NAME             = "full_name";
        public static final String COLUMN_TYPE                  = "type";
        public static final String COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID = "default_transfer_account_uid";

        public static final String INDEX_UID                    = "account_uid_index";
    }

    /**
     * Column schema for the transaction table in the database
     */
    public static abstract class TransactionEntry implements CommonColumns {

        public static final String TABLE_NAME                   = "transactions";

        public static final String COLUMN_NAME                  = "name";
        public static final String COLUMN_DESCRIPTION           = "description";
        public static final String COLUMN_CURRENCY              = "currency_code";
        public static final String COLUMN_TIMESTAMP             = "timestamp";
        public static final String COLUMN_EXPORTED              = "is_exported";
        public static final String COLUMN_RECURRENCE_PERIOD     = "recurrence_period";

        public static final String INDEX_UID                    = "transaction_uid_index";
    }

    /**
     * Column schema for the splits table in the database
     */
    public static abstract class SplitEntry implements CommonColumns {

        public static final String TABLE_NAME                   = "splits";

        public static final String COLUMN_TYPE                  = "type";
        public static final String COLUMN_AMOUNT                = "amount";
        public static final String COLUMN_MEMO                  = "memo";
        public static final String COLUMN_ACCOUNT_UID           = "account_uid";
        public static final String COLUMN_TRANSACTION_UID       = "transaction_uid";

        public static final String INDEX_UID                    = "split_uid_index";
    }
}
