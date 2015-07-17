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
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

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


/**
 * Base activity implementing the navigation drawer, to be extended by all activities requiring one
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class BaseDrawerActivity extends AppCompatActivity {
    protected DrawerLayout  mDrawerLayout;
    protected NavigationView mNavigationView;

    protected CharSequence  mTitle;
    private ActionBarDrawerToggle mDrawerToggle;

    private class DrawerItemClickListener implements NavigationView.OnNavigationItemSelectedListener {

        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
            selectItem(menuItem.getItemId());
            return true;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Sets up the navigation drawer for this activity.
     *
     * This should be called from the activity's
     * {@link Activity#onCreate(Bundle)} method after calling
     * {@link Activity#setContentView(int)}.
     *
     */
    protected void setUpDrawer() {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);

        }
        mDrawerLayout   = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);

        mNavigationView.setNavigationItemSelectedListener(new DrawerItemClickListener());

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
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
        if (!mDrawerLayout.isDrawerOpen(mNavigationView))
            mDrawerLayout.openDrawer(mNavigationView);
        else
            mDrawerLayout.closeDrawer(mNavigationView);

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handler for the navigation drawer items
     * */
    protected void selectItem(int itemId) {
        switch (itemId){
            case R.id.nav_item_open: { //Open... files
                Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
                pickIntent.setType("application/*");
                Intent chooser = Intent.createChooser(pickIntent, getString(R.string.title_select_gnucash_xml_file));

                startActivityForResult(chooser, AccountsActivity.REQUEST_PICK_ACCOUNTS_FILE);
            }
            break;

            case R.id.nav_item_favorites: { //favorite accounts
                Intent intent = new Intent(this, AccountsActivity.class);
                intent.putExtra(AccountsActivity.EXTRA_TAB_INDEX,
                        AccountsActivity.INDEX_FAVORITE_ACCOUNTS_FRAGMENT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
                break;

            case R.id.nav_item_reports:
                startActivity(new Intent(this, ChartReportActivity.class));
                break;

            case R.id.nav_item_scheduled_trn: { //show scheduled transactions
                Intent intent = new Intent(this, ScheduledActionsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(ScheduledActionsActivity.EXTRA_DISPLAY_MODE,
                        ScheduledActionsActivity.DisplayMode.TRANSACTION_ACTIONS);
                startActivity(intent);
            }
                break;

            case R.id.nav_item_export:{
                AccountsActivity.showExportDialog(this);
            }
                break;

            case R.id.nav_item_scheduled_export: //scheduled backup
                Intent intent = new Intent(this, ScheduledActionsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(ScheduledActionsActivity.EXTRA_DISPLAY_MODE,
                        ScheduledActionsActivity.DisplayMode.EXPORT_ACTIONS);
                startActivity(intent);
                break;

            case R.id.nav_item_settings: //Settings activity
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            //TODO: add help option
        }
        mDrawerLayout.closeDrawer(mNavigationView);
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
