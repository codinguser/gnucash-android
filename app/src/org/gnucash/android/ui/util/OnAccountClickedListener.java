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
 * an action on account has been requested
 * This is typically used for Fragment-to-Activity communication
 * 
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public interface OnAccountClickedListener {

	/**
	 * Callback when an account is selected (clicked) from in a list of accounts
	 * @param accountUID GUID of the selected account
	 */
	public void accountSelected(String accountUID);
	
}
