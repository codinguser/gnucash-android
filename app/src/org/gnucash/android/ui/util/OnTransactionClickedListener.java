/*
 * Copyright (c) 2012 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.util;

/**
 * Interface for implemented by activities which wish to be notified when
 * an action has been requested on a transaction (either creation or edit)
 * This is typically used for Fragment-to-Activity communication
 * 
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public interface OnTransactionClickedListener {

	/**
	 * Callback for creating a new transaction
	 * @param accountRowId Database row ID of the account in which to create the new transaction
	 */
	public void createNewTransaction(long accountRowId);
	
	/**
	 * Callback request to edit a transaction
	 * @param transactionId Database row Id of the transaction to be edited
	 */
	public void editTransaction(long transactionId);	
}
