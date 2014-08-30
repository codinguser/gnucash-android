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
package org.gnucash.android.ui;

/**
 * Collection of constants which are passed across multiple pieces of the UI (fragments, activities, dialogs)
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public final class UxArgument {

    /**
     * Arguments key for database ID of transaction.
     * Is used to pass a transaction ID into a bundle or intent
     */
    public static final String SELECTED_TRANSACTION_ID  = "selected_transaction_id";

    /**
     * Key for passing the selected account ID as an argument in a bundle or intent
     */
    public static final String SELECTED_ACCOUNT_ID 		= "selected_account_id";

    /**
	 * Key for passing list of IDs selected transactions as an argument in a bundle or intent
	 */
	public static final String SELECTED_TRANSACTION_IDS = "selected_transactions";

    /**
	 * Key for the origin account as argument when moving accounts
	 */
	public static final String ORIGIN_ACCOUNT_ID        = "origin_acccount_id";

    /**
     * Key for passing argument for the parent account ID.
     */
    public static final String PARENT_ACCOUNT_ID        = "parent_account_id";

    /**
     * Key for checking whether the passcode is enabled or not.
     */
    public static final String ENABLED_PASSCODE         = "enabled_passcode";

    /**
     * Key for storing the passcode.
     */
    public static final String PASSCODE                 = "passcode";

    /**
     * Amount passed as a string
     */
    public static final String AMOUNT_STRING = "starting_amount";

    /**
     * Class caller, which will be launched after the unlocking
     */
    public static final String PASSCODE_CLASS_CALLER = "passcode_class_caller";

    //prevent initialization of instances of this class
    private UxArgument(){
        //prevent even the native class from calling the ctor
        throw new AssertionError();
    }
}
