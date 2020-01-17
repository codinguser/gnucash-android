package org.gnucash.android.ui.util.widget.searchablespinner;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import org.gnucash.android.R;

import java.io.Serializable;
import java.util.List;

public class SearchableListDialog
        extends DialogFragment
        implements SearchView.OnQueryTextListener,
                   SearchView.OnCloseListener {

    private static final String ITEMS = "items";

    private ArrayAdapter listAdapter;

    private ListView _listViewItems;

    private SearchableItem _searchableItem;

    private OnSearchTextChanged _onSearchTextChanged;

    private SearchView _searchView;

    private String _strTitle;

    private String _strPositiveButtonText;

    private DialogInterface.OnClickListener _onClickListener;

    public SearchableListDialog() {

    }

    public static SearchableListDialog newInstance(List items) {

        SearchableListDialog multiSelectExpandableFragment = new SearchableListDialog();

        Bundle args = new Bundle();
        args.putSerializable(ITEMS,
                             (Serializable) items);

        multiSelectExpandableFragment.setArguments(args);

        return multiSelectExpandableFragment;
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
            _searchableItem = (SearchableItem) savedInstanceState.getSerializable("item");
        }
        // Change End

        View rootView = inflater.inflate(R.layout.searchable_list_dialog,
                                         null);

        setData(rootView);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(rootView);

        String strPositiveButton = _strPositiveButtonText == null
                                   ? "CLOSE"
                                   : _strPositiveButtonText;
        alertDialogBuilder.setPositiveButton(strPositiveButton,
                                             _onClickListener);

        String strTitle = _strTitle == null
                          ? "Select Item"
                          : _strTitle;
        alertDialogBuilder.setTitle(strTitle);

        final AlertDialog dialog = alertDialogBuilder.create();
//        dialog.getWindow()
//              .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        hideKeyboard(_searchView);
        return dialog;
    }

    // Crash on orientation change #7
    // Change Start
    // Description: Saving the instance of searchable item instance.
    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putSerializable("item",
                                 _searchableItem);
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
        _onClickListener = onClickListener;
    }

    public void setOnSearchableItemClickListener(SearchableItem searchableItem) {

        this._searchableItem = searchableItem;
    }

    public void setOnSearchTextChangedListener(OnSearchTextChanged onSearchTextChanged) {

        this._onSearchTextChanged = onSearchTextChanged;
    }

    private void setData(View rootView) {

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        _searchView = (SearchView) rootView.findViewById(R.id.search);
        _searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        _searchView.setIconifiedByDefault(false);
        _searchView.setOnQueryTextListener(this);
        _searchView.setOnCloseListener(this);
        _searchView.clearFocus();

        // Hide Soft Keybord
//        InputMethodManager keyboard = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//        keyboard.hideSoftInputFromWindow(_searchView.getWindowToken(),
//                                         0);

        List items = (List) getArguments().getSerializable(ITEMS);

        _listViewItems = (ListView) rootView.findViewById(R.id.listItems);

        //create the adapter by passing your ArrayList data
//        listAdapter = new ArrayAdapter(getActivity(),
//                                       android.R.layout.simple_list_item_1,
//                                       items);
        listAdapter = new ArrayAdapterWithContainsFilter(getActivity(),
                                                         android.R.layout.simple_list_item_1,
                                                         items);
        //attach the adapter to the list
        _listViewItems.setAdapter(listAdapter);

        _listViewItems.setTextFilterEnabled(true);

        _listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,
                                    View view,
                                    int position,
                                    long id) {

                _searchableItem.onSearchableItemClicked(listAdapter.getItem(position),
                                                        position);
                getDialog().dismiss();
            }
        });
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

        _searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {

        if (TextUtils.isEmpty(s)) {

//            ((ArrayAdapter) _listViewItems.getAdapter()).getFilter()
//                                                        .filter(null);
            ((ArrayAdapterWithContainsFilter) _listViewItems.getAdapter()).filter(null);
        } else {
//            ((ArrayAdapter) _listViewItems.getAdapter()).getFilter()
//                                                        .filter(s);
            ((ArrayAdapterWithContainsFilter) _listViewItems.getAdapter()).filter(s);
        }

        if (null != _onSearchTextChanged) {
            _onSearchTextChanged.onSearchTextChanged(s);
        }
        return true;
    }

    public interface SearchableItem<T>
            extends Serializable {
        void onSearchableItemClicked(T item,
                                     int position);
    }

    public interface OnSearchTextChanged {
        void onSearchTextChanged(String strText);
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

//    /**
//     * <p>An array filter constrains the content of the array adapter with
//     * items containing a text.</p>
//     */
//    private class ArrayTextFilter
//            extends Filter {
//
//        @Override
//        protected FilterResults performFiltering(CharSequence prefix) {
//
//            final FilterResults results = new FilterResults();
//
//            if (mOriginalValues == null) {
//                synchronized (mLock) {
//                    mOriginalValues = new ArrayList<>(mObjects);
//                }
//            }
//
//            if (prefix == null || prefix.length() == 0) {
//                final ArrayList<T> list;
//                synchronized (mLock) {
//                    list = new ArrayList<>(mOriginalValues);
//                }
//                results.values = list;
//                results.count = list.size();
//            } else {
//                final String prefixString = prefix.toString()
//                                                  .toLowerCase();
//
//                final ArrayList<T> values;
//                synchronized (mLock) {
//                    values = new ArrayList<>(mOriginalValues);
//                }
//
//                final int          count     = values.size();
//                final ArrayList<T> newValues = new ArrayList<>();
//
//                for (int i = 0; i < count; i++) {
//                    final T      value     = values.get(i);
//                    final String valueText = value.toString()
//                                                  .toLowerCase();
//
//                    // First match against the whole, non-splitted value
//                    if (valueText.startsWith(prefixString)) {
//                        newValues.add(value);
//                    } else {
//                        final String[] words = valueText.split(" ");
//                        for (String word : words) {
//                            if (word.startsWith(prefixString)) {
//                                newValues.add(value);
//                                break;
//                            }
//                        }
//                    }
//                }
//
//                results.values = newValues;
//                results.count = newValues.size();
//            }
//
//            return results;
//        }
//
//        @Override
//        protected void publishResults(CharSequence constraint,
//                                      FilterResults results) {
//            //noinspection unchecked
//            mObjects = (List<T>) results.values;
//            if (results.count > 0) {
//                notifyDataSetChanged();
//            } else {
//                notifyDataSetInvalidated();
//            }
//        }
//    }
}
