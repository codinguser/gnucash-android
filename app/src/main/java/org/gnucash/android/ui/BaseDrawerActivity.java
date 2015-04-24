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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.commonsware.cwac.merge.MergeAdapter;
import com.crashlytics.android.Crashlytics;

import org.gnucash.android.R;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.importer.ImportAsyncTask;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.chart.ChartReportActivity;
import org.gnucash.android.ui.settings.SettingsActivity;
import org.gnucash.android.ui.transaction.ScheduledActionsActivity;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;


/**
 * Base activity implementing the navigation drawer, to be extended by all activities requiring one
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class BaseDrawerActivity extends SherlockFragmentActivity {
    protected DrawerLayout  mDrawerLayout;
    protected ListView      mDrawerList;

    protected CharSequence  mTitle;
    private ActionBarDrawerToggle mDrawerToggle;

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

        MergeAdapter mergeAdapter = createNavDrawerMergeAdapter();

        mDrawerList.setAdapter(mergeAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        //FIXME: Migrate to the non-deprecated version when we remove ActionBarSherlock and support only API level 15 and above
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle("GnuCash");
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private MergeAdapter createNavDrawerMergeAdapter() {
        //TODO: Localize nav drawer entries when features are finalized
        ArrayList<String> accountNavOptions = new ArrayList<>();
        accountNavOptions.add(getString(R.string.nav_menu_open));
        accountNavOptions.add(getString(R.string.nav_menu_favorites));
        accountNavOptions.add(getString(R.string.nav_menu_reports));

        ArrayAdapter<String> accountsNavAdapter = new ArrayAdapter<>(this,
                R.layout.drawer_list_item, accountNavOptions);

        int titleColorGreen = getResources().getColor(R.color.title_green);

        ArrayList<String> transactionsNavOptions = new ArrayList<>();
        transactionsNavOptions.add(getString(R.string.nav_menu_scheduled_transactions));
        transactionsNavOptions.add(getString(R.string.nav_menu_export));

        ArrayAdapter<String> transactionsNavAdapter = new ArrayAdapter<>(this,
                R.layout.drawer_list_item, transactionsNavOptions);

        LayoutInflater inflater = getLayoutInflater();
        TextView accountHeader = (TextView) inflater.inflate(R.layout.drawer_section_header, null);
        accountHeader.setText(R.string.title_accounts);
        accountHeader.setTextColor(titleColorGreen);

        TextView transactionHeader = (TextView) inflater.inflate(R.layout.drawer_section_header, null);
        transactionHeader.setText(R.string.title_transactions);
        transactionHeader.setTextColor(titleColorGreen);
        MergeAdapter mergeAdapter = new MergeAdapter();
        mergeAdapter.addView(accountHeader);
        mergeAdapter.addAdapter(accountsNavAdapter);
        mergeAdapter.addView(transactionHeader);
        mergeAdapter.addAdapter(transactionsNavAdapter);

        mergeAdapter.addView(inflater.inflate(R.layout.horizontal_line, null));
        TextView settingsHeader = (TextView) inflater.inflate(R.layout.drawer_section_header, null);
        settingsHeader.setText(R.string.title_settings);
        settingsHeader.setTextColor(titleColorGreen);

        ArrayList<String> aboutNavOptions = new ArrayList<>();
//        aboutNavOptions.add("Backup & Export");
        aboutNavOptions.add(getString(R.string.nav_menu_settings));
        //TODO: add help view
        ArrayAdapter<String> aboutNavAdapter = new ArrayAdapter<>(this,
                R.layout.drawer_list_item, aboutNavOptions);

        mergeAdapter.addView(settingsHeader);
        mergeAdapter.addAdapter(aboutNavAdapter);
        return mergeAdapter;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mDrawerLayout.isDrawerOpen(mDrawerList))
            mDrawerLayout.openDrawer(mDrawerList);
        else
            mDrawerLayout.closeDrawer(mDrawerList);

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handler for the navigation drawer items
     * */
    protected void selectItem(int position) {
        switch (position){
            case 1: { //Open... files
                Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
                pickIntent.setType("application/*");
                Intent chooser = Intent.createChooser(pickIntent, getString(R.string.title_select_gnucash_xml_file));

                startActivityForResult(chooser, AccountsActivity.REQUEST_PICK_ACCOUNTS_FILE);
            }
            break;

            case 2: { //favorite accounts
                Intent intent = new Intent(this, AccountsActivity.class);
                intent.putExtra(AccountsActivity.EXTRA_TAB_INDEX,
                        AccountsActivity.INDEX_FAVORITE_ACCOUNTS_FRAGMENT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
                break;

            case 3:
                startActivity(new Intent(this, ChartReportActivity.class));
                break;

            case 5: { //show scheduled transactions
                Intent intent = new Intent(this, ScheduledActionsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(ScheduledActionsActivity.EXTRA_DISPLAY_MODE,
                        ScheduledActionsActivity.DisplayMode.TRANSACTION_ACTIONS);
                startActivity(intent);
            }
                break;

            case 6:{
                AccountsActivity.showExportDialog(this);
            }
                break;

            case 9: //Settings activity
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            //TODO: add help option
        }

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
//        setTitle(mNavDrawerEntries[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED){
            return;
        }

        switch (requestCode) {
            case AccountsActivity.REQUEST_PICK_ACCOUNTS_FILE:
                try {
                    GncXmlExporter.createBackup();
                    InputStream accountInputStream = getContentResolver().openInputStream(data.getData());
                    new ImportAsyncTask(this).execute(accountInputStream);
                } catch (FileNotFoundException e) {
                    Crashlytics.logException(e);
                    Toast.makeText(this, R.string.toast_error_importing_accounts, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
