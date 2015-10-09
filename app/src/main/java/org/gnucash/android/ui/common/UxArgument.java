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
package org.gnucash.android.ui.common;

/**
 * Collection of constants which are passed across multiple pieces of the UI (fragments, activities, dialogs)
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public final class UxArgument {

    /**
     * Key for passing the transaction GUID as parameter in a bundle
     */
    public static final String SELECTED_TRANSACTION_UID  = "selected_transaction_uid";

    /**
	 * Key for passing list of IDs selected transactions as an argument in a bundle or intent
	 */
	public static final String SELECTED_TRANSACTION_IDS = "selected_transactions";

    /**
	 * Key for the origin account as argument when moving accounts
	 */
	public static final String ORIGIN_ACCOUNT_UID = "origin_acccount_uid";

    /**
     * Key for checking whether the passcode is enabled or not
     */
    public static final String ENABLED_PASSCODE         = "enabled_passcode";

    /**
     * Key for disabling the passcode
     */
    public static final String DISABLE_PASSCODE         = "disable_passcode";

    /**
     * Key for storing the passcode
     */
    public static final String PASSCODE                 = "passcode";

    /**
     * Key for skipping the passcode screen. Use this only when there is no other choice.
     */
    public static final String SKIP_PASSCODE_SCREEN     = "skip_passcode_screen";

    /**
     * Amount passed as a string
     */
    public static final String AMOUNT_STRING = "starting_amount";

    /**
     * Class caller, which will be launched after the unlocking
     */
    public static final String PASSCODE_CLASS_CALLER = "passcode_class_caller";

    /**
     * Key for passing the account unique ID as argument to UI
     */
    public static final String SELECTED_ACCOUNT_UID = "account_uid";

    /**
     * Key for passing argument for the parent account GUID.
     */
    public static final String PARENT_ACCOUNT_UID = "parent_account_uid";

    /**
     * Key for passing the scheduled action UID to the transactions editor
     */
    public static final String SCHEDULED_ACTION_UID = "scheduled_action_uid";

    /**
     * Type of form displayed in the {@link FormActivity}
     */
    public static final String FORM_TYPE = "form_type";

    /**
     * List of splits which have been created using the split editor
     */
    public static final String SPLIT_LIST = "split_list";

    /**
     * GUID of a budget
     */
    public static final String BUDGET_UID = "budget_uid";

    /**
     * GUID of splits which have been removed from the split editor
     */
    public static String REMOVED_SPLITS = "removed_split_guids";


    //prevent initialization of instances of this class
    private UxArgument(){
        //prevent even the native class from calling the ctor
        throw new AssertionError();
    }
}
