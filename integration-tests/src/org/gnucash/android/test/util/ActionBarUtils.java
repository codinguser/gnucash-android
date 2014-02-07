/*
 * Copyright (c) 2013 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.test.util;

import android.view.View;
import com.jayway.android.robotium.solo.Solo;

/**
 * @author Ngewi
 */
public class ActionBarUtils {
    /**
     * Finds a view in the action bar and clicks it, since the native methods are not supported by ActionBarSherlock
     * @param id
     */
    public static void clickSherlockActionBarItem(Solo solo, int id){
        View view = solo.getView(id);
        solo.clickOnView(view);
    }
}
