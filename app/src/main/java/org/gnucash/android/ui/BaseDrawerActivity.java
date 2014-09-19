package org.gnucash.android.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.gnucash.android.R;
import org.gnucash.android.ui.transaction.ScheduledTransactionsListFragment;


/**
 * Date: 25.01.14
 *
 * @author Ngewi
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

        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mNavDrawerEntries));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    /** Swaps fragments in the main content view */
    protected void selectItem(int position) {
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
        getActionBar().setTitle(mTitle);
    }

}
