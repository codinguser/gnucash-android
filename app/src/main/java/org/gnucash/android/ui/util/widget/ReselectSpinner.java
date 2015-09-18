package org.gnucash.android.ui.util.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

/**
 * Spinner which fires OnItemSelectedListener even when an item is reselected.
 * Normal Spinners only fire item selected notifications when the selected item changes.
 * <p>This is used in {@code ReportsActivity} for the time range</p>
 */
public class ReselectSpinner extends Spinner {
    public ReselectSpinner(Context context) {
        super(context);
    }

    public ReselectSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReselectSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setSelection(int position) {
        boolean sameSelected = getSelectedItemPosition() == position;
        super.setSelection(position);
        if (position == 5 && sameSelected){
            getOnItemSelectedListener().onItemSelected(this, getSelectedView(), position, getSelectedItemId());
        }
        super.setSelection(position);
    }
}
