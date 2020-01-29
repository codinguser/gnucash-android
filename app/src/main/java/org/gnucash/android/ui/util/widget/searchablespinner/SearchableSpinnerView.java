package org.gnucash.android.ui.util.widget.searchablespinner;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SpinnerAdapter;

import org.gnucash.android.R;

import java.util.ArrayList;
import java.util.List;

public class SearchableSpinnerView
        extends android.support.v7.widget.AppCompatSpinner
        implements View.OnTouchListener,
                   SearchableListDialogFragment.OnSearchableItemClickedListener {

    public static final int                  NO_ITEM_SELECTED = -1;

    // TODO TW C 2020-01-17 : a remplacer par getContext()
    private             Context              _context;

    private             List                 _items;
    private             List                 _allItems;

    private SearchableListDialogFragment _searchableListDialogFragment;

    private boolean       _isDirty;

//    // Adpater for Spinner based on data in a DB Cursor
//    private CursorAdapter _cursorAdapter;

    private String        _strHintText;
    private boolean       _isFromInit;

    public SearchableSpinnerView(Context context) {

        super(context);
        this._context = context;
        init();
    }

    public SearchableSpinnerView(Context context,
                                 AttributeSet attrs) {

        super(context,
              attrs);

        this._context = context;

        TypedArray a = context.obtainStyledAttributes(attrs,
                                                      R.styleable.SearchableSpinnerView);
        final int N = a.getIndexCount();
        for (int i = 0; i < N; ++i) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.SearchableSpinnerView_hintText) {
                _strHintText = a.getString(attr);
            }
        }
        a.recycle();
        init();
    }

    public SearchableSpinnerView(Context context,
                                 AttributeSet attrs,
                                 int defStyleAttr) {

        super(context,
              attrs,
              defStyleAttr);

        this._context = context;

        init();
    }

    private void init() {

        _allItems=new ArrayList();
        _items = new ArrayList();

        // Create Dialog instance
        // TODO TW C 2020-01-25 : Supprimer _items
        _searchableListDialogFragment = SearchableListDialogFragment.makeInstance(this, _items);

        // S'abonner aux clicks sur un item
        _searchableListDialogFragment.setOnSearchableItemClickListener(this);

        // S'abonner aux évènements onTouch
        setOnTouchListener(this);

//        _cursorAdapter = (CursorAdapter) getAdapter();

        // TODO TW C 2020-01-17 : Supprimer la partie ArrayAdapter
//        if (!TextUtils.isEmpty(_strHintText)) {
//            ArrayAdapter arrayAdapter = new ArrayAdapter(_context,
//                                                         android.R.layout.simple_list_item_1,
//                                                         new String[]{_strHintText});
//            _isFromInit = true;
//            setAdapter(arrayAdapter);
//        }
    }

    @Override
    public boolean onTouch(View v,
                           MotionEvent event) {

        if (_searchableListDialogFragment.isAdded()) {
            // dialog is already visible

            // NTD

        } else {
            // dialog is not visible

            if (event.getAction() == MotionEvent.ACTION_UP) {
                // User has just clicked on the spinner

//                if (null != _cursorAdapter) {
//
//                    //
//                    // Open Search & List Dialog
//                    //
//
//                    // Refresh content #6
//                    // Change Start
//                    // Description: The items were only set initially, not reloading the data in the
//                    // spinner every time it is loaded with items in the adapter.
//                    _items.clear();
//                    _allItems.clear();
//
//                    // Create items from DB Cursor
//                    for (int i = 0; i < _cursorAdapter.getCount(); i++) {
//
//                        Cursor cursorOnRow = (Cursor) _cursorAdapter.getItem(i);
//
//                        final String accountFullName = cursorOnRow.getString(cursorOnRow.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_FULL_NAME));
//
//                        // TODO TW C 2020-01-17 : Ajouter l'étoile pour les Favoris
//
//                        _items.add(accountFullName);
//
//                    } // for
//
//                    // Create a copy of the items
//                    _allItems.addAll(_items);

                    // Display SearchableListDialogFragment
                    _searchableListDialogFragment.show(scanForActivity(_context).getFragmentManager(),
                                                       "TAG");
//                }
            }
        }

        return true;
    }

    @Override
    public void onSearchableItemClicked(Object item,
                                        int position) {

//        String accountFullName = (String) item;

//        setSelection(_items.indexOf(item));
//        setSelection(_allItems.indexOf(item));
        setSelection(position);

//        if (!_isDirty) {
//            _isDirty = true;
//            setAdapter(_cursorAdapter);
////            setSelection(_items.indexOf(item));
//            setSelection(_allItems.indexOf(item));
//        }
    }


    @Override
    public void setAdapter(SpinnerAdapter adapter) {

        super.setAdapter(adapter);

//        _cursorAdapter = (CursorAdapter) adapter;
//        _searchableListDialogFragment.setCursorAdapter((CursorAdapter) adapter);

//        if (!_isFromInit) {
//
//            // TODO TW C 2020-01-17 : Supprimer la partie ArrayAdapter
//            if (!TextUtils.isEmpty(_strHintText) && !_isDirty) {
//                //
//
//                //
//                ArrayAdapter arrayAdapter = new ArrayAdapter(_context,
//                                                             android.R.layout.simple_list_item_1,
//                                                             new String[]{_strHintText});
//                super.setAdapter(arrayAdapter);
//
//            } else {
//                super.setAdapter(adapter);
//            }
//
//        } else {
//            _isFromInit = false;
//            super.setAdapter(adapter);
//        }
    }

    public void setTitle(String strTitle) {

        _searchableListDialogFragment.setTitle(strTitle);
    }

    public void setPositiveButton(String strPositiveButtonText) {

        _searchableListDialogFragment.setPositiveButton(strPositiveButtonText);
    }

    public void setPositiveButton(String strPositiveButtonText,
                                  DialogInterface.OnClickListener onPositiveBtnClickListener) {

        _searchableListDialogFragment.setPositiveButton(strPositiveButtonText,
                                                        onPositiveBtnClickListener);
    }

    public void setOnSearchTextChangedListener(SearchableListDialogFragment.OnSearchTextChangedListener onSearchTextChangedListener) {

        _searchableListDialogFragment.setOnSearchTextChangedListener(onSearchTextChangedListener);
    }

    private Activity scanForActivity(Context cont) {

        if (cont == null) {
            return null;
        } else if (cont instanceof Activity) {
            return (Activity) cont;
        } else if (cont instanceof ContextWrapper) {
            return scanForActivity(((ContextWrapper) cont).getBaseContext());
        }

        return null;
    }

    @Override
    public int getSelectedItemPosition() {

        if (!TextUtils.isEmpty(_strHintText) && !_isDirty) {
            return NO_ITEM_SELECTED;
        } else {
            return super.getSelectedItemPosition();
        }
    }

    @Override
    public Object getSelectedItem() {

        if (!TextUtils.isEmpty(_strHintText) && !_isDirty) {
            return null;
        } else {
            return super.getSelectedItem();
        }
    }

    private void showKeyboard(final EditText ettext) {

        ettext.requestFocus();
        ettext.postDelayed(new Runnable() {
                               @Override
                               public void run() {

                                   InputMethodManager keyboard =
                                           (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                                   keyboard.showSoftInput(ettext,
                                                          0);
                               }
                           },
                           200);
    }

}
