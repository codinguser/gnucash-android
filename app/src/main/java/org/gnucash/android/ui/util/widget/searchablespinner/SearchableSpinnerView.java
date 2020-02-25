package org.gnucash.android.ui.util.widget.searchablespinner;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

public class SearchableSpinnerView
        extends android.support.v7.widget.AppCompatSpinner
        implements View.OnTouchListener,
                   SearchableListDialogFragment.OnSearchableItemClickedListener<String> {

    /**
     * Logging tag
     */
    protected static final String LOG_TAG = "SearchableSpinnerView";

    // Clause WHERE du Cursor (en vue de pouvoir la rejouer pour la filtrer
    private String   mCursorWhere;
    private String[] mCursorWhereArgs;

    // Embedded DialogFragment
    private SearchableListDialogFragment _searchableListDialogFragment;

    public SearchableSpinnerView(Context context) {

        super(context);

        init();
    }

    public SearchableSpinnerView(Context context,
                                 AttributeSet attrs) {

        super(context,
              attrs);

        // TODO TW C 2020-02-16 : A supprimer ?
//        //
//        // Retrieve attribute value
//        //
//
//        TypedArray a = context.obtainStyledAttributes(attrs,
//                                                      R.styleable.SearchableSpinnerView);
//
//        final int N = a.getIndexCount();
//
//        for (int i = 0; i < N; ++i) {
//
//            int attr = a.getIndex(i);
//
//            if (attr == R.styleable.SearchableSpinnerView_hintText) {
//
//                _strHintText = a.getString(attr);
//            }
//        }
//
//        a.recycle();

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

        boolean handled = false;

        if (_searchableListDialogFragment != null) {
            // There is a DialogFragment defined

            handled = true;

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

        } else {
            // There is no DialogFragment defined

        }

        return handled;
    }

    private Activity scanForActivity(Context context) {

        if (context == null) {
            return null;

        } else if (context instanceof Activity) {
            return (Activity) context;

        } else if (context instanceof ContextWrapper) {
            return scanForActivity(((ContextWrapper) context).getBaseContext());
        }

        return null;
    }

    @Override
    public void onSearchableItemClicked(String item) {

        if (CursorAdapter.class.isAssignableFrom(getAdapter().getClass())) {
            // The Adapter is a CursorAdapter

            final Cursor cursor = ((CursorAdapter) getAdapter()).getCursor();

            selectSpinnerAccount(cursor,
                                 item,
                                 this);

            // TODO TW C 2020-02-25 : A enlever ?
        } else if (getAdapter() instanceof ArrayAdapter) {
            // The Adapter is a ListAdapter

            setSelection(((ArrayAdapter) getAdapter()).getPosition(item));

        } else {

            // NTD
        }
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
            String accountFullName = accountsCursor.getString(accountsCursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_FULL_NAME));

            // TODO TW C 2020-02-20 : Remettre les UID
//            if (accountUID.equals(uid)) {
            if (accountUID.equals(accountFullName)) {
                // Found

                Log.d(LOG_TAG,
                      "Account found in current Cursor for ("
                      + accountUID
                      + ") => ("
                      + accountFullName
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
        setAdapter(adapter);

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

        if (QualifiedAccountNameCursorAdapter.class.isAssignableFrom(getAdapter().getClass())) {
            // The SpinnerAdapter is a QualifiedAccountNameCursorAdapter

            // NTD

        } else {
            // The SpinnerAdapter is not a QualifiedAccountNameCursorAdapter

            // Remove DialogFragment
            _searchableListDialogFragment = null;
        }

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

    public void setPositiveButton(String strPositiveButtonText,
                                  DialogInterface.OnClickListener onPositiveBtnClickListener) {

        _searchableListDialogFragment.setPositiveButtonText(strPositiveButtonText);

        _searchableListDialogFragment.setPositiveButtonClickListener(onPositiveBtnClickListener);
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

//    @Override
//    public int getSelectedItemPosition() {
//
//        if (!TextUtils.isEmpty(_strHintText) && !_isDirty) {
//            return NO_ITEM_SELECTED;
//        } else {
//            // TODO TW M 2020-02-16 : Est-ce que ça devrait aller le chercher dans la SearchableListDialogFragment._listView ?
//            return super.getSelectedItemPosition();
////            return _searchableListDialogFragment._listView.getSelectedItemPosition();
//        }
//    }

//    @Override
//    public Object getSelectedItem() {
//
//        if (!TextUtils.isEmpty(_strHintText) && !_isDirty) {
//            return null;
//        } else {
//            // TODO TW M 2020-02-16 : Est-ce que ça devrait aller le chercher dans la SearchableListDialogFragment._listView ?
//            return super.getSelectedItem();
////            return _searchableListDialogFragment._listView.getSelectedItem();
//        }
//    }

}
