package org.gnucash.android.ui.util.widget.searchablespinner;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;

import java.io.Serializable;
import java.util.List;

public class SearchableListDialogFragment
        extends DialogFragment
        implements SearchView.OnQueryTextListener,
                   SearchView.OnCloseListener {

    /**
     * Listener to call when user clicks on an item
     *
     * @param <T>
     */
    public interface OnSearchableItemClickedListener<T>
            extends Serializable {

        void onSearchableItemClicked(T item,
                                     int position);
    }

    /**
     * Listener to call when Search text change
     */
    public interface OnSearchTextChangedListener {
        void onSearchTextChanged(String strText);
    }


    private static final String ITEMS = "items";

    // Dialog Title
    private String _strTitle;

    // Parent View
    private AdapterView _parentAdapterView;

    // Search Edit text zone
    private SearchView _searchTextEditView;

    // Item list
    private ListView _listView;

    // Bottom right button to close the pop-up
    private String _strPositiveButtonText;

    // Adpater for Spinner based on data in a DB Cursor
//    private CursorAdapter _cursorAdapter;
//    private ArrayAdapter _withContainingTextArrayFilterArrayAdapter;

    private OnSearchTextChangedListener _onSearchTextChangedListener;

    private OnSearchableItemClickedListener _onSearchableItemClickedListener;

    private DialogInterface.OnClickListener _onPositiveBtnClickListener;


    /**
     * Constructor
     */
    public SearchableListDialogFragment() {

    }

    /**
     * Factory
     *
     * @param items
     *
     * @return
     */
    public static SearchableListDialogFragment makeInstance(AdapterView parentAdapterView, List items) {

        SearchableListDialogFragment searchableListDialogFragment = new SearchableListDialogFragment();

        searchableListDialogFragment.setParentAdapterView(parentAdapterView);

        Bundle args = new Bundle();

        args.putSerializable(ITEMS,
                             (Serializable) items);

        searchableListDialogFragment.setArguments(args);

        return searchableListDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        getDialog().getWindow()
                   .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        return super.onCreateView(inflater,
                                  container,
                                  savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Getting the layout inflater to inflate the view in an alert dialog.
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        // Crash on orientation change #7
        // Change Start
        // Description: As the instance was re initializing to null on rotating the device,
        // getting the instance from the saved instance
        if (null != savedInstanceState) {
            _onSearchableItemClickedListener = (OnSearchableItemClickedListener) savedInstanceState.getSerializable("item");
        }
        // Change End

        //
        // Prepare the searchableListView
        //

        // Instantiate the searchableListView from XML
        View searchableListRootView = inflater.inflate(R.layout.searchable_list_dialog,
                                                       null);

        // Configure the searchableListView
        configureView(searchableListRootView);

        //
        // Create dialog builder
        //

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        // Indicate to put the searchableListView in the alertDialog
        alertDialogBuilder.setView(searchableListRootView);

        // Title

        String strTitle = _strTitle == null
                          ? "Select Item"
                          : _strTitle;

        alertDialogBuilder.setTitle(strTitle);

        // Positive Button

        String strPositiveButton = _strPositiveButtonText == null
                                   ? "CLOSE"
                                   : _strPositiveButtonText;

        alertDialogBuilder.setPositiveButton(strPositiveButton,
                                             _onPositiveBtnClickListener);

        //
        // Create searchableListDialog
        //

        final AlertDialog searchableListDialog = alertDialogBuilder.create();

        // Hide Soft Keybord
//        hideKeyboard(_searchTextEditView);
        hideKeyboard(searchableListRootView);

        return searchableListDialog;
    }

    private void configureView(View searchableListRootView) {

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        //
        // Search Edit text zone
        //

        _searchTextEditView = (SearchView) searchableListRootView.findViewById(R.id.search);

        _searchTextEditView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        _searchTextEditView.setIconifiedByDefault(false);
        _searchTextEditView.setOnQueryTextListener(this);
        _searchTextEditView.setOnCloseListener(this);
        _searchTextEditView.clearFocus();

        //
        // Items list
        //

        _listView = (ListView) searchableListRootView.findViewById(R.id.listItems);

        List items = (List) getArguments().getSerializable(ITEMS);

//        // Create an ArrayAdapter for items, with filtering capablity based on item containing a text
//        _withContainingTextArrayFilterArrayAdapter = new WithContainingTextArrayFilterArrayAdapter(getActivity(),
//                                                                                                   android.R.layout.simple_list_item_1,
//                                                                                                   items);
        // Attach the adapter to the list
        _listView.setAdapter((ListAdapter) getParentAdapterView().getAdapter());

        _listView.setTextFilterEnabled(true);

        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent,
                                    View view,
                                    int position,
                                    long id) {

//                final Object accountFullName = _withContainingTextArrayFilterArrayAdapter.getItem(position);
                final CursorAdapter cursorAdapter   = (CursorAdapter) getParentAdapterView().getAdapter();
                final Cursor        cursor          = (Cursor) cursorAdapter.getItem(position);
                final String        accountFullName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_FULL_NAME));

                // Call Listener
                _onSearchableItemClickedListener.onSearchableItemClicked(accountFullName,
                                                                         position);
                getDialog().dismiss();
            }
        });
    }

    // Crash on orientation change #7
    // Change Start
    // Description: Saving the instance of searchable item instance.
    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putSerializable("item",
                                 _onSearchableItemClickedListener);
        super.onSaveInstanceState(outState);
    }
    // Change End

    public void setTitle(String strTitle) {

        _strTitle = strTitle;
    }

    public void setPositiveButton(String strPositiveButtonText) {

        _strPositiveButtonText = strPositiveButtonText;
    }

    public void setPositiveButton(String strPositiveButtonText,
                                  DialogInterface.OnClickListener onClickListener) {

        _strPositiveButtonText = strPositiveButtonText;
        _onPositiveBtnClickListener = onClickListener;
    }

    public void setOnSearchableItemClickListener(OnSearchableItemClickedListener onSearchableItemClickedListener) {

        this._onSearchableItemClickedListener = onSearchableItemClickedListener;
    }

    public void setOnSearchTextChangedListener(OnSearchTextChangedListener onSearchTextChangedListener) {

        this._onSearchTextChangedListener = onSearchTextChangedListener;
    }


    @Override
    public boolean onClose() {

        return false;
    }

    @Override
    public void onPause() {

        super.onPause();
        dismiss();
    }

    @Override
    public boolean onQueryTextSubmit(String s) {

        _searchTextEditView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {

        //
        // Filter item list
        //

        final WithContainingTextArrayFilterArrayAdapter listViewAdapter = (WithContainingTextArrayFilterArrayAdapter) _listView.getAdapter();

        if (TextUtils.isEmpty(s)) {

            listViewAdapter.getFilter()
                           .filter(null);

        } else {

            listViewAdapter.getFilter()
                           .filter(s);
        }

        //
        // Call Search Text Change Listener
        //

        if (null != _onSearchTextChangedListener) {

            // Call Listener
            _onSearchTextChangedListener.onSearchTextChanged(s);
        }

        return true;
    }

    private void hideKeyboard(final View ettext) {

        ettext.requestFocus();

        // Delay the keyboard hiding
        ettext.postDelayed(new Runnable() {
                               @Override
                               public void run() {

                                   //
                                   // Hide keyboard
                                   //

                                   InputMethodManager keyboard = (InputMethodManager) ettext.getContext()
                                                                                            .getSystemService(Context.INPUT_METHOD_SERVICE);

                                   keyboard.hideSoftInputFromWindow(ettext.getWindowToken(),
                                                                    0);
                               }
                           },
                           200);
    }

    public AdapterView getParentAdapterView() {

        return _parentAdapterView;
    }

    public void setParentAdapterView(AdapterView parentAdapterView) {

        _parentAdapterView = parentAdapterView;
    }
}
