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
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
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
public class SearchableListDialogFragment<T_ITEM>
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
    public static final String KEY_IS_FAVORITE_ACCOUNT      = "key_isFavoriteAccount";

    /**
     * Listener to call when user clicks on an item
     *
     * @param <T_ITEM>
     *      item Type
     */
    public interface OnSearchableItemClickedListener<T_ITEM>
            extends Serializable {

        void onSearchableListItemClicked(T_ITEM item);
    }

    /**
     * Listener to call when Search text change
     */
    public interface OnSearchTextChangedListener {
        void onSearchTextChanged(String strText);
    }

    //
    // Parent SearchableSpinnerView
    //

    private SearchableSpinnerView _parentSearchableSpinnerView;

    //
    // Dialog
    //

    // Dialog Title
    private String _strTitle;

    // Search Edit text zone
    private SearchView _searchTextEditView;

    // Item list
    ListView _listView;

    // Bottom right button to close the pop-up
    private String _strPositiveButtonText;

    //
    // ListView Adapter
    //

    // items (all or filtered) given to the mListViewAdapter to be displayed in the _listView
    private List<T_ITEM> mItems;

    // TODO TW C 2020-02-26 : A enlever
    // Adapter for the _listView
    private BaseAdapter mListViewAdapter;

    //
    // Listeners
    //

    private OnSearchTextChangedListener _onSearchTextChangedListener;

    private OnSearchableItemClickedListener<T_ITEM> mOnSearchableListItemClickedListener;

    private DialogInterface.OnClickListener _onPositiveBtnClickListener;

    private DialogInterface.OnCancelListener _onCancelListener;


    private boolean mIsDismissing;


    /**
     * Constructor
     */
    public SearchableListDialogFragment() {

        setItems(new ArrayList<T_ITEM>());
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
            setOnSearchableListItemClickListener((OnSearchableItemClickedListener<T_ITEM>) savedInstanceState.getSerializable("item"));
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

            mListViewAdapter.notifyDataSetChanged();

        } else {
            // There is none filtered items

            mListViewAdapter.notifyDataSetInvalidated();
        }
    }

    // Crash on orientation change #7
    // Change Start
    // Description: Saving the instance of searchable item instance.
    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putSerializable("item",
                                 getOnSearchableListItemClickedListener());
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

//        // Clear items
//        getItems().clear();

        // TODO TW C 2020-02-26 : Ajouter un test de cast

        final BaseAdapter parentSpinnerAdapter = (BaseAdapter) getParentSearchableSpinnerView().getAdapter();

        mListViewAdapter = parentSpinnerAdapter;

        if (QualifiedAccountNameCursorAdapter.class.isAssignableFrom(parentSpinnerAdapter.getClass())) {
            // The parentSpinnerAdapter is a QualifiedAccountNameCursorAdapter

            QualifiedAccountNameCursorAdapter parentCursorAdapter = (QualifiedAccountNameCursorAdapter) parentSpinnerAdapter;

            //
            // Put temporarily DropDownItemLayout in selectedItemView,
            // because ListView use only selectedItemView for list item
            //

        parentCursorAdapter.setViewResource(parentCursorAdapter.getSpinnerDropDownItemLayout());
//
//            setItems(new ArrayList<HashMap<String, String>>());
//            HashMap<String, String> item;
//
//            // Create items from DB Cursor
//            for (int i = 0; i < parentCursorAdapter.getCount(); i++) {
//
//                item = new HashMap<String, String>();
//
//                Cursor cursorOnRow = (Cursor) parentCursorAdapter.getItem(i);
//
//                String accountUID = cursorOnRow.getString(cursorOnRow.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_UID));
//                item.put(KEY_ACCOUNT_UID,
//                         accountUID);
//
//                String accountSimpleName = cursorOnRow.getString(cursorOnRow.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_NAME));
//                item.put(KEY_ACCOUNT_SIMPLE_NAME,
//                         accountSimpleName);
//
//                String parentAccountFullName = cursorOnRow.getString(cursorOnRow.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_FULL_NAME));
//                parentAccountFullName = QualifiedAccountNameCursorAdapter.getParentAccountFullName(parentAccountFullName);
//                item.put(KEY_PARENT_ACCOUNT_FULL_NAME,
//                         parentAccountFullName);
//
//                String accountFullName = cursorOnRow.getString(cursorOnRow.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_FULL_NAME));
//                item.put(KEY_ACCOUNT_FULL_NAME,
//                         accountFullName);
//
//                Integer isFavorite = cursorOnRow.getInt(cursorOnRow.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_FAVORITE));
//                item.put(KEY_IS_FAVORITE_ACCOUNT,
//                         isFavorite.toString());
//
//
//                getItems().add(item);
//
//            } // for
//
//            //
//            // Instanciate a customized SimpleAdapter which can :
//            //   1) Filter on Account Full Name
//            //   2) Display items in account_spinner_dropdown_item_2lines
//            //
//            mListViewAdapter = new SimpleAdapter(getActivity(),
//                                                 getItems(),
//                                                 // Layout englobant de chaque item
////                                                 parentCursorAdapter.getSpinnerDropDownItemLayout(),
//                                                 R.layout.account_spinner_dropdown_item_2lines,
//                                                 // Keys
//                                                 new String[]{KEY_ACCOUNT_SIMPLE_NAME,
//                                                          KEY_PARENT_ACCOUNT_FULL_NAME,
//                                                          KEY_ACCOUNT_FULL_NAME},
//                                                 // Layout de chaque TextView englobé
//                                                 new int[]{R.id.text2,
//                                                       R.id.text3,
//                                                           // TODO TW C 2020-02-26 : A supprimer
//                                                       android.R.id.text1}) {
//
//                protected Filter mFilter = null;
//
//                @Override
//                public Filter getFilter() {
//
//                    if (mFilter == null) {
//                        mFilter = new ItemContainingTextFilter<HashMap<String, String>>(getItems()) {
//
//                            /**
//                             * Return true if textToSearch has been found in item
//                             * <p>
//                             * In this implementation, the text is found
//                             * if the lower case text
//                             * is found in the lower cases of the item's account full name
//                             *
//                             * @param textToSearch
//                             * @param item
//                             *
//                             * @return
//                             */
//                            @Override
//                            protected boolean isFoundInItem(final CharSequence textToSearch,
//                                                            final HashMap<String, String> item) {
//
//                                // get the item full name
//                                final String itemTextLowerCase = item.get(KEY_ACCOUNT_FULL_NAME)
//                                                                     .toLowerCase();
//
//                                final String textToSearchLowerCase = textToSearch.toString()
//                                                                                 .toLowerCase();
//
//                                // First match against the whole, non-splitted value
//                                return itemTextLowerCase.contains(textToSearchLowerCase);
//                            }
//
//                        };
//                    }
//                    return mFilter;
//                }
//
//                @Override
//                public View getView(final int position,
//                                    final View convertView,
//                                    final ViewGroup parent) {
//
//                    View view = super.getView(position,
//                                              convertView,
//                                              parent);
//
//                    // Get the item (which is a Map in a SimpleAdapter)
//                    Map<String, String> item = (HashMap<String, String>) getItem(position);
//
//                    String  accountUID = item.get(KEY_ACCOUNT_UID);
//                    Integer isFavorite = Integer.valueOf(item.get(KEY_IS_FAVORITE_ACCOUNT));
//
//                    // Get Account color
//                    int iColor = AccountsDbAdapter.getActiveAccountColorResource(accountUID);
//
//                    TextView simpleAcoountNameTextView = (TextView) view.findViewById(R.id.text2);
//
//                    if (simpleAcoountNameTextView != null) {
//                        //
//
//                        // Override color
//                        simpleAcoountNameTextView.setTextColor(iColor);
//
//                    } else {
//                        //  n' pas
//
//                        // RAF
//                    }
//
//                    //
//                    // Add or not Favorite Star Icon
//                    //
//
//                    QualifiedAccountNameCursorAdapter.displayFavoriteAccountStarIcon(view,
//                                                                                     isFavorite);
//
//                    return view;
//                }
//            };

        } else {
            // The parentSpinnerAdapter is another Adapter

//            mListViewAdapter = null;
//
//            Log.e(LOG_TAG,
//                  "parentSpinnerAdapter is neither QualifiedAccountNameCursorAdapter nor ArrayAdapter");
        }

        if (mListViewAdapter != null) {

            //
            // Register a Listener to close dialog if there is only one item remaining in the filtered list, and select it
            // automatically
            //

            mListViewAdapter.registerDataSetObserver(new DataSetObserver() {

                @Override
                public void onChanged() {

                    if (mListViewAdapter.getCount() == 1) {
                        // only one account

                        dismissDialog();

                        final T_ITEM item = (T_ITEM) mListViewAdapter.getItem(0);

                        // Simulate a onSearchableListItemClicked
                        getOnSearchableListItemClickedListener().onSearchableListItemClicked(item);

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

                    dismissDialog();

                    final T_ITEM item = (T_ITEM) mListViewAdapter.getItem(position);

                    // Call Listener
                    getOnSearchableListItemClickedListener().onSearchableListItemClicked(item);
                }
            });

            //
            // Attach the adapter to the list
            //

            getListView().setAdapter(mListViewAdapter);

            // Enable filtering based on search text field
            getListView().setTextFilterEnabled(true);
        }

        // Simulate an empty search text field to build the full accounts list
        onQueryTextChange(null);
    }

    protected String getAccountUidFromItem(final Object itemAsObject) {

        String accountUID = "";

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
        return accountUID;
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
                // The Adapter is a QualifiedAccountNameCursorAdapter

                QualifiedAccountNameCursorAdapter parentCursorAdapter = (QualifiedAccountNameCursorAdapter) getParentSearchableSpinnerView().getAdapter();

                parentCursorAdapter.setViewResource(parentCursorAdapter.getSpinnerSelectedItemLayout());

                // Refresh spinner selected item using spinner selected item layout
                parentCursorAdapter.notifyDataSetChanged();

            } else {
                // The Adapter is not a QualifiedAccountNameCursorAdapter

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

    protected List getItems() {

        return mItems;
    }

    protected void setItems(final List<T_ITEM> items) {

        this.mItems = items;
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

    protected void setOnSearchableListItemClickListener(OnSearchableItemClickedListener<T_ITEM> onSearchableListItemClickedListener) {

        this.mOnSearchableListItemClickedListener = onSearchableListItemClickedListener;
    }

    protected OnSearchableItemClickedListener<T_ITEM> getOnSearchableListItemClickedListener() {

        return mOnSearchableListItemClickedListener;
    }

    protected void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {

        this._onCancelListener = onCancelListener;
    }

    protected void setOnSearchTextChangedListener(OnSearchTextChangedListener onSearchTextChangedListener) {

        this._onSearchTextChangedListener = onSearchTextChangedListener;
    }


}
