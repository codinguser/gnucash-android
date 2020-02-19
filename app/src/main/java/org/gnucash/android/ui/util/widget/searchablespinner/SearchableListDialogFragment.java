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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.util.KeyboardUtils;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Pop-up that display a ListView with a search text field
 */
public class SearchableListDialogFragment
        extends DialogFragment
        implements SearchView.OnQueryTextListener,
                   SearchView.OnCloseListener {

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

    private List<String> _allItems;

    private ListAdapter mWithContainingTextArrayFilterArrayAdapter;

    private OnSearchTextChangedListener _onSearchTextChangedListener;

    private OnSearchableItemClickedListener<String> _onSearchableItemClickedListener;

    private DialogInterface.OnClickListener _onPositiveBtnClickListener;

    private DialogInterface.OnCancelListener _onCancelListener;

    // Parent SpinnerView
    private SearchableSpinnerView _parentSpinnerView;

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
    public static SearchableListDialogFragment makeInstance(SearchableSpinnerView parentAdapterView) {

        SearchableListDialogFragment searchableListDialogFragment = new SearchableListDialogFragment();

        // Store a link to the Parent SearchableSpinnerView which holds the CursorAdapter
        searchableListDialogFragment.setParentAdapterView(parentAdapterView);

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

        // Hide Keyboard
//        hideKeyboard();
//        getDialog().getWindow()
//                   .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

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

    private void configureView(View searchableListRootView) {

        mIsDismissing=false;

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        //
        // Search Edit text zone
        //

        _searchTextEditView = (SearchView) searchableListRootView.findViewById(R.id.search);

        _searchTextEditView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
//        _searchTextEditView.setIconifiedByDefault(false); // Already done in xml
        _searchTextEditView.setOnQueryTextListener(this);
        _searchTextEditView.setOnCloseListener(this);

        // Get Preference about double back button press to exit
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

        // TODO TW C 2020-02-19 : A remplacer par une copie de la liste d'item plutôt qu'une désérialisation
//        List items = (List) getArguments().getSerializable(ITEMS);

        //
        // Put temporarily DropDownItemLayout in selectedItemView,
        // because ListView use only selectedItemView for list item
        //

        QualifiedAccountNameCursorAdapter parentCursorAdapter = (QualifiedAccountNameCursorAdapter) getParentSpinnerView().getAdapter();

//        parentCursorAdapter.setViewResource(parentCursorAdapter.getSpinnerDropDownItemLayout());

        if (parentCursorAdapter != null) {

            //
            // Open Search & List Dialog
            //

            // Refresh content #6
            // Change Start
            // Description: The items were only set initially, not reloading the data in the
            // spinner every time it is loaded with items in the adapter.
            getAllItems().clear();

            // Create items from DB Cursor
            for (int i = 0; i < parentCursorAdapter.getCount(); i++) {

                Cursor cursorOnRow = (Cursor) parentCursorAdapter.getItem(i);

                final String accountFullName = cursorOnRow.getString(cursorOnRow.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_FULL_NAME));

                getAllItems().add(accountFullName);

            } // for

            // Create an ArrayAdapter for items, with filtering capablity based on item containing a text
            mWithContainingTextArrayFilterArrayAdapter = new WithContainingTextArrayFilterArrayAdapter(getActivity(),
//                                                                                                       parentCursorAdapter.getSpinnerDropDownItemLayout(),
                                                                                                       android.R.layout.simple_list_item_1,
                                                                                                       getAllItems());

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
//                final String[] cursorWhereArgs = getParentSpinnerView().getCursorWhereArgs();
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
//                final String where = getParentSpinnerView().getCursorWhere()
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

            mWithContainingTextArrayFilterArrayAdapter.registerDataSetObserver(new DataSetObserver() {

                @Override
                public void onChanged() {

//                final String accountUID = (String) mWithContainingTextArrayFilterArrayAdapter.getItem(position);
//                final Cursor filteredAccountsCursor = parentCursorAdapter.getCursor();

                    if (getAllItems().size() == 1) {
                        // only one account

//                    filteredAccountsCursor.moveToFirst();

//                    final String accountUID = filteredAccountsCursor.getString(filteredAccountsCursor.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_UID));
                        final String accountUID = (String) getAllItems().get(0);

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

//                final String accountFullName = (String) mWithContainingTextArrayFilterArrayAdapter.getItem(position);
                    final String accountUID = (String) mWithContainingTextArrayFilterArrayAdapter.getItem(position);

                    dismissDialog();

                    // Call Listener
                    _onSearchableItemClickedListener.onSearchableItemClicked(accountUID);
                }
            });

            //
            // Attach the adapter to the list
            //

            getListView().setAdapter((ListAdapter) mWithContainingTextArrayFilterArrayAdapter);

            // Do not use this, because it makes a big black square appears when typing text
//        // Enable filtering based on search text field
//        getListView().setTextFilterEnabled(true);
        }

        // Simulate an empty search text field to build the full accounts list
        onQueryTextChange(null);
    }

    @Override
    public boolean onQueryTextChange(String s) {

        //
        // Start List filtering Thread
        //

        final ArrayAdapter listViewCursorAdapter = (WithContainingTextArrayFilterArrayAdapter) getListView().getAdapter();

        if (TextUtils.isEmpty(s)) {


            // Force filtering with null string to get the full account list

            listViewCursorAdapter.getFilter()
                                 .filter(null);

        } else {

            // Perform filtering

            // Do not use this, because it makes a big black square appears when typing text
//            getListView().setFilterText(s);

            // instead, use this
            listViewCursorAdapter.getFilter()
                                 .filter(s);
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

    protected void dismissDialog() {

        if (!mIsDismissing) {
            // It is the first time dismissing has been requested

            // Avoid infinite looping
            mIsDismissing = true;

            //
            // Restore original Spinner Selected Item Layout
            //

            QualifiedAccountNameCursorAdapter parentCursorAdapter = (QualifiedAccountNameCursorAdapter) getParentSpinnerView().getAdapter();

            parentCursorAdapter.setViewResource(parentCursorAdapter.getSpinnerSelectedItemLayout());

            // Refresh spinner selected item using spinner selected item layout
            parentCursorAdapter.notifyDataSetChanged();

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

    public void setPositiveButtonText(String strPositiveButtonText) {

        _strPositiveButtonText = strPositiveButtonText;
    }

    public void setPositiveButtonClickListener(DialogInterface.OnClickListener onClickListener) {

        _onPositiveBtnClickListener = onClickListener;
    }

    public void setOnSearchableItemClickListener(OnSearchableItemClickedListener<String> onSearchableItemClickedListener) {

        this._onSearchableItemClickedListener = onSearchableItemClickedListener;
    }

    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {

        this._onCancelListener = onCancelListener;
    }

    public void setOnSearchTextChangedListener(OnSearchTextChangedListener onSearchTextChangedListener) {

        this._onSearchTextChangedListener = onSearchTextChangedListener;
    }


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

    public SearchableSpinnerView getParentSpinnerView() {

        return _parentSpinnerView;
    }

    public void setParentAdapterView(SearchableSpinnerView parentAdapterView) {

        _parentSpinnerView = parentAdapterView;
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

}
