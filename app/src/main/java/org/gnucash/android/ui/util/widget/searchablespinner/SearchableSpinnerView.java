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

import java.util.HashMap;

public class SearchableSpinnerView<T_ITEM>
        extends android.support.v7.widget.AppCompatSpinner
        implements View.OnTouchListener,
                   SearchableListDialogFragment.OnSearchableItemClickedListener<T_ITEM> {

    /**
     * Logging tag
     */
    protected static final String LOG_TAG = "SearchableSpinnerView";

    // Embedded DialogFragment
    private SearchableListDialogFragment<T_ITEM> _searchableListDialogFragment;

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
        _searchableListDialogFragment.setOnSearchableListItemClickListener(this);

        // S'abonner aux évènements onTouch
        setOnTouchListener(this);
    }

    @Override
    public void setAdapter(SpinnerAdapter adapter) {

        // Use given adapter for spinner item (not drop down)
        super.setAdapter(adapter);

        // TODO TW C 2020-02-26 : A supprimer ?
        if (QualifiedAccountNameCursorAdapter.class.isAssignableFrom(getAdapter().getClass())) {
            // The SpinnerAdapter is a QualifiedAccountNameCursorAdapter

            // NTD

        } else {
            // The SpinnerAdapter is not a QualifiedAccountNameCursorAdapter

            // Remove DialogFragment
            // TODO TW C 2020-02-26 : A remettre ?
//            _searchableListDialogFragment = null;
        }

    }


    //
    // Listeners
    //

    @Override
    public boolean onTouch(View v,
                           MotionEvent event) {

        boolean handled = false;

        // TODO TW C 2020-02-26 : A enlever si j'ai réussi à génériciser même pour Pie Chart
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

        // TODO TW C 2020-02-26 : A supprimer ?
//        // TODO TW C 2020-02-26 : Il faut chercher dans la liste du spinner (pas dans la ListView du Dialog) l'item fourni en
//        //  paramètre. Pas besoin de passer le Cursor, car l'Adapter sait naviguer dans ses items, peu importe d'où ils viennent
//        // parcourir les items, regarder si ça matche. En déduire la position, puis la sélectionner
//
//        int itemsCount = getAdapter().getCount();
//
//        for (int position = 0; position < itemsCount; position++) {
//
//            T_ITEM itemAtPosition = (T_ITEM) getAdapter().getItem(position);
//
//            String accountUIDAtPosition = cursor.getString(cursor.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_UID));
//
//            if (item.equals() == itemAtPosition) {
//                //
//
//                //
//                setSelection(position);
//
//                break;
//
//            } else {
//                //  n' pas
//
//                // RAF
//            }
//
//        }

        if (CursorAdapter.class.isAssignableFrom(getAdapter().getClass())) {
            // The Adapter is a CursorAdapter

//            final Cursor cursor = ((CursorAdapter) getAdapter()).getCursor();
            final Cursor cursor = (Cursor) item;

            String accountUID = cursor.getString(cursor.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_UID));

            selectSpinnerAccount(cursor,
                                 accountUID,
                                 this);

        } else if (getAdapter() instanceof ArrayAdapter) {
            // The Adapter is a ListAdapter

            setSelection(((ArrayAdapter) getAdapter()).getPosition(item));

        } else {

            // TODO TW C 2020-02-26 : Logguer une erreur
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
