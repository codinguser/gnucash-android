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

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.view.MenuItem;

import org.gnucash.android.R;
import org.gnucash.android.ui.passcode.PassLockActivity;

import java.util.MissingFormatArgumentException;

/**
 * Activity for displaying scheduled actions
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ScheduledActionsActivity extends PassLockActivity {

    public enum DisplayMode {ALL_ACTIONS, TRANSACTION_ACTIONS, EXPORT_ACTIONS}

    public static final String EXTRA_DISPLAY_MODE = "org.gnucash.android.extra.DISPLAY_MODE";

    private DisplayMode mDisplayMode = DisplayMode.ALL_ACTIONS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_scheduled_events);
        super.onCreate(savedInstanceState);

        mDisplayMode = (DisplayMode) getIntent().getSerializableExtra(EXTRA_DISPLAY_MODE);
        if (mDisplayMode == null)
            throw new MissingFormatArgumentException("Missing argument for which kind of scheduled events to display");

        switch (mDisplayMode){
            case ALL_ACTIONS:
                showAllScheduledEventsFragment();
                break;

            case TRANSACTION_ACTIONS:
                showScheduledTransactionsFragment();
                break;

            case EXPORT_ACTIONS:

                break;
        }
    }

    private void showAllScheduledEventsFragment(){

    }

    /**
     * Launches the fragment which lists the recurring transactions in the database
     */
    private void showScheduledTransactionsFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();

        ScheduledTransactionsListFragment scheduledTransactionsListFragment = new ScheduledTransactionsListFragment();

        fragmentTransaction.replace(R.id.fragment_container,
                scheduledTransactionsListFragment, "fragment_recurring_transactions");

        fragmentTransaction.commit();
    }
}
