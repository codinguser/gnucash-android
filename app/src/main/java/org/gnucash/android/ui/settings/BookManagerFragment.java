/*
 * Copyright (c) 2016 Ngewi Fet <ngewif@gmail.com>
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

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseSchema.BookEntry;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.util.PreferencesHelper;

/**
 * Fragment for managing the books in the database
 */
public class BookManagerFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, Refreshable{

    private static String LOG_TAG = "BookManagerFragment";

    SimpleCursorAdapter mCursorAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_book_list, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCursorAdapter = new BooksCursorAdapter(getActivity(), R.layout.cardview_book,
                null, new String[]{BookEntry.COLUMN_DISPLAY_NAME, BookEntry.COLUMN_SOURCE_URI},
                new int[]{R.id.primary_text, R.id.secondary_text});

        setListAdapter(mCursorAdapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        assert actionBar != null;
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.title_manage_books);
        setHasOptionsMenu(true);

        getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_list_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_create_book:
                AccountsActivity.createDefaultAccounts(GnuCashApplication.getDefaultCurrencyCode(), getActivity());
                return true;

            default:
                return false;
        }

    }

    @Override
    public void refresh() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void refresh(String uid) {
        refresh();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "Creating loader for books");
        return new BooksCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "Finished loading books from database");
        mCursorAdapter.swapCursor(data);
        mCursorAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(LOG_TAG, "Resetting books list loader");
        mCursorAdapter.swapCursor(null);
    }

    private class BooksCursorAdapter extends SimpleCursorAdapter {

        public BooksCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to, 0);
        }

        @Override
        public void bindView(View view, final Context context, Cursor cursor) {
            super.bindView(view, context, cursor);

            final String bookUID = cursor.getString(cursor.getColumnIndexOrThrow(BookEntry.COLUMN_UID));

            TextView lastSyncText = (TextView) view.findViewById(R.id.last_sync_time);
            lastSyncText.setText(PreferencesHelper.getLastExportTime().toString());

            TextView labelLastSync = (TextView) view.findViewById(R.id.label_last_sync);
            labelLastSync.setText(R.string.label_last_export_time);
            ImageView optionsMenu = (ImageView) view.findViewById(R.id.options_menu);

            optionsMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(context, v);
                    MenuInflater menuInflater = popupMenu.getMenuInflater();
                    menuInflater.inflate(R.menu.book_context_menu, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()){
                                case R.id.ctx_menu_sync_book:
                                    //TODO implement sync
                                    return false;
                                default:
                                    return true;
                            }
                        }
                    });
                    popupMenu.show();
                }
            });

            ImageView deleteBookBtn = (ImageView) view.findViewById(R.id.delete_book);
            String activeBookUID = BooksDbAdapter.getInstance().getActiveBookUID();
            if (activeBookUID.equals(bookUID)) //we cannot delete the active book
                deleteBookBtn.setVisibility(View.GONE);
            else {
                deleteBookBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //// TODO: extract strings
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                        dialogBuilder.setTitle(getString(R.string.title_confirm_delete_book))
                                .setIcon(R.drawable.ic_close_black_24dp)
                                .setMessage(getString(R.string.msg_all_book_data_will_be_deleted));
                        dialogBuilder.setPositiveButton(getString(R.string.btn_delete_book), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BooksDbAdapter.getInstance().deleteRecord(bookUID);
                                mContext.deleteDatabase(bookUID);
                                refresh();
                            }
                        });
                        dialogBuilder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog = dialogBuilder.create();
                        dialog.show(); //must be called before you can access buttons
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setTextColor(getResources().getColor(R.color.account_red));


                    }
                });
            }

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //do nothing if the active book is tapped
                    if (!BooksDbAdapter.getInstance().getActiveBookUID().equals(bookUID)) {
                        GnuCashApplication.loadBook(bookUID);
                    }
                }
            });
        }
    }

    /**
     * {@link DatabaseCursorLoader} for loading the book list from the database
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    private static class BooksCursorLoader extends DatabaseCursorLoader {
        public BooksCursorLoader(Context context){
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            BooksDbAdapter booksDbAdapter = BooksDbAdapter.getInstance();
            Cursor cursor = booksDbAdapter.fetchAllRecords();

            registerContentObserver(cursor);
            return cursor;
        }
    }
}
