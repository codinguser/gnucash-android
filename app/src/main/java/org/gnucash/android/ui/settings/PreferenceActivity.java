/*
 * Copyright (c) 2015 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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

package org.gnucash.android.ui.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.ui.passcode.PasscodeLockActivity;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Activity for unified preferences
 */
public class PreferenceActivity extends PasscodeLockActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback{

    public static final String ACTION_MANAGE_BOOKS = "org.gnucash.android.intent.action.MANAGE_BOOKS";

    @Bind(R.id.slidingpane_layout) SlidingPaneLayout mSlidingPaneLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ButterKnife.bind(this);

        mSlidingPaneLayout.setPanelSlideListener(new SlidingPaneLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                //nothing to see here, move along
            }

            @Override
            public void onPanelOpened(View panel) {
                ActionBar actionBar = getSupportActionBar();
                assert actionBar != null;
                actionBar.setTitle(R.string.title_settings);
            }

            @Override
            public void onPanelClosed(View panel) {
                //nothing to see here, move along
            }
        });

        String action = getIntent().getAction();
        if (action != null && action.equals(ACTION_MANAGE_BOOKS)){
            loadFragment(new BookListFragment());
            mSlidingPaneLayout.closePane();
        } else {
            mSlidingPaneLayout.openPane();
            loadFragment(new GeneralPreferenceFragment());
        }

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.title_settings);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT) //for one of the exceptions caught
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        String key = pref.getKey();
        Fragment fragment = null;
        try {
            Class<?> clazz = Class.forName(pref.getFragment());
            fragment = (Fragment) clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            //if we do not have a matching class, do nothing
            return false;
        }
        loadFragment(fragment);
        mSlidingPaneLayout.closePane();
        return false;
    }

    /**
     * Load the provided fragment into the right pane, replacing the previous one
     * @param fragment BaseReportFragment instance
     */
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    android.app.FragmentManager fm = getFragmentManager();
                    if (fm.getBackStackEntryCount() > 0) {
                        fm.popBackStack();
                    } else {
                        finish();
                    }
                } else {
                    finish();
                }
                return true;

            default:
                return false;
        }
    }

    /**
     * Returns the shared preferences file for the currently active book.
     * Should be used instead of {@link PreferenceManager#getDefaultSharedPreferences(Context)}
     * @param context Application context
     * @return Shared preferences file
     */
    public static SharedPreferences getActiveBookSharedPreferences(Context context){
        //// TODO: 21.12.2015 optimize the getActiveBookUID retrieval by caching it somewhere
        return context.getSharedPreferences(BooksDbAdapter.getInstance().getActiveBookUID(), Context.MODE_PRIVATE);
    }

    @Override
    public void onBackPressed() {
        if (mSlidingPaneLayout.isOpen())
            super.onBackPressed();
        else
            mSlidingPaneLayout.openPane();
    }
}
