package org.gnucash.android.ui.util.widget.searchablespinner;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SpinnerAdapter;

import org.gnucash.android.R;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import static org.gnucash.android.ui.transaction.TransactionsActivity.selectSpinnerAccount;

public class SearchableSpinnerView
        extends android.support.v7.widget.AppCompatSpinner
        implements View.OnTouchListener,
                   SearchableListDialogFragment.OnSearchableItemClickedListener {

    public static final int                  NO_ITEM_SELECTED = -1;

    private boolean _allowPlaceHolderAccounts;

    private SearchableListDialogFragment _searchableListDialogFragment;

    private boolean       _isDirty;

    private String                           _strHintText;

    public SearchableSpinnerView(Context context) {

        super(context);

        init();
    }

    public SearchableSpinnerView(Context context,
                                 AttributeSet attrs) {

        super(context,
              attrs);

        //
        // Retrieve attribute value
        //

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

        //
        // Init
        //

        init();
    }

    public SearchableSpinnerView(Context context,
                                 AttributeSet attrs,
                                 int defStyleAttr) {

        super(context,
              attrs,
              defStyleAttr);

        init();
    }

    private void init() {

        // Create Dialog instance
        _searchableListDialogFragment = SearchableListDialogFragment.makeInstance(this);

        // S'abonner aux clicks sur un item
        _searchableListDialogFragment.setOnSearchableItemClickListener(this);

        // By default, don't allow PlaceHolderAccounts in spinners
        setAllowPlaceHolderAccounts(false);

        // S'abonner aux évènements onTouch
        setOnTouchListener(this);
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

                // Display SearchableListDialogFragment
                _searchableListDialogFragment.show(scanForActivity(getContext()).getFragmentManager(),
                                                       "LOG_TAG");
            }
        }

        return true;
    }

    @Override
    public void onSearchableItemClicked(Object accountUID,
                                        int position) {

        final Cursor cursor = ((QualifiedAccountNameCursorAdapter) getAdapter()).getCursor();

        selectSpinnerAccount(cursor,
                             (String) accountUID,
                             this);

//        setSelection(position);
    }


    @Override
    public void setAdapter(SpinnerAdapter adapter) {

        // Use given adapter for spinner item (not drop down)
        super.setAdapter(adapter);
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

    /**
     * Register a callback to be invoked when an item in this AdapterView has
     * been selected.
     *
     * @param listener The callback that will run
     */
    public void setOnCancelListener(DialogInterface.OnCancelListener listener) {

        _searchableListDialogFragment.setOnCancelListener(listener);
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

    public void setAllowPlaceHolderAccounts(boolean allowPlaceHolderAccounts) {

        _allowPlaceHolderAccounts = allowPlaceHolderAccounts;
    }

    public boolean isAllowPlaceHolderAccounts() {

        return _allowPlaceHolderAccounts;
    }
}
