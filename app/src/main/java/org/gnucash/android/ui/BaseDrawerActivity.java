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
package org.gnucash.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.commonsware.cwac.merge.MergeAdapter;

import org.gnucash.android.R;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.transaction.ScheduledTransactionsListFragment;
import org.w3c.dom.Text;

import java.util.ArrayList;


/**
 * Base activity implementing the navigation drawer, to be extended by all activities requiring one
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class BaseDrawerActivity extends SherlockFragmentActivity {
    protected DrawerLayout  mDrawerLayout;
    protected ListView      mDrawerList;
    protected String[]      mNavDrawerEntries;

    protected CharSequence  mTitle;

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDrawerLayout   = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList     = (ListView) findViewById(R.id.left_drawer);
        mNavDrawerEntries = getResources().getStringArray(R.array.nav_drawer_entries);

//        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
//                R.layout.drawer_list_item, mNavDrawerEntries));
//        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        ArrayList<String> accountNavOptions = new ArrayList<String>();
        accountNavOptions.add("Accounts");
        accountNavOptions.add("Favorites");
        accountNavOptions.add("Open...");

        ArrayAdapter<String> accountsNavAdapter = new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, accountNavOptions);


        ArrayList<String> transactionsNavOptions = new ArrayList<String>();
        transactionsNavOptions.add("Transactions");
        transactionsNavOptions.add("Scheduled Transactions");
        transactionsNavOptions.add("Export...");

        ArrayAdapter<String> transactionsNavAdapter = new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, transactionsNavOptions);

        LayoutInflater inflater = getLayoutInflater();
        TextView accountHeader = (TextView) inflater.inflate(R.layout.drawer_section_header, null);
        accountHeader.setText("Accounts");

        TextView transactionHeader = (TextView) inflater.inflate(R.layout.drawer_section_header, null);
        transactionHeader.setText("Transactions");
        MergeAdapter mergeAdapter = new MergeAdapter();
        mergeAdapter.addView(accountHeader);
        mergeAdapter.addAdapter(accountsNavAdapter);
        mergeAdapter.addView(transactionHeader);
        mergeAdapter.addAdapter(transactionsNavAdapter);

        mergeAdapter.addView(inflater.inflate(R.layout.horizontal_line, null));
        TextView settingsHeader = (TextView) inflater.inflate(R.layout.drawer_section_header, null);
        settingsHeader.setText("Settings");
        TextView settingsTextView = (TextView) inflater.inflate(R.layout.drawer_list_item, null);
        settingsTextView.setText("Settings");

        mergeAdapter.addView(settingsHeader);
        mergeAdapter.addView(settingsTextView);

        mDrawerList.setAdapter(mergeAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        getSupportActionBar().setHomeButtonEnabled(true);
    }

    /** Swaps fragments in the main content view */
    protected void selectItem(int position) {
        switch (position){
            case 0: {
                Intent intent = new Intent(this, AccountsActivity.class);
                intent.putExtra(AccountsActivity.EXTRA_TAB_INDEX,
                        AccountsActivity.INDEX_TOP_LEVEL_ACCOUNTS_FRAGMENT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
                break;

            case 1: {
                Intent intent = new Intent(this, AccountsActivity.class);
                intent.putExtra(AccountsActivity.EXTRA_TAB_INDEX,
                        AccountsActivity.INDEX_FAVORITE_ACCOUNTS_FRAGMENT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
                break;

            case 2:{
                AccountsActivity.showExportDialog(this);
            }
                break;

        }

        // Create a new fragment and specify the planet to show based on position
        Fragment fragment = new ScheduledTransactionsListFragment();
        Bundle args = new Bundle();
        args.putInt("account_list_type", position);
        fragment.setArguments(args);


        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mNavDrawerEntries[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

}
