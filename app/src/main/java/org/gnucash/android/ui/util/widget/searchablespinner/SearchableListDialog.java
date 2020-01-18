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

    private ArrayAdapter withContaingTextArrayFilterArrayAdapter;

    private ListView _listView;

    private OnSearchTextChangedListener _onSearchTextChangedListener;

    private OnSearchableItemClickedListener _onSearchableItemClickedListener;

    private DialogInterface.OnClickListener _onPositiveBtnClickListener;

    private SearchView _searchView;

    private String _strTitle;

    private String _strPositiveButtonText;


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
            _onSearchableItemClickedListener = (OnSearchableItemClickedListener) savedInstanceState.getSerializable("item");
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
                                             _onPositiveBtnClickListener);

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

        _listView = (ListView) rootView.findViewById(R.id.listItems);

        // Create an ArrayAdapter for items, with filtering capablity based on item containing a text
        withContaingTextArrayFilterArrayAdapter = new WithContaingTextArrayFilterArrayAdapter(getActivity(),
                                                                                              android.R.layout.simple_list_item_1,
                                                                                              items);
        //attach the adapter to the list
        _listView.setAdapter(withContaingTextArrayFilterArrayAdapter);

        _listView.setTextFilterEnabled(true);

        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent,
                                    View view,
                                    int position,
                                    long id) {

                final Object accountFullName = withContaingTextArrayFilterArrayAdapter.getItem(position);

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

        _searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {

        if (TextUtils.isEmpty(s)) {

            ((WithContaingTextArrayFilterArrayAdapter) _listView.getAdapter()).getFilter()
                                                                              .filter(null);

        } else {

            ((WithContaingTextArrayFilterArrayAdapter) _listView.getAdapter()).getFilter()
                                                                              .filter(s);
        }

        if (null != _onSearchTextChangedListener) {

            // Call Listener
            _onSearchTextChangedListener.onSearchTextChanged(s);
        }

        return true;
    }

    public interface OnSearchableItemClickedListener<T>
            extends Serializable {

        void onSearchableItemClicked(T item,
                                     int position);
    }

    public interface OnSearchTextChangedListener {
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

}
