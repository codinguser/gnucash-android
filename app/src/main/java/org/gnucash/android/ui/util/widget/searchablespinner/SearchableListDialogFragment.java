package org.gnucash.android.ui.util.widget.searchablespinner;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;

import org.gnucash.android.R;
import org.gnucash.android.util.KeyboardUtils;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.io.Serializable;

/**
 * Pop-up that display a ListView with a search text field
 *
 * @author JeanGarf
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
    private String mStrTitle;

    // Search Edit text zone
    private SearchView mSearchTextEditView;

    // Item list
    ListView mListView;

    // Bottom right button to close the pop-up
    private String mStrPositiveButtonText;

    //
    // ListView Adapter
    //

    // Adapter for the mListView
    private BaseAdapter mListViewAdapter;

    //
    // Listeners
    //

    private OnSearchTextChangedListener mOnSearchTextChangedListener;

    private OnSearchableItemClickedListener<T_ITEM> mOnSearchableListItemClickedListener;

    private DialogInterface.OnClickListener mOnPositiveBtnClickListener;

    private DialogInterface.OnCancelListener mOnCancelListener;

    // true if dismiss dialog has already been requested
    private boolean mIsDismissing;


    /**
     * Constructor
     */
    public SearchableListDialogFragment() {

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

        String strTitle = mStrTitle == null
                          ? "Select Item"
                          : mStrTitle;

        alertDialogBuilder.setTitle(strTitle);

        // Positive Button

        String strPositiveButton = mStrPositiveButtonText == null
                                   ? "CLOSE"
                                   : mStrPositiveButtonText;

        alertDialogBuilder.setPositiveButton(strPositiveButton,
                                             new DialogInterface.OnClickListener() {

                                                 @Override
                                                 public void onClick(final DialogInterface dialog,
                                                                     final int which) {

                                                     // Dismiss dialog
                                                     dismissDialog();

                                                     if (mOnPositiveBtnClickListener != null) {
                                                         //

                                                         // Call listener
                                                         mOnPositiveBtnClickListener.onClick(dialog,
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
                // getListView().setFilterText(s);

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

        if (mOnSearchTextChangedListener != null) {

            // Call Listener
            mOnSearchTextChangedListener.onSearchTextChanged(s);
        }

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {

        mSearchTextEditView.clearFocus();

        return true;
    }

    @Override
    public void onFilterComplete(final int count) {

        if (count > 0) {
            // There are filtered items

            getListViewAdapter().notifyDataSetChanged();

        } else {
            // There is none filtered items

            getListViewAdapter().notifyDataSetInvalidated();
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

        if (mOnCancelListener != null) {
            // There is a listener

            // Call listener
            mOnCancelListener.onCancel(dialog);

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

        mSearchTextEditView = (SearchView) searchableListRootView.findViewById(R.id.search);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        mSearchTextEditView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        // mSearchTextEditView.setIconifiedByDefault(false); // Already done in xml
        mSearchTextEditView.setOnQueryTextListener(this);
        mSearchTextEditView.setOnCloseListener(this);

        // Get Preference about opening keyboard, default to false
        boolean prefShallOpenKeyboard = PreferenceManager.getDefaultSharedPreferences(getActivity())
                                                         .getBoolean(getString(R.string.key_shall_open_keyboard_in_account_searchable_spinner),
                                                                     false);

        if (prefShallOpenKeyboard) {
            // Want to open keyboard

            // Set Focus on searchTextEditView to open cursor
            mSearchTextEditView.setFocusable(true);
            mSearchTextEditView.requestFocus();

        } else {
            // Do not want to open keyboard

            // Clear Focus
            mSearchTextEditView.clearFocus();

            //
            // Hide keyboard after 500ms to let keyboard appeared before hiding it
            //

            KeyboardUtils.hideKeyboard(mSearchTextEditView,
                                       500);
        }

        //
        // Items list
        //

        setListView((ListView) searchableListRootView.findViewById(R.id.listItems));

        // Use the parent spinner view adapter for the list view
        setListViewAdapter((BaseAdapter) getParentSearchableSpinnerView().getAdapter());

        if (getListViewAdapter() != null) {

            if (QualifiedAccountNameCursorAdapter.class.isAssignableFrom(getListViewAdapter().getClass())) {
                // The parentSpinnerAdapter is a QualifiedAccountNameCursorAdapter

                QualifiedAccountNameCursorAdapter parentCursorAdapter = (QualifiedAccountNameCursorAdapter) getListViewAdapter();

                //
                // Put temporarily DropDownItemLayout in selectedItemView,
                // because ListView use only selectedItemView for list item
                //

                parentCursorAdapter.setViewResource(parentCursorAdapter.getSpinnerDropDownItemLayout());

            } else {
                // The parentSpinnerAdapter is another kind of Adapter

                // NTD
            }

//            //
//            // Register a Listener to close dialog if there is only one item remaining in the filtered list, and select it
//            // automatically
//            //
//
//            getListViewAdapter().registerDataSetObserver(new DataSetObserver() {
//
//                @Override
//                public void onChanged() {
//
//                    if (getListViewAdapter().getCount() == 1) {
//                        // only one account
//
//                        dismissDialog();
//
//                        final T_ITEM item = (T_ITEM) getListViewAdapter().getItem(0);
//
//                        // Simulate a onSearchableListItemClicked
//                        getOnSearchableListItemClickedListener().onSearchableListItemClicked(item);
//
//                    } else {
//                        // only one account n' pas
//
//                        // RAF
//                    }
//
//                }
//            });

            // On item click listener
            getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent,
                                        View view,
                                        int position,
                                        long id) {

                    dismissDialog();

                    final T_ITEM item = (T_ITEM) getListViewAdapter().getItem(position);

                    // Call Listener
                    getOnSearchableListItemClickedListener().onSearchableListItemClicked(item);
                }
            });

            //
            // Attach the adapter to the list
            //

            getListView().setAdapter(getListViewAdapter());

            // Enable filtering based on search text field
            getListView().setTextFilterEnabled(true);
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

            KeyboardUtils.hideKeyboard(mSearchTextEditView);

            //
            // Close Dialog
            //

            getDialog().dismiss();

        } else {
            // Dismissing has already been requested

            // NTD
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

        return mListView;
    }

    protected void setListView(final ListView listView) {

        this.mListView = listView;
    }

    protected BaseAdapter getListViewAdapter() {

        return mListViewAdapter;
    }

    protected void setListViewAdapter(final BaseAdapter listViewAdapter) {

        mListViewAdapter = listViewAdapter;
    }

    protected void setTitle(String strTitle) {

        mStrTitle = strTitle;
    }

    protected void setPositiveButtonText(String strPositiveButtonText) {

        mStrPositiveButtonText = strPositiveButtonText;
    }

    protected void setPositiveButtonClickListener(DialogInterface.OnClickListener onClickListener) {

        mOnPositiveBtnClickListener = onClickListener;
    }

    protected void setOnSearchableListItemClickListener(OnSearchableItemClickedListener<T_ITEM> onSearchableListItemClickedListener) {

        this.mOnSearchableListItemClickedListener = onSearchableListItemClickedListener;
    }

    protected OnSearchableItemClickedListener<T_ITEM> getOnSearchableListItemClickedListener() {

        return mOnSearchableListItemClickedListener;
    }

    protected void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {

        this.mOnCancelListener = onCancelListener;
    }

    protected void setOnSearchTextChangedListener(OnSearchTextChangedListener onSearchTextChangedListener) {

        this.mOnSearchTextChangedListener = onSearchTextChangedListener;
    }


}
