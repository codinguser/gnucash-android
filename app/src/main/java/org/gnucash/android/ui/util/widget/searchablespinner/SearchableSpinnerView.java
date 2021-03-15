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

/**
 * Spinner that open a dialog box with text search criteria
 * to filter the item list
 *
 * @param <T_ITEM>
 *     Type of an item
 *
 *  * @author JeanGarf
 */
public class SearchableSpinnerView<T_ITEM>
        extends android.support.v7.widget.AppCompatSpinner
        implements View.OnTouchListener,
                   SearchableListDialogFragment.OnSearchableItemClickedListener<T_ITEM> {

    /**
     * Logging tag
     */
    protected static final String LOG_TAG = "SearchableSpinnerView";

    // Embedded DialogFragment
    private SearchableListDialogFragment<T_ITEM> mSearchableListDialogFragment;

    public SearchableSpinnerView(Context context) {

        super(context);

        init();
    }

    public SearchableSpinnerView(Context context,
                                 AttributeSet attrs) {

        super(context,
              attrs);

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
        setSearchableListDialogFragment(SearchableListDialogFragment.makeInstance(this));

        // S'abonner aux clicks sur un item
        getSearchableListDialogFragment().setOnSearchableListItemClickListener(this);

        // S'abonner aux évènements onTouch
        setOnTouchListener(this);
    }

    //
    // Listeners
    //

    @Override
    public boolean onTouch(View v,
                           MotionEvent event) {

        boolean handled = false;

        if (getSearchableListDialogFragment() != null) {
            // There is a DialogFragment defined

            handled = true;

            if (getSearchableListDialogFragment().isAdded()) {
                // dialog is already visible

                // NTD

            } else {
                // dialog is not visible

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // User has just clicked on the spinner

                    // Display SearchableListDialogFragment
                    getSearchableListDialogFragment().show(scanForActivity(getContext()).getFragmentManager(),
                                                       "LOG_TAG");
                }
            }

        } else {
            // There is no DialogFragment defined

            // NTD
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
    public void onSearchableListItemClicked(T_ITEM item) {

        if (CursorAdapter.class.isAssignableFrom(getAdapter().getClass())) {
            // The Adapter is a CursorAdapter

            final Cursor cursor = (Cursor) item;

            String accountUID = cursor.getString(cursor.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_UID));

            selectSpinnerAccount(cursor,
                                 accountUID,
                                 this);

        } else if (getAdapter() instanceof ArrayAdapter) {
            // The Adapter is a ListAdapter

            setSelection(((ArrayAdapter) getAdapter()).getPosition(item));

        } else {

            throw new IllegalArgumentException("SearchableSpinnerView can only handle ArrayAdapter and CursorAdapter");
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

            if (accountUID.equals(uid)) {
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

    //
    // Getters/Setters
    //


    protected SearchableListDialogFragment<T_ITEM> getSearchableListDialogFragment() {

        return mSearchableListDialogFragment;
    }

    protected void setSearchableListDialogFragment(final SearchableListDialogFragment<T_ITEM> searchableListDialogFragment) {

        mSearchableListDialogFragment = searchableListDialogFragment;
    }

    public void setTitle(String strTitle) {

        getSearchableListDialogFragment().setTitle(strTitle);
    }

    public void setPositiveButton(String strPositiveButtonText,
                                  DialogInterface.OnClickListener onPositiveBtnClickListener) {

        getSearchableListDialogFragment().setPositiveButtonText(strPositiveButtonText);

        getSearchableListDialogFragment().setPositiveButtonClickListener(onPositiveBtnClickListener);
    }


    public void setOnSearchTextChangedListener(SearchableListDialogFragment.OnSearchTextChangedListener onSearchTextChangedListener) {

        getSearchableListDialogFragment().setOnSearchTextChangedListener(onSearchTextChangedListener);
    }

    /**
     * Register a callback to be invoked when an item in this AdapterView has
     * been selected.
     *
     * @param listener The callback that will run
     */
    public void setOnCancelListener(DialogInterface.OnCancelListener listener) {

        getSearchableListDialogFragment().setOnCancelListener(listener);
    }

}
