package org.gnucash.android.ui.util.widget.searchablespinner;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

// TODO TW C 2020-02-13 : A renommer SearchableCursorSpinnerView
public class SearchableSpinnerView
        extends android.support.v7.widget.AppCompatSpinner
        implements View.OnTouchListener,
                   SearchableListDialogFragment.OnSearchableItemClickedListener<String> {

    public static final int                  NO_ITEM_SELECTED = -1;

    /**
     * Logging tag
     */
    protected static final String LOG_TAG = "SearchableSpinnerView";

    // Clause WHERE du Cursor (en vue de pouvoir la rejouer pour la filtrer
    private String   mCursorWhere;
    private String[] mCursorWhereArgs;

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
    public void onSearchableItemClicked(String itemAccountUID) {

        final Cursor cursor = ((QualifiedAccountNameCursorAdapter) getAdapter()).getCursor();

        selectSpinnerAccount(cursor,
                             itemAccountUID,
                             this);
    }

    /**
     *
     * @param accountsCursor
     * @param accountUID
     * @param spinnerView
     */
    public static void selectSpinnerAccount(Cursor accountsCursor,
                                            final String accountUID,
                                            final Spinner spinnerView) {

        //
        // set the selected item in the spinner
        //

        int     spinnerSelectedPosition = 0;
        boolean found                   = false;

        for (accountsCursor.moveToFirst(); !accountsCursor.isAfterLast(); accountsCursor.moveToNext()) {

            String uid  = accountsCursor.getString(accountsCursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_UID));
            String name = accountsCursor.getString(accountsCursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_FULL_NAME));

            if (accountUID.equals(uid)) {
                // Found

                Log.d(LOG_TAG,
                      "Account found in current Cursor for ("
                      + accountUID
                      + ") => ("
                      + name
                      + "), position ("
                      + spinnerSelectedPosition
                      + ")");

                // Set Spinner selection
                spinnerView.setSelection(spinnerSelectedPosition);

                found = true;
                break;
            }

            ++spinnerSelectedPosition;

        } // for

        if (found) {
            // Account has found

            // NTD

        } else {
            // Account has not been found

            // Log message
            Log.e(LOG_TAG,
                  "No Account found in current Cursor for (" + accountUID + ")");
        }
    }

    /**
     * Set the SpinnerAdapter and store the where clause
     * in order to be able to filter by running the where clause
     * completed with constraint on the account full name
     *
     * @param adapter
     * @param cursorWhere
     * @param cursorWhereArgs
     */
    public void setAdapter(SpinnerAdapter adapter,
                           String cursorWhere,
                           String[] cursorWhereArgs) {

        // Use given adapter for spinner item (not drop down)
        super.setAdapter(adapter);

        // Store the WHERE clause associated with the Cursor
        setCursorWhere(cursorWhere);
        setCursorWhereArgs(cursorWhereArgs);
    }

    /**
     * DO NOT USE this method.
     *
     * Use the above one
     *
     * @param adapter
     */
    @Override
    @Deprecated
    public void setAdapter(SpinnerAdapter adapter) {

        // Use given adapter for spinner item (not drop down)
        super.setAdapter(adapter);
    }

    String getCursorWhere() {

        return mCursorWhere;
    }

    protected void setCursorWhere(final String cursorWhere) {

        mCursorWhere = cursorWhere;
    }

    String[] getCursorWhereArgs() {

        return mCursorWhereArgs;
    }

    protected void setCursorWhereArgs(final String[] cursorWhereArgs) {

        mCursorWhereArgs = cursorWhereArgs;
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

}
