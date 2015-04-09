/*
 * Copyright (c) 2013 - 2014 Ngewi Fet <ngewif@gmail.com>
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
 * Interface for fragments which are refreshable
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public interface Refreshable {

    /**
     * Refresh the list, typically be restarting the loader
     */
    public void refresh();

    /**
     * Refresh the list with modified parameters
     * @param uid GUID of relevant item to be refreshed
     */
    public void refresh(String uid);
}
