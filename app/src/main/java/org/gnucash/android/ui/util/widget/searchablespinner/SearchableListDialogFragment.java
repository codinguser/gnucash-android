package org.gnucash.android.ui.util.widget.searchablespinner;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.SpinnerAdapter;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.util.KeyboardUtils;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pop-up that display a ListView with a search text field
 */
public class SearchableListDialogFragment
        extends DialogFragment
        implements SearchView.OnQueryTextListener,
                   SearchView.OnCloseListener,
                   Filter.FilterListener {

    /**
     * Logging tag
     */
    protected static final String LOG_TAG = "SearchLstDlgFragment";

    public static final String KEY_ACCOUNT_UID              = "key_accountUID";
    public static final String KEY_ACCOUNT_SIMPLE_NAME      = "key_accountName";
    public static final String KEY_ACCOUNT_FULL_NAME        = "key_accountFullName";
    public static final String KEY_PARENT_ACCOUNT_FULL_NAME = "key_parentAccountFullName";

    /**
     * Listener to call when user clicks on an item
     *
     * @param <T>
     *      item Type
     */
    public interface OnSearchableItemClickedListener<T>
            extends Serializable {

        void onSearchableItemClicked(T item);
    }

    /**
     * Listener to call when Search text change
     */
    public interface OnSearchTextChangedListener {
        void onSearchTextChanged(String strText);
    }


//    private static final String ITEMS = "items";

    // Dialog Title
    private String _strTitle;

    // Search Edit text zone
    private SearchView _searchTextEditView;

    // Item list
    ListView _listView;

    // Bottom right button to close the pop-up
    private String _strPositiveButtonText;

    private List<?> _allItems;

    private BaseAdapter mBaseAdapter;

    private OnSearchTextChangedListener _onSearchTextChangedListener;

    private OnSearchableItemClickedListener<String> _onSearchableItemClickedListener;

    private DialogInterface.OnClickListener _onPositiveBtnClickListener;

    private DialogInterface.OnCancelListener _onCancelListener;

    // Parent SpinnerView
    private SearchableSpinnerView _parentSearchableSpinnerView;

    private boolean mIsDismissing;



    /**
     * Constructor
     */
    public SearchableListDialogFragment() {

        setAllItems(new ArrayList());
    }

    /**
     * Factory
     *
     * @return
     */
    public static SearchableListDialogFragment makeInstance(SearchableSpinnerView parentSearchableSpinnerView) {

        SearchableListDialogFragment searchableListDialogFragment = new SearchableListDialogFragment();

        // Store a link to the Parent SearchableSpinnerView which holds the CursorAdapter
        searchableListDialogFragment.setParentSearchableSpinnerView(parentSearchableSpinnerView);

        return searchableListDialogFragment;
    }

    //
    // Event handlers
    //

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

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
            _onSearchableItemClickedListener = (OnSearchableItemClickedListener<String>) savedInstanceState.getSerializable("item");
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
                                             new DialogInterface.OnClickListener() {

                                                 @Override
                                                 public void onClick(final DialogInterface dialog,
                                                                     final int which) {

                                                     // Dismiss dialog
                                                     dismissDialog();

                                                     if (_onPositiveBtnClickListener != null) {
                                                         //

                                                         // Call listener
                                                         _onPositiveBtnClickListener.onClick(dialog,
                                                                                             which);

                                                     } else {
                                                         //  n' pas

                                                         // RAF
                                                     }
                                                 }
                                             });

        //
        // Create searchableListDialog
        //

        final AlertDialog searchableListDialog = alertDialogBuilder.create();

        return searchableListDialog;
    }

    @Override
    public boolean onQueryTextChange(String s) {

        if (getListView().isTextFilterEnabled()) {
            // Filtering is enabled

            //
            //
            // Start List filtering Thread
            //

//        final ArrayAdapter listViewCursorAdapter = (ArrayAdapter) getListView().getAdapter();
            final Filterable listViewCursorAdapter = (Filterable) getListView().getAdapter();

            if (TextUtils.isEmpty(s)) {


                // Force filtering with null string to get the full account list

                listViewCursorAdapter.getFilter()
                                     .filter(null,
                                             this);

            } else {

                // Perform filtering

                // Do not use this, because it makes a big black square appears when typing text
//            getListView().setFilterText(s);

                // instead, use this
                listViewCursorAdapter.getFilter()
                                     .filter(s,
                                             this);
            }

        } else {
            // Filtering is enabled n' pas

            // RAF
        }

        //
        // Call Search Text Change Listener
        //

        if (_onSearchTextChangedListener != null) {

            // Call Listener
            _onSearchTextChangedListener.onSearchTextChanged(s);
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {

        _searchTextEditView.clearFocus();

        return true;
    }

    @Override
    public void onFilterComplete(final int count) {

        if (count > 0) {
            // There are filtered items

            mBaseAdapter.notifyDataSetChanged();

        } else {
            // There is none filtered items

            mBaseAdapter.notifyDataSetInvalidated();
        }
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


    @Override
    public void onCancel(DialogInterface dialog) {

        dismissDialog();

        if (_onCancelListener != null) {
            // There is a listener

            // Call listener
            _onCancelListener.onCancel(dialog);

        } else {
            // There is no listener

            // RAF
        }
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

    //
    // local methods
    //

    private void configureView(View searchableListRootView) {

        mIsDismissing = false;

        //
        // Search Edit text
        //

        _searchTextEditView = (SearchView) searchableListRootView.findViewById(R.id.search);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        _searchTextEditView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
//        _searchTextEditView.setIconifiedByDefault(false); // Already done in xml
        _searchTextEditView.setOnQueryTextListener(this);
        _searchTextEditView.setOnCloseListener(this);

        // Get Preference about opening keyboard, default to false
        boolean prefShallOpenKeyboard = PreferenceManager.getDefaultSharedPreferences(getActivity())
                                                         .getBoolean(getString(R.string.key_shall_open_keyboard_in_account_searchable_spinner),
                                                                     false);

        if (prefShallOpenKeyboard) {
            // Want to open keyboard

            // Set Focus on searchTextEditView to open cursor
            _searchTextEditView.setFocusable(true);
            _searchTextEditView.requestFocus();

        } else {
            // Do not want to open keyboard

            // Clear Focus
            _searchTextEditView.clearFocus();

            //
            // Hide keyboard after 500ms to let keyboard appeared before hiding it
            //

            KeyboardUtils.hideKeyboard(_searchTextEditView,
                                       500);
        }

        //
        // Items list
        //

        setListView((ListView) searchableListRootView.findViewById(R.id.listItems));

        // Clear items
        getAllItems().clear();

        final SpinnerAdapter parentSpinnerAdapter = getParentSearchableSpinnerView().getAdapter();

        if (QualifiedAccountNameCursorAdapter.class.isAssignableFrom(parentSpinnerAdapter.getClass())) {
            // The parentSpinnerAdapter is a QualifiedAccountNameCursorAdapter

            QualifiedAccountNameCursorAdapter parentCursorAdapter = (QualifiedAccountNameCursorAdapter) parentSpinnerAdapter;

            //
            // Put temporarily DropDownItemLayout in selectedItemView,
            // because ListView use only selectedItemView for list item
            //

//        parentCursorAdapter.setViewResource(parentCursorAdapter.getSpinnerDropDownItemLayout());

            setAllItems(new ArrayList<HashMap<String, String>>());
            HashMap<String, String> dataSet;

            // Create items from DB Cursor
            for (int i = 0; i < parentCursorAdapter.getCount(); i++) {

                dataSet = new HashMap<String, String>();

                Cursor cursorOnRow = (Cursor) parentCursorAdapter.getItem(i);

                final String accountUID = cursorOnRow.getString(cursorOnRow.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_UID));
                dataSet.put(KEY_ACCOUNT_UID,
                            accountUID);

                final String accountSimpleName = cursorOnRow.getString(cursorOnRow.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_NAME));
                dataSet.put(KEY_ACCOUNT_SIMPLE_NAME,
                            accountSimpleName);

                final String parentAccountFullName = cursorOnRow.getString(cursorOnRow.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_FULL_NAME));
                dataSet.put(KEY_PARENT_ACCOUNT_FULL_NAME,
                            parentAccountFullName);

                final String accountFullName = cursorOnRow.getString(cursorOnRow.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_FULL_NAME));
                dataSet.put(KEY_ACCOUNT_FULL_NAME,
                            accountFullName);

                getAllItems().add(dataSet);

            } // for

            // Create an ArrayAdapter for items, with filtering capablity based on item containing a text
//            mBaseAdapter = new WithContainingTextArrayFilterArrayAdapter(getActivity(),
////                                                                                                       parentCursorAdapter.getSpinnerDropDownItemLayout(),
//                                                                         android.R.layout.simple_list_item_1,
//                                                                         getAllItems());

            mBaseAdapter = new SimpleAdapter(getActivity(),
//                                                                                                       parentCursorAdapter.getSpinnerDropDownItemLayout(),
                                             getAllItems(),
                                             // Layout englobant de chaque item
                                             R.layout.account_spinner_dropdown_item_2lines,
                                             // Keys
                                             new String[]{KEY_ACCOUNT_SIMPLE_NAME,
                                                          KEY_PARENT_ACCOUNT_FULL_NAME,
                                                          KEY_ACCOUNT_FULL_NAME},
                                             // Layout de chaque TextView englobé
                                             new int[]{R.id.text2,
                                                       R.id.text3,
                                                       android.R.id.text1}) {

                protected Filter mFilter = null;

                @Override
                public Filter getFilter() {

                    if (mFilter == null) {
                        mFilter = new ItemToStringContainsTextFilter(this,
                                                                     getAllItems());
                    }
                    return mFilter;
                }
            };

            // TODO TW C 2020-02-25 : A supprimer
        } else if (ArrayAdapter.class.isAssignableFrom(parentSpinnerAdapter.getClass())) {
            // The parentSpinnerAdapter is an ArrayAdapter

            ArrayAdapter parentArrayAdapter = (ArrayAdapter) parentSpinnerAdapter;

            // Create items from ArrayAdapter's items
            for (int i = 0; i < parentArrayAdapter.getCount(); i++) {

                getAllItems().add(parentArrayAdapter.getItem(i));

            } // for

            // Create an ArrayAdapter for items, with filtering capablity based on item containing a text
            mBaseAdapter = new WithContainingTextArrayFilterArrayAdapter(getActivity(),
//                                                                                                       parentCursorAdapter.getSpinnerDropDownItemLayout(),
                                                                         android.R.layout.simple_list_item_1,
                                                                         getAllItems());

        } else {
            // The parentSpinnerAdapter is another Adapter

            mBaseAdapter = null;

            Log.e(LOG_TAG,
                  "parentSpinnerAdapter is neither QualifiedAccountNameCursorAdapter nor ArrayAdapter");
        }

        if (mBaseAdapter != null) {

//        //
//        // Set a filter that rebuild Cursor by running a new query based on a LIKE criteria
//        // with or without Placeholder accounts
//        //
//
//        parentCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
//
//            public Cursor runQuery(CharSequence constraint) {
//
//                //
//                // Add %constraint% at the end of the whereArgs
//                //
//
//                // Convert WhereArgs into List
//                final String[] cursorWhereArgs = getParentSearchableSpinnerView().getCursorWhereArgs();
//                final List<String> whereArgsAsList = (cursorWhereArgs != null)
//                                                     ? new ArrayList<String>(Arrays.asList(cursorWhereArgs))
//                                                     : new ArrayList<String>();
//
//                // Add the %constraint% for the LIKE added in the where clause
//                whereArgsAsList.add("%" + ((constraint != null)
//                                           ? constraint.toString()
//                                           : "") + "%");
//
//                // Convert List into WhereArgs
//                final String[] whereArgs = whereArgsAsList.toArray(new String[whereArgsAsList.size()]);
//
//
//                //
//                // Run the original query but constrained with full account name containing constraint
//                //
//
//                final AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
//
//                final String where = getParentSearchableSpinnerView().getCursorWhere()
//                                     + " AND "
//                                     + DatabaseSchema.AccountEntry.COLUMN_FULL_NAME
//                                     + " LIKE ?";
//
//                final Cursor accountsCursor = accountsDbAdapter.fetchAccountsOrderedByFavoriteAndFullName(where,
//                                                                                                          whereArgs);
//
//                return accountsCursor;
//            }
//        });

            //
            // Register a Listener to close dialog if there is only one item remaining in the filtered list, and select it
            // automatically
            //

            mBaseAdapter.registerDataSetObserver(new DataSetObserver() {

                @Override
                public void onChanged() {

//                final String accountUID = (String) mBaseAdapter.getItem(position);
//                final Cursor filteredAccountsCursor = parentCursorAdapter.getCursor();

                    if (getAllItems().size() == 1) {
                        // only one account

                        String accountUID="";

//                    filteredAccountsCursor.moveToFirst();

//                    final String accountUID = filteredAccountsCursor.getString(filteredAccountsCursor.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_UID));
                        final Object itemAsObject = getAllItems().get(0);

                        if (itemAsObject instanceof Map) {
                            //

                            HashMap<String, String> item = (HashMap<String, String>) itemAsObject;

                            accountUID = item.get(KEY_ACCOUNT_UID);

                        } else if (itemAsObject instanceof String) {

                            accountUID = (String) itemAsObject;

                        } else {
                            //  n' pas

                            // RAF
                        }

                        dismissDialog();

                        // Simulate a onSearchableItemClicked
                        _onSearchableItemClickedListener.onSearchableItemClicked(accountUID);

                    } else {
                        // only one account n' pas

                        // RAF
                    }

                }
            });

            // On item click listener
            getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent,
                                        View view,
                                        int position,
                                        long id) {

//                final String accountFullName = (String) mBaseAdapter.getItem(position);
                    final String accountUID = (String) mBaseAdapter.getItem(position);

                    dismissDialog();

                    // Call Listener
                    _onSearchableItemClickedListener.onSearchableItemClicked(accountUID);
                }
            });

            //
            // Attach the adapter to the list
            //

            getListView().

                                 setAdapter(mBaseAdapter);

            //
            // Define Filter
            //

            // Enable filtering based on search text field
            getListView().

                                 setTextFilterEnabled(true);
        }

        // Simulate an empty search text field to build the full accounts list
        onQueryTextChange(null);
    }

    protected void dismissDialog() {

        if (!mIsDismissing) {
            // It is the first time dismissing has been requested

            // Avoid infinite looping
            mIsDismissing = true;

            //
            // Restore original Spinner Selected Item Layout
            //

            if (QualifiedAccountNameCursorAdapter.class.isAssignableFrom(getParentSearchableSpinnerView().getAdapter()
                                                                                                         .getClass())) {
                // The Adapter is a CursorAdapter

                QualifiedAccountNameCursorAdapter parentCursorAdapter = (QualifiedAccountNameCursorAdapter) getParentSearchableSpinnerView().getAdapter();

                parentCursorAdapter.setViewResource(parentCursorAdapter.getSpinnerSelectedItemLayout());

                // Refresh spinner selected item using spinner selected item layout
                parentCursorAdapter.notifyDataSetChanged();

            } else {
                // The Adapter is not a CursorAdapter

                // NTD
            }

            //
            // Hide keyboard
            //

            KeyboardUtils.hideKeyboard(_searchTextEditView);

            //
            // Close Dialog
            //

            getDialog().dismiss();

        } else {
            // dismissing has already been requested

            // RAF
        }

    }

    //
    // Getters / Setters
    //

    protected SearchableSpinnerView getParentSearchableSpinnerView() {

        return _parentSearchableSpinnerView;
    }

    protected void setParentSearchableSpinnerView(SearchableSpinnerView parentSearchableSpinnerView) {

        _parentSearchableSpinnerView = parentSearchableSpinnerView;
    }

    protected ListView getListView() {

        return _listView;
    }

    protected void setListView(final ListView listView) {

        this._listView = listView;
    }

    protected List getAllItems() {

        return _allItems;
    }

    protected void setAllItems(final List allItems) {

        this._allItems = allItems;
    }

    protected void setTitle(String strTitle) {

        _strTitle = strTitle;
    }

    protected void setPositiveButtonText(String strPositiveButtonText) {

        _strPositiveButtonText = strPositiveButtonText;
    }

    protected void setPositiveButtonClickListener(DialogInterface.OnClickListener onClickListener) {

        _onPositiveBtnClickListener = onClickListener;
    }

    protected void setOnSearchableItemClickListener(OnSearchableItemClickedListener<String> onSearchableItemClickedListener) {

        this._onSearchableItemClickedListener = onSearchableItemClickedListener;
    }

    protected void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {

        this._onCancelListener = onCancelListener;
    }

    protected void setOnSearchTextChangedListener(OnSearchTextChangedListener onSearchTextChangedListener) {

        this._onSearchTextChangedListener = onSearchTextChangedListener;
    }


}
