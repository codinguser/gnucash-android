/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.transaction;

import org.gnucash.android.model.Money;

/**
 * Interface to be implemented by classes which start the transfer funds fragment
 */
public interface OnTransferFundsListener {

    /**
     * Method called after the funds have been converted to the desired currency
     * @param amount Funds in new currency
     */
    void transferComplete(Money amount);
}
