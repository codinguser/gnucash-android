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
package org.gnucash.android.ui.common;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.uservoice.uservoicesdk.UserVoice;

import org.gnucash.android.R;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.budget.BudgetsActivity;
import org.gnucash.android.ui.passcode.PasscodeLockActivity;
import org.gnucash.android.ui.report.ReportsActivity;
import org.gnucash.android.ui.settings.SettingsActivity;
import org.gnucash.android.ui.transaction.ScheduledActionsActivity;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * Base activity implementing the navigation drawer, to be extended by all activities requiring one.
 * <p>
 *     Each activity inheriting from this class has an indeterminate progress bar at the top,
 *     (above the action bar) which can be used to display busy operations. See {@link #getProgressBar()}
 * </p>
 *
 * <p>Sub-classes should simply provide their layout using {@link #getContentView()} and then annotate
 * any variables they wish to use with {@link ButterKnife#bind(Activity)} annotations. The view
 * binding will be done in this base abstract class.<br>
 * The activity layout of the subclass is expected to contain {@code DrawerLayout} and
 * a {@code NavigationView}.<br>
 * Sub-class should also consider using the {@code toolbar.xml} or {@code toolbar_with_spinner.xml}
 * for the action bar in their XML layout. Otherwise provide another which contains widgets for the
 * toolbar and progress indicator with the IDs {@code R.id.toolbar} and {@code R.id.progress_indicator} respectively.
 * </p>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public abstract class BaseDrawerActivity extends PasscodeLockActivity {
    @Bind(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @Bind(R.id.nav_view) NavigationView mNavigationView;
    @Bind(R.id.toolbar) Toolbar mToolbar;
    @Bind(R.id.toolbar_progress) ProgressBar mToolbarProgress;

    protected ActionBarDrawerToggle mDrawerToggle;


    private class DrawerItemClickListener implements NavigationView.OnNavigationItemSelectedListener {

        @Override
        public boolean onNavigationItemSelected(MenuItem menuItem) {
            onDrawerMenuItemClicked(menuItem.getItemId());
            return true;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());

        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getTitleRes());
        }

        setUpNavigationDrawer();
    }

    /**
     * Return the layout to inflate for this activity
     * @return Layout resource identifier
     */
    public abstract @LayoutRes int getContentView();

    /**
     * Return the title for this activity.
     * This will be displayed in the action bar
     * @return String resource identifier
     */
    public abstract @StringRes int getTitleRes();

    /**
     * Returns the progress bar for the activity.
     * <p>This progress bar is displayed above the toolbar and should be used to show busy status
     * for long operations.<br/>
     * The progress bar visibility is set to {@link View#GONE} by default. Make visible to use </p>
     * @return Indeterminate progress bar.
     */
    public ProgressBar getProgressBar(){
        return mToolbarProgress;
    }

    /**
     * Sets up the navigation drawer for this activity.
     */
    private void setUpNavigationDrawer() {
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
        if (item.getItemId() == android.R.id.home){
            if (!mDrawerLayout.isDrawerOpen(mNavigationView))
                mDrawerLayout.openDrawer(mNavigationView);
            else
                mDrawerLayout.closeDrawer(mNavigationView);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handler for the navigation drawer items
     * */
    protected void onDrawerMenuItemClicked(int itemId) {
        mNavigationView.getMenu().findItem(itemId).setChecked(true);
        switch (itemId){
            case R.id.nav_item_open: { //Open... files
                AccountsActivity.startXmlFileChooser(this);
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

            case R.id.nav_item_reports: {
                Intent intent = new Intent(this, ReportsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
                break;

            case R.id.nav_item_budgets:
                startActivity(new Intent(this, BudgetsActivity.class));
                break;

            case R.id.nav_item_scheduled_actions: { //show scheduled transactions
                Intent intent = new Intent(this, ScheduledActionsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
                break;

            case R.id.nav_item_export:
                AccountsActivity.openExportFragment(this);
                break;

            case R.id.nav_item_settings: //Settings activity
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.nav_item_help:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                prefs.edit().putBoolean(UxArgument.SKIP_PASSCODE_SCREEN, true).apply();
                UserVoice.launchUserVoice(this);
                break;
        }
        mDrawerLayout.closeDrawer(mNavigationView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        switch (requestCode) {
            case AccountsActivity.REQUEST_PICK_ACCOUNTS_FILE:
                AccountsActivity.importXmlFileFromIntent(this, data, null);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

}
